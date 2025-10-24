package com.example.modeling;

import com.example.modeling.components.Component;
import com.example.modeling.device.Device;
import com.example.utils.DeviceRand;
import com.example.utils.DeviceRand.NextPriority;

public class Producer extends Component {
    private final Device device;

    public Producer(
            NextPriority priority,
            DeviceRand rand,
            String name) {
        super(priority, name);

        this.device = new Device(rand, name);
        this.device.process();
    }

    public Producer(
            DeviceRand rand,
            String name) {
        super(name);

        this.device = new Device(rand, name);
        this.device.process();
    }

    @Override
    public void run(double time) {
        if (this.device.getWorkTime().isEmpty()) {
            this.device.process();
        }

        double workTime = this.device.getWorkTime().get();
        double currentTime = time;

        while (currentTime > workTime) {
            if(this.device.run(workTime)) {
                this.device.process(); 

                var next = this.getNextChosen();
                if (next.isPresent()) {
                    next.get().process();
                }
            }

            currentTime -= workTime;
            workTime = this.device.getWorkTime().get();
        }

        if (currentTime > 0) {
            if(this.device.run(currentTime)) {
                this.getNextChosen().ifPresent(next -> next.process());
                this.device.process();
            };
        }

        if (this.device.getWorkTime().isEmpty()) {
            this.device.process();
        }
    }

    @Override
    public boolean process() {
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public Object getStats() {
        return this.device.getStats();
    }

    @Override
    public java.util.Optional<Double> getWorkTime() {
        return this.device.getWorkTime();
    }
}
