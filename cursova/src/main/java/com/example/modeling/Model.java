package com.example.modeling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.decimal4j.immutable.Decimal6f;

public class Model {
    private final ArrayList<SMO> elems;

    public Model(ArrayList<SMO> elems) {
        if (elems.isEmpty()) {
            throw new IllegalArgumentException("Model elements list must be not empty");
        }

        elems.sort(Comparator.comparingInt(SMO::getEventPriority).reversed());
        this.elems = elems;
    }

    public void run(double runTime) {
        Decimal6f leftTimeSim = Decimal6f.valueOf(runTime);

        while (leftTimeSim.isGreaterThan(Decimal6f.ZERO)) {
            Decimal6f minLeftTime = elems.stream()
                    .map(SMO::getLeftTime)
                    .min(Decimal6f::compareTo)
                    .orElseThrow();

            Decimal6f step = minLeftTime.compareTo(leftTimeSim) < 0
                    ? minLeftTime
                    : leftTimeSim;

            for (SMO smo : elems) {
                smo.run(step);
            }

            for (SMO smo : elems) {
                smo.handleEvents();
            }

            leftTimeSim = leftTimeSim.subtract(step);
        }
    }

    public void clearStats() {
        for (SMO smo : elems) {
            smo.getStats().clear();
        }
    }

    public List<SMO.Stats> getStats() {
        return this.elems.stream().map(SMO::getStats).toList().reversed();
    }
}
