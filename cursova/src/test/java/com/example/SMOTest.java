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
    private Decimal6f nextT = Decimal6f.ZERO;
    
    private final Supplier<Double> fixedTimeGenerator = () -> 5.0;

    @BeforeEach
    void setUp() {
        this.smoWithQueue = new SingleChannelSMO("TestSMO1", 2, fixedTimeGenerator, 0);
        this.smo = new SingleChannelSMO("TestSMO2", fixedTimeGenerator, 0);
    }

    @Test
    void testInitialStatus() {
        assertEquals(State.READY, this.smoWithQueue.getState());
        assertEquals(0, this.smoWithQueue.getStats().getRequests());

        assertEquals(State.READY, this.smo.getState());
        assertEquals(0, this.smo.getStats().getRequests());
    }

    @Test
    void testProcessWithReadyDevice() {
        this.smoWithQueue.process();
        this.smo.process();

        assertEquals(1, this.smoWithQueue.getStats().getRequests());
        assertEquals(1, this.smo.getStats().getRequests());
        
        assertEquals(State.BUSY, this.smoWithQueue.getChannelState());
        assertEquals(State.BUSY, this.smo.getChannelState());

        assertEquals(State.READY, this.smoWithQueue.getState());
        assertEquals(State.BUSY, this.smo.getState());
    }

    @Test
    void testProcessOverflow() {
        this.smoWithQueue.process();
        this.smoWithQueue.process();
        this.smoWithQueue.process();

        this.smo.process();

        assertEquals(3, this.smoWithQueue.getStats().getRequests());
        assertEquals(1, this.smo.getStats().getRequests());

        assertThrows(
            IllegalStateException.class, 
            () -> this.smoWithQueue.process(), 
            "Should throw if SMO is BUSY (full)"
        );
        
        assertThrows(
            IllegalStateException.class, 
            () -> this.smo.process(), 
            "Should throw if SMO is BUSY (full)"
        );
    }

    @Test
    void testRunSimulation() {
        this.smoWithQueue.process();
        this.smo.process();

        Decimal6f last = Decimal6f.ZERO;
        this.nextT = this.smo.getNextT();

        this.smoWithQueue.recordStats(this.nextT.subtract(last));
        this.smo.recordStats(this.nextT.subtract(last));

        last = this.nextT;

        this.smoWithQueue.setCurrT(this.nextT);
        this.smo.setCurrT(this.nextT);

        this.smoWithQueue.processEvent();
        this.smo.processEvent();

        assertEquals(1, this.smoWithQueue.getStats().getServed());
        assertEquals(5.0, this.smoWithQueue.getStats().getTotalSimTime());

        assertEquals(1, this.smo.getStats().getServed());
        assertEquals(5.0, this.smo.getStats().getTotalSimTime());


        this.smoWithQueue.process();
        this.smo.process();

        this.nextT = this.smo.getNextT();

        this.smoWithQueue.recordStats(this.nextT.subtract(last));
        this.smo.recordStats(this.nextT.subtract(last));

        this.smoWithQueue.setCurrT(this.nextT);
        this.smo.setCurrT(this.nextT);

        assertEquals(2, this.smoWithQueue.getStats().getServed());
        assertEquals(10.0, this.smoWithQueue.getStats().getTotalSimTime());

        assertEquals(2, this.smo.getStats().getServed());
        assertEquals(10.0, this.smo.getStats().getTotalSimTime());
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

        smo1.setCurrT(Decimal6f.ZERO);
        smo2.setCurrT(Decimal6f.ZERO);

        smo2.processEvent();
        smo1.processEvent();

        assertEquals(1, smo2.getStats().getRequests());

        smo1.setCurrT(Decimal6f.ZERO);
        smo2.setCurrT(Decimal6f.ZERO);

        smo2.processEvent();
        smo1.processEvent();

        assertEquals(2, smo1.getStats().getServed());
        assertEquals(1, smo2.getStats().getRequests());
        
        smo1.setCurrT(Decimal6f.valueOf(5.0));  
        smo2.setCurrT(Decimal6f.valueOf(5.0)); 

        smo2.processEvent();
        smo1.processEvent();

        assertEquals(2, smo2.getStats().getRequests());
    }
}