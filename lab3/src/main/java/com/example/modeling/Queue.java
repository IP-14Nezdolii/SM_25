package com.example.modeling;

import java.util.ArrayList;
import java.util.Optional;

import com.example.utils.DeviceRand.NextPriority;
import com.example.modeling.components.Component;
import com.example.utils.Pair;

import lombok.Getter;

public class Queue extends Component {
    private final Stats stats = new Stats();
    private long size = 0;

    public Queue(NextPriority priority, String name) {
        super(priority, name);
    }

    public Queue(String name) {
        super(name);
    }

    public long getSize() {
        return this.size;
    }

    /*
     * Records queue size over time
     */

    @Override
    public boolean process() {
        this.stats.addRequest();

        this.size += 1;

        if (this.priority == NextPriority.Priority || this.priority == NextPriority.Probability) {
            Optional<Component> next = this.getNextChosen();
            while (next.isPresent() && this.size > 0) {
                next.get().process();

                this.size -= 1;
                next = this.getNextChosen();
            }
        } else {
            // NextChoosing.ProbabilityWithBusy
            Optional<Component> next = this.getNextChosen();
            if (next.isPresent()) {
                next.get().process();

                this.size -= 1;
            }
        }
        
        return true;
    }

    @Override
    public void run(double time) {
        if (this.priority == NextPriority.Priority || this.priority == NextPriority.Probability) {
            Optional<Component> next = this.getNextChosen();
            while (next.isPresent() && this.size > 0) {
                next.get().process();

                this.size -= 1;
                next = this.getNextChosen();
            }
        } else {
            // NextChoosing.ProbabilityWithBusy
            Optional<Component> next = this.getNextChosen();
            if (next.isPresent()) {
                next.get().process();

                this.size -= 1;
            }
        }

        this.stats.addWaitTime(time);
    }

    @Override
    public Optional<Double> getWorkTime() {
        return Optional.empty();
    }

    @Override
    public Object getStats() {
        return this.stats;
    }

    @Getter
    public class Stats {
        private ArrayList<Pair<Long, Double>> queueSizies = new ArrayList<>();
        private long requestsNumber = 0;
        private double totalWaitTime = 0.0;

        public void addWaitTime(double time) {
            this.totalWaitTime += time;
            this.queueSizies.add(Pair.createPair(Queue.this.size, time));
        }

        public void addRequest() {
            this.requestsNumber += 1;
        }

        public double getAverageQueueSize() {
            if (this.queueSizies.size() == 0) {
                return 0.0;
            }

            double total = this.queueSizies.stream()
                    .mapToDouble(p -> p.get0() * p.get1())
                    .sum();

            double totalTime = this.queueSizies.stream()
                    .mapToDouble(Pair::get1)
                    .sum();

            return total / totalTime;
        }

        public double getAvgWaitTime() {
            return this.requestsNumber != 0
                    ? this.totalWaitTime / this.requestsNumber
                    : 0.0;
        }

        public String toString() {
            return String.format(
                    "%s:{averageQueueSize=%.2f, requestsNumber=%d, totalWaitTime=%.2f, avgWaitTime=%.2f}",
                    Queue.this.name,
                    this.getAverageQueueSize(),
                    this.requestsNumber,
                    this.totalWaitTime,
                    this.getAvgWaitTime());
        }
    }
}