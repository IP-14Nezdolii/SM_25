package com.example;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.modeling.Model;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.NextRulesImpl;

public class AppTest {

    @Test
    public void test() {
        double TIME = 1440;

        var truckWork = FunRand.getCombined(List.of(
                FunRand.getNotNullNorm(22, 10), 
                FunRand.getUniform(2, 8))
        );
        var truckCooldown = FunRand.getNotNullNorm(18, 10);


        var producer1 = new Producer(FunRand.getErlang(8, 32), "Producer1");

        var q = new PairQueue("Queue");
        
        var a1 = new CompDeviceWithCooldown(FunRand.getExponential(14), FunRand.getFixed(5), "Loader1");
        var a2 = new CompDeviceWithCooldown(FunRand.getExponential(12),  FunRand.getFixed(5), "Loader2");

        var prob0 = new NextRulesImpl.Probability();
        var con0 = new Connection(prob0, "Con0");
        var con1 = new Connection(new NextRulesImpl.Probability(), "Con1");

        var p1 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck1");
        var p2 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck2");
        var p3 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck3");
        var p4 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck4");
        var p5 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck5");

        prob0.setPredicator(() -> {
            int countA = 0;
            int countB = 0;

            if (a1.getLeftTime().isPresent()) countA++;
            if (a2.getLeftTime().isPresent()) countA++;

            if (p1.getLeftTime().isEmpty()) countB++;
            if (p2.getLeftTime().isEmpty()) countB++;
            if (p3.getLeftTime().isEmpty()) countB++;
            if (p4.getLeftTime().isEmpty()) countB++;
            if (p5.getLeftTime().isEmpty()) countB++;

            if (countA == 3) return false;
            return countB > countA;
        });

        producer1.setNext(q);
        q.setNext(con0);

        con0.addNext(a1, 1);
        con0.addNext(a2, 1);

        a1.setNext(con1);
        a2.setNext(con1);

        con1.addNext(p1, 1);
        con1.addNext(p2, 1);
        con1.addNext(p3, 1);
        con1.addNext(p4, 1);
        con1.addNext(p5, 1);

        var proc = new Model(producer1);

        proc.run(TIME * 10);
        proc.getStats().clear();
        proc.run(TIME);

        System.out.println(producer1.getStats());
        System.out.println(q.getStats());
        System.out.println(con0.getStats());
        
        System.out.println(a1.getStats());
        System.out.println(a2.getStats());

        System.out.println(con1.getStats());

        System.out.println(p1.getStats());
        System.out.println(p2.getStats());
        System.out.println(p3.getStats());
        System.out.println(p4.getStats());
        System.out.println(p5.getStats());
    }
}
