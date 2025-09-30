mod implementation;
mod modeling;

use modeling::{
    Model,
    device::{Device, DeviceRand},
};

use implementation::{ModelProcess, ModelProducer};

fn main() {
    Model::simulate(100.0, 1.0, |model| {
        // Construct processes and devices here
        let proc1 = model.add_process(ModelProcess::new(10));
        let proc12 = model.add_process(ModelProcess::new(10));
        let proc2 = model.add_process(ModelProcess::new(10));

        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(0.005, 0.015)));
        proc12
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(0.005, 0.015)));

        proc2
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(0.006, 0.007)));
        proc2
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(0.006, 0.007)));

        // Set up process chain
        proc1.borrow_mut().add_next(proc2.clone(), 1);
        proc1.borrow_mut().set_if_failure(proc12.clone());

        proc2.borrow_mut().add_next(proc1.clone(), 1); // Loop back to proc1

        // Set up producer
        model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(0.001, 0.005)));
    });
}
