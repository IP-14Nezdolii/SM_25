package com.example.test;

import java.util.List;
import java.util.function.Supplier;

import com.example.CompDeviceWithCooldown;
import com.example.PairQueue;
import com.example.modeling.Model;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Producer;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.NextRulesImpl;

public class Tester {
    static final int N_SAMPLES = 1000;
    static final double TIME = 1440;

    static final String savePath = "C:\\Users\\vladi\\.vscode\\Git\\SM_25\\SM_25\\cursova\\";
    static final String verificationBaseName = "verification";
    static final String transPeriodName = "trans";
    static final String resultName = "result";
    static final String modName = "mod";

    public static void modTest() {
        var producerWork = FunRand.getErlang(8, 32);
        var loader1Work = FunRand.getExponential(14);
        var loader2Work = FunRand.getExponential(12);
        var loaderCooldown = FunRand.getFixed(5);
        var truckWork = FunRand.getCombined(List.of(
            FunRand.getNotNullNorm(22, 10), 
            FunRand.getUniform(2, 8))
        );
        var truckCooldown = FunRand.getNotNullNorm(18, 10);

        var init = getModModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var statsSaver = new StatsSaver();
        for (int j = 0; j < N_SAMPLES; j++) {
            var model = init.get();

            model.run(12_000);
            model.getStats().clear();

            model.run(TIME);
            statsSaver.addStats(model.getStats(), 0);
        }

        statsSaver.save(savePath + modName + ".xlsx");
    }

    public static void resultTest() {
        var producerWork = FunRand.getErlang(8, 32);
        var loader1Work = FunRand.getExponential(14);
        var loader2Work = FunRand.getExponential(12);
        var loaderCooldown = FunRand.getFixed(5);
        var truckWork = FunRand.getCombined(List.of(
            FunRand.getNotNullNorm(22, 10), 
            FunRand.getUniform(2, 8))
        );
        var truckCooldown = FunRand.getNotNullNorm(18, 10);

        var init = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var statsSaver = new StatsSaver();
        for (int j = 0; j < N_SAMPLES; j++) {
            var model = init.get();

            model.run(12_000);
            model.getStats().clear();

            model.run(TIME);
            statsSaver.addStats(model.getStats(), 0);
        }

        statsSaver.save(savePath + resultName + ".xlsx");
    }

    public static void transPeriodTest() {
        var producerWork = FunRand.getErlang(8, 32);
        var loader1Work = FunRand.getExponential(14);
        var loader2Work = FunRand.getExponential(12);
        var loaderCooldown = FunRand.getFixed(5);
        var truckWork = FunRand.getCombined(List.of(
            FunRand.getNotNullNorm(22, 10), 
            FunRand.getUniform(2, 8))
        );
        var truckCooldown = FunRand.getNotNullNorm(18, 10);

        var init = getBaseModelInitializer(
            producerWork,
            loader1Work,
            loader2Work,
            loaderCooldown,
            truckWork,
            truckCooldown
        );

        var preRunTimes = List.of(0, 4000, 8000, 12000, 16000);

        var statsSaver = new StatsSaver();

        for (int i = 0; i < preRunTimes.size(); i++) {
            for (int j = 0; j < N_SAMPLES; j++) {
                var model = init.get();

                model.run(preRunTimes.get(i));
                model.getStats().clear();

                model.run(TIME);
                statsSaver.addStats(model.getStats(), i);
            }
        }

        statsSaver.save(savePath + transPeriodName + ".xlsx");
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

        var lst = List.of(
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                loader2Work,
                loaderCooldown,
                truckWork,
                truckCooldown
            ),
            getBaseModelInitializer(
                FunRand.getErlang(4, 16),
                loader1Work,
                loader2Work,
                loaderCooldown,
                truckWork,
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                FunRand.getExponential(7),
                loader2Work,
                loaderCooldown,
                truckWork,
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                FunRand.getExponential(6),
                loaderCooldown,
                truckWork,
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                loader2Work,
                FunRand.getFixed(1),
                truckWork,
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                loader2Work,
                loaderCooldown,
                FunRand.getCombined(List.of(
                    FunRand.getNotNullNorm(11, 5), 
                    FunRand.getUniform(2, 8))
                ),
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                loader2Work,
                loaderCooldown,
                FunRand.getCombined(List.of(
                    FunRand.getNotNullNorm(22, 10), 
                    FunRand.getUniform(2, 4))
                ),
                truckCooldown
            ),
            getBaseModelInitializer(
                producerWork,
                loader1Work,
                loader2Work,
                loaderCooldown,
                truckWork,
                FunRand.getNotNullNorm(9,5)
            )
        );

        var statsSaver = new StatsSaver();

        for (int i = 0; i < lst.size(); i++) {

            for (int j = 0; j < N_SAMPLES; j++) {
                var model = lst.get(i).get();

                model.run(TIME);
                statsSaver.addStats(model.getStats(), i);
            }
        }
        statsSaver.save(savePath + verificationBaseName + ".xlsx");
    }

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

            var rule0 = new NextRulesImpl.Probability();
            var con0 = new Connection(rule0, "Con0");
            var con1 = new Connection(new NextRulesImpl.Probability(), "Con1");

            var p1 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck1");
            var p2 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck2");
            var p3 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck3");
            var p4 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck4");

            rule0.setPredicator(() -> {
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

            var proc = new Model(producer1);

            return proc;
        };
    }

    public static Supplier<Model> getModModelInitializer(
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

            var rule0 = new NextRulesImpl.Probability();
            var con0 = new Connection(rule0, "Con0");
            var con1 = new Connection(new NextRulesImpl.Probability(), "Con1");

            var p1 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck1");
            var p2 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck2");
            var p3 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck3");
            var p4 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck4");
            var p5 = new CompDeviceWithCooldown(truckWork, truckCooldown, "Truck5");

            rule0.setPredicator(() -> {
                int countA = 0;
                int countB = 0;

                if (a1.getLeftTime().isPresent()) countA++;
                if (a2.getLeftTime().isPresent()) countA++;

                if (p1.getLeftTime().isEmpty()) countB++;
                if (p2.getLeftTime().isEmpty()) countB++;
                if (p3.getLeftTime().isEmpty()) countB++;
                if (p4.getLeftTime().isEmpty()) countB++;
                if (p5.getLeftTime().isEmpty()) countB++;

                if (countA == 2) return false;
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

            return proc;
        };
    }
    
}
