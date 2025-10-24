package com.example;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.modeling.Producer;
import com.example.utils.DeviceRand.NextPriority;
import com.example.modeling.Process;

/**
 * Unit test for simple App.
 */
public class ModelTest 
{
    @Test
    public void deviceTest()
    {
        var device = Model.newDevice(Model.getFixed(5.0), "Device", NextPriority.Priority);

        // initially device is free
        assertTrue( device.getWorkTime().isEmpty() );
        device.run(5.0);
        assertTrue( device.getWorkTime().isEmpty() );

        // process an item
        assertTrue( device.process() );
        device.run(5.0);
        assertTrue( device.getWorkTime().isEmpty() );

        // process an item again
        assertTrue( device.process() );
        device.run(5.0);
        assertTrue( device.getWorkTime().isEmpty() );
        
        System.out.println("processSingleDeviceTest:\n" + device.getStats() );
    }

    @Test
    public void queueWithSingleDeviceTest()
    {
        var queue = Model.newFixedQueue(3, "Queue", NextPriority.Priority);

        assertTrue( queue.process() );
        assertTrue( queue.process() );
        assertTrue( queue.process() );
        assertTrue( queue.process() == false );
        assertTrue( queue.process() == false );

        System.out.println("queueWithSingleDeviceTest:\n" + queue.getStats() );
    }

    @Test
    public void simpleProcessTest()
    {
        var producer = new Producer(
            NextPriority.ProbabilityWithBusy, Model.getUniform(2.0, 3.0), "Producer");

        var queue = Model.newFixedQueue(10, "Queue", NextPriority.Priority);
        var device = Model.newDevice(Model.getUniform(1.0, 4.0), "Device", NextPriority.Priority);  
        
        producer.addNext(queue, 1);
        queue.addNext(device, 1);

        var process = new Process(producer, "Process");
        process.run(1000.0);

        System.out.println("simpleProcessTest:\n" + process.getStats() );
    }

    @Test
    public void doubleDevicePerProcessTest()
    {
        var producer = new Producer(
            NextPriority.ProbabilityWithBusy, Model.getUniform(2.0, 3.0), "Producer");

        var queue = Model.newFixedQueue(10, "Queue", NextPriority.Priority);
        
        var device1 = Model.newDevice(Model.getUniform(4.0, 8.0), "Device1", NextPriority.Priority);
        var device2 = Model.newDevice(Model.getUniform(4.0, 8.0), "Device2", NextPriority.Priority);

        producer.addNext(queue, 1);

        queue.addNext(device1, 1);
        queue.addNext(device2, 1);

        var process = new Process(producer, "Process");
        process.run(1000.0);

        System.out.println("simpleProcessTest:\n" + process.getStats() );
    }

    @Test
    public void ifFailureProcessTest()
    {
        var producer = new Producer(
            NextPriority.ProbabilityWithBusy, Model.getUniform(2.0, 3.0), "Producer");

        var queue1 = Model.newFixedQueue(10, "Queue1", NextPriority.Priority);
        var queue2 = Model.newFixedQueue(10, "Queue2", NextPriority.Priority);
        var queue3 = Model.newFixedQueue(10, "Queue3", NextPriority.Priority);

        var device1 = Model.newDevice(Model.getUniform(1.0, 4.0), "Device1", NextPriority.ProbabilityWithBusy);
        var device2 = Model.newDevice(Model.getUniform(10.0, 20.0), "Device2", NextPriority.Priority);
        var device3 = Model.newDevice(Model.getUniform(1.0, 4.0), "Device3", NextPriority.Priority);

        producer.addNext(queue1, 1);
        queue1.addNext(device1, 1);

        device1.addNext(queue2, 75);
        device1.addNext(queue3, 25);

        queue2.addNext(device2, 1);
        queue3.addNext(device3, 1);

        queue2.setIfFailure(queue1);

        var process = new Process(producer, "MainProcess");
        process.run(1000.0);

        System.out.println("simpleProcessTest:\n" + process.getStats() );
    }
}
