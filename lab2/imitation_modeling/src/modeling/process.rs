use std::cell::{Ref, RefMut};
use std::rc::Weak;
use std::{cell::RefCell, rc::Rc};
use std::any::type_name;
use crate::modeling::device::Device;

pub trait Process {
    fn get_work_time(&self) -> Option<f64>;

    fn process(&mut self) -> bool;

    fn set_next(&mut self, next: SharedProcess);
    fn set_if_failure(&mut self, proc: SharedProcess);

    fn get_base(&mut self) -> &mut ProcessBase;
    fn get_next(&self) -> Option<SharedProcess>;
    fn get_if_failure(&self) -> Option<SharedProcess>;
    
    fn measure_stats(&mut self);
    fn print_stats(&self);

    fn to_shared(self) -> SharedProcess where Self: Sized + 'static {
        SharedProcess::new(Box::new(self))
    }

    fn run(&mut self, time: f64) {
        let processed = self.get_base().run(time);

        if let Some(next) = self.get_next() {
            for _ in 0..processed {
                next.borrow_mut().process();
            }
        }
    }

    fn add_device(&mut self, device: Device) {
        self.get_base().add_device(device);
    }
}

pub struct SharedProcess {
    inner: Rc<RefCell<Box<dyn Process>>>,
}

impl SharedProcess {
    pub fn new(proc: Box<dyn Process>) -> Self {
        Self {
            inner: Rc::new(RefCell::new(proc)),
        }
    }

    pub fn borrow(&self) -> Ref<'_, Box<dyn Process>> {
        self.inner.borrow()
    }

    pub fn borrow_mut(&self) -> RefMut<'_, Box<dyn Process>> {
        self.inner.borrow_mut()
    }

    pub fn downgrade(&self) -> WeakSharedProcess {
        WeakSharedProcess::new(self)
    }
}

impl Clone for SharedProcess {
    fn clone(&self) -> Self {
        Self { inner: self.inner.clone() }
    }
}

pub struct WeakSharedProcess {
    inner: Weak<RefCell<Box<dyn Process>>>,
}

impl WeakSharedProcess {
    fn new(proc: &SharedProcess) -> Self {
        Self {
            inner: Rc::downgrade(&proc.inner),
        }
    }

    pub fn upgrade(&self) -> Option<SharedProcess> {
        self.inner.upgrade().map(|rc| SharedProcess { inner: rc })
    }
}

pub struct ProcessBase {
    queue_capacity: i32,
    queue_size: i32,

    devices: Vec<Device>,
 
    queue_sizies: Vec<i32>,
    number_of_requests: i32,
    failures: i32,
    processed: i32,
}

impl ProcessBase {
    pub fn new(
        queue_capacity: i32,
    ) -> Self {
        ProcessBase {
            queue_capacity: queue_capacity,
            queue_size: 0,
            devices: Vec::new(),

            queue_sizies: Vec::new(),
            number_of_requests: 0,
            failures: 0,
            processed: 0,
        }
    }

    pub fn add_device(&mut self, device: Device) {
        self.devices.push(device);
    }
    
    pub fn get_work_time(&self) -> Option<f64> {
        self.devices.iter()
            .map(|d| d.get_work_time())
            .fold(None, |a, b| {
                match (a, b) {
                    (Some(x), Some(y)) => Some(x.min(y)),
                    (Some(x), None) => Some(x),
                    (None, Some(y)) => Some(y),
                    (None, None) => None,
                }
            })
    }

    pub fn run(&mut self, time: f64) -> i32 {
        assert!(
            !self.devices.is_empty(), 
            "No devices in process {}", type_name::<Self>()
        );

        let mut result = 0;

        for device in self.devices.iter_mut() {
            // if device is free   
            if device.get_work_time().is_none() {
                // load new resourse to device
                if self.queue_size > 0 {
                    self.queue_size -= 1;
                    device.process();
                }
            }

            device.run(time);

            // if resourse is processed
            if device.get_work_time().is_none() {
                result += 1;

                // load new resourse to device
                if self.queue_size > 0 {
                    self.queue_size -= 1;
                    device.process();
                }

                // count of processed requests
                self.processed += result;
            }
        }

        result
    }

    pub fn process(&mut self) -> bool {
        self.number_of_requests += 1;

        for device in self.devices.iter_mut() {
            if device.get_work_time().is_none() {
                device.process();
                return true;
            }
        }

        if self.queue_size < self.queue_capacity {
            self.queue_size += 1;
            
            return true;
        } else {
            self.failures += 1;
            return false;
        }
    }

    pub fn measure_stats(&mut self) {
        self.queue_sizies.push(self.queue_size);
    }

    pub fn print_stats(&self) {
        let avg_queue_size = self.queue_sizies.iter()
            .sum::<i32>() as f64 / self.queue_sizies.len() as f64;

        let failure_probability = self.failures as f64 / self.number_of_requests as f64;
        let throughput = self.processed as f64 / self.number_of_requests as f64;

        println!("--- Process Stats ---");

        println!("Number of requests: {}", self.number_of_requests);
        println!("Number of failures: {}", self.failures);
        println!("Number of processed: {}", self.processed);

        println!("Avg queue size: {}", avg_queue_size);
        println!("Failure probability: {}", failure_probability);
        println!("Throughput: {}", throughput);
    }
}