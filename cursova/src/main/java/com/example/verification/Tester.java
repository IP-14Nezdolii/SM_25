package com.example.verification;

import java.util.List;
import java.util.function.Supplier;

import com.example.modeling.Model;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.Pair;

public class Tester {
    static final int N_SAMPLES = 1000;
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
        FunRand.getNotNullNorm(22, 10),
        FunRand.getUniform(2, 8),
        FunRand.getFixed(5),
        FunRand.getNotNullNorm(18, 10)
    };

    public static void main( String[] args )
    {
        verificationTest();
        transPeriodTest();
        resultTest();
    }

    public static void verificationTest() {
        Supplier<Double>[] params1 = defaultParams;

        Supplier<Double>[] params2 = defaultParams.clone();
        params2[0] = FunRand.getErlang(4, 16);

        Supplier<Double>[] params3 = defaultParams.clone();
        params3[1] = FunRand.getExponential(7);
        params3[2] = FunRand.getExponential(7);

        Supplier<Double>[] params4 = defaultParams.clone();
        params4[3] = FunRand.getNotNullNorm(11, 5);

        Supplier<Double>[] params5 = defaultParams.clone();
        params5[4] = FunRand.getUniform(1, 4);

        Supplier<Double>[] params6 = defaultParams.clone();
        params6[5] = FunRand.getFixed(1);

        Supplier<Double>[] params7 = defaultParams.clone();
        params7[6] = FunRand.getNotNullNorm(9, 5);

        var lst = List.of(
            params1,
            params2,
            params3,
            params4,
            params5,
            params6,
            params7
        );

        var statsSaver = new StatsSaver();

        for (int i = 0; i < lst.size(); i++) {

            Supplier<Pair<Model, ResultCalculator>> init = 
                ModelInitProducer.getModelInit(lst.get(i));

            for (int j = 0; j < N_SAMPLES; j++) {
                var model = init.get();

                model.get0().simulate(TIME);
                statsSaver.addStats( model.get1(), i);
            }
        }

        statsSaver.save(SAVE_PATH + VERIFICATION_NAME + ".xlsx");
    }

    public static void transPeriodTest() {
        var statsSaver = new StatsSaver();

        var preRunTimes = List.of(0, 4_000, 8_000, 12_000, 16_000, 20_000);
        for (Integer preRunTime : preRunTimes) {

            Supplier<Pair<Model, ResultCalculator>> init = 
                ModelInitProducer.getModelInit(defaultParams);

            for (int j = 0; j < N_SAMPLES; j++) {
                var model = init.get();

                model.get0().simulate(preRunTime);
                model.get0().clearStats();

                model.get0().simulate(TIME);
                statsSaver.addStats( model.get1(), preRunTime);
            }
        }

        statsSaver.save(SAVE_PATH + TRANS_PERIOD_NAME + ".xlsx");
    }

    public static void resultTest() {
        var statsSaver = new StatsSaver();

        Supplier<Pair<Model, ResultCalculator>> init = 
            ModelInitProducer.getModelInit(defaultParams);

        for (int j = 0; j < 10_000; j++) {
            var model = init.get();

            model.get0().simulate(TRANS_PERIOD);
            model.get0().clearStats();

            model.get0().simulate(TIME);
            statsSaver.addStats( model.get1(), 0);
        }

        statsSaver.save(SAVE_PATH + RESULT_NAME + ".xlsx");
    }
}
