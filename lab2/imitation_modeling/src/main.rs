mod modeling;

use modeling::{
    Model, Producer,
    device::{Device, DeviceRand},
    process::{Process, ProcessBase, SharedProcess, WeakSharedProcess},
};

fn main() {
    Model::simulate(100.0, 1.0, |model| {
        let mut proc1 = ModelProcess::new(10);
        let mut proc2 = ModelProcess::new(10);

        proc1.add_device(Device::new(DeviceRand::Uniform(0.005, 0.015)));
        proc2.add_device(Device::new(DeviceRand::Uniform(0.006, 0.007)));

        proc1.set_next(proc2.to_shared());

        model.set_producer(ModelProducer::new(
            proc1.to_shared(),
            DeviceRand::Uniform(0.001, 0.005),
        ));
    });
}

pub struct ModelProcess {
    base: ProcessBase,

    next: Option<SharedProcess>,
    if_failure: Option<WeakSharedProcess>,
}

impl ModelProcess {
    pub fn new(queue_capacity: i32) -> Self {
        ModelProcess {
            base: ProcessBase::new(queue_capacity),
            next: None,
            if_failure: None,
        }
    }
}

impl Process for ModelProcess {
    fn get_work_time(&self) -> Option<f64> {
        self.base.get_work_time()
    }

    fn process(&mut self) -> bool {
        self.base.process()
    }

    fn set_next(&mut self, next: SharedProcess) {
        self.next = Some(next);
    }

    fn set_if_failure(&mut self, proc: SharedProcess) {
        self.if_failure = Some(proc.downgrade());
    }

    fn measure_stats(&mut self) {
        self.base.measure_stats();
    }

    fn print_stats(&self) -> () {
        self.base.print_stats();
    }

    fn get_next(&self) -> Option<SharedProcess> {
        self.next.clone()
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

    fn get_base(&mut self) -> &mut ProcessBase {
        &mut self.base
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

    fn get_next(&self) -> Option<SharedProcess> {
        Some(self.next.clone())
    }

    fn get_time(&self) -> f64 {
        self.required_time - self.current_time
    }
}
