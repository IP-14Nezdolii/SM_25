package com.example.modeling.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.components.Component;
import com.example.modeling.components.Connection.NextPriority;

public final class PriorityImpl {

    public static class Priority extends NextPriority {

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> arr) {
            arr.sort((a,b)-> Long.compare(a.get1(), b.get1()));

            List<Pair<Component, Long>> next = arr.stream()
                .filter(e -> e.get0().getLeftTime().isEmpty())
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
        public Optional<Decimal6f> getLeftTime(ArrayList<Pair<Component, Long>> next) {
            return Optional.empty();
        }
    }

    public static class Probability extends NextPriority {

        @Override
        public Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> arr) {
            List<Pair<Component, Long>> next = arr.stream()
                .filter(e -> e.get0().getLeftTime().isEmpty())
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
