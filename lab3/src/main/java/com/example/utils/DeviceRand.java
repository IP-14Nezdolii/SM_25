package com.example.utils;

@FunctionalInterface
public interface DeviceRand {
    double next_rand();

    public enum NextPriority {
        Priority,
        Probability,
        ProbabilityWithBusy
    }
}