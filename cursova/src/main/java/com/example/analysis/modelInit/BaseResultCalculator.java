package com.example.analysis.modelInit;

import java.util.function.Supplier;

public record BaseResultCalculator(
    Supplier<Integer> prod_served,

    Supplier<Double> mean_q_size,
    Supplier<Integer> max_q_size,
    Supplier<Double> mean_wait_q,
 
    Supplier<Integer> loader1_served,
    Supplier<Integer> loader2_served,
    Supplier<Double> mean_loader_q_size,
    Supplier<Double> mean_loader_wait_q,

    Supplier<Double> mean_truck_q_size,
    Supplier<Double> mean_truck_wait_q,

    Supplier<Double> productivity,
    Supplier<Double> processing_time) {
}
