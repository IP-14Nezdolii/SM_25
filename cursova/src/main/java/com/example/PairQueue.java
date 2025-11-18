package com.example;

import org.decimal4j.immutable.Decimal6f;

import com.example.modeling.components.Queue;

public class PairQueue extends Queue {

    public PairQueue(String name) {
        super(name);
    }

    @Override
    public boolean process() {
        this.stats.addRequest();
        this.enqueue();
        this.tryServe();
        
        return true;
    }

    @Override
    public void run(Decimal6f time) {
        this.stats.record(time.doubleValue());
        this.tryServe();
    }

    private void tryServe() {
        if (this.next.isPresent()) {
            var next = this.next.get();

            while (next.getLeftTime().isEmpty() && this.size > 1) {
                next.process();

                this.dequeue();
                this.dequeue();

                this.stats.addServed();
                this.stats.addServed();
            }
        }
    }
}
