package com.example;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Component;

public class CompDeviceWithCooldown implements Component {
    private final Stats stats = new Stats();
    private final String name;

    private final CompDevice device;
    private final CompDevice cooldown;

    public CompDeviceWithCooldown(
            Supplier<Double> rand,
            Supplier<Double> cooldownRand,
            String name) {
        this.name = name;
        this.device = new CompDevice(rand, name + "_device");
        this.cooldown = new CompDevice(cooldownRand, name + "_cooldown_timer");
    }

    @Override
    public List<Component> getAllNext() {
        return device.getAllNext();
    }

    public void setNext(Component next) {
        this.device.setNext(next);
    }

    @Override
    public Optional<Component> getNextChosen() {
        return device.getNextChosen();
    }

    @Override
    public Stats getStats() {
        return this.stats;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<Decimal6f> getLeftTime() {
        if (this.device.getLeftTime().isPresent()) {
            return Optional.of(this.device.getLeftTime().get());
        }

        if (this.cooldown.getLeftTime().isPresent()) {
            return Optional.of(this.cooldown.getLeftTime().get());
        }

        return Optional.empty();
    }

    @Override
    public void run(Decimal6f time) {
        if (this.device.getLeftTime().isPresent()) {
            Decimal6f td = this.device.getLeftTime().get();

            this.device.run(time);
            this.stats.addWorkTime(time.doubleValue());

            if (td.isEqualTo(time)) {
                this.cooldown.process();
            }

        } else if (this.cooldown.getLeftTime().isPresent()) {
            this.cooldown.run(time);
            this.stats.addCooldownTime(time.doubleValue());

        } else {
            this.stats.addWaitTime(time.doubleValue());
        }
    }

    @Override
    public boolean process() {
        if (this.cooldown.getLeftTime().isPresent()) {
            throw new IllegalStateException(
                "Cooldown is busy. Required time left: " + this.cooldown.getLeftTime().get());
        }

        this.device.process();
        return true;
    }

    public class Stats implements ComponentStats {
        private double workTime = 0;
        private double cooldownTime = 0;
        private double totalTime = 0;

        @Override
        public void clear() {
            this.workTime = 0;
            this.cooldownTime = 0;
            this.totalTime = 0;
            CompDeviceWithCooldown.this.device.getStats().clear();
            CompDeviceWithCooldown.this.cooldown.getStats().clear();
        }

        public double getBusyTime() {
            return this.workTime;
        }

        public double getCooldownTime() {
            return this.cooldownTime;
        }

        public double getTotal() {
            return this.totalTime;
        }

        public long getServed() {
            return CompDeviceWithCooldown.this.device.getStats().getServed();
        }

        public double getUtilization() {
            return this.totalTime != 0
                    ? (this.workTime + this.cooldownTime) / this.totalTime
                    : 0;
        }

        public void addWorkTime(double time) {
            this.workTime += time;
            this.totalTime += time;
        }

        public void addWaitTime(double time) {
            this.totalTime += time;
        }

        public void addCooldownTime(double time) {
            this.cooldownTime += time;
            this.totalTime += time;
        }

        public String getName() {
            return CompDeviceWithCooldown.this.name;
        }

        public String toString() {
            String format = "%s:{busy_time=%.2f, cooldown_time=%.2f, total_time=%.2f, utilization=%.2f, served=%d}";

            return String.format(
                    format,
                    this.getName(),
                    this.workTime,
                    this.cooldownTime,
                    this.totalTime,
                    this.getUtilization(),
                    this.getServed());
        }
    }
}
