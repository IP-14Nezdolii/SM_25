#[derive(Clone, Debug)]
pub struct Device {
    current_time: f64,
    required_time: Option<f64>,

    rand: DeviceRand,
}

impl Device {
    pub fn new(rand: DeviceRand) -> Self {
        Device {
            current_time: 0.0,
            required_time: None,

            rand,
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
                    self.current_time = 0.0;
                    self.required_time = None;
                }
            },
            None => panic!("Device is not busy"),
        }
    }

    pub fn process(&mut self) {
        match self.required_time {
            Some(_) => panic!("Device is busy"),
            None => {
                self.required_time = Some(self.rand.next_rand());
                self.current_time = 0.0;
            },
        }
    }
}

#[derive(Clone, Debug)]
pub enum DeviceRand {
    Exponential(f64),
    Uniform(f64, f64),
}

impl DeviceRand {
    pub fn next_rand(&self) -> f64 {
        let mut n: f64 = rand::random();
        while n == 0.0 {
            n = rand::random();
        }

        match self {
            DeviceRand::Exponential(lambda) => {
                    - (1.0 / lambda) * n.ln()
                },
            DeviceRand::Uniform(a, b) => {
                    a + (b - a) * n
                },
        }
    }
}

