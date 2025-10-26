package com.example.modeling.components;

import com.example.modeling.components.device.Device.DeviceRand;

public class Producer extends CompDevice {

    public Producer(DeviceRand rand, String name) {
        super(rand, name);
    }

    @Override
    public void run(double time) {
        if (super.getWorkTime().isEmpty()) {
            super.process();
        }

        double workTime = super.getWorkTime().get();
        double currentTime = time;

        while (currentTime > workTime) {
            super.run(workTime);
            super.process();

            currentTime -= workTime;
            workTime = super.getWorkTime().get();
        }

        if (currentTime > 0) {
            super.run(currentTime);
            super.process();
        }
    }

    @Override
    public boolean process() {
        return super.process();
    }
}
