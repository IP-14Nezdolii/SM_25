mod implementation;
mod modeling;

use modeling::{
    Model,
    device::{Device, DeviceRand},
};

use implementation::{ModelProcess, ModelProducer};

fn main() {
    println!("------Task12------");
    task12();

    println!("------Task34------");
    task34(5, 1. , 6.);
    task34(5, 1. , 5.);
    task34(10, 1. , 5.);
    task34(10, 1. , 4.);

    println!("------Task5------");
    task5();

    println!("------Task6------");
    task6();
}

fn task12() {
    Model::simulate(1000.0, 10.0, |model| {
        // Construct processes and devices here
        let proc1 = model.add_process(ModelProcess::new(10));

        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));

        // Set up producer
        model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
    });
}

fn task34(
    q_len: usize, 
    min_work_time: f64, 
    max_work_time: f64
) {
    Model::simulate(1000.0, 10.0, |model| {
        // Construct processes and devices here
        let proc1 = model.add_process(ModelProcess::new(q_len));
        let proc2 = model.add_process(ModelProcess::new(q_len));
        let proc3 = model.add_process(ModelProcess::new(q_len));

        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(min_work_time, max_work_time)));
        proc2
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(min_work_time, max_work_time)));
        proc3
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(min_work_time, max_work_time)));

        // Set up process chain
        proc1.borrow_mut().add_next(proc2.clone(), 1);
        proc2.borrow_mut().add_next(proc3.clone(), 1);

        // Set up producer
        model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
    });
}

fn task5() {
    Model::simulate(1000.0, 1.0, |model| {
        // Construct processes and devices here
        let proc1 = model.add_process(ModelProcess::new(10));
        let proc2 = model.add_process(ModelProcess::new(10));

        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));
        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));
        proc2
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));

        // Set up process chain
        proc1.borrow_mut().add_next(proc2.clone(), 1);

        // Set up producer
        model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
    });
}

fn task6() {
    Model::simulate(1000.0, 1.0, |model| {
        let proc1 = model.add_process(ModelProcess::new(10));
        let proc2 = model.add_process(ModelProcess::new(10));
        let proc3 = model.add_process(ModelProcess::new(10));

        proc1
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));
        proc2
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(4.0, 10.0)));
        proc3
            .borrow_mut()
            .add_device(Device::new(DeviceRand::Uniform(1.0, 4.0)));

        // Set up process chain
        proc1.borrow_mut().add_next(proc2.clone(), 75);
        proc1.borrow_mut().add_next(proc3.clone(), 25);

        proc2.borrow_mut().set_if_failure(proc1.clone());

        // Set up producer
        model.set_producer(ModelProducer::new(proc1, DeviceRand::Uniform(2.0, 3.0)));
    });
}
