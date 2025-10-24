package com.example;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.modeling.Connection;
import com.example.modeling.Producer;
import com.example.modeling.device.Device;
import com.example.utils.DeviceRand.NextPriority;

/**
 * Unit test for simple App.
 */
public class ProducerTest 
{
    @Test
    public void deviceTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 10.0), "Producer");
        var conn = new Connection(NextPriority.Priority, "Connection");

        producer.addNext(conn, 1);

        producer.run(1000.0);

        var st1 = (Device.Stats)producer.getStats(); 
        var st2 = (Connection.Stats)conn.getStats(); 

        assertTrue( st1.getProcessed() == st2.getRequestsNumber() );
    }
}
