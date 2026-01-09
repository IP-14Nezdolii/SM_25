package com.example.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.example.analysis.modelInit.BaseModelInitProducer;
import com.example.analysis.modelInit.BaseResultCalculator;
import com.example.analysis.modelInit.ParamModelInitProducer;
import com.example.analysis.modelInit.ParamResultCalculator;
import com.example.analysis.utils.BaseStatsSaver;
import com.example.analysis.utils.BestModelSearcher;
import com.example.analysis.utils.ParamStatsSaver;
import com.example.modeling.Model;
import com.example.modeling.utils.FunRand;
import com.example.modeling.utils.Pair;

public class Tester {
    static final int N_SAMPLES = 1_600;
    static final double TIME = 1440;
    static final double TRANS_PERIOD = 16_000;
    static final double TRANS_MOD_PERIOD = 45_000;

    static final String SAVE_PATH = "C:\\Users\\vladi\\.vscode\\Git\\SM_25\\SM_25\\cursova\\";

    static final String VERIFICATION_NAME = "verification";
    static final String TRANS_PERIOD_NAME = "trans";
    static final String RESULT_NAME = "result";

    static final String TRANS_PERIOD_MOD_NAME = "trans_mod";
    static final String MOD_NAME = "mod";

    @SuppressWarnings("unchecked")
    static final Supplier<Double>[] defaultVars = new Supplier[] {
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
        transPeriodTest();
        resultTest();

        var s = new BestModelSearcher(N_SAMPLES, TIME, TRANS_PERIOD);
        s.findOptimalParams();

        transModPeriodTest();

        modTest();
    }

