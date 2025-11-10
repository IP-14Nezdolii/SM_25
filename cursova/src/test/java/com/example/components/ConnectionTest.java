package com.example.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.decimal4j.immutable.Decimal4f;
import org.junit.jupiter.api.Test;

import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.PriorityImpl;


public class ConnectionTest 
{
    @Test
    public void connectionSimpleTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 10.0), "Producer");

        var conn1 = new Connection(new PriorityImpl.Priority(), "Connection1");
        var conn2 = new Connection(new PriorityImpl.Priority(), "Connection2");

        producer.setNext(conn1);
        conn1.addNext(conn2, 1);

        producer.run(Decimal4f.valueOf(1000.0));

        var st1 = (Device.Stats)producer.getStats(); 
        var st2 = (Connection.Stats)conn1.getStats(); 
        var st3 = (Connection.Stats)conn2.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequestsNumber() );
    }

    @Test
    public void connectionPriorityTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new PriorityImpl.Priority(), "Connection1");
        var conn2 = new Connection(new PriorityImpl.Priority(), "Connection2");
        var conn3 = new Connection(new PriorityImpl.Priority(), "Connection3");

        producer.setNext(conn1);

        conn1.addNext(conn2, 1);
        conn1.addNext(conn3, 2);

        producer.run(Decimal4f.valueOf(1000.0));

        var st1 = (Device.Stats)producer.getStats(); 
        var st2 = (Connection.Stats)conn1.getStats(); 
        var st3 = (Connection.Stats)conn2.getStats(); 
        var st4 = (Connection.Stats)conn3.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st3.getRequestsNumber() == 0 );
        assertTrue( st1.getServed() == st4.getRequestsNumber() );
    }

    @Test
    public void connectionProbabilityWithBusyTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new PriorityImpl.ProbabilityWithBusy(), "Connection1");
        var conn2 = new Connection(new PriorityImpl.ProbabilityWithBusy(), "Connection2");
        var conn3 = new Connection(new PriorityImpl.ProbabilityWithBusy(), "Connection3");

        producer.setNext(conn1);

        conn1.addNext(conn2, 1);
        conn1.addNext(conn3, 2);

        producer.run(Decimal4f.valueOf(1000.0));

        var st1 = (Device.Stats)producer.getStats(); 
        var st2 = (Connection.Stats)conn1.getStats(); 
        var st3 = (Connection.Stats)conn2.getStats(); 
        var st4 = (Connection.Stats)conn3.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st3.getRequestsNumber() > 0 );
        assertTrue( st4.getRequestsNumber() > 0 );
    }

    @Test
    public void connectionWithManyQueueTest1()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new PriorityImpl.ProbabilityWithBusy(), "Connection1");

        producer.setNext(conn1);

        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");

        conn1.addNext(queue1, 1);
        conn1.addNext(queue2, 1);

        producer.run(Decimal4f.valueOf(100000.0));

        var st1 = producer.getStats(); 
        var st2 = conn1.getStats(); 
        var st3 = queue1.getStats(); 
        var st4 = queue2.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequests() + st4.getRequests()  );
    }
}
