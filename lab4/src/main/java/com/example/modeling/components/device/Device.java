package com.example.modeling.components.device;

import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class Device {
    final DeviceRand rand;
    final Stats stats;
    final String name;

    double current_time;
    Optional<Double> required_time;

    public Device(DeviceRand rand, String name) {
        this.rand = rand;
        this.current_time = 0.0;
        this.required_time = Optional.empty();
        this.stats = new Stats();
        this.name = name;
    }

    public Optional<Double> getWorkTime() {
        return Optional.ofNullable(required_time
                .map(t -> t - this.current_time)
                .orElse(null));
    };

    /*
     * Throws exception if device is not busy
     */
    public boolean run(double time) {
        boolean done = false;

        if (this.required_time.isPresent()) {
            var t = this.required_time.get();

            this.current_time += time;

            if (this.current_time >= t) {
                done = true;

                // stats
                this.stats.addProcessed();
                this.stats.addBusyTime(t - (this.current_time - time));
                this.stats.addWaitTime(this.current_time - t);

                // change state
                this.current_time = 0.0;
                this.required_time = Optional.empty();
            } else {
                this.stats.addBusyTime(time);
            }
        } else {
            throw new IllegalStateException("Device is not busy");
        }

        return done;
    }

    /*
     * Throws exception if device is busy
     */
    public void wait(double time) {
        this.required_time.ifPresentOrElse(t -> {
            throw new IllegalStateException(
                "Device is busy. Required time left: " + (t - this.current_time));
        }, () -> {
            this.stats.addWaitTime(time);
        });
    }

    /*
     * Throws exception if device is busy
     */
    public void process() {
        if (this.required_time.isPresent()) {
            throw new IllegalStateException(
                "Device is busy, Required time left: " + (this.required_time.get() - this.current_time));
        } else {
            this.required_time = Optional.of(this.rand.next_rand());
            this.current_time = 0.0;
        }
    }

    public Stats getStats() {
        return this.stats;
    };

    @Getter
    @EqualsAndHashCode
    public class Stats {
        private double busy_time = 0;
        private double total_time = 0;
        private long processed = 0;

        public void addBusyTime(double time) {
            this.busy_time += time;
            this.total_time += time;
        }

        public void addWaitTime(double time) {
            this.total_time += time;
        }

        public void addProcessed() {
            this.processed += 1;
        }

        public double getUtilization() {
            return this.total_time != 0
                    ? this.busy_time / this.total_time
                    : 0;
        }

        public String toString() {
            String format = "%s:{busy_time=%.2f, total_time=%.2f, utilization=%.2f, processed=%d}";

            return String.format(
                    format,
                    Device.this.name,
                    this.busy_time,
                    this.total_time,
                    this.getUtilization(),
                    this.processed);
        }
    }

    @FunctionalInterface
    public interface DeviceRand {
        double next_rand();
    }
}
