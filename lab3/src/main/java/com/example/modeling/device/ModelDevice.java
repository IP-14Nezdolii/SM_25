package com.example.modeling.device;

import java.util.Optional;

import com.example.modeling.components.Component;
import com.example.utils.DeviceRand;
import com.example.utils.DeviceRand.NextPriority;

public class ModelDevice extends Component {
    private final Device device;

    public ModelDevice(
        NextPriority priority,
        DeviceRand rand, 
        String name
    ) {
        super(priority, name);
        this.device = new Device(rand, name);
    }

    public ModelDevice(
        DeviceRand rand, 
        String name
    ) {
        super(name);
        this.device = new Device(rand, name);
    }

    @Override
    public void run(double time) {
        if (this.device.getWorkTime().isPresent()) {
            if(this.device.run(time)) {
                this.getNextChosen().ifPresent(next -> next.process());
            }
        } else {
            this.device.wait(time);
        }
    }

    @Override
    public boolean process() {
        if (this.device.getWorkTime().isPresent()) {
            return false;
        } else {
            this.device.process();
            return true;
        }
    }

    @Override
    public Optional<Double> getWorkTime() {
        return this.device.getWorkTime();
    }

    @Override
    public Object getStats() {
        return this.device.getStats();
    }
}