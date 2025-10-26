package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

import com.example.utils.Pair;

import lombok.Getter;

public class Queue implements Component {
    private final Stats stats = new Stats();
    private final String name;

    private final PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> {
        if (a.equals(1)) return -1;
        if (b.equals(1)) return 1;
        return 0;
    });
    
    private Optional<Component> next = Optional.empty();

    public Queue(String name) {
        this.name = name;
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    public long getSize() {
        return this.queue.size();
    }

    @Override
    public boolean process(int typ) {
        this.stats.addRequest();

        if (this.next.isPresent()) {
            var next = this.next.get();

            if (next.getWorkTime().isEmpty()) {
                this.queue.add(typ);     
                this.next.get().process(this.queue.poll());

                this.stats.addServed();
            } else {
                this.queue.add(typ);
            }
            
        } else {
            this.queue.add(typ);
        }
        
        return true;
    }

    /*
     * Records queue size over time
     */
    @Override
    public void run(double time) {
        this.stats.record(time);

        if (this.next.isPresent()) {
            var next = this.next.get();

            while (next.getWorkTime().isEmpty() && this.queue.size() > 0) {
                this.next.get().process(this.queue.poll());

                this.stats.addServed();
            }
        }
    }

    @Override
    public Optional<Double> getWorkTime() {
        return Optional.empty();
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    @Override
    public List<Component> getAllNext() {
        return this.next.isPresent()
            ? List.of(this.next.get())
            : List.of();
    }

    @Override
    public Optional<Component> getNextChosen() {
        return next;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Getter
    public class Stats {
        private ArrayList<Pair<Integer, Double>> queueSizies = new ArrayList<>();
        private long requests = 0;
        private long served = 0;

        public void record(double time) {
            this.queueSizies.add(Pair.createPair(Queue.this.queue.size(), time));
        }

        public long getServed() {
            return this.served;
        }

        public void addRequest() {
            this.requests += 1;
        }

        public void addServed() {
            this.served+=1;
        }

        public double getAverageQueueSize() {
            if (this.queueSizies.size() == 0) {
                return 0.0;
            }

            double total = this.getTotalWaitTime();

            double totalTime = this.queueSizies.stream()
                    .mapToDouble(Pair::get1)
                    .sum();

            return total / totalTime;
        }

        public double getAvgWaitTime() {
            return this.served != 0
                    ? getTotalWaitTime() / this.requests
                    : 0.0;
        }

        public double getTotalWaitTime() {
            return this.queueSizies.stream()
                    .mapToDouble(p -> p.get0() * p.get1())
                    .sum();
        }

        public String toString() {
            return String.format(
                    "%s:{averageQueueSize=%.2f, requests=%d, served=%d, totalWaitTime=%.2f, avgWaitTime=%.2f}",
                    Queue.this.name,
                    this.getAverageQueueSize(),
                    this.requests,
                    this.served,
                    this.getTotalWaitTime(),
                    this.getAvgWaitTime());
        }
    }
}