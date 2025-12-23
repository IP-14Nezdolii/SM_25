package com.example.modeling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.decimal4j.immutable.Decimal6f;

public class Model {
    private final ArrayList<SingleChannelSMO> elems;
    private Decimal6f currT = Decimal6f.ZERO;

    public Model(ArrayList<SingleChannelSMO> elems) {
        if (elems.isEmpty()) {
            throw new IllegalArgumentException("Model elements list must be not empty");
        }

        // sort elements by event priority in descending order
        elems.sort(Comparator.comparingInt(SingleChannelSMO::getEventProcessPriority).reversed());
        this.elems = elems;
    }

    public void simulate(double runTime) {
        // calculate absolute end time of the simulation (current time + run time) 
        // considering possible pre-run
        Decimal6f timeModeling = Decimal6f.valueOf(runTime).add(currT);

        while (timeModeling.isGreaterThan(this.currT)) {
            
            // next event time
            this.currT = elems.stream()
                    .map(SingleChannelSMO::getNextT)
                    .min(Decimal6f::compareTo)
                    .orElseThrow();

            // do not advance simulation time beyond the modeling end time
            this.currT = currT.compareTo(timeModeling) < 0
                    ? currT
                    : timeModeling;

            // accumulate statistics
            Decimal6f deltaT = this.currT.subtract(elems.get(0).currT);
            for (SingleChannelSMO smo : elems) {
                smo.recordStats(deltaT);
            }

            // update current simulation time 
            for (SingleChannelSMO smo : elems) {
                smo.setCurrT(this.currT);
            }

            // process events scheduled at the current simulation time
            for (SingleChannelSMO smo : elems) {
                smo.processEvent();
            }
        }
    }

    public void clearStats() {
        for (SingleChannelSMO smo : elems) 
            smo.getStats().clear();
    }

    // returns statistics in reverse order of event priority
    public List<SingleChannelSMO.Stats> getStats() {
        return this.elems.stream().map(SingleChannelSMO::getStats).toList().reversed();
    }
}
