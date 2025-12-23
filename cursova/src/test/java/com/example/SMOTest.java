package com.example;

import com.example.modeling.Connection;
import com.example.modeling.SingleChannelSMO;
import com.example.modeling.utils.State;
import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SMOTest {
    private SingleChannelSMO smoWithQueue;
    private SingleChannelSMO smo;
    
    private final Supplier<Double> fixedTimeGenerator = () -> 5.0;

    @BeforeEach
    void setUp() {
        this.smoWithQueue = new SingleChannelSMO("TestSMO1", 2, fixedTimeGenerator, 0);
        this.smo = new SingleChannelSMO("TestSMO2", fixedTimeGenerator, 0);
    }

    @Test
    void testInitialStatus() {
        assertEquals(State.READY, smoWithQueue.getState());
        assertEquals(0, smoWithQueue.getStats().getRequests());

        assertEquals(State.READY, smo.getState());
        assertEquals(0, smo.getStats().getRequests());
    }

    @Test
    void testProcessWithReadyDevice() {
        smoWithQueue.process();
        smo.process();

        assertEquals(1, smoWithQueue.getStats().getRequests());
        assertEquals(1, smo.getStats().getRequests());
        
        assertEquals(State.BUSY, smoWithQueue.getChannelState());
        assertEquals(State.BUSY, smo.getChannelState());

        assertEquals(State.READY, smoWithQueue.getState());
        assertEquals(State.BUSY, smo.getState());
    }

    @Test
    void testProcessOverflow() {
        smoWithQueue.process();
        smoWithQueue.process();
        smoWithQueue.process();

        smo.process();

        assertEquals(3, smoWithQueue.getStats().getRequests());
        assertEquals(1, smo.getStats().getRequests());

        assertThrows(
            IllegalStateException.class, 
            () -> smoWithQueue.process(), 
            "Should throw if SMO is BUSY (full)"
        );
        
        assertThrows(
            IllegalStateException.class, 
            () -> smo.process(), 
            "Should throw if SMO is BUSY (full)"
        );
    }

    @Test
    void testRunSimulation() {
        smoWithQueue.process();
        smo.process();

        smoWithQueue.recordStats(Decimal6f.valueOf(5.0));
        smo.recordStats(Decimal6f.valueOf(5.0));

        smoWithQueue.setCurrT(Decimal6f.valueOf(5.0));
        smo.setCurrT(Decimal6f.valueOf(5.0));

        assertEquals(1, smoWithQueue.getStats().getServed());
        assertEquals(5.0, smoWithQueue.getStats().getTotalTime());

        assertEquals(1, smo.getStats().getServed());
        assertEquals(5.0, smo.getStats().getTotalTime());

        smoWithQueue.eventProcess();
        smo.eventProcess();

        smoWithQueue.recordStats(Decimal6f.valueOf(5.0));
        smo.recordStats(Decimal6f.valueOf(5.0));

        smoWithQueue.setCurrT(Decimal6f.valueOf(10.0));
        smo.setCurrT(Decimal6f.valueOf(10.0));

        assertEquals(1, smoWithQueue.getStats().getServed());
        assertEquals(10.0, smoWithQueue.getStats().getTotalTime());

        assertEquals(1, smo.getStats().getServed());
        assertEquals(10.0, smo.getStats().getTotalTime());
    }

    @Test
    void testZero() {
		SingleChannelSMO smo1 = new SingleChannelSMO(
			"Zero",1000,() -> 0.0,2);
		SingleChannelSMO smo2 = new SingleChannelSMO(
			"SMO",() -> 5.0,3);

        Connection connection = new Connection();

        smo1.setNext(connection);
        connection.addNext(smo2);

        smo1.process();
        smo1.process();
        smo1.process();

        smo2.eventProcess();
        smo1.eventProcess();

        assertEquals(1, smo2.getStats().getRequests());

        smo1.setCurrT(Decimal6f.valueOf(5.0));  
        smo2.setCurrT(Decimal6f.valueOf(5.0)); 

        smo2.eventProcess();
        smo1.eventProcess();

        assertEquals(2, smo2.getStats().getRequests());
    }
}