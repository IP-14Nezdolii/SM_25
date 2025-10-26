package com.example.components;


import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.example.Model;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;

public class ConstraintTest 
{
    @Test
    public void simpleTest()
    {
        var producer = new Producer(Model.getUniform(1.0, 10.0), "Producer");

        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");

        var con = new Constraint(() -> {return queue1.getSize() < 4;}, "Constraint");

        producer.setNext(con);
        con.setNext(queue1);
        con.setIfFailure(queue2);

        producer.run(1000.0);

        var st1 = producer.getStats(); 
        var st2 = con.getStats(); 
        var st3 = queue1.getStats(); 
        var st4 = queue2.getStats(); 

        assertTrue( st1.getProcessed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == st3.getRequests() + + st4.getRequests());

        System.out.println(st1);
        System.out.println(st2);
        System.out.println(st3);
        System.out.println(st4);
    }
}
