package com.example.modeling.components;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

public class Constraint implements Component {
    private final Stats stats = new Stats();
    private final String name;
    private final Predicate predicate;

    private Optional<Component> next = Optional.empty();
    private Optional<Component> ifFailure = Optional.empty();

    public Constraint(Predicate predicate, String name) {
        this.predicate = predicate;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    } 

    public void setIfFailure(Component ifFailure) {
        this.ifFailure = Optional.of(ifFailure);
    }

    public Optional<Component> getIfFailure() {
        return this.ifFailure;
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    
    @Override
    public List<Component> getAllNext() {
        return this.next.isPresent()
            ? List.of(this.next.get())
            : List.of();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return this.next;
    }

    @Override
    public Optional<Double> getWorkTime() {
        return this.next.isPresent()
            ? this.next.get().getWorkTime()
            : Optional.empty();
    }

    @Override
    public void run(double time) {
        return;
    }

    @Override
    public boolean process() {
        this.stats.addRequest();

        if (this.predicate.check()) {
            if (this.next.isPresent()) {
                this.next.get().process();
            } 

            return true;
        } else {
            this.stats.addFailure();

            if (this.ifFailure.isPresent()) {
                this.ifFailure.get().process();
            }
            return false;
        }
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    
    @Getter
    public class Stats {
        private long failures = 0;
        private long requestsNumber = 0;

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public void addFailure() {
            this.failures += 1;
        }

        public double getFailureProbability() {
            return this.requestsNumber != 0.0
                ? (double)this.failures / (double)this.requestsNumber
                : 0.0;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d, failures=%d, failureProbability=%.2f}",
                    Constraint.this.name,
                    this.requestsNumber,
                    this.failures,
                    this.getFailureProbability()
            );
        }
    }

    @FunctionalInterface
    public interface Predicate {
        boolean check();
    }
}
