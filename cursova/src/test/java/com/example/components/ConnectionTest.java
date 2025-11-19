package com.example.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.Test;

import com.example.modeling.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.NextRulesImpl;


public class ConnectionTest 
{
    @Test
    public void connectionSimpleTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 10.0), "Producer");

        var conn1 = new Connection(new NextRulesImpl.Priority(), "Connection1");
        var conn2 = new Connection(new NextRulesImpl.Priority(), "Connection2");

        producer.setNext(conn1);
        conn1.addNext(conn2, 1);

        producer.run(Decimal6f.valueOf(1000.0));

        var st1 = producer.getStats(); 
        var st2 = conn1.getStats(); 
        var st3 = conn2.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequestsNumber() );
    }

    @Test
    public void connectionPriorityTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new NextRulesImpl.Priority(), "Connection1");
        var conn2 = new Connection(new NextRulesImpl.Priority(), "Connection2");
        var conn3 = new Connection(new NextRulesImpl.Priority(), "Connection3");

        producer.setNext(conn1);

        conn1.addNext(conn2, 1);
        conn1.addNext(conn3, 2);

        producer.run(Decimal6f.valueOf(1000.0));

        var st1 = producer.getStats(); 
        var st2 = conn1.getStats(); 
        var st3 = conn2.getStats(); 
        var st4 = conn3.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st3.getRequestsNumber() == 0 );
        assertTrue( st1.getServed() == st4.getRequestsNumber() );
    }

    @Test
    public void connectionProbabilityTest()
    {
        var producer1 = new Producer(FunRand.getFixed(2), "Producer1");

        var q = new Queue("Queue");
        var a1 = new CompDevice(FunRand.getFixed(4), "Loader1");
        var a2 = new CompDevice(FunRand.getFixed(4), "Loader2");

        var con0 = new Connection(new NextRulesImpl.Probability(), "Con0");

        producer1.setNext(q);
        q.setNext(con0);

        con0.addNext(a1, 1);
        con0.addNext(a2, 1);

        var proc = new Model(producer1);

        a1.process();
        a2.process();

        proc.run(10);


        var st1 = producer1.getStats();
        var st2 = q.getStats();

        var st4 = a1.getStats();
        var st5 = a2.getStats();

        assertTrue( st1.getServed() == st2.getRequests() );

        assertEquals(st4.getUtilization(), 1.0, 0.0001);
        assertEquals(st5.getUtilization(), 1.0, 0.0001);
    }

    @Test
    public void connectionWithManyQueueTest1()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 2.0), "Producer");

        var conn1 = new Connection(new NextRulesImpl.Priority(), "Connection1");

        producer.setNext(conn1);

        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");

        conn1.addNext(queue1, 1);
        conn1.addNext(queue2, 1);

        producer.run(Decimal6f.valueOf(100000.0));

        var st1 = producer.getStats(); 
        var st2 = conn1.getStats(); 
        var st3 = queue1.getStats(); 
        var st4 = queue2.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequests() + st4.getRequests()  );
    }

    @Test
    public void predicatorTest()
    {
        var queue1 = new Queue("Queue1");

        var rules = new NextRulesImpl.Priority();
        var conn1 = new Connection(rules, "Connection1");
        rules.setPredicator(() -> false);

        queue1.setNext(conn1);

        for (int i = 0; i < 100; i++) {
            queue1.process();
        }
     
        var st1 = queue1.getStats(); 

        assertTrue( st1.getServed() == 0 );
    }
}
