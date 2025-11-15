package com.example.modeling.components.device;

import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

public class Device {
    final Supplier<Double> rand;
    final Stats stats;
    final String name;

    Decimal6f currentTime;
    Optional<Decimal6f> requiredTime;

    public Device(Supplier<Double> rand, String name) {
        this.rand = rand;

        this.currentTime = Decimal6f.ZERO;
        this.requiredTime = Optional.empty();

        this.stats = new Stats();
        this.name = name;
    }

    public Optional<Decimal6f> getWorkTime() {
        return Optional.ofNullable(requiredTime
                .map(t -> t.subtract(this.currentTime))
                .orElse(null));
    };

    /*
     * Throws exception if device is not busy
     */
    public boolean run(Decimal6f time) {
        boolean done = false;

        if (this.requiredTime.isPresent()) {
            var t = this.requiredTime.get();

            this.currentTime = this.currentTime.add(time);

            if (this.currentTime.isGreaterThanOrEqualTo(t)) {
                done = true;

                // stats
                this.stats.addProcessed();
                this.stats.addBusyTime(t.subtract(this.currentTime.subtract(time)).doubleValue());
                this.stats.addWaitTime(this.currentTime.subtract(t).doubleValue());

                // change state
                this.currentTime = Decimal6f.ZERO;
                this.requiredTime = Optional.empty();
            } else {
                this.stats.addBusyTime(time.doubleValue());
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
        this.requiredTime.ifPresentOrElse(t -> {
            throw new IllegalStateException(
                "Device is busy. Required time left: " + t.subtract(this.currentTime));
        }, () -> {
            this.stats.addWaitTime(time);
        });
    }

    /*
     * Throws exception if device is busy
     */
    public void process() {
        if (this.requiredTime.isPresent()) {
            throw new IllegalStateException(
                name + " is busy, Required time left: " + (this.requiredTime.get().subtract( this.currentTime)));
        } else {
            Decimal6f num = Decimal6f.valueOf(this.rand.get());
            if (num.isLessThanOrEqualTo(Decimal6f.ZERO)) {
                throw new IllegalStateException(
                    "Invalid rand generator: generated value is less or equal 0. Device name: " + this.name
                );
            }

            this.requiredTime = Optional.of(num);
            this.currentTime = Decimal6f.ZERO;
        }
    }

    public Stats getStats() {
        return this.stats;
    };

    public class Stats {
        private double busyTime = 0;
        private double totalTime = 0;
        private long served = 0;

        public double getBusyTime() {
            return this.busyTime;
        }

        public double getTotal() {
            return this.totalTime;
        }

        public long getServed() {
            return this.served;
        }

        public double getUtilization() {
            return this.totalTime != 0
                    ? this.busyTime / this.totalTime
                    : 0;
        }


        public void addBusyTime(double time) {
            this.busyTime += time;
            this.totalTime += time;
        }

        public void addWaitTime(double time) {
            this.totalTime += time;
        }

        public void addProcessed() {
            this.served += 1;
        }

        public String getName() {
            return Device.this.name;
        }

        public String toString() {
            String format = "%s:{busy_time=%.2f, total_time=%.2f, utilization=%.2f, served=%d}";

            return String.format(
                    format,
                    this.getName(),
                    this.busyTime,
                    this.totalTime,
                    this.getUtilization(),
                    this.served);
        }
    }
}
