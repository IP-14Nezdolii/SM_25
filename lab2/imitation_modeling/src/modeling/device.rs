use std::fmt;

#[derive(Clone, Debug)]
pub struct Device {
    current_time: f64,
    required_time: Option<f64>,

    rand: DeviceRand,
    stats: DeviceStats,
}

impl Device {
    pub fn new(rand: DeviceRand) -> Self {
        Device {
            current_time: 0.0,
            required_time: None,
            rand: rand,
            stats: DeviceStats {
                busy_time: 0.0,
                total_time: 0.0,
                processed: 0,
            },
        }
    }

    /// If device is busy, returns remaining work time
    pub fn get_work_time(&self) -> Option<f64> {
        match self.required_time {
            Some(t) => Some(t - self.current_time),
            None => None,
        }
    }

    pub fn run(&mut self, time: f64) {
        match self.required_time {
            Some(t) => {
                self.current_time += time;

                if self.current_time >= t {
                    // stats
                    self.stats.add_processed();
                    self.stats.add_busy_time(t - (self.current_time - time));
                    self.stats.add_wait_time(self.current_time - t);

                    // change state
                    self.current_time = 0.0;
                    self.required_time = None;
                } else {
                    self.stats.add_busy_time(time);
                }
            }
            None => panic!("Device is not busy"),
        }
    }

    /// Panics if device is busy
    pub fn wait(&mut self, time: f64) {
        if self.required_time.is_some() {
            panic!("Device is busy");
        }

        self.stats.add_wait_time(time);
    }

    pub fn process(&mut self) {
        match self.required_time {
            Some(_) => panic!("Device is busy"),
            None => {
                self.required_time = Some(self.rand.next_rand());
                self.current_time = 0.0;
            }
        }
    }

    pub fn get_stats(&self) -> DeviceStats {
        self.stats.clone()
    }
}


#[allow(dead_code)]
#[derive(Clone, Debug)]
pub enum DeviceRand {
    Exponential(f64),
    Uniform(f64, f64),
    Fixed(f64),
    Mixed(Vec<DeviceRand>),
}

impl DeviceRand {
    pub fn next_rand(&self) -> f64 {
        let mut n: f64 = rand::random();
        while n == 0.0f64 {
            n = rand::random();
        }

        match self {
            DeviceRand::Exponential(lambda) => -(1.0 / lambda) * n.ln(),
            DeviceRand::Uniform(a, b) => a + (b - a) * n,
            DeviceRand::Fixed(a) => *a,
            DeviceRand::Mixed(rands) => rands.iter().map(|rand| rand.next_rand()).sum(),
        }
    }
}

#[derive(Clone)]
pub struct DeviceStats {
    busy_time: f64,
    total_time: f64,
    processed: usize,
}

impl DeviceStats {
    pub fn add_busy_time(&mut self, time: f64) {
        self.busy_time += time;
        self.total_time += time;
    }

    pub fn add_wait_time(&mut self, time: f64) {
        self.total_time += time;
    }

    pub fn add_processed(&mut self) {
        self.processed += 1;
    }
}

impl fmt::Debug for DeviceStats {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "DeviceStats {{")?;
        writeln!(f, "   busy_time: {:?}", self.busy_time)?;
        writeln!(f, "   total_time: {:?}", self.total_time)?;
        writeln!(f, "   processed: {:?}", self.processed)?;
        writeln!(f, "   utilization: {:?}", self.busy_time / self.total_time)?;
        writeln!(f, "}}")
    }
}
