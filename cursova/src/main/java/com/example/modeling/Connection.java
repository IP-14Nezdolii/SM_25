package com.example.modeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.example.modeling.utils.Pair;
import com.example.modeling.utils.Status;

public class Connection {
    private final Random rand = new Random();
    private final ArrayList<Pair<SingleChannelSMO, Supplier<Boolean>>> next = new ArrayList<>();

    private final int groupSize;
    private int size = 0;

    private int outputCount = 0;

    public Connection(int groupSize) {
        if (groupSize <= 0) {
            throw new IllegalArgumentException("Group size must be positive");
        }

        this.groupSize = groupSize;
    }

    public Connection() {
        this(1);
    }

    public int getOutputCount() {
        return this.outputCount;
    }

    public void clearStats() {
        this.outputCount = 0;
    }

    public void push() {
        this.size++;

        if (this.size == this.groupSize) {
            this.size = 0;

            if (!this.next.isEmpty()) {
                List<SingleChannelSMO> lst = this.next.stream()
                        .filter(elem -> elem.get0().getStatus().isReady() && elem.get1().get())
                        .map(elem -> elem.get0())
                        .toList();

                if (lst.isEmpty()) {
                    throw new IllegalStateException("No available SMO to push the item");
                }

                lst.get(rand.nextInt(lst.size())).process();
            }

            this.outputCount++;
        }
    }

    public void addNext(SingleChannelSMO smo) {
        this.next.add(Pair.createPair(smo, () -> true));
    }

    public void addNext(SingleChannelSMO smo, Supplier<Boolean> condition) {
        this.next.add(Pair.createPair(smo, condition));
    }

    public Status getStatus() {
        if (this.next.isEmpty()) {
            return Status.READY;
        }

        for (var elem : this.next) {
            if (elem.get0().getStatus().isReady() && elem.get1().get()) {
                return Status.READY;
            }
        }

        return Status.BUSY;
    }
}
