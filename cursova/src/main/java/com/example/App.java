package com.example;

import java.util.List;
import java.util.function.Supplier;

import com.example.modeling.Model;
import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Component;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Constraint;
import com.example.modeling.components.Producer;
import com.example.modeling.utils.Tester;
import com.example.modeling.utils.Tester.StatsSaver;
import com.example.modeling.utils.PriorityImpl;
import com.example.modeling.utils.FunRand;

public class App 
{
    static int N_SAMPLES = 100;
    static double TIME = 1000;
    static String savePath = "C:\\Users\\vladi\\.vscode\\Git\\SM_25\\SM_25\\cursova\\";

    public static void main( String[] args )
    {
        // var model = new Tester(baseModelInit);

        // var statsSaver = new StatsSaver();

        // for (int i = 0; i < N_SAMPLES; i++) {
        //     statsSaver.addStats(model.test(480));
        // }

        // statsSaver.save(savePath + "result1.xlsx");

        var producer1 = new Producer(FunRand.getErlang(8, 32), "Producer1");

        var q = new PairQueue("Queue");

        var a1 = new CompDeviceWithCooldown(FunRand.getExponential(14), FunRand.getFixed(5), "Loader1");
        var a2 = new CompDeviceWithCooldown(FunRand.getExponential(12),  FunRand.getFixed(5), "Loader2");

        var con0 = new Connection(new PriorityImpl.Probability(), "Con0");
        var con1 = new Connection(new PriorityImpl.Probability(), "Con1");

        var p1 = newTruckProcess("Truck1");
        var p2 = newTruckProcess("Truck2");
        var p3 = newTruckProcess("Truck3");
        var p4 = newTruckProcess("Truck4");

        var cons = new Constraint(() -> {
            int countA = 0;
            int countB = 0;

            if (a1.getLeftTime().isPresent()) countA++;
            if (a2.getLeftTime().isPresent()) countA++;

            if (p1.getLeftTime().isEmpty()) countB++;
            if (p2.getLeftTime().isEmpty()) countB++;
            if (p3.getLeftTime().isEmpty()) countB++;
            if (p4.getLeftTime().isEmpty()) countB++;

            if (countA == 2) return false;
            return countB > countA;
        }, "Constraint");

        producer1.setNext(q);
        q.setNext(cons);

        cons.setNext(con0);

        con0.addNext(a1, 1);
        con0.addNext(a2, 1);

        a1.setNext(con1);
        a2.setNext(con1);

        con1.addNext(p1, 1);
        con1.addNext(p2, 1);
        con1.addNext(p3, 1);
        con1.addNext(p4, 1);

        var proc = new Model(producer1);

        proc.run(480);

        System.out.println(producer1.getStats());
        System.out.println(q.getStats());
        System.out.println(q.getStats().getAvgBatchWaitTime(2));
        System.out.println(cons.getStats());
        System.out.println(con0.getStats());
        
        System.out.println(a1.getStats());
        System.out.println(a2.getStats());

        System.out.println(p1.getStats());
        System.out.println(p2.getStats());
        System.out.println(p3.getStats());
        System.out.println(p4.getStats());
    }

    static Supplier<Model> baseModelInit = () -> {
        var producer1 = new Producer(FunRand.getErlang(8, 32), "Producer1");

        var q = new PairQueue("Queue");

        var a1 = new CompDeviceWithCooldown(FunRand.getExponential(14), FunRand.getFixed(5), "Loader1");
        var a2 = new CompDeviceWithCooldown(FunRand.getExponential(12),  FunRand.getFixed(5), "Loader2");

        var con0 = new Connection(new PriorityImpl.Probability(), "Con0");
        var con1 = new Connection(new PriorityImpl.Probability(), "Con1");

        var p1 = newTruckProcess("Proc1");
        var p2 = newTruckProcess("Proc2");
        var p3 = newTruckProcess("Proc3");
        var p4 = newTruckProcess("Proc4");

        var cons = new Constraint(() -> {
            int countA = 0;
            int countB = 0;

            if (a1.getLeftTime().isPresent()) countA++;
            if (a2.getLeftTime().isPresent()) countA++;

            if (p1.getLeftTime().isEmpty()) countB++;
            if (p2.getLeftTime().isEmpty()) countB++;
            if (p3.getLeftTime().isEmpty()) countB++;
            if (p4.getLeftTime().isEmpty()) countB++;

            if (countA == 2) return false;
            return countB > countA;
        }, "Constraint");

        producer1.setNext(q);
        q.setNext(cons);

        cons.setNext(con0);

        con0.addNext(a1, 1);
        con0.addNext(a2, 1);

        a1.setNext(con1);
        a2.setNext(con1);

        con1.addNext(p1, 1);
        con1.addNext(p2, 1);
        con1.addNext(p3, 1);
        con1.addNext(p4, 1);

        var proc = new Model(producer1);

        return proc;
    };

    static CompDeviceWithCooldown newTruckProcess(String name) {
        return new CompDeviceWithCooldown(
            FunRand.getCombined(List.of(
                FunRand.getNotNullNorm(22, 10), 
                FunRand.getUniform(2, 8))
            ), 
            FunRand.getNotNullNorm(18, 10), 
            name
        );
    }
}
