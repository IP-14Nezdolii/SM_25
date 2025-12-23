package com.example.modeling;
import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.utils.State;

public class Producer extends SingleChannelSMO {

    public Producer(String name, Supplier<Double> rand, int eventProcessPriority) {
        super(name, 0, rand, eventProcessPriority);
        super.process();
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Producer cannot process incoming tasks");
    }

    @Override
    public void eventProcess() {
        switch (this.channelState) {
            case BUSY:
                break;
            case DONE:
                this.next.ifPresent((next) -> next.push());
                this.nextT = Decimal6f.MAX_VALUE;
                this.channelState = State.READY;
            case READY:
                super.process();
        }
    }
}
