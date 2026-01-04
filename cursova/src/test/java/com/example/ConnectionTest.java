package com.example;

import com.example.modeling.Connection;
import com.example.modeling.SingleChannelSMO;
import com.example.modeling.utils.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionTest {

    private Connection connection;
    private Connection groupConnection;

    @BeforeEach
    void setUp() {
        connection = new Connection();
        groupConnection = new Connection(2);
    }

    private SingleChannelSMO createSimpleSMO(String name, int queueSize) {
        return new SingleChannelSMO("SMO", queueSize, () -> 1.0, 0);
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Connection(0), "Group size must be > 0");
        assertThrows(IllegalArgumentException.class, () -> new Connection(-5));

        assertThrows(IllegalArgumentException.class, () -> connection.addNext(null));
        assertThrows(IllegalArgumentException.class, () -> connection.addNext(null, () -> true));
    }

    @Test
    void testStateEmptyNext() {
        assertEquals(Status.READY, connection.getStatus());
        assertEquals(Status.READY, groupConnection.getStatus());

        connection.push();

        groupConnection.push();
        groupConnection.push();

        assertEquals(1, connection.getOutputCount());
        assertEquals(Status.READY, connection.getStatus());

        assertEquals(1, groupConnection.getOutputCount());
        assertEquals(Status.READY, groupConnection.getStatus());
    }

    @Test
    void testState() {
        SingleChannelSMO target1 = createSimpleSMO("Target1", 0);
        SingleChannelSMO target2 = createSimpleSMO("Target2", 0);

        connection.addNext(target1);
        groupConnection.addNext(target2);
        
        assertEquals(Status.READY, connection.getStatus());
        assertEquals(Status.READY, groupConnection.getStatus());

        connection.push();

        groupConnection.push();
        groupConnection.push();

        assertEquals(1, connection.getOutputCount());
        assertEquals(Status.BUSY, target1.getStatus());
        assertEquals(Status.BUSY, connection.getStatus());

        assertThrows(IllegalStateException.class, () -> connection.push());

        assertEquals(1, groupConnection.getOutputCount());
        assertEquals(Status.BUSY, target2.getStatus());
        assertEquals(Status.BUSY, groupConnection.getStatus());
        
        assertThrows(IllegalStateException.class, () -> groupConnection.push());
    }

    @Test
    void testIntegrationChain() {
        SingleChannelSMO source = createSimpleSMO("Source", 0);
        SingleChannelSMO sink = createSimpleSMO("Sink", 0);
        
        source.setNext(connection);
        connection.addNext(sink);

        source.process(); 

        assertEquals(1, source.getStats().getRequests());
        assertEquals(0, sink.getStats().getRequests());

        var t = source.getNextT();

        source.setCurrT(t);
        sink.setCurrT(t);

        sink.processEvent();
        source.processEvent();

        assertEquals(Status.READY, source.getStatus());
        assertEquals(1, sink.getStats().getRequests());
        assertEquals(Status.BUSY, sink.getStatus());
    }

    @Test
    void testIntegrationRouting() {
        SingleChannelSMO source = createSimpleSMO("Source", 0);
        
        SingleChannelSMO targetA = createSimpleSMO("TargetA", 0);
        SingleChannelSMO targetB = createSimpleSMO("TargetB", 0);

        source.setNext(connection);

        connection.addNext(targetA, () -> false);
        connection.addNext(targetB, () -> true); 

        source.process();
        
        var t = source.getNextT();

        source.setCurrT(t);
        targetA.setCurrT(t);
        targetB.setCurrT(t);

        targetA.processEvent();
        targetB.processEvent();
        source.processEvent();

        assertEquals(1, source.getStats().getRequests());
        assertEquals(0, targetA.getStats().getRequests());
        assertEquals(1, targetB.getStats().getRequests());
    }
}
