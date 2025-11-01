package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.example.utils.Pair;
import com.example.modeling.components.Connection.NextPriority;
import com.example.modeling.components.device.Device.DeviceRand;

public final class Model {
    private static final Object lock = new Object();
    private static final Random r = new Random();

        public static DeviceRand getFixed(double num) {
        return () -> num;
    }

    public static DeviceRand getNorm(double mean, double std) {
        return () -> {
            synchronized (lock) {
               return r.nextGaussian(mean, std);
            }
        };
    }

    public static DeviceRand getUniform(double from, double to) {
        return () -> {
            synchronized (lock) {
                double n = r.nextDouble();
                while (n == 0.0) {
                    n = r.nextDouble();
                }

                return from + (to - from) * n;
            }
        };
    }

    public static DeviceRand getExponential(double mean) {
        return () -> {
            synchronized (lock) {
                double lambda = 1/mean;

                double n = r.nextDouble();
                while (n == 0.0) {
                    n = r.nextDouble();
                }

                return -(1.0 / lambda) * Math.log(n);
            }
        };
    }

    public static class Priority extends NextPriority {

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> arr) {
            arr.sort((a,b)-> Long.compare(a.get1(), b.get1()));

            List<Pair<Component, Long>> next = arr.stream()
                .filter(e -> e.get0().getWorkTime().isEmpty())
                .collect(Collectors.toList())
                .reversed();
            
            if (next.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(next.getFirst().get0());
            }
        }
    }

    public static class ProbabilityWithBusy extends NextPriority {

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> arr) {
            List<Pair<Component, Long>> next = arr;

            if (next.isEmpty()) {
                return Optional.empty();
            }
            
            Long total = next.stream()
                    .mapToLong(p -> p.get1().longValue())
                    .sum();

            double r = 0;
            synchronized (lock) {
               r = Math.random() * total;
            }
            
            double cumulative = 0.0;

            for (Pair<Component, Long> p : next) {
                cumulative += p.get1();
                if (r <= cumulative) {
                    return Optional.of(p.get0());
                }
            }

            // Fallback (should not reach here)
            throw new IllegalStateException("Wrong algorithm!");
        }

        @Override
        public Optional<Double> getWorkTime(ArrayList<Pair<Component, Long>> next) {
            return Optional.empty();
        }
    }

    public static class Probability extends NextPriority {

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> arr) {
            List<Pair<Component, Long>> next = arr.stream()
                .filter(e -> e.get0().getWorkTime().isEmpty())
                .collect(Collectors.toList());
            
            if (next.isEmpty()) {
                return Optional.empty();
            }
            
            Long total = next.stream()
                    .mapToLong(p -> p.get1().longValue())
                    .sum();

            double r = 0;
            synchronized (lock) {
               r = Math.random() * total;
            }
            double cumulative = 0.0;

            for (Pair<Component, Long> p : next) {
                cumulative += p.get1();
                if (r <= cumulative) {
                    return Optional.of(p.get0());
                }
            }

            // Fallback (should not reach here)
            throw new IllegalStateException("Wrong algorithm!");
        }
    }
}
