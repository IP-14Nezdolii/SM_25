pub mod device;
pub mod process;

use process::SharedProcess;
use std::{collections::HashSet};

use crate::modeling::process::Process;

pub trait Producer {
    fn run(&mut self, time: f64);
    fn get_time(&self) -> f64;
    fn get_next(&self) -> SharedProcess;
}

pub struct ModelConstructor<T: Producer> {
    producer: Option<T>,
    __processes: Vec<SharedProcess>,
}

impl<T: Producer> ModelConstructor<T> {
    fn new(config: fn(&mut Self)) -> Self {
        let mut constructor = ModelConstructor {
            producer: None,
            __processes: Vec::new(),
        };

        (config)(&mut constructor);

        assert!(constructor.producer.is_some(), "No producer set");

        constructor
    }

    pub fn set_producer(&mut self, producer: T) {
        self.producer = Some(producer);
    }

    pub fn add_process<P: Process + 'static>(&mut self, proc: P) -> SharedProcess {
        let proc = proc.to_shared();
        self.__processes.push(proc.clone());
        return proc;
    }

    fn construct(self) -> Model<T> {
        let mut state = ModelState {
            producer: self.producer.unwrap(),
            processes: Vec::new(),
        };

        let mut id_s: HashSet<usize> = HashSet::new();
        state.add_processes(state.producer.get_next(), &mut id_s);

        let processes: HashSet<usize> = self
            .__processes
            .iter()
            .map(|proc| proc.borrow().get_id())
            .collect();

        assert!(id_s == processes, "incorrect config!");

        Model { state: state }
    }
}

pub struct Model<T: Producer> {
    state: ModelState<T>,
}

impl<T: Producer> Model<T> {
    pub fn simulate(total_time: f64, check_period: f64, config: fn(&mut ModelConstructor<T>)) {
        ModelConstructor::<T>::new(config)
            .construct()
            .sim(total_time, check_period);
    }

    fn sim(mut self, time: f64, check_period: f64) {
        let mut current_time = 0.0;
        let mut period_time = 0.0;

        while current_time < time {
            // Determine the next time step
            let delta_time = self.state.get_delta_time().min(check_period - period_time);

            // Run all processes and producer for delta_time
            self.state.run(delta_time);

            period_time += delta_time;
            current_time += delta_time;

            // Check and record stats
            if period_time == check_period {
                period_time = 0.0;

                self.state.measure_stats();
            }
        }

        self.state.print_stats(time, check_period);
    }
}

struct ModelState<T: Producer> {
    producer: T,
    processes: Vec<(SharedProcess, usize)>,
}

impl<T: Producer> ModelState<T> {
    fn add_processes(&mut self, proc: SharedProcess, id_s: &mut HashSet<usize>) {
        let id = proc.borrow().get_id();

        if id_s.contains(&id) {
            return;
        }
        id_s.insert(id);

        if let Some(if_failure) = proc.borrow().get_if_failure() {
            self.add_processes(if_failure, id_s);
        }

        self.processes.push((proc.clone(), id));

        for next in proc.borrow().get_all_next().iter() {
            self.add_processes(next.clone(), id_s);
        }
    }

    fn get_delta_time(&self) -> f64 {
        self.processes
            .iter()
            .map(|(p, _)| p.borrow().get_work_time())
            .fold(self.producer.get_time(), |a, b| match b {
                Some(v) => a.min(v),
                None => a,
            })
    }

    fn measure_stats(&mut self) {
        for (p, _) in self.processes.iter() {
            p.borrow_mut().measure_stats();
        }
    }

    fn run(&mut self, time: f64) {
        for (proc, _) in self.processes.iter_mut().rev() {
            proc.borrow_mut().run(time);
        }
        self.producer.run(time);
    }

    fn print_stats(&self, total_time: f64, check_period: f64) {
        println!("--- Model Stats ---");
        println!("Total time: {}", total_time);
        println!("Check period: {}", check_period);
        println!("Number of processes: {}", self.processes.len());
        println!("-------------------");

        for i in 0..self.processes.len() {
            let (proc, _) = &self.processes[i];

            println!("--- Process #{} ---", i);
            proc.borrow().print_stats();
            //println!("-------------------");
        }
    }
}
