package com.example.modeling.utils;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class FunRand {
    private static Random r = new Random();

    public static double nextRandom() {
        double n = r.nextDouble();
        while (n == 0.0) {
            n = r.nextDouble();
        }

        return n;
    }

    public static Supplier<Double> getFixed(double num) {
        return () -> num;
    }

    public static Supplier<Double> getNotNullNorm(double mean, double std) {
        return () -> {
            double n = r.nextGaussian(mean, std);
            while (n <= 0) {
                n = r.nextGaussian(mean, std);
            }
            return n;
        };
    }

    public static Supplier<Double> getUniform(double from, double to) {
        return () -> from + (to - from) * r.nextDouble();
    }

    public static Supplier<Double> getExponential(double mean) {
        return () -> {
            return -mean * Math.log(nextRandom());
        };
    }

    public static Supplier<Double> getCombined(List<Supplier<Double>> lst) {
        return () -> {
            double t = 0;

            for (var elem : lst) {
                t += elem.get();
            }

            return t;
        };
    }
 
    public static Supplier<Double> getErlang(double mean, double var) {
        if (var <= 0 || mean <= 0) {
            throw new IllegalArgumentException("mean and variance must be positive");
        }

        double kReal = (mean * mean) / var;
        final int k = Math.max(1, (int) Math.round(kReal));

        final double lam = (double)k / mean;

        return () -> {
            double sum = 0.0;

            for (int i = 0; i < k; i++) {
                double u = nextRandom();
                while (u == 0.0) {
                    u = nextRandom();
                }
                sum += -Math.log(u) / lam;
            }
            return sum;
        };
    }
}
