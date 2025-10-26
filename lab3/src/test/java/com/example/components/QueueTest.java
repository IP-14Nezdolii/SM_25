package com.example.components;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Queue;

public class QueueTest 
{
    @Test
    public void queueWithDeviceTest()
    {
        var queue = new Queue("Queue");
        var device = new CompDevice(Model.getUniform(1.0, 5.0), "Device");

        queue.setNext(device);

        assertTrue( queue.process() );
        assertTrue( queue.process() );

        assertTrue( device.getWorkTime().isPresent() );
        assertTrue( queue.process() );

        double time = device.getWorkTime().get();
        device.run(time);
        queue.run(time);

        assertTrue( "queue: "+ queue.getSize(), queue.getSize() == 1);
        assertTrue( device.getWorkTime().isPresent() );
    }

    @Test
    public void queueWithQueueWithDeviceTest()
    {
        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");
        var device = new CompDevice(Model.getUniform(1.0, 5.0), "Device");

        queue1.setNext(queue2);
        queue2.setNext(device);

        assertTrue( queue1.process() );
        assertTrue( queue1.process() );

        assertTrue( device.getWorkTime().isPresent() );
        assertTrue( queue1.process() );

        double time = device.getWorkTime().get();
        device.run(time);
        queue2.run(time);
        queue1.run(time);

        assertTrue( "queue1: "+ queue1.getSize(), queue1.getSize() == 0);
        assertTrue( "queue2: "+ queue2.getSize(), queue2.getSize() == 1);
        assertTrue( device.getWorkTime().isPresent() );
    }
}
