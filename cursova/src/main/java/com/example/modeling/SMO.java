package com.example.modeling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.Status;

public class SMO {
    protected final Stats stats;
    private final String name;

    private final int eventPriority;
    private final int maxQueueSize;
    private int queueSize;

    protected ArrayDeque<Device> readyDevices = new ArrayDeque<>();
    protected ArrayDeque<Device> busyDevices = new ArrayDeque<>();
    protected ArrayDeque<Device> doneDevices = new ArrayDeque<>();

    protected Optional<Connection> next = Optional.empty();
    protected boolean selfCheck = false;

    public SMO(
        String name,
        int maxQueueSize, 
        List<Device> devices, 
        int priority
    ) {
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

        for (Device device : devices) {
            switch (device.getStatus()) {
                case READY -> this.readyDevices.add(device);
                case BUSY -> this.busyDevices.add(device);
                case DONE -> this.doneDevices.add(device);
            }
        }

        this.stats = new Stats(devices);
    }

    public SMO(
        String name,
        List<Device> devices, 
        int priority
    ) {
        this.name = name;
        this.maxQueueSize = 0;
        this.queueSize = 0;

        this.eventPriority = priority;

        if (devices == null) {
            throw new IllegalArgumentException("Devices list must be not null");
        }

        if (devices.isEmpty()) {
            throw new IllegalArgumentException("Devices list must be not empty");
        }

        for (Device device : devices) {
            switch (device.getStatus()) {
                case READY -> this.readyDevices.add(device);
                case BUSY -> this.busyDevices.add(device);
                case DONE -> this.doneDevices.add(device);
            }
        }

        this.stats = new Stats(devices);
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

        if (this.readyDevices.isEmpty() == false) {
            return Status.READY;
        }

        if (this.maxQueueSize > this.queueSize) {
            return Status.READY;
        }

        return Status.BUSY;
    }

    public Decimal6f getLeftTime() {
        return this.busyDevices.stream()
            .map(Device::getLeftTime)
            .min(Decimal6f::compareTo)
            .orElse(Decimal6f.MAX_VALUE);
    }

    public void process() {
        this.stats.addRequest();

        // no ready devices, but there is space in the queue
        if (this.maxQueueSize > this.queueSize && this.readyDevices.isEmpty()) {
            this.queueSize += 1;
            // there are ready devices
        } else if (this.readyDevices.isEmpty() == false) {
            this.processReadyDevice();
        } else {
            throw new IllegalStateException("SMO is busy");
        }
    }

    private void processReadyDevice() {
        Device device = this.readyDevices.pop();
        device.process();

        switch (device.getStatus()) {
            case BUSY -> this.busyDevices.add(device);
            case DONE -> {
                this.stats.addServed();

                this.next.ifPresentOrElse((next) -> {

                    if (next.getStatus() == Status.READY) {
                        next.process();
                        device.setStatus(Status.READY);
                        this.readyDevices.add(device);
                    } else {
                        this.doneDevices.add(device);
                    }
                }, () -> {
                    device.setStatus(Status.READY);
                    this.readyDevices.add(device);
                });
            }
            case READY -> throw new IllegalStateException("Device is still ready after processing");
        }
    }

    public void handleEvents() {
        while (true) {

            while (readyDevices.isEmpty() == false && this.queueSize > 0) {
                this.queueSize -= 1;
                this.processReadyDevice();
            }

            this.next.ifPresentOrElse((next) -> {

                this.selfCheck = true;

                while (this.doneDevices.isEmpty() == false && next.getStatus() == Status.READY) {
                    Device device = this.doneDevices.pop();
                    device.setStatus(Status.READY);
                    this.readyDevices.add(device);

                    next.process();
                }

                this.selfCheck = false;
            }, () -> {
                this.doneDevices.forEach(device -> device.setStatus(Status.READY));
                this.readyDevices.addAll(this.doneDevices);
                this.doneDevices.clear();
            });

            if (this.readyDevices.isEmpty()) {
                break;
            } else if (this.queueSize == 0) {
                break;
            }
        }
    }

    public void run(Decimal6f time) {
        this.doneDevices.forEach(device -> device.wait(time));
        this.readyDevices.forEach(device -> device.wait(time));

        ArrayList<Device> newBusy = new ArrayList<>(this.busyDevices.size());

        while (this.busyDevices.isEmpty() == false) {
            Device device = this.busyDevices.pop();
            device.run(time);

            switch (device.getStatus()) {
                case BUSY -> newBusy.add(device);
                case DONE -> {
                    this.stats.addServed();
                    this.doneDevices.add(device);
                }
                case READY -> throw new IllegalStateException("Device is READY while being BUSY");
            }
        }
        this.busyDevices.addAll(newBusy);

        this.stats.record(time.doubleValue());
    }

    public class Stats {
        private final int maxQueueSize;
        private final ArrayList<Device.Stats> deviceStats = new ArrayList<>();

        private double totalWaitTime = 0;
        private long requests = 0;
        private long served = 0;
        private double totalTime = 0;

        Stats(List<Device> devices) {
            deviceStats.addAll(
                devices.stream().map(Device::getStats).toList()
            );

            this.maxQueueSize = SMO.this.maxQueueSize;
        }

        public ArrayList<Device.Stats> getDeviceStats() {
            return this.deviceStats;
        }

        public int getMaxQueueSize() {
            return this.maxQueueSize;
        }

        public void clear() {
            this.deviceStats.forEach(device -> device.clear());
            this.totalWaitTime = 0;

            this.totalTime = 0;
            this.served = 0;
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

            if (this.maxQueueSize == 0) {
                smoFormat = "%s:{requests=%d, served=%d}";

                smoStatsString = String.format(
                    smoFormat,
                    this.getName(),
                    this.requests,
                    this.served
                );
                
            } else {
                smoFormat = "%s:{requests=%d, served=%d, avg_wait_time=%.4f, avg_queue_size=%.4f}";
            
                smoStatsString = String.format(
                    smoFormat,
                    this.getName(),
                    this.requests,
                    this.served,
                    this.getAverageWaitTime(),
                    this.getAverageQueueSize()
                );
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
