package com.example.modeling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.Status;

public class SMO {
    protected final Stats stats;
    private final String name;

    private final int eventPriority;
    private final int maxQueueSize;
    private int queueSize;

    protected final ArrayDeque<Device> devices = new ArrayDeque<>();

    protected Optional<Connection> next = Optional.empty();
    protected boolean selfCheck = false;

    public SMO(
            String name,
            int maxQueueSize,
            List<Device> devices,
            int priority) {
        this.name = name;
        this.maxQueueSize = maxQueueSize;
        this.queueSize = 0;

        this.eventPriority = priority;

        if (maxQueueSize < 0) {
            throw new IllegalArgumentException("Max queue size must be non-negative");
        }

        if (devices == null) {
            throw new IllegalArgumentException("Devices list must be not null");
        }

        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Devices list must be not empty");
        }

        this.devices.addAll(devices);
        this.stats = new Stats(devices);
    }

    public SMO(
            String name,
            int maxQueueSize,
            Device device,
            int priority) {
        this(name, maxQueueSize, List.of(device), priority);
    }

    public SMO(
            String name,
            Device device,
            int priority) {
        this(name, 0, List.of(device), priority);
    }

    public SMO(
            String name,
            List<Device> devices,
            int priority) {
        this(name, 0, devices, priority);
    }

    public Stats getStats() {
        return this.stats;
    };

    public int getEventPriority() {
        return this.eventPriority;
    }

    public void setNext(Connection next) {
        this.next = Optional.of(next);
    }

    public Status getStatus() {
        if (this.selfCheck) {
        return Status.READY;
        }

        for (Device device : devices) {
            if (device.getStatus() == Status.READY) {
                return Status.READY;
            }
        }

        if (this.maxQueueSize > this.queueSize) {
            return Status.READY;
        }

        return Status.BUSY;
    }

    public Decimal6f getLeftTime() {
        return this.devices.stream()
                .map(Device::getLeftTime)
                .min(Decimal6f::compareTo)
                .orElseThrow();
    }

    public void process() {
        this.stats.addRequest();

        var readyDevices = getReadyDevices();

        if (readyDevices.isEmpty() && this.maxQueueSize > this.queueSize) {
            this.queueSize += 1;
            return;
        } else if (!readyDevices.isEmpty()) {
            this.processDevice(readyDevices);
        } else {
            throw new IllegalStateException("SMO is busy");
        }
    }

    protected List<Device> getReadyDevices() {
        return this.devices.stream()
                .filter(device -> device.getStatus() == Status.READY)
                .collect(Collectors.toList());
    }

    private void processDevice(List<Device> readyDevices) {
        Device device = readyDevices.removeFirst();
        device.process();

        switch (device.getStatus()) {
            case BUSY -> {
            }
            case DONE -> {
                this.stats.addServed();

                this.next.ifPresentOrElse((next) -> {

                    if (next.getStatus() == Status.READY) {
                        next.process();
                        device.setStatus(Status.READY);
                    }
                }, () -> {
                    device.setStatus(Status.READY);
                });
            }
            case READY -> throw new IllegalStateException("Device is still ready after processing");
        }
    }

    public void handleEvents() {
        var readyDevices = this.getReadyDevices();

        while (true) {
            
            while (readyDevices.isEmpty() == false && this.queueSize > 0) {
                this.queueSize -= 1;
                this.processDevice(readyDevices);
            }

            this.next.ifPresentOrElse((next) -> {
                this.selfCheck = true;

                for (Device device : this.devices) {
                    if (device.getStatus() == Status.DONE && next.getStatus() == Status.READY) {
                        device.setStatus(Status.READY);
                        readyDevices.add(device);

                        next.process();
                    }
                }

                this.selfCheck = false;
            }, () -> {
                for (Device device : this.devices) { 
                    if (device.getStatus() == Status.DONE) {
                        device.setStatus(Status.READY);
                        readyDevices.add(device);
                    }
                }
            });

            if (readyDevices.isEmpty()) {
                break;
            } else if (this.queueSize == 0) {
                break;
            }
        }
    }

    public void run(Decimal6f time) {
        for (Device device : this.devices) {
            if (device.getStatus() != Status.BUSY) {
                device.wait(time);
            } else {
                device.run(time);

                switch (device.getStatus()) {
                    case BUSY -> {}
                    case DONE -> this.stats.addServed();
                    case READY -> throw new IllegalStateException("Device is READY while being BUSY");
                }
            }
        }

        this.stats.record(time.doubleValue());
    }

    public class Stats {
        private final ArrayList<Device.Stats> deviceStats = new ArrayList<>();

        private double totalWaitTime = 0;
        private long requests = 0;
        private long served = 0;
        private double totalTime = 0;

        Stats(List<Device> devices) {
            deviceStats.addAll(
                    devices.stream().map(Device::getStats).toList());
        }

        public ArrayList<Device.Stats> getDeviceStats() {
            return this.deviceStats;
        }

        public void clear() {
            this.deviceStats.forEach(device -> device.clear());

            this.totalWaitTime = 0;
            this.requests = 0;
            this.served = 0;
            this.totalTime = 0;
        }

        public double getTotalTime() {
            return this.totalTime;
        }

        public long getServed() {
            return this.served;
        }

        public void addServed() {
            this.served += 1;
        }

        public long getRequests() {
            return this.requests;
        }

        public void addRequest() {
            this.requests += 1;
        }

        public String getName() {
            return SMO.this.name;
        }

        public void record(double time) {
            this.totalTime += time;
            this.totalWaitTime += time * SMO.this.queueSize;
        }

        public double getTotalWaitTime() {
            return this.totalWaitTime;
        }

        public double getAverageWaitTime() {
            return this.served != 0
                    ? this.totalWaitTime / this.served
                    : 0;
        }

        public double getAverageQueueSize() {
            return this.totalTime != 0
                    ? this.totalWaitTime / this.totalTime
                    : 0;
        }

        @Override
        public String toString() {
            String smoFormat;
            String smoStatsString;

            if (SMO.this.maxQueueSize == 0) {
                smoFormat = "%s:{requests=%d, served=%d}";

                smoStatsString = String.format(
                        smoFormat,
                        this.getName(),
                        this.requests,
                        this.served);

            } else {
                smoFormat = "%s:{requests=%d, served=%d, avg_wait_time=%.4f, avg_queue_size=%.4f}";

                smoStatsString = String.format(
                        smoFormat,
                        this.getName(),
                        this.requests,
                        this.served,
                        this.getAverageWaitTime(),
                        this.getAverageQueueSize());
            }

            StringBuilder devicesStats = new StringBuilder();

            devicesStats.append(" {");
            for (Device.Stats deviceStat : deviceStats) {
                devicesStats.append("\n     ").append(deviceStat.toString());
            }
            devicesStats.append("\n}");

            return smoStatsString + devicesStats.toString();
        }
    }
}
