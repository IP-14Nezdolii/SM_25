package com.example.modeling;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.Status;

public class SingleChannelSMO {
    protected final Stats stats = new Stats();
    private final String name;

    private final int eventProcessPriority;
    private final int maxQueueSize;
    private int queueSize = 0;

    final Supplier<Double> delay;
    Decimal6f currT = Decimal6f.ZERO;
    Decimal6f nextT = Decimal6f.MAX_VALUE;

    Status channelStatus = Status.READY;

    protected Optional<Connection> next = Optional.empty();
    private boolean selfCheck = false;

    public SingleChannelSMO(String name, int maxQueueSize, Supplier<Double> rand, int eventProcessPriority) {
        if (maxQueueSize < 0) {
            throw new IllegalArgumentException("Max queue size must be non-negative");
        }

        this.name = name;
        this.maxQueueSize = maxQueueSize;
        this.eventProcessPriority = eventProcessPriority;
        this.delay = rand;
    }

    public SingleChannelSMO(String name, Supplier<Double> delay, int eventProcessPriority) {
        this(name, 0, delay, eventProcessPriority);
    }

    public Decimal6f getNextT() {
        return this.nextT;
    }

    public void setCurrT(Decimal6f currT) {
        if (currT.isGreaterThan(this.nextT)) {
            throw new IllegalArgumentException(
                    "Value currT is greater than this.nextT. Time currT: " + currT +
                            ", Time this.nextT: " + this.nextT);
        }

        if (currT.isLessThan(this.currT)) {
            throw new IllegalArgumentException(
                    "Value currT is less than this.currT. Time this.currT: " + currT +
                            ", Time this.currT: " + this.currT);
        }
        
        this.currT = currT;
        if (this.channelStatus.isBusy() && 
            this.currT.isEqualTo(this.nextT)) {

            this.setDoneStatus();
            this.stats.addServed();
        }
    }

    /*
     * returns the status that determines the choice of process() call
     */
    public Status getStatus() {
        // checks if the object is calling process() on itself
        if (this.selfCheck) {
            return Status.READY;
        }

        if (this.maxQueueSize > this.queueSize) {
            return Status.READY;
        }

        return this.channelStatus;
    }

    public Status getChannelStatus() {
        return this.channelStatus;
    }

    public void setDoneStatus() {
        this.channelStatus = Status.DONE;
        this.nextT = Decimal6f.MAX_VALUE;
    }

    /*
     * if this.channelStatus is not READY, enqueue
     */
    public void process() {
        if (this.channelStatus.isReady()) {
            this.nextT = currT.add(Decimal6f.valueOf(this.delay.get()));
            this.channelStatus = Status.BUSY;
        } else {
            if (this.maxQueueSize > this.queueSize) {
                this.queueSize += 1;
            } else {
                throw new IllegalStateException("SMO is not READY");
            }
        }

        this.stats.addRequest();
    }

    public void processEvent() {
        switch (this.channelStatus) {
            case BUSY: return;
            case DONE:
                if (this.next.isPresent()) {
                    var next = this.next.get();

                    // if the object is calling push() on itself 
                    // getStatus() returns READY
                    this.selfCheck = true;

                    if (next.getStatus().isReady()) {
                        this.nextT = Decimal6f.MAX_VALUE;
                        this.channelStatus = Status.READY;

                        next.push();
                    } else {
                        break;
                    }
                } else {
                    this.nextT = Decimal6f.MAX_VALUE;
                    this.channelStatus = Status.READY;
                }
            case READY:
                if (this.queueSize > 0) {
                    this.queueSize -= 1;

                    this.nextT = currT.add(Decimal6f.valueOf(this.delay.get()));
                    this.channelStatus = Status.BUSY;
                }
        }

        this.selfCheck = false;
    }

    public void recordStats(Decimal6f deltaT) {
        switch (this.channelStatus) {
            case READY -> this.stats.addReadyTime(deltaT);
            case DONE -> this.stats.addBlockTime(deltaT); 
            case BUSY -> this.stats.addBusyTime(deltaT);
        }

        this.stats.recordQSize(deltaT);
    }

    public Stats getStats() {
        return this.stats;
    };

    public int getEventProcessPriority() {
        return this.eventProcessPriority;
    }

    public void setNext(Connection next) {
        this.next = Optional.of(next);
    }

    public class Stats {
        private double busyTime = 0;
        private double blockTime = 0;
        private double readyTime = 0;

        private double waitQTime = 0;
        private int maxQSize = 0;

        private int requests = 0;
        private int served = 0;

        public void clear() {
            this.busyTime = 0;
            this.blockTime = 0;
            this.readyTime = 0;

            this.waitQTime = 0;
            this.maxQSize = 0;
            
            this.requests = 0;
            this.served = 0;
        }

        // Device stats

        public void addBusyTime(Decimal6f deltaT) {
            this.busyTime += deltaT.doubleValue();
        }

        public void addBlockTime(Decimal6f deltaT) {  
            this.blockTime += deltaT.doubleValue();
        }

        public void addReadyTime(Decimal6f deltaT) {
            this.readyTime += deltaT.doubleValue();
        }

        public void addServed() {
            this.served += 1;
        }

        public int getServed() {
            return this.served;
        }

        public double getBlockTime() {
            return this.blockTime;
        }

        public double getBusyTime() {
            return this.busyTime;
        }

        public double getTotalSimTime() {
            return this.busyTime + this.blockTime + this.readyTime;
        }

        public int getRequests() {
            return this.requests;
        }

        public void addRequest() {
            this.requests += 1;
        }

        // Queue stats

        public void recordQSize(Decimal6f deltaT) {
            this.waitQTime += deltaT.doubleValue() * SingleChannelSMO.this.queueSize;
            this.maxQSize = Math.max(this.maxQSize, SingleChannelSMO.this.queueSize);
        }

        public int getMaxQSize() {
            return this.maxQSize;
        }

        public double getAverageWaitTime() {
            return this.served != 0
                    ? this.waitQTime / this.served
                    : 0;
        }

        public double getAverageQSize() {
            double totalSimTime = this.getTotalSimTime();
            return totalSimTime != 0
                    ? this.waitQTime / totalSimTime
                    : 0;
        }

        public double getWaitQTime() {
            return this.waitQTime;
        }

        @Override
        public String toString() {
            var format = new StringBuilder();
            ArrayList<Object> args = new ArrayList<>();

            format.append("%s:{requests=%d, served=%d, ");
            args.add(SingleChannelSMO.this.name);
            args.add(this.requests);
            args.add(this.served);

            if (SingleChannelSMO.this.maxQueueSize != 0) {
                format.append("avg_wait_time=%.3f, avg_queue_size=%.3f, max_queue_size=%d, ");
                args.add(this.getAverageWaitTime());
                args.add(this.getAverageQSize());
                args.add(this.maxQSize);
            }

            format.append("Device:{busy_time=%.3f, block_time=%.3f, total_time=%.3f}}");
            args.add(this.busyTime);
            args.add(this.blockTime);
            args.add(this.getTotalSimTime());

            return String.format(format.toString(), args.toArray());
        }
    }
}
