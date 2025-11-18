package com.example.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.decimal4j.immutable.Decimal6f;
import org.junit.jupiter.api.Test;

import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.NextRulesImpl;

public class ProducerTest 
{
    @Test
    public void producerTest()
    {
        var producer = new Producer(FunRand.getUniform(1.0, 10.0), "Producer");
        var conn = new Connection(new NextRulesImpl.Priority(), "Connection");

        producer.setNext(conn);

        producer.run(Decimal6f.valueOf(1000.0));

        var st1 = producer.getStats(); 
        var st2 = conn.getStats(); 

        assertTrue( st1.getServed() == st2.getRequestsNumber() );
    }
}
