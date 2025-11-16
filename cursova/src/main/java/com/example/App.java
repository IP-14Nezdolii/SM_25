package com.example;

import java.util.List;
import java.util.function.Supplier;

import com.example.modeling.Model;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Predicate;
import com.example.modeling.components.Producer;
import com.example.modeling.utils.Tester;
import com.example.modeling.utils.Tester.StatsSaver;
import com.example.modeling.utils.PriorityImpl;
import com.example.modeling.utils.FunRand;

public class App 
{
    static int N_SAMPLES = 1000;
    static double TIME = 480;
    static String savePath = "C:\\Users\\vladi\\.vscode\\Git\\SM_25\\SM_25\\cursova\\";

    public static void main( String[] args )
    {
 

    }

    public static void verificationTest() {
        var producerWork = FunRand.getErlang(8, 32);
        var loader1Work = FunRand.getExponential(14);
        var loader2Work = FunRand.getExponential(12);
        var loaderCooldown = FunRand.getFixed(5);
        var truckWork = FunRand.getCombined(List.of(
                FunRand.getNotNullNorm(22, 10), 
                FunRand.getUniform(2, 8))
        );
        var truckCooldown = FunRand.getNotNullNorm(18, 10);

        var init1 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init2 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init3 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init4 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init5 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init6 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init7 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var init8 = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        Tester tester = null;

        tester = new Tester(init1);
        var statsSaver = new StatsSaver();
        for (int i = 0; i < N_SAMPLES; i++) {
            statsSaver.addStats(tester.test(TIME));
        }

        statsSaver.save(savePath + "result1.xlsx");
    }

    //public static void runTest

    public static Supplier<Model> getBaseModelInitializer(
        Supplier<Double> producerWork,
        Supplier<Double> loader1Work,
        Supplier<Double> loader2Work,
        Supplier<Double> loaderCooldown,
        Supplier<Double> truckWork,
        Supplier<Double> truckCooldown
    ) {
        return () -> {
            var producer1 = new Producer(producerWork, "Producer1");

            var q = new PairQueue("Queue");

            var a1 = new CompDeviceWithCooldown(loader1Work, loaderCooldown, "Loader1");
            var a2 = new CompDeviceWithCooldown(loader2Work,  loaderCooldown, "Loader2");

            var con0 = new Connection(new PriorityImpl.Probability(), "Con0");
            var con1 = new Connection(new PriorityImpl.Probability(), "Con1");

            var p1 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck1");
            var p2 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck2");
            var p3 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck3");
            var p4 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck4");

            var pred = new Predicate(() -> {
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
            }, "Predicate");

            producer1.setNext(q);
            q.setNext(pred);

            pred.setNext(con0);

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
    }
}
