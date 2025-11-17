package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.Pair;

public class Connection implements Component {
    protected final Stats stats = new Stats();
    protected final String name;

    protected final ArrayList<Pair<Component, Long>> next = new ArrayList<>();
    protected final NextPriority priority;

    private Supplier<Boolean> predicator = () -> true;

    public Connection(NextPriority priority, String name) {
        this.priority = priority;
        this.name = name;
    }

    public void addNext(Component next, long score) {
        this.next.add(Pair.createPair(next, score));
    }

    public void setPredicator(Supplier<Boolean> predicate) {
        this.predicator = predicate;
    }

    @Override
    public void run(Decimal6f time) {
        this.stats.updateTime(time.doubleValue());
    }

    @Override
    public boolean process() {
        stats.addRequest();

        if (predicator.get() == false) {
            return false;
        }

        stats.addServed();

        var next = this.getNextChosen();
        if (next.isPresent()) {
            return next.get().process();
        }

        return true;
    }

    @Override
    public Optional<Decimal6f> getLeftTime() {
        if (predicator.get() == false) {
            return Optional.of(Decimal6f.MAX_VALUE);
        }

        return priority.getLeftTime(next);
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    public class Stats implements ComponentStats {
        private ArrayList<Pair<Double, Double>> availability = new ArrayList<>();
        private long requestsNumber = 0;
        private long served = 0;
        private double totalTime = 0;

        public long getRequestsNumber() {
            return this.requestsNumber;
        }

        private void checkAvailability(double time) {
            availability.add(Pair.createPair(
                (Connection.this.predicator.get() ? 1.0 : 0), 
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

        @Override
        public void clear() {
            this.requestsNumber = 0;
            this.served = 0;
            this.totalTime = 0;
            this.availability.clear();
        }

        public double getTotalTime() {
            return this.totalTime;
        }

        public void addServed() {
            this.served += 1;
        }

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public void updateTime(double time) {
            this.checkAvailability(time);
            this.totalTime += time;
        }

        public double getThroughput() {
            return totalTime / Double.valueOf(requestsNumber) ;
        }

        public String getName() {
            return Connection.this.name;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d, served=%d, throughput=%.2f, availability=%.2f}",
                    this.getName(),
                    this.requestsNumber,
                    this.served,
                    this.getThroughput(),
                    this.getAvailability()
            );
        }
    }

    @Override
    public List<Component> getAllNext() {
        return this.next.stream().map(p -> p.get0()).toList();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return this.priority.getNextChosen(next);
    }

    @Override
    public String getName() {
        return this.name;
    }  

    public static abstract class NextPriority {

        public abstract Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> allNext);

        public Optional<Decimal6f> getLeftTime(ArrayList<Pair<Component, Long>> allNext) {
            Decimal6f time = Decimal6f.MAX_VALUE;

            for (Pair<Component,Long> pair : allNext) {
                var elem = pair.get0();

                if (elem.getLeftTime().isPresent()) {
                    time = time.min(elem.getLeftTime().get());
                }
            }

            return time != Decimal6f.MAX_VALUE 
                ? Optional.of(time)
                : Optional.empty();
        }
    }
}