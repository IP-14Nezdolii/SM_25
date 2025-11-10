package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.decimal4j.immutable.Decimal4f;

import com.example.modeling.utils.Pair;

public class Queue implements Component {
    protected final Stats stats = new Stats();
    protected final String name;
    
    protected long size = 0;
    protected Optional<Component> next = Optional.empty();

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
        this.enqueue();

        if (this.next.isPresent()) {
            var next = this.next.get();

            while (next.getLeftTime().isEmpty() && this.size > 0) {
                this.next.get().process();
                this.stats.addServed();

                this.dequeue();
            }
        }
        
        return true;
    }

    /*
     * Records queue size over time
     */
    @Override
    public void run(Decimal4f time) {
        this.stats.record(time.doubleValue());

        if (this.next.isPresent()) {
            var next = this.next.get();

            while (next.getLeftTime().isEmpty() && this.size > 0) {
                next.process();

                this.dequeue();
                this.stats.addServed();
            }
        }
    }

    @Override
    public Optional<Decimal4f> getLeftTime() {
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

        public long getRequests() {
            return this.requests;
        }

        public void addRequest() {
            this.requests += 1;
        }

        public void addServed() {
            this.served+=1;
        }

        public double getAverageQueueSize() {
            if (this.queueSizies.isEmpty()) {
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
                    ? getTotalWaitTime() / this.served
                    : 0.0;
        }

        public double getTotalWaitTime() {
            return this.queueSizies.stream()
                    .mapToDouble(p -> p.get0() * p.get1())
                    .sum();
        }

        public String getName() {
            return Queue.this.name;
        }

        public String toString() {
            return String.format(
                    "%s:{averageQueueSize=%.2f, requests=%d, served=%d, totalWaitTime=%.2f, avgWaitTime=%.2f}",
                    this.getName(),
                    this.getAverageQueueSize(),
                    this.requests,
                    this.served,
                    this.getTotalWaitTime(),
                    this.getAvgWaitTime());
        }
    }
}