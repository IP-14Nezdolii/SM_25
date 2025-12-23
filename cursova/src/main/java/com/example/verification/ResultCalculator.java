package com.example.verification;

import java.util.function.Supplier;

public record ResultCalculator(
    Supplier<Long> prod_served,

    Supplier<Double> mean_q_size,
    Supplier<Double> mean_wait_q,

     Supplier<Integer> q_served,
 
    Supplier<Double> m_loader_util,
    Supplier<Double> mean_loader_q_size,
    Supplier<Double> mean_loader_wait_q,

    Supplier<Double> m_truck_util,

    Supplier<Double> productivity,
    Supplier<Double> processing_time) {
}
