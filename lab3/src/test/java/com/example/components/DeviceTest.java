package com.example.components;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.modeling.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.components.device.Device;

public class DeviceTest 
{
    @Test
    public void deviceTest()
    {
        var device = new Device(Model.getUniform(1.0, 100.0), "Device");

        for (int i = 0; i < 1_000_000; i++) {
            device.process();
            device.run(device.getWorkTime().get());
        }

        System.out.println(device.getStats());
    }

    @Test
    public void compDeviceTest()
    {
        var device = new CompDevice(Model.getUniform(1.0, 100.0), "Device");

        for (int i = 0; i < 1_000_000; i++) {
            assertTrue( device.process() );
            device.run(device.getWorkTime().get());
        }

        System.out.println(device.getStats());
    }

    @Test
    public void producerTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 10.0), "Producer");
        var conn = new Connection(new Model.Priority(), "Connection");

        producer.setNext(conn);

        producer.run(1000.0);

        var st1 = producer.getStats(); 
        var st2 = conn.getStats(); 

        assertTrue( st1.getProcessed() == st2.getRequestsNumber() );
    }
}
