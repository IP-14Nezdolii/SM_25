package com.example;

import com.example.modeling.Producer;
import com.example.modeling.utils.State;
import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ProducerTest {

    private Producer producer;
    
    private final Supplier<Double> fixedTimeGenerator = () -> 5.0;

    @BeforeEach
    void setUp() {
        this.producer = new Producer("Producer", fixedTimeGenerator, 0);
    }

    @Test
    void testInitialStatus() {
        assertEquals(State.BUSY,  this.producer.getState());
        assertEquals(1,  this.producer.getStats().getRequests());
    }

    @Test
    void testRunSimulation() {
        this.producer.recordStats(Decimal6f.valueOf(5.0));
        this.producer.setCurrT(Decimal6f.valueOf(5.0));
        

        assertEquals(1, this.producer.getStats().getServed());
        assertEquals(5.0, this.producer.getStats().getTotalTime());
        this.producer.processEvent();

        this.producer.recordStats(Decimal6f.valueOf(5.0));
        this.producer.setCurrT(Decimal6f.valueOf(10.0));
        
        assertEquals(2, this.producer.getStats().getServed());
        assertEquals(10.0, this.producer.getStats().getTotalTime());
    }
}
