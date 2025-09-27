pub mod process;
pub mod device;

use process::SharedProcess;

pub trait Producer {
    fn run(&mut self, time: f64);
    fn get_time(&self) -> f64;
    fn get_next(&self) -> Option<SharedProcess>;
}

pub struct Model<T: Producer> {
    config: fn(&mut Self),

    producer: Option<T>,
    processes: Vec<(i32, SharedProcess)>,
}

impl <T: Producer> Model<T> {
    pub fn simulate(
        total_time: f64, 
        check_period: f64, 
        config: fn(&mut Self)
    ) {
        let mut model = Model {
            config,
            processes: Vec::new(),
            producer: None,
        };

        (model.config)(&mut model);

        match &model.producer {
            Some(p) => model.add_processes(p.get_next().unwrap()),
            None => panic!("No producer set"),
        }

        model.sim(total_time, check_period);
    }

    pub fn set_producer(&mut self, producer: T) {
        self.producer = Some(producer);
    }

    fn sim(
        mut self, 
        time: f64, 
        check_period: f64
    ) {
        let mut current_time = 0.0;
        let mut period_time = 0.0;

        while current_time < time {
            // Determine the next time step
            let delta_time= self.get_delta_time(check_period, period_time);

            // Run all processes and producer for delta_time
            for (_, proc) in self.processes.iter().rev() {
                let mut proc = proc.borrow_mut();

                if proc.get_work_time().is_some() {
                    proc.run(delta_time);
                }
            }
            self.producer.as_mut().unwrap().run(delta_time);
            
            period_time += delta_time;
            current_time += delta_time;

            // Check and record stats
            if  period_time == check_period {
                period_time = 0.0;

                for (_, proc) in self.processes.iter() {
                    proc.borrow_mut().measure_stats();
                }
            }
        }

        self.print_stats(time, check_period);
    }

    fn get_delta_time(
        &self, 
        check_period: f64, 
        period_time: f64
    ) -> f64 {
        let producer: &T = self.producer.as_ref().unwrap();

        let delta_time: f64 = self.processes
            .iter()
            .map(|(_, p)| p.borrow().get_work_time())
            .fold(check_period - period_time, |a, b| match b {
                None => a,
                Some(v) => a.min(v)
                    
            });

        let delta_time = 
            delta_time.min(producer.get_time());

        delta_time
    }

    fn print_stats(
        &self, 
        total_time: f64 , 
        check_period: f64
    ) {
        println!("--- Model Stats ---");
        println!("Total time: {}", total_time);
        println!("Check period: {}", check_period);
        println!("Number of processes: {}", self.processes.len());
        println!("-------------------");

        for (num, proc) in self.processes.iter() {
            println!("Process #{}:", num);
            proc.borrow().print_stats();
        }
    }

    fn add_processes(&mut self, proc: SharedProcess) {
        self.processes.push((self.processes.len() as i32, proc.clone()));
        
        if let Some(next) = proc.borrow().get_next() {
            self.add_processes(next);
        }
    }
}