    public static void verificationTest() {
        Supplier<Double>[] vars1 = defaultVars;

        Supplier<Double>[] vars2 = defaultVars.clone();
        vars2[0] = FunRand.getErlang(16, 32);

        Supplier<Double>[] vars3 = defaultVars.clone();
        vars3[1] = FunRand.getExponential(7);
        vars3[2] = FunRand.getExponential(6);
        vars3[3] = FunRand.getFixed(2.5);

        Supplier<Double>[] vars4 = defaultVars.clone();
        vars4[4] = FunRand.getNotNullNorm(11, 5);
        vars4[5] = FunRand.getUniform(1, 4);
        vars4[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] vars5 = defaultVars.clone();
        vars5[1] = FunRand.getExponential(7);
        vars5[2] = FunRand.getExponential(6);
        vars5[3] = FunRand.getFixed(2.5);
        vars5[4] = FunRand.getNotNullNorm(11, 5);
        vars5[5] = FunRand.getUniform(1, 4);
        vars5[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] vars6 = defaultVars.clone();
        vars6[0] = FunRand.getErlang(4, 16);
        vars6[1] = FunRand.getExponential(7);
        vars6[2] = FunRand.getExponential(6);
        vars6[3] = FunRand.getFixed(2.5);
        vars6[4] = FunRand.getNotNullNorm(11, 5);
        vars6[5] = FunRand.getUniform(1, 4);
        vars6[6] = FunRand.getNotNullNorm(9, 5);

        Supplier<Double>[] vars7 = defaultVars.clone();
        vars7[1] = FunRand.getExponential(28);

        Supplier<Double>[] vars8 = defaultVars.clone();
        vars8[2] = FunRand.getExponential(24);

        Supplier<Double>[] vars9 = defaultVars.clone();
        vars9[0] = FunRand.getErlang(4, 16);


        var lst = List.of(
            vars1,
            vars2,
            vars3,
            vars4,
            vars5,
            vars6,
            vars7,
            vars8,
            vars9
        );

        String filename = SAVE_PATH + VERIFICATION_NAME + ".xlsx";
        var statsSaver = new BaseStatsSaver(filename);

        try (ExecutorService executor = Executors.newFixedThreadPool(lst.size())) {
            for (int i = 0; i < lst.size(); i++) {
                final int mask = i;
                
                executor.execute(() -> {
                    Supplier<Pair<Model, BaseResultCalculator>> init = BaseModelInitProducer.getModelInit(lst.get(mask));
                    ArrayList<Pair<BaseResultCalculator, Integer>> statsBuffer = new ArrayList<>();

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

        System.out.println("File written: " + filename);
    }

    public static void transPeriodTest() {
        var preRunTimes = List.of(0, 4_000, 8_000, 12_000, 16_000, 20_000);

        String filename = SAVE_PATH + TRANS_PERIOD_NAME + ".xlsx";
        var statsSaver = new BaseStatsSaver(filename);

        try (ExecutorService executor = Executors.newFixedThreadPool(preRunTimes.size())) {
            for (Integer preRunTime : preRunTimes) {

                executor.execute(() -> {
                    Supplier<Pair<Model, BaseResultCalculator>> init = BaseModelInitProducer.getModelInit(defaultVars);
                    ArrayList<Pair<BaseResultCalculator, Integer>> statsBuffer = new ArrayList<>();

                    for (int j = 0; j < N_SAMPLES; j++) {
                        var model = init.get();

                        model.get0().simulate(preRunTime);
                        model.get0().clearStats();

                        model.get0().simulate(TIME);
                        statsBuffer.add(Pair.createPair(model.get1(), preRunTime));
                    }

                    synchronized (statsSaver) {
                        statsSaver.addStats(statsBuffer);
                        statsSaver.appendToFile();
                    }
                    statsBuffer.clear();
                });
            }
        }

        System.out.println("File written: " + filename);
    }

    public static void transModPeriodTest() {
        var preRunTimes = List.of(0, 
            4_000, 
            8_000, 
            12_000, 
            16_000, 
            20_000, 
            24_000, 
            28_000, 
            32_000, 
            36_000, 
            40_000, 
            44_000, 
            48_000,
            52_000,
            56_000,
            60_000
        );

        String filename = SAVE_PATH + TRANS_PERIOD_MOD_NAME + ".xlsx";
        var statsSaver = new ParamStatsSaver(filename);

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (Integer preRunTime : preRunTimes) {

                executor.execute(() -> {
                    Supplier<Pair<Model, ParamResultCalculator>> init = ParamModelInitProducer.getModelInit(1,2,0,5);
                    ArrayList<Pair<ParamResultCalculator, Integer>> statsBuffer = new ArrayList<>();

                    for (int j = 0; j < N_SAMPLES; j++) {
                        var model = init.get();

                        model.get0().simulate(preRunTime);
                        model.get0().clearStats();

                        model.get0().simulate(TIME);
                        statsBuffer.add(Pair.createPair(model.get1(), preRunTime));
                    }

                    synchronized (statsSaver) {
                        statsSaver.addStats(statsBuffer);
                        statsSaver.appendToFile();
                    }
                    statsBuffer.clear();
                });
            }
        }

        System.out.println("File written: " + filename);
    }

    public static void resultTest() {
        String filename = SAVE_PATH + RESULT_NAME + ".xlsx";
        var statsSaver = new BaseStatsSaver(filename);

        Supplier<Pair<Model, BaseResultCalculator>> init = BaseModelInitProducer.getModelInit(defaultVars);
        ArrayList<Pair<BaseResultCalculator, Integer>> statsBuffer = new ArrayList<>();

        for (int j = 0; j < N_SAMPLES; j++) {
            var model = init.get();

            model.get0().simulate(TRANS_PERIOD);
            model.get0().clearStats();

            model.get0().simulate(TIME);
            statsBuffer.add(Pair.createPair(model.get1(), 0));
        }

        statsSaver.addStats(statsBuffer);
        statsSaver.appendToFile();

        System.out.println("File written: " + filename);
    }

    public static void modTest() {
        String filename = SAVE_PATH + MOD_NAME + ".xlsx";
        var statsSaver = new ParamStatsSaver(filename);

        Supplier<Pair<Model, ParamResultCalculator>> init = ParamModelInitProducer.getModelInit(1,2,0,5);
        ArrayList<Pair<ParamResultCalculator, Integer>> statsBuffer = new ArrayList<>();

        for (int j = 0; j < N_SAMPLES; j++) {
            var model = init.get();

            model.get0().simulate(TRANS_MOD_PERIOD);
            model.get0().clearStats();  

            model.get0().simulate(TIME);
            statsBuffer.add(Pair.createPair(model.get1(), 0));
        }

        statsSaver.addStats(statsBuffer);
        statsSaver.appendToFile();

        System.out.println("File written: " + filename);
    }
}