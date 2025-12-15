package com.example.modeling;

import java.util.List;

import com.example.modeling.utils.Status;

public class Producer extends SMO {

    public Producer(
        String name,
        List<Device> devices,
        int priority
    ) {
        super(name, 0, devices, priority);

        this.getReadyDevices().forEach((device) -> {
            device.process();
            this.stats.addRequest();
        });
    }

    public Producer(
        String name,
        Device device,
        int priority
    ) {
        this(name, List.of(device), priority);
    }

    @Override
    public void process() {
        throw new UnsupportedOperationException("Producer cannot process incoming tasks");
    }

    @Override
    public void handleEvents() {
        for (Device device : devices) {
            if (device.getStatus() == Status.DONE) {
                device.setStatus(Status.READY);
                device.process();

                this.stats.addRequest();
                this.next.ifPresent((next) -> next.process());
            }
        }
    }
}
