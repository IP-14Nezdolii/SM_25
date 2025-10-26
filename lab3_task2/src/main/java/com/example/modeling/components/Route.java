package com.example.modeling.components;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.modeling.components.device.Device.DeviceRand;
import com.example.utils.Pair;

import lombok.Getter;

public class Route implements Component {
    private final ArrayList<Person> lst = new ArrayList<>();
    private final String name;
    private final Stats stats = new Stats();
    private final DeviceRand rand;

    private Optional<Component> next = Optional.empty();

    public Route(DeviceRand rand, String name) {
        this.name = name;
        this.rand = rand;
    }

    public void setNext(Component next) {
        this.next = Optional.of(next);
    }

    @Override
    public Optional<Double> getWorkTime() {
        double time = Double.MAX_VALUE;

        for (var elem: lst) {
            time = Math.min(time, elem.getTime());
        }

        return time != Double.MAX_VALUE 
            ? Optional.of(time)
            : Optional.empty();
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

    @Override
    public void run(double time) {
        this.stats.record(time);

        for (int i = this.lst.size() - 1; i >= 0; i--) {
            var p = this.lst.get(i);
            p.run(time);

            if (p.isReady()) {
                next.ifPresent((next) -> next.process(p.getTyp()));

                this.lst.remove(i);
                this.stats.addServed();
            }
        };
    }

    @Override
    public boolean process(int typ) {
        var p = new Person(typ, this.rand.next_rand(typ));

        this.lst.add(p);
        this.stats.addRequest();

        return true;
    }
    
    private class Person {
        private final int typ;
        private final double servingTime;
        private double currentTime = 0;

        public Person(int typ, double servingTime) {
            this.typ = typ;
            this.servingTime = servingTime;
        }

        public double getTime() {
            return this.servingTime - this.currentTime;
        }

        public void run(double time) {
            this.currentTime += time;
        }

        public int getTyp() {
            return this.typ;
        }

        public boolean isReady() {
            return this.currentTime >= servingTime;
        }
    }

    @Getter
    public class Stats {
        private ArrayList<Pair<Integer, Double>> queueSizies = new ArrayList<>();
        private long requests = 0;
        private long served = 0;

        public void record(double time) {
            this.queueSizies.add(Pair.createPair(Route.this.lst.size(), time));
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
                    ? getTotalWaitTime() / this.served
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
                    Route.this.name,
                    this.getAverageQueueSize(),
                    this.requests,
                    this.served,
                    this.getTotalWaitTime(),
                    this.getAvgWaitTime());
        }
    }
}
