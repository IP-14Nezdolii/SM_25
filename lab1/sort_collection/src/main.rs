use rand::{
    Rng, SeedableRng,
    distr::{Alphanumeric, SampleString},
    prelude::StdRng,
};
use rayon::{
    ThreadPool, ThreadPoolBuilder,
    iter::{IntoParallelIterator, ParallelIterator},
};
use std::fs::File;
use std::io::Write;
use std::time::Instant;
use std::{cmp::Ordering, hint::black_box};

const NUM_MEASURES: u64 = 10_000;
const ARRAY_SIZE: u64 = 1_000_000;

fn main() {
    let pool = ThreadPoolBuilder::new().num_threads(4).build().unwrap();

    let f64_data = measure_f64(&pool);
    let string_data = measure_string(&pool);

    write_times(&f64_data, "sort_times1.txt");
    write_times(&string_data, "sort_times2.txt");
}

fn measure_time<T>(arr: &mut Vec<T>, cmp: fn(&T, &T) -> Ordering) -> f64 {
    let start = Instant::now();

    arr.sort_by(cmp);
    black_box(&arr);

    return start.elapsed().as_nanos() as f64 / 1_000_000.0;
}

fn measure_f64(pool: &ThreadPool) -> Vec<f64> {
    pool.install(|| {
        (0..NUM_MEASURES)
            .into_par_iter()
            .map(|i| {
                let mut rng = rand::rngs::StdRng::seed_from_u64(i);
                let mut arr = (0..ARRAY_SIZE)
                    .map(|_| rng.random_range(0.0..=1_000_000.0))
                    .collect();

                measure_time(&mut arr, f64_cmp)
            })
            .collect()
    })
}

fn measure_string(pool: &ThreadPool) -> Vec<f64> {
    pool.install(|| {
        (0..NUM_MEASURES)
            .into_par_iter()
            .map(|i| {
                let mut rng = rand::rngs::StdRng::seed_from_u64(i);
                let mut arr = (0..ARRAY_SIZE)
                    .map(|_| generate_random_string(6, 14, &mut rng))
                    .collect();

                measure_time(&mut arr, |a: &String, b: &String| a.cmp(b))
            })
            .collect()
    })
}

#[inline]
fn f64_cmp(a: &f64, b: &f64) -> Ordering {
    if a > b {
        Ordering::Greater
    } else if a == b {
        Ordering::Equal
    } else {
        Ordering::Less
    }
}

#[inline]
fn generate_random_string(min_len: usize, max_len: usize, rng: &mut StdRng) -> String {
    let len = rng.random_range(min_len..=max_len);

    Alphanumeric.sample_string(rng, len)
}

fn write_times(times: &Vec<f64>, filename: &str) {
    let mut file = File::create(filename).unwrap();
    for time in times.iter() {
        writeln!(file, "{}", time).unwrap();
    }
}