package com.example.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.Test;

import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.components.Queue;
import com.example.modeling.utils.FunRand;

public class ConstraintTest 
{
    @Test
    public void simpleTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 10.0), "Producer");

        var queue1 = new Queue("Queue1");
        var queue2 = new Queue("Queue2");

        var cons = new Constraint(() -> {return queue1.getSize() < 4;}, "Constraint");

        producer.setNext(cons);
        cons.setNext(queue1);
        cons.setIfFailure(queue2);

        producer.run(Decimal6f.valueOf(1000.0));

        var st1 = producer.getStats(); 
        var st2 = cons.getStats(); 
        var st3 = queue1.getStats(); 
        var st4 = queue2.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
        assertTrue( st2.getRequestsNumber() == (st3.getRequests() + st4.getRequests()));
    }
}
