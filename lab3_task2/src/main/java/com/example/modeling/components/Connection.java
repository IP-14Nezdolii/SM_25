package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.example.utils.Pair;

import lombok.Getter;

public class Connection implements Component {
    private final Stats stats = new Stats();
    private final String name;

    private final ArrayList<Pair<Component, Long>> next = new ArrayList<>();
    private final NextPriority priority;
    private final Function<Integer, Integer> trannsformer;

    public Connection(
        NextPriority priority, 
        Function<Integer, Integer> transformer, 
        String name
    ) {
        this.priority = priority;
        this.name = name;
        this.trannsformer = transformer;
    }

    public void addNext(Component next, long score) {
        this.next.add(Pair.createPair(next, score));
    }

    @Override
    public void run(double time) {
        this.stats.updateTime(time);
        priority.run(time);
    }

    @Override
    public boolean process(int typ) {
        stats.addRequest();

        if (this.getNextChosen().isPresent()) {
            var next = this.getNextChosen().get();
            return next.process(this.trannsformer.apply(typ));
        }

        return true;
    }

    @Override
    public Optional<Double> getWorkTime() {
        return priority.getWorkTime(next);
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    @Getter
    public class Stats {
        private long requestsNumber = 0;
        private double totalTime = 0;

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public void updateTime(double time) {
            this.totalTime += time;
        }

        public double getThroughput() {
            return totalTime / Double.valueOf(requestsNumber) ;
        }

        public String toString() {
            return String.format(
                    "%s:{requests=%d, throughput=%.2f}",
                    Connection.this.name,
                    this.requestsNumber,
                    this.getThroughput()
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

        public abstract Optional<Component> getNextChosen(ArrayList<Pair<Component, Long>> next);

        public Optional<Double> getWorkTime(ArrayList<Pair<Component, Long>> next) {
            double time = Double.MAX_VALUE;

            for (Pair<Component,Long> pair : next) {
                var elem = pair.get0();

                if (elem.getWorkTime().isPresent()) {
                    time = Math.min(time, elem.getWorkTime().get());
                } else {
                    return Optional.empty();
                }
            }

            return time != Double.MAX_VALUE 
                ? Optional.of(time)
                : Optional.empty();
        }

        public void run(double time) {
            return;
        }
    }
}