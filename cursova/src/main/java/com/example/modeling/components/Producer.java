package com.example.modeling.components;

import java.util.function.Supplier;

import org.decimal4j.immutable.Decimal6f;

public class Producer extends CompDevice {

    public Producer(Supplier<Double> rand, String name) {
        super(rand, name);

        if (super.getLeftTime().isEmpty()) {
            super.process();
        }
    }

    @Override
    public void run(Decimal6f time) {
        if (super.getLeftTime().isEmpty()) {
            super.process();
        }

        Decimal6f workTime = super.getLeftTime().get();
        Decimal6f currentTime = time;

        while (currentTime.isGreaterThanOrEqualTo(workTime)) {
            super.run(workTime);
            super.process();

            currentTime = currentTime.subtract(workTime);
            workTime = super.getLeftTime().get();
        }

        if (currentTime.isGreaterThan(Decimal6f.ZERO)) {
            super.run(currentTime);
        }
    }
}
