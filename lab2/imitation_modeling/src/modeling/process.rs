use crate::modeling::device::Device;
use std::any::type_name;
use std::cell::{Ref, RefMut};
use std::collections::HashSet;
use std::rc::Weak;
use std::{cell::RefCell, rc::Rc};

pub trait Process {
    fn get_work_time(&self) -> Option<f64>;

    /// Must be downgraded to avoid cyclic references
    fn add_next(&mut self, next: SharedProcess, priority: u16);

    /// Must be downgraded to avoid cyclic references
    fn set_if_failure(&mut self, proc: SharedProcess);

    fn get_base(&self) -> &ProcessBase;
    fn get_mut_base(&mut self) -> &mut ProcessBase;

    /// Next chosen process
    fn get_next(&self) -> Option<SharedProcess>;

    fn get_all_next(&self) -> Vec<SharedProcess>;
    fn get_if_failure(&self) -> Option<SharedProcess>;

    fn measure_stats(&mut self);
    fn print_stats(&self);

    /// Must be unique for each process
    fn get_id(&self) -> usize {
        self.get_base().get_id()
    }

    fn to_shared(self) -> SharedProcess
    where
        Self: Sized + 'static,
    {
        SharedProcess::new(Box::new(self))
    }

    fn process(&mut self) -> bool {
        if self.get_mut_base().process() {
            return true;
        }

        let mut id_s: HashSet<usize> = HashSet::new();
        id_s.insert(self.get_id());

        let mut current = self.get_if_failure();
        while let Some(next_failure) = current.clone() {
            if id_s.contains(&next_failure.get_process_id()) {
                return false;
            }

            if next_failure.borrow_mut().get_mut_base().process() {
                return true;
            } else {
                // check next if_failure
                current = next_failure.borrow_mut().get_if_failure();
                id_s.insert(next_failure.get_process_id());
            }
        }

        return false;
    }

    fn run(&mut self, time: f64) {
        let processed = self.get_mut_base().run(time);

        for _ in 0..processed {
            if let Some(next) = self.get_next() {
                let mut next = next.borrow_mut();

                if next.get_mut_base().process() {
                    continue;
                }

                // check if_failure
                let mut id_s: HashSet<usize> = HashSet::new();
                id_s.insert(next.get_id());

                let mut current = next.get_if_failure();
                while let Some(next_failure) = current.clone() {
                    if id_s.contains(&next_failure.get_process_id()) {
                        break;
                    }

                    if next_failure.get_process_id() == self.get_id() {
                        if self.process() {
                            break;
                        }

                        // check next if_failure
                        current = self.get_if_failure();
                        id_s.insert(self.get_id());
                        continue;
                    }

                    if next_failure.borrow_mut().get_mut_base().process() {
                        break;
                    } else {
                        // check next if_failure
                        current = next_failure.borrow_mut().get_if_failure();
                        id_s.insert(next_failure.get_process_id());
                    }
                }
            }
        }
    }

    fn add_device(&mut self, device: Device) {
        self.get_mut_base().add_device(device);
    }
}

pub struct SharedProcess {
    inner: Rc<RefCell<Box<dyn Process>>>,
    id: usize,
}

impl SharedProcess {
    pub fn new(proc: Box<dyn Process>) -> Self {
        let inner = Rc::new(RefCell::new(proc));
        let id = inner.borrow().get_id();

        Self {
            inner: inner,
            id: id,
        }
    }

    pub fn get_process_id(&self) -> usize {
        self.id
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
        Self {
            inner: self.inner.clone(),
            id: self.get_process_id(),
        }
    }
}

pub struct WeakSharedProcess {
    inner: Weak<RefCell<Box<dyn Process>>>,
    id: usize,
}

impl WeakSharedProcess {
    fn new(proc: &SharedProcess) -> Self {
        Self {
            inner: Rc::downgrade(&proc.inner),
            id: proc.id,
        }
    }

