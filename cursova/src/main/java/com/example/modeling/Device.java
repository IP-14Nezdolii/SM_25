package com.example.modeling;

import com.example.modeling.utils.Status;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

public class Device {
    final Stats stats;

    final Supplier<Double> rand;
    final String name;

    Decimal6f leftTime = Decimal6f.ZERO;
    Status status = Status.READY;

    public Device(Supplier<Double> rand, String name) {
        this.rand = rand;

        this.stats = new Stats();
        this.name = name;
    }

    public Status getStatus() {
        return this.status;
    }

    public Status setStatus(Status status) {
        return this.status = status;
    }

    public Decimal6f getLeftTime() {
        return switch (this.status) {
            case Status.BUSY -> this.leftTime;
            case Status.DONE, Status.READY -> Decimal6f.MAX_VALUE;
        };
    }; 

    /*
     * Throws exception if device is not BUSY
     */
    public boolean run(Decimal6f time) {
        return switch (this.status) {
            case Status.BUSY -> {
                if (time.isGreaterThan(this.leftTime)) {
                    throw new IllegalArgumentException(
                        "Time step is greater than left time. Time step: " + time + ", Left time: " + this.leftTime);
                }

                this.leftTime = this.leftTime.subtract(time);
                this.stats.addBusyTime(time.doubleValue());

                if (this.leftTime.isEqualTo(Decimal6f.ZERO)) {
                    this.status = Status.DONE;
                    this.stats.addServed();
                    yield true;
                } else {
                    yield false;
                }
            }
            case Status.DONE, Status.READY -> {
                throw new IllegalStateException("Device is not busy");
            }
        };
    }

    /*
     * Throws exception if device is BUSY
     */
    public void wait(Decimal6f time) {
        if (this.status == Status.BUSY) {
            throw new IllegalStateException(
                "Device is busy. Required time left: " + this.leftTime);
        } else {
            this.stats.addWaitTime(time.doubleValue());
        }
    }

    /*
     * Throws exception if device is not READY
     */
    public void process() {
        switch (this.status) {
            case Status.READY -> {
                this.leftTime = Decimal6f.valueOf(this.rand.get());

                if (this.leftTime.isGreaterThan(Decimal6f.ZERO)) {
                    this.status = Status.BUSY;
                } else {
                    this.status = Status.DONE;
                    this.stats.addServed();
                }
                
            }
            case Status.DONE, Status.BUSY -> {
                if (this.status != Status.READY) {
                    throw new IllegalStateException("Device is not READY");
                }
            }
        }
    }

    public Stats getStats() {
        return this.stats;
    };

    public class Stats {
        private double busyTime = 0;
        private double doneTime = 0;
        private double totalTime = 0;
        private long served = 0;

        public void clear() {
            this.busyTime = 0;
            this.doneTime = 0;
            this.totalTime = 0;
            this.served = 0;
        }

        public double getDoneTime() {
            return this.doneTime;
        }

        public double getBusyTime() {
            return this.busyTime;
        }

        public double getTotal() {
            return this.totalTime;
        }

        public long getServed() {
            return this.served;
        }

        public void addBusyTime(double time) {
            this.busyTime += time;
            this.totalTime += time;
        }

        public void addWaitTime(double time) {
            if (Device.this.getStatus() == Status.DONE) {
                this.doneTime += time;
            }

            this.totalTime += time;
        }

        public void addServed() {
            this.served += 1;
        }

        public String getName() {
            return Device.this.name;
        }

        public String toString() {
            String format = "%s:{busy_time=%.2f, total_time=%.2f, served=%d}";

            return String.format(
                    format,
                    this.getName(),
                    this.busyTime,
                    this.totalTime,
                    this.served
            );
        }
    }
}