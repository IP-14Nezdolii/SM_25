package com.example.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.Test;

import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Queue;
import com.example.modeling.utils.FunRand;

public class QueueTest 
{
    @Test
    public void queueWithDeviceTest()
    {
        var queue = new Queue("Queue");
        var device = new CompDevice(FunRand.getUniform(1.0, 5.0), "Device");

        queue.setNext(device);

        assertTrue( queue.process() );
        assertTrue( queue.process() );

        assertTrue( device.getLeftTime().isPresent() );
        assertTrue( queue.process() );

        Decimal6f time = device.getLeftTime().get();
        device.run(time);
        queue.run(time);

        assertTrue( queue.getSize() == 1);
        assertTrue( device.getLeftTime().isPresent() );
    }

    @Test
    public void queueWithQueueWithDeviceTest()
    {
        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");
        var device = new CompDevice(FunRand.getUniform(1.0, 5.0), "Device");

        queue1.setNext(queue2);
        queue2.setNext(device);

        assertTrue( queue1.process() );
        assertTrue( queue1.process() );

        assertTrue( device.getLeftTime().isPresent() );
        assertTrue( queue1.process() );

        Decimal6f time = device.getLeftTime().get();
        device.run(time);
        queue2.run(time);
        queue1.run(time);

        assertTrue( queue1.getSize() == 0);
        assertTrue( queue2.getSize() == 1);
        assertTrue( device.getLeftTime().isPresent() );
    }
}
