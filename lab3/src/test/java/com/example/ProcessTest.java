package com.example;

import org.junit.Test;

import com.example.modeling.Process;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;

public class ProcessTest 
{
    @Test
    public void simpleTest()
    {
        var producer = new Producer(Model.getUniform(2.0, 4.0), "Producer"); 
          
        var queue = new Queue("Queue");
        var con = new Constraint(() -> queue.getSize() < 5, "Constraint");

        var device = new CompDevice(Model.getUniform(4.0, 6.0), "Device");

        var connection = new Connection(new Model.Priority(), "Output");

        producer.setNext(con);
        con.setNext(queue);
        queue.setNext(device);
        device.setNext(connection);

        queue.process();
        queue.process();

        var proc = new Process(producer, "MainProc");
        proc.run(1000.0);

        System.out.println(proc.getStats());
    }
}
