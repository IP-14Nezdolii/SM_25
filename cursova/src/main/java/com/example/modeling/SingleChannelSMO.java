package com.example.modeling;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.State;

public class SingleChannelSMO {
    protected final Stats stats;
    private final String name;

    private final int eventProcessPriority;
    private final int maxQueueSize;
    private int queueSize;

    final Supplier<Double> delay;
    Decimal6f currT = Decimal6f.ZERO;
    Decimal6f nextT = Decimal6f.MAX_VALUE;

    State channelState = State.READY;

    protected Optional<Connection> next = Optional.empty();
    protected boolean selfCheck = false;

    public SingleChannelSMO(String name, int maxQueueSize, Supplier<Double> rand, int eventProcessPriority) {
        this.name = name;
        this.maxQueueSize = maxQueueSize;
        this.queueSize = 0;

        this.eventProcessPriority = eventProcessPriority;

        if (maxQueueSize < 0) {
            throw new IllegalArgumentException("Max queue size must be non-negative");
        }

        this.delay = rand;
        this.stats = new Stats();
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
        if (this.channelState.isBusy() && 
            this.currT.isEqualTo(this.nextT)) {

            this.setDoneStatus();
            this.stats.addServed();
        }
    }

    /*
     * returns the status that determines the choice of process() call
     */
    public State getState() {
        // checks if the object is calling process() on itself
        if (this.selfCheck) {
            return State.READY;
        }

        if (this.maxQueueSize > this.queueSize) {
            return State.READY;
        }

        return this.channelState;
    }

    public State getChannelState() {
        return this.channelState;
    }

    public void setDoneStatus() {
        this.channelState = State.DONE;
        this.nextT = Decimal6f.MAX_VALUE;

    }

    /*
     * if this.channelStatus is not READY, enqueue
     */
    public void process() {
        if (this.channelState.isReady()) {
            this.nextT = currT.add(Decimal6f.valueOf(this.delay.get()));
            this.channelState = State.BUSY;
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
        switch (this.channelState) {
            case BUSY: return;
            case DONE:
                if (this.next.isPresent()) {
                    var next = this.next.get();

                    // if the object is calling push() on itself 
                    // getState() returns READY
                    this.selfCheck = true;

                    if (next.getState().isReady()) {
                        this.nextT = Decimal6f.MAX_VALUE;
                        this.channelState = State.READY;

                        next.push();
                    } else {
                        break;
                    }
                } else {
                    this.nextT = Decimal6f.MAX_VALUE;
                    this.channelState = State.READY;
                }
            case READY:
                if (this.queueSize > 0) {
                    this.queueSize -= 1;

                    this.nextT = currT.add(Decimal6f.valueOf(this.delay.get()));
                    this.channelState = State.BUSY;
                }
        }

        this.selfCheck = false;
    }

    public void recordStats(Decimal6f deltaT) {
        switch (this.channelState) {
            case READY -> this.stats.addDeviceWaitTime(deltaT);
            case DONE -> this.stats.addDeviceBlockTime(deltaT); 
            case BUSY -> this.stats.addDeviceBusyTime(deltaT);
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

        private double waitTime = 0;
        private double totalSimTime = 0;

        private long requests = 0;
        private long served = 0;

        public void clear() {
            this.busyTime = 0;
            this.blockTime = 0;

            this.waitTime = 0;
            this.totalSimTime = 0;

            this.requests = 0;
            this.served = 0;
        }

        // Device stats

        public void addDeviceBusyTime(Decimal6f deltaT) {
            double t = deltaT.doubleValue();

            this.busyTime += t;
            this.totalSimTime += t;
        }

        public void addDeviceBlockTime(Decimal6f deltaT) {  
            double t = deltaT.doubleValue();

            this.blockTime += t;
            this.totalSimTime += t;
        }

        public void addDeviceWaitTime(Decimal6f deltaT) {
            this.totalSimTime += deltaT.doubleValue();
        }

        public void addServed() {
            this.served += 1;
        }

        public long getServed() {
            return this.served;
        }

        public double getBlockTime() {
            return this.blockTime;
        }

        public double getBusyTime() {
            return this.busyTime;
        }

        public double getTotalSimTime() {
            return this.totalSimTime;
        }

        // Queue stats

        public void recordQSize(Decimal6f deltaT) {
            this.waitTime += deltaT.doubleValue() * SingleChannelSMO.this.queueSize;
        }

        public double getAverageWaitTime() {
            return this.served != 0
                    ? this.waitTime / this.served
                    : 0;
        }

        public double getAverageQueueSize() {
            return this.totalSimTime != 0
                    ? this.waitTime / this.totalSimTime
                    : 0;
        }

        public long getRequests() {
            return this.requests;
        }

        public void addRequest() {
            this.requests += 1;
        }

        public double getWaitTime() {
            return this.waitTime;
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
                format.append("avg_wait_time=%.3f, avg_queue_size=%.3f, ");
                args.add(this.getAverageWaitTime());
                args.add(this.getAverageQueueSize());
            }

            format.append("Device:{busy_time=%.3f, block_time=%.3f, total_time=%.3f}}");
            args.add(this.busyTime);
            args.add(this.blockTime);
            args.add(this.totalSimTime);

            return String.format(format.toString(), args.toArray());
        }
    }
}
