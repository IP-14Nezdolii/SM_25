package com.example;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.modeling.Device;
import com.example.modeling.Model;
import com.example.modeling.utils.Status;
import com.example.modeling.Producer;
import com.example.modeling.Connection;
import com.example.modeling.SMO;
import com.example.modeling.utils.FunRand;

public class AppTest {

    @Test
    public void test1() {
        ArrayList<SMO> list = new ArrayList<>();

        Producer producer = new Producer(
                "Producer1",
                List.of(new Device(FunRand.getFixed(5), "Device1")),
                1);

        Connection connection = new Connection();

        SMO smo = new SMO(
                "SMO1",
                1000,
                List.of(
                        new Device(FunRand.getFixed(25), "Device2"),
                        new Device(FunRand.getFixed(25), "Device3")),
                2);

        producer.setNext(connection);
        connection.addNext(smo);

        list.add(producer);
        list.add(smo);

        Model model = new Model(list);
        model.run(100.0);

        model.getStats().forEach((stats) -> System.out.println(stats));
    }

    @Test
    public void test2() {
        ArrayList<SMO> list = new ArrayList<>();

        Producer producer = new Producer(
                "Producer1",
                List.of(new Device(FunRand.getFixed(5), "Device1")),
                1);

        Connection connection = new Connection();

        var device2 = new Device(FunRand.getFixed(15), "Device2");

        SMO smo1 = new SMO(
                "SMO1",
                1000,
                List.of(
                        device2),
                2);

        SMO smo2 = new SMO(
                "SMO2",
                1000,
                List.of(
                        new Device(FunRand.getFixed(15), "Device3")),
                2);

        producer.setNext(connection);
        connection.addNext(smo1);
        connection.addNext(smo2, () -> device2.getStatus() == Status.BUSY);

        list.add(producer);
        list.add(smo1);
        list.add(smo2);

        Model model = new Model(list);
        model.run(100.0);

        model.getStats().forEach((stats) -> System.out.println(stats));
    }

    @Test
    public void test3() {
        ArrayList<SMO> list = new ArrayList<>();

        Producer producer = new Producer(
                "Producer1",
                List.of(new Device(FunRand.getFixed(5), "Device1")),
                1);

        Connection connection = new Connection(2);

        var device2 = new Device(FunRand.getFixed(15), "Device2");

        SMO smo1 = new SMO(
                "SMO1",
                1000,
                List.of(
                        device2),
                2);

        SMO smo2 = new SMO(
                "SMO2",
                1000,
                List.of(
                        new Device(FunRand.getFixed(15), "Device3")),
                2);

        producer.setNext(connection);
        connection.addNext(smo1);
        connection.addNext(smo2, () -> device2.getStatus() == Status.BUSY);

        list.add(producer);
        list.add(smo1);
        list.add(smo2);

        Model model = new Model(list);
        model.run(100.0);

        model.getStats().forEach((stats) -> System.out.println(stats));
    }

    @Test
    public void test4() {
        ArrayList<SMO> list = new ArrayList<>();

        var device2 = new Device(FunRand.getFixed(3), "Device2");

        var device3 = new Device(FunRand.getFixed(2), "Device3");
        device3.setStatus(Status.DONE);

        SMO producer = new Producer("Producer1", List.of(new Device(FunRand.getFixed(2.5), "Device1")), 1);
        SMO smo1 = new SMO("SMO1", 1, List.of(device2), 2);

        SMO rest = new SMO("Rest1", List.of(device3), 3);

        Connection connection1 = new Connection(2);
        Connection connection2 = new Connection();

        producer.setNext(connection1);
        connection1.addNext(smo1, () -> device3.getStatus() == Status.DONE);

        connection2.addNext(rest, () -> device2.getStatus() == Status.DONE);
        rest.setNext(connection2);

        list.add(producer);
        list.add(smo1);
        list.add(rest);

        Model model = new Model(list);
        model.run(100.0);

        model.getStats().forEach((stats) -> System.out.println(stats));
    }
}
