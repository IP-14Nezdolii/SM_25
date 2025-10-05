use crate::modeling::{
    Producer,
    device::DeviceRand,
    process::{Process, ProcessBase, SharedProcess, WeakSharedProcess},
};

pub struct ModelProcess {
    base: ProcessBase,

    next: Vec<(WeakSharedProcess, u16)>,
    if_failure: Option<WeakSharedProcess>,
}

impl ModelProcess {
    pub fn new(queue_capacity: usize) -> Self {
        ModelProcess {
            base: ProcessBase::new(queue_capacity),
            next: Vec::new(),
            if_failure: None,
        }
    }
}

impl Process for ModelProcess {

    fn get_work_time(&self) -> Option<f64> {
        self.base.get_work_time()
    }

    fn add_next(&mut self, next: SharedProcess, priority: u16) {
        assert!(
            self.get_id() != next.get_process_id(),
            "A process cannot point to itself as next"
        );

        for (proc, _) in self.next.iter() {
            if let Some(proc) = proc.upgrade() {
                if proc.get_process_id() == next.get_process_id() {
                    return;
                }
            }
        }
        self.next.push((next.downgrade(), priority));
    }

    fn set_if_failure(&mut self, proc: SharedProcess) {
        assert!(
            self.get_id() != proc.get_process_id(),
            "A process cannot point to itself as failed"
        );

        self.if_failure = Some(proc.downgrade());
    }

    fn measure_stats(&mut self) {
        self.base.measure_stats();
    }

    fn get_next(&self) -> Option<SharedProcess> {
        match self.next.len() {
            0 => None,
            1 => self.next.first().unwrap().0.upgrade(),
            _ => {
                let weights: Vec<u32> = self
                    .next
                    .iter()
                    .map(|(_, priority)| *priority as u32)
                    .collect();

                let total: u32 = weights.iter().sum();
                let mut r: u32 = rand::random_range(0..total);

                for (i, &w) in weights.iter().enumerate() {
                    if r < w {
                        match self.next[i].0.upgrade() {
                            Some(proc) => return Some(proc),
                            None => panic!("Deleted process!"),
                        }
                    }
                    r -= w;
                }

                panic!("Wrong algorithm!");
            }
        }
    }

    fn get_if_failure(&self) -> Option<SharedProcess> {
        match &self.if_failure {
            Some(wp) => match wp.upgrade() {
                Some(sp) => return Some(sp),
                None => return None,
            },
            None => return None,
        }
    }

    fn get_mut_base(&mut self) -> &mut ProcessBase {
        &mut self.base
    }

    fn get_base(&self) -> &ProcessBase {
        &self.base
    }

    fn print_stats(&self) {
        self.base.print_stats();
    }

    fn get_all_next(&self) -> Vec<SharedProcess> {
        self.next
            .iter()
            .filter_map(|(weak_proc, _)| weak_proc.upgrade())
            .collect()
    }

    fn get_id(&self) -> usize {
        self.get_base().get_id()
    }

    fn to_shared(self) -> SharedProcess
    where
        Self: Sized + 'static,
    {
        SharedProcess::new(Box::new(self))
    }

    fn add_device(&mut self, device: crate::modeling::device::Device) {
        self.get_mut_base().add_device(device);
    }
}

pub struct ModelProducer {
    next: SharedProcess,
    rand: DeviceRand,

    current_time: f64,
    required_time: f64,

    produced: usize,
}

impl ModelProducer {
    pub fn new(concumer: SharedProcess, rand: DeviceRand) -> Self {
        let required_time = rand.next_rand();

        ModelProducer {
            next: concumer,
            rand: rand,
            current_time: 0.0,
            required_time: required_time,
            produced: 0,
        }
    }
}

impl Producer for ModelProducer {
    fn run(&mut self, time: f64) {
        self.current_time += time;

        while self.current_time >= self.required_time {
            self.next.borrow_mut().process();

            self.current_time -= self.required_time;
            self.required_time = self.rand.next_rand();

            self.produced += 1;
        }
    }

    fn get_next(&self) -> SharedProcess {
        self.next.clone()
    }

    fn get_time(&self) -> f64 {
        self.required_time - self.current_time
    }

    fn get_produced(&self) -> usize {
        self.produced
    }
}

#[cfg(test)]
mod tests {
    use crate::modeling::{Model, device::Device};

    use super::*;

