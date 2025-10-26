package com.example.components;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.Model;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device;


public class ConnectionTest 
{
    @Test
    public void connectionSimpleTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 10.0), "Producer");

        var conn1 = new Connection(new Model.Priority(), "Connection1");
        var conn2 = new Connection(new Model.Priority(), "Connection2");

        producer.setNext(conn1);
        conn1.addNext(conn2, 1);

        producer.run(1000.0);

        var st1 = (Device.Stats)producer.getStats(); 
        var st2 = (Connection.Stats)conn1.getStats(); 
        var st3 = (Connection.Stats)conn2.getStats(); 

        assertTrue( st1.getProcessed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequestsNumber() );
    }

    @Test
    public void connectionPriorityTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new Model.Priority(), "Connection1");
        var conn2 = new Connection(new Model.Priority(), "Connection2");
        var conn3 = new Connection(new Model.Priority(), "Connection3");

        producer.setNext(conn1);

        conn1.addNext(conn2, 1);
        conn1.addNext(conn3, 2);

        producer.run(1000.0);

        System.out.println(producer.getStats());
        System.out.println(conn1.getStats());
        System.out.println(conn2.getStats());
    }

    @Test
    public void connectionProbabilityWithBusyTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new Model.ProbabilityWithBusy(), "Connection1");
        var conn2 = new Connection(new Model.ProbabilityWithBusy(), "Connection2");
        var conn3 = new Connection(new Model.ProbabilityWithBusy(), "Connection3");

        producer.setNext(conn1);

        conn1.addNext(conn2, 1);
        conn1.addNext(conn3, 2);

        producer.run(1000.0);

        System.out.println(producer.getStats());
        System.out.println(conn1.getStats());
        System.out.println(conn2.getStats());
        System.out.println(conn3.getStats());
    }

    @Test
    public void connectionWithManyQueueTest1()
    {
        var producer = new Producer(Model.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new Model.ProbabilityWithBusy(), "Connection1");

        producer.setNext(conn1);

        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");

        conn1.addNext(queue1, 1);
        conn1.addNext(queue2, 1);

        producer.run(100000.0);

        var st1 = producer.getStats(); 
        var st2 = conn1.getStats(); 
        var st3 = queue1.getStats(); 
        var st4 = queue2.getStats(); 

        assertTrue( st1.getProcessed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequests() + st4.getRequests()  );

        System.out.println(st1);
        System.out.println(st2);
        System.out.println(st3);
        System.out.println(st4);
    }
}
