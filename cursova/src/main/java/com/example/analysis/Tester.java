package com.example.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.example.modeling.Model;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.Pair;

public class Tester {
    static final int N_SAMPLES = 1_600;
    static final double TIME = 1440;
    static final double TRANS_PERIOD = 12_000;

    static final String SAVE_PATH = "C:\\Users\\vladi\\.vscode\\Git\\SM_25\\SM_25\\cursova\\";
    static final String VERIFICATION_NAME = "verification";
    static final String TRANS_PERIOD_NAME = "trans";
    static final String RESULT_NAME = "result";

    @SuppressWarnings("unchecked")
    static final Supplier<Double>[] defaultParams = new Supplier[] {
        FunRand.getErlang(8, 32),
        FunRand.getExponential(14),
        FunRand.getExponential(12),
        FunRand.getFixed(5),
        FunRand.getNotNullNorm(22, 10),
        FunRand.getUniform(2, 8),
        FunRand.getNotNullNorm(18, 10)
    };

    public static void main( String[] args )
    {
        verificationTest();
        //transPeriodTest();
        //resultTest();
    }

    public static void verificationTest() {
        Supplier<Double>[] params1 = defaultParams;

        Supplier<Double>[] params2 = defaultParams.clone();
        params2[0] = FunRand.getErlang(16, 32);

        Supplier<Double>[] params3 = defaultParams.clone();
        params3[1] = FunRand.getExponential(7);
        params3[2] = FunRand.getExponential(6);
        params3[3] = FunRand.getFixed(2.5);

        Supplier<Double>[] params4 = defaultParams.clone();
        params4[4] = FunRand.getNotNullNorm(11, 5);
        params4[5] = FunRand.getUniform(1, 4);
        params4[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] params5 = defaultParams.clone();
        params5[1] = FunRand.getExponential(7);
        params5[2] = FunRand.getExponential(6);
        params5[3] = FunRand.getFixed(2.5);
        params5[4] = FunRand.getNotNullNorm(11, 5);
        params5[5] = FunRand.getUniform(1, 4);
        params5[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] params6 = defaultParams.clone();
        params6[0] = FunRand.getErlang(4, 16);
        params6[1] = FunRand.getExponential(7);
        params6[2] = FunRand.getExponential(6);
        params6[3] = FunRand.getFixed(2.5);
        params6[4] = FunRand.getNotNullNorm(11, 5);
        params6[5] = FunRand.getUniform(1, 4);
        params6[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] params7 = defaultParams.clone();
        params7[1] = FunRand.getExponential(28);

        Supplier<Double>[] params8 = defaultParams.clone();
        params8[2] = FunRand.getExponential(24);

        Supplier<Double>[] params9 = defaultParams.clone();
        params9[0] = FunRand.getErlang(4, 16);


        var lst = List.of(
            params1,
            params2,
            params3,
            params4,
            params5,
            params6,
            params7,
            params8,
            params9
        );

        var statsSaver = new StatsSaver(SAVE_PATH + VERIFICATION_NAME + ".xlsx");

        try (ExecutorService executor = Executors.newFixedThreadPool(9)) {
            for (int i = 0; i < lst.size(); i++) {
                final int mask = i;
                
                executor.execute(() -> {
                    Supplier<Pair<Model, ResultCalculator>> init = BaseModelInitProducer.getModelInit(lst.get(mask));
                    ArrayList<Pair<ResultCalculator, Integer>> statsBuffer = new ArrayList<>();

                    for (int j = 0; j < N_SAMPLES; j++) {
                        var model = init.get();

                        model.get0().simulate(TIME);
                        statsBuffer.add(Pair.createPair(model.get1(), mask));
                    }

                    synchronized (statsSaver) {
                        statsSaver.addStats(statsBuffer);
                        statsSaver.appendToFile();
                    }
                    statsBuffer.clear();
                });
            }
        }
    }

    // public static void transPeriodTest() {
    //     var statsSaver = new StatsSaver();

    //     var preRunTimes = List.of(0, 4_000, 8_000, 12_000, 16_000, 20_000);
    //     for (Integer preRunTime : preRunTimes) {

    //         Supplier<Pair<Model, ResultCalculator>> init = 
    //             BaseModelInitProducer.getModelInit(defaultParams);

    //         for (int j = 0; j < N_SAMPLES; j++) {
    //             var model = init.get();

    //             model.get0().simulate(preRunTime);
    //             model.get0().clearStats();

    //             model.get0().simulate(TIME);
    //             statsSaver.addStats( model.get1(), preRunTime);
    //         }
    //     }

    //     statsSaver.saveAsNew(SAVE_PATH + TRANS_PERIOD_NAME + ".xlsx");
    // }

    // public static void resultTest() {
    //     var statsSaver = new StatsSaver();

    //     Supplier<Pair<Model, ResultCalculator>> init = 
    //         BaseModelInitProducer.getModelInit(defaultParams);

    //     for (int j = 0; j < N_SAMPLES; j++) {
    //         var model = init.get();

    //         model.get0().simulate(TRANS_PERIOD);
    //         model.get0().clearStats();

    //         model.get0().simulate(TIME);
    //         statsSaver.addStats( model.get1(), 0);
    //     }

    //     statsSaver.saveAsNew(SAVE_PATH + RESULT_NAME + ".xlsx");
    // }
}
