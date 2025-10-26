package com.example.modeling.components;

import java.util.function.Supplier;

import com.example.modeling.components.device.Device.DeviceRand;

public class Producer extends CompDevice {
    private final Supplier<Integer> typGenerator;

    public Producer(
        DeviceRand rand, 
        Supplier<Integer> typGenerator, 
        String name
    ) {
        super(rand, name);
        this.typGenerator = typGenerator;
    }

    @Override
    public void run(double time) {
        if (super.getWorkTime().isEmpty()) {
            super.process(this.typGenerator.get());
        }

        double workTime = super.getWorkTime().get();
        double currentTime = time;

        while (currentTime > workTime) {
            super.run(workTime);
            super.process(this.typGenerator.get());

            currentTime -= workTime;
            workTime = super.getWorkTime().get();
        }

        if (currentTime > 0) {
            super.run(currentTime);
            super.process(this.typGenerator.get());
        }
    }

    @Override
    public boolean process(int typ) {
        return super.process(typ);
    }

    public boolean process() {
        return super.process(this.typGenerator.get());
    }
}