    #[test]
    fn simple_test() {
        for i in 2..=10 {
            for j in 2..=10 {
                for k in 0..=20 {
                    Model::simulate(10000.0, j as f64, |model| {
                        // Construct processes and devices here
                        let proc1 = model.add_process(ModelProcess::new(k));

                        proc1
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));

                        // Set up producer
                        model
                            .set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
                    });
                }
            }
        }
    }

    #[test]
    fn next_test() {
        for i in 2..=10 {
            for j in 2..=10 {
                for k in 0..=20 {
                    Model::simulate(10000.0, j as f64, |model| {
                        // Construct processes and devices here
                        let proc1 = model.add_process(ModelProcess::new(k));
                        let proc2 = model.add_process(ModelProcess::new(k));
                        let proc3 = model.add_process(ModelProcess::new(k));

                        proc1
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));
                        proc2
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));
                        proc3
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));

                        proc1.borrow_mut().add_next(proc2, 2);
                        proc1.borrow_mut().add_next(proc3, 1);
                        // Set up producer
                        model
                            .set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
                    });
                }
            }
        }
    }

    #[test]
    fn failure_test1() {
        for i in 2..=10 {
            for j in 2..=10 {
                for k in 0..=20 {
                    Model::simulate(10000.0, j as f64, |model| {
                        // Construct processes and devices here
                        let proc1 = model.add_process(ModelProcess::new(k));
                        let proc2 = model.add_process(ModelProcess::new(k));
                        let proc3 = model.add_process(ModelProcess::new(k));

                        proc1
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));
                        proc2
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(100.0, 1000.0)));
                        proc3
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));

                        proc1.borrow_mut().add_next(proc2.clone(), 1);
                        proc2.borrow_mut().add_next(proc3.clone(), 1);

                        proc1.borrow_mut().set_if_failure(proc2.clone());
                        proc2.borrow_mut().set_if_failure(proc3.clone());
                        proc3.borrow_mut().set_if_failure(proc1.clone());
                        // Set up producer
                        model
                            .set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
                    });
                }
            }
        }
    }

    #[test]
    fn failure_test2() {
        Model::simulate(10000.0, 100.0, |model| {
            // Construct processes and devices here
            let proc1 = model.add_process(ModelProcess::new(2));
            let proc2 = model.add_process(ModelProcess::new(2));
            let proc3 = model.add_process(ModelProcess::new(2));
            let proc4 = model.add_process(ModelProcess::new(2));
            let proc5 = model.add_process(ModelProcess::new(2));
            let proc6 = model.add_process(ModelProcess::new(2));

            proc1
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));
            proc2
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));
            proc3
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));
            proc4
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));
            proc5
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));
            proc6
                .borrow_mut()
                .add_device(Device::new(DeviceRand::Uniform(10.0, 100.0)));

            proc1.borrow_mut().add_next(proc2.clone(), 1);

            proc2.borrow_mut().set_if_failure(proc1.clone());
            proc1.borrow_mut().set_if_failure(proc3.clone());
            proc3.borrow_mut().set_if_failure(proc4.clone());
            proc4.borrow_mut().set_if_failure(proc5.clone());
            proc5.borrow_mut().set_if_failure(proc6.clone());
            proc6.borrow_mut().set_if_failure(proc1.clone());
            // Set up producer
            model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(1.0, 2.0)));
        });
    }

    #[test]
    #[should_panic]
    fn self_next_test() {
        for i in 2..=10 {
            for j in 2..=10 {
                for k in 0..=20 {
                    Model::simulate(1000.0, j as f64, |model| {
                        // Construct processes and devices here
                        let proc1 = model.add_process(ModelProcess::new(k));

                        proc1
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));

                        // Set up process chain
                        proc1.borrow_mut().add_next(proc1.clone(), 1);

                        // Set up producer
                        model
                            .set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
                    });
                }
            }
        }
    }

    #[test]
    #[should_panic]
    fn self_failure_test() {
        for i in 2..=10 {
            for j in 2..=10 {
                for k in 0..=20 {
                    Model::simulate(1000.0, j as f64, |model| {
                        // Construct processes and devices here
                        let proc1 = model.add_process(ModelProcess::new(k));

                        proc1
                            .borrow_mut()
                            .add_device(Device::new(DeviceRand::Uniform(1.0, i as f64)));

                        // Set up process chain
                        proc1.borrow_mut().set_if_failure(proc1.clone());

                        // Set up producer
                        model
                            .set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
                    });
                }
            }
        }
    }
}
