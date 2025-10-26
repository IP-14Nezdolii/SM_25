package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.utils.Pair;

import lombok.Getter;

public class Queue implements Component {
    private final Stats stats = new Stats();
    private final String name;
    
    protected long size = 0;
    private Optional<Component> next = Optional.empty();

    public Queue(String name) {
        this.name = name;
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    public long getSize() {
        return this.size;
    }

    public void setSize(long newSize) {
        this.size = newSize;
    }

    public void enqueue() {
        this.size++;
    }

    public void dequeue() {
        if (this.size == 0) {
            throw new IllegalStateException("Queue:" + this.name + ",size == 0"); 
        }

        this.size--;
    }

    @Override
    public boolean process() {
        this.stats.addRequest();

        if (this.next.isPresent()) {
            var next = this.next.get();

            if (next.getWorkTime().isEmpty()) {
                this.next.get().process();
                this.stats.addServed();
            } else {
                this.enqueue();
            }
            
        } else {
            this.enqueue();
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

            while (next.getWorkTime().isEmpty() && this.size > 0) {
                next.process();

                this.dequeue();
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
        private ArrayList<Pair<Long, Double>> queueSizies = new ArrayList<>();
        private long requests = 0;
        private long served = 0;

        public void record(double time) {
            this.queueSizies.add(Pair.createPair(Queue.this.size, time));
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