    pub fn upgrade(&self) -> Option<SharedProcess> {
        match self.inner.upgrade() {
            Some(inner) => Some(SharedProcess { inner, id: self.id }),
            None => None,
        }
    }
}

pub struct ProcessBase {
    queue_capacity: usize,
    queue_size: usize,

    devices: Vec<Device>,

    stats: ProcessStats,
}

impl ProcessBase {
    pub fn new(queue_capacity: usize) -> Self {
        ProcessBase {
            queue_capacity: queue_capacity,
            queue_size: 0,
            devices: Vec::new(),
            stats: ProcessStats {
                queue_sizies: Vec::new(),
                requests_number: 0,
                failures: 0,
                processed: 0,
                total_wait_time: 0.0,
            },
        }
    }

    pub fn add_device(&mut self, device: Device) {
        self.devices.push(device);
    }

    pub fn get_work_time(&self) -> Option<f64> {
        self.devices
            .iter()
            .map(|d| d.get_work_time())
            .fold(None, |a, b| match (a, b) {
                (Some(x), Some(y)) => Some(x.min(y)),
                (Some(x), None) => Some(x),
                (None, Some(y)) => Some(y),
                (None, None) => None,
            })
    }

    pub fn run(&mut self, time: f64) -> usize {
        assert!(
            !self.devices.is_empty(),
            "No devices in process {}",
            type_name::<Self>()
        );

        let mut result = 0;

        for device in self.devices.iter_mut() {
            // if device is free
            if device.get_work_time().is_none() {
                // load new resourse to device
                if self.queue_size > 0 {
                    self.queue_size -= 1;
                    device.process();
                // if load failed, wait
                } else {
                    device.wait(time);
                    continue;
                }
            }
            device.run(time);

            // if resourse is processed
            if device.get_work_time().is_none() {
                result += 1;
            }
        }
        // count of processed requests
        self.stats.add_processed(result);

        // total wait time in queue
        self.stats.add_wait_time(self.queue_size as f64 * time);

        result
    }

    pub fn get_id(&self) -> usize {
        self as *const Self as usize
    }

    pub fn process(&mut self) -> bool {
        self.stats.add_request();

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
            self.stats.add_failures();
            return false;
        }
    }

    pub fn measure_stats(&mut self) {
        self.stats.add_queue_size(self.queue_size);
    }

    pub fn print_stats(&self) {
        self.stats.print(&self.devices);
    }
}

pub struct ProcessStats {
    queue_sizies: Vec<usize>,
    requests_number: usize,
    failures: usize,
    processed: usize,

    total_wait_time: f64,
}

impl ProcessStats {
    pub fn add_queue_size(&mut self, queue_size: usize) {
        self.queue_sizies.push(queue_size);
    }

    pub fn add_request(&mut self) {
        self.requests_number += 1;
    }

    pub fn add_failures(&mut self) {
        self.failures += 1;
    }

    pub fn add_processed(&mut self, processed: usize) {
        self.processed += processed;
    }

    pub fn add_wait_time(&mut self, time: f64) {
        self.total_wait_time += time;
    }

    fn print(&self, devices: &Vec<Device>) {
        let avg_queue_size =
            self.queue_sizies.iter().sum::<usize>() as f64 / self.queue_sizies.len() as f64;

        let failure_probability = self.failures as f64 / self.requests_number as f64;
        let throughput = self.processed as f64 / self.requests_number as f64;

        println!("Number of devices: {:?}", devices.len());

        println!("total requests number: {:?}", self.requests_number);
        println!("failures: {:?}", self.failures);
        println!("processed: {:?}", self.processed);
        println!("failure probability: {:?}", failure_probability);

        println!("total wait time: {:?}", self.total_wait_time);
        println!("avg queue size: {:?}", avg_queue_size);
        println!(
            "avg waiting time: {:?}",
            self.total_wait_time / self.requests_number as f64
        );
        println!("throughput: {:?}", throughput);

        for device in devices.iter() {
            println!("{:?}", device.get_stats());
        }
    }
}
