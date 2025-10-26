package com.example.modeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.example.utils.Pair;
import com.example.modeling.components.Component;
import com.example.modeling.components.Connection.NextPriority;

public final class Model {
    public static Random r = new Random();

    public static Supplier<Double> getFixed(double num) {
        return () -> num;
    }

    public static Supplier<Double> getNorm(double mean, double std) {
        return () -> r.nextGaussian(mean, std);
    }

    public static Supplier<Double> getUniform(double from, double to) {
        return () -> {
            double n = r.nextDouble();
            while (n == 0.0) {
                n = r.nextDouble();
            }

            return from + (to - from) * n;
        };
    }

    public static Supplier<Double> getExponential(double mean) {
        return () -> {
            double lambda = 1/mean;

            double n = r.nextDouble();
            while (n == 0.0) {
                n = r.nextDouble();
            }

            return -(1.0 / lambda) * Math.log(n);
        };
    }

    public static Supplier<Double> getErlang(int k, double mean) {
        return () -> {
            double lambda = k/mean;

            double sum = 0;
            for (int i = 0; i < k; i++) {
                double n = r.nextDouble();
                while (n == 0.0) {
                    n = r.nextDouble();
                }

                sum += -Math.log(n) / lambda;
            }
            return sum;
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

            double r = Math.random() * total;
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

            double r = Math.random() * total;
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
