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

    fn process(&mut self) -> bool {
        if self.base.process() == false {
            if let Some(failure_proc) = self.get_if_failure() {
                return failure_proc.borrow_mut().process();
            }

            return false;
        }

        return true;
    }

    fn add_next(&mut self, next: SharedProcess, priority: u16) {
        assert!(
            self.get_id() != next.borrow().get_id(),
            "A process cannot point to itself as next"
        );

        for (proc, _) in self.next.iter() {
            if let Some(proc) = proc.upgrade() {
                if proc.borrow().get_id() == next.borrow().get_id() {
                    return;
                }
            }
        }
        self.next.push((next.downgrade(), priority));
    }

    fn set_if_failure(&mut self, proc: SharedProcess) {
        assert!(
            self.get_id() != proc.borrow().get_id(),
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

    fn run(&mut self, time: f64) {
        let processed = self.get_mut_base().run(time);

        for _ in 0..processed {
            if let Some(next) = self.get_next() {
                next.borrow_mut().process();
            }
        }
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
}

impl ModelProducer {
    pub fn new(concumer: SharedProcess, rand: DeviceRand) -> Self {
        let required_time = rand.next_rand();

        ModelProducer {
            next: concumer,
            rand: rand,
            current_time: 0.0,
            required_time: required_time,
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
        }
    }

    fn get_next(&self) -> SharedProcess {
        self.next.clone()
    }

    fn get_time(&self) -> f64 {
        self.required_time - self.current_time
    }
}
