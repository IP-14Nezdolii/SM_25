package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal4f;

import com.example.modeling.utils.Pair;

public class Constraint implements Component {
    private final Stats stats = new Stats();
    private final Supplier<Boolean> predicate;
    private final String name;
    
    private Optional<Component> next = Optional.empty();
    private Optional<Component> ifFailure = Optional.empty();

    public Constraint(Supplier<Boolean> predicate, String name) {
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
    public Optional<Decimal4f> getLeftTime() {
        if (!this.predicate.get()) {
            return Optional.of(Decimal4f.MAX_VALUE);
        }

        return this.next.isPresent()
            ? this.next.get().getLeftTime()
            : Optional.empty();
    }

    @Override
    public void run(Decimal4f time) {
        this.stats.checkAvailability(time.doubleValue());
    }

    @Override
    public boolean process() {
        this.stats.addRequest();

        if (this.predicate.get()) {
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

    public class Stats {
        private long failures = 0;
        private long requestsNumber = 0;
        private ArrayList<Pair<Double, Double>> availability = new ArrayList<>();

        public void checkAvailability(double time) {
            availability.add(Pair.createPair(
                (Constraint.this.predicate.get() ? 1.0 : 0), 
                time)
            );
        }

        public double getAvailability() {
            if (this.availability.isEmpty()) {
                return 0.0;
            }

            double total = this.availability.stream()
                .mapToDouble((var p)->{return p.get0() * p.get1();})
                .sum();

            double totalTime = this.availability.stream()
                .mapToDouble(Pair::get1)
                .sum();

            return total / totalTime;
        }

        public long getFailures() {
            return this.failures;
        }

        public long getRequestsNumber() {
            return this.requestsNumber;
        }

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

        public String getName() {
            return Constraint.this.name;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d, failures=%d, failureProbability=%.2f, availability=%.2f}",
                    this.getName(),
                    this.requestsNumber,
                    this.failures,
                    this.getFailureProbability(),
                    this.getAvailability()
            );
        }
    }
}