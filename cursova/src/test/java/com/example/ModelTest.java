package com.example;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.modeling.Model;
import com.example.modeling.Producer;
import com.example.modeling.Connection;
import com.example.modeling.SingleChannelSMO;
import com.example.modeling.utils.FunRand;

public class ModelTest {

	@Test
	public void producerConcumerTest() {
		ArrayList<SingleChannelSMO> list = new ArrayList<>();

		Producer producer = new Producer("Producer1", () -> 5.0, 1);
		SingleChannelSMO smo = new SingleChannelSMO("SMO1",1000, () -> 5.0,2);

		Connection connection = new Connection();

		producer.setNext(connection);
		connection.addNext(smo);

		list.add(producer);
		list.add(smo);

		Model model = new Model(list);

		model.simulate(100.0);
		model.getStats().forEach((stats) -> System.out.println(stats));
	}

	@Test
	public void producerTwoConcumerTest() {
		ArrayList<SingleChannelSMO> list = new ArrayList<>();

		Producer producer = new Producer(
			"Producer",() -> 5.0,1);

		Connection connection = new Connection();

		SingleChannelSMO smo1 = new SingleChannelSMO(
			"SMO1",1000, () -> 10.0,2);
		SingleChannelSMO smo2 = new SingleChannelSMO(
			"SMO2",1000, () -> 10.0,2);

		producer.setNext(connection);
		connection.addNext(smo1);
		connection.addNext(smo2, () -> smo1.getStats().getServed() > smo2.getStats().getServed() * 2);

		list.add(producer);
		list.add(smo1);
		list.add(smo2);

		Model model = new Model(list);
		model.simulate(200.0);

		model.getStats().forEach((stats) -> System.out.println(stats));
	}

	@Test
	public void restTest() {
		ArrayList<SingleChannelSMO> list = new ArrayList<>();

		Producer producer = new Producer(
			"Producer", () -> 5.0,1);

		SingleChannelSMO smo1 = new SingleChannelSMO(
			"Zero",1000,() -> 0.0,2);

		SingleChannelSMO smo2 = new SingleChannelSMO(
			"SMO",() -> 5.0,3);

		SingleChannelSMO rest = new SingleChannelSMO(
			"Rest",() -> 5.0,4); 
		rest.setDoneStatus();

		
		Connection connection1 = new Connection();
		Connection connection2 = new Connection();
		Connection restcConnection = new Connection();

		producer.setNext(connection1);
		connection1.addNext(smo1);

		smo1.setNext(connection2);
		connection2.addNext(smo2, () -> rest.getChannelState().isDone());

		restcConnection.addNext(rest, () -> smo2.getChannelState().isDone());
		rest.setNext(restcConnection);


		list.add(producer);
		list.add(smo1);
		list.add(smo2);
		list.add(rest);

		Model model = new Model(list);
		model.simulate(50.0);

		model.getStats().forEach((stats) -> System.out.println(stats));
	}

	@Test
	public void modelTest() {
		ArrayList<SingleChannelSMO> list = new ArrayList<>();

		SingleChannelSMO producer = new Producer(
			"Prod", FunRand.getErlang(8,32), 1);

		SingleChannelSMO smoWithQueue = new SingleChannelSMO(
			"Q_smo", Integer.MAX_VALUE, FunRand.getFixed(0),2);

		SingleChannelSMO loader1 = new SingleChannelSMO(
			"Loader1", FunRand.getExponential(14), 3);

		SingleChannelSMO loader2 = new SingleChannelSMO(
			"Loader2", FunRand.getExponential(12), 3);


		SingleChannelSMO truck1 = new SingleChannelSMO(
			"Truck1", FunRand.getCombined(List.of(
				FunRand.getNotNullNorm(22, 10), 
				FunRand.getUniform(2, 8)
		)), 4); 

		SingleChannelSMO truck2 = new SingleChannelSMO(
			"Truck2", FunRand.getCombined(List.of(
				FunRand.getNotNullNorm(22, 10), 
				FunRand.getUniform(2, 8)
		)), 4);

		SingleChannelSMO truck3 = new SingleChannelSMO(
			"Truck3", FunRand.getCombined(List.of(
				FunRand.getNotNullNorm(22, 10), 
				FunRand.getUniform(2, 8)
		)), 4);

		SingleChannelSMO truck4 = new SingleChannelSMO(
			"Truck4", FunRand.getCombined(List.of(
				FunRand.getNotNullNorm(22, 10), 
				FunRand.getUniform(2, 8)
		)), 4);


		SingleChannelSMO rest1 = new SingleChannelSMO(
			"Rest1", FunRand.getFixed(5), 5);
		rest1.setDoneStatus();

		SingleChannelSMO rest2 = new SingleChannelSMO(
			"Rest2", FunRand.getFixed(5), 5);
		rest2.setDoneStatus();

		SingleChannelSMO rest11 = new SingleChannelSMO(
			"Rest11", FunRand.getNotNullNorm(18, 10), 6);
		rest11.setDoneStatus();

		SingleChannelSMO rest12 = new SingleChannelSMO(
			"Rest12", FunRand.getNotNullNorm(18, 10), 6);
		rest12.setDoneStatus();

		SingleChannelSMO rest13 = new SingleChannelSMO(
			"Rest13", FunRand.getNotNullNorm(18, 10), 6);
		rest13.setDoneStatus();

		SingleChannelSMO rest14 = new SingleChannelSMO(
			"Rest14", FunRand.getNotNullNorm(18, 10), 6);
		rest14.setDoneStatus();


		Connection connection1 = new Connection(2);
		Connection connection2 = new Connection();
		Connection connection3 = new Connection();

		producer.setNext(connection1);
		connection1.addNext(smoWithQueue);
		smoWithQueue.setNext(connection2);

		connection2.addNext(loader1, () -> {
			int countB = 0;

			if (truck1.getChannelState().isReady() && rest11.getChannelState().isDone()) 
				countB++;
			if (truck2.getChannelState().isReady() && rest12.getChannelState().isDone()) 
				countB++;
			if (truck3.getChannelState().isReady() && rest13.getChannelState().isDone()) 
				countB++;
			if (truck4.getChannelState().isReady() && rest14.getChannelState().isDone()) 
				countB++;

			if (loader2.getChannelState().isBusy())
				countB--;

			return countB > 0 && rest1.getChannelState().isDone();
		});

		connection2.addNext(loader2, () -> {
			int countB = 0;

			if (truck1.getChannelState().isReady() && rest11.getChannelState().isDone()) 
				countB++;
			if (truck2.getChannelState().isReady() && rest12.getChannelState().isDone()) 
				countB++;
			if (truck3.getChannelState().isReady() && rest13.getChannelState().isDone()) 
				countB++;
			if (truck4.getChannelState().isReady() && rest14.getChannelState().isDone()) 
				countB++;

			if (loader1.getChannelState().isBusy())
				countB--;

			return countB > 0 && rest2.getChannelState().isDone();
		});

		loader1.setNext(connection3);
		loader2.setNext(connection3);

		connection3.addNext(truck1, () -> {
			return rest11.getChannelState().isDone();
		});

		connection3.addNext(truck2, () -> {
			return rest12.getChannelState().isDone();
		});

		connection3.addNext(truck3, () -> {
			return rest13.getChannelState().isDone();
		});

		connection3.addNext(truck4, () -> {
			return rest14.getChannelState().isDone();
		});

		Connection restCon1 = new Connection();
		rest1.setNext(restCon1);
		restCon1.addNext(rest1, () -> loader1.getChannelState().isDone());

		Connection restCon2 = new Connection();
		rest2.setNext(restCon2);
		restCon2.addNext(rest2, () -> loader2.getChannelState().isDone());


		Connection restCon11 = new Connection();
		rest11.setNext(restCon11);
		restCon11.addNext(rest11, () -> truck1.getChannelState().isDone());

		Connection restCon12 = new Connection();
		rest12.setNext(restCon12);
		restCon12.addNext(rest12, () -> truck2.getChannelState().isDone());

		Connection restCon13 = new Connection();
		rest13.setNext(restCon13);
		restCon13.addNext(rest13, () -> truck3.getChannelState().isDone());

		Connection restCon14 = new Connection();
		rest14.setNext(restCon14);
		restCon14.addNext(rest14, () -> truck4.getChannelState().isDone());


		list.add(producer);
		list.add(smoWithQueue);

		list.add(loader1);
		list.add(loader2);

		list.add(truck1);
		list.add(truck2);
		list.add(truck3);
		list.add(truck4);

		list.add(rest1);
		list.add(rest2);

		list.add(rest11);
		list.add(rest12);
		list.add(rest13);
		list.add(rest14);

		Model model = new Model(list);
		model.simulate(1440.0);

	
		var loader1St = loader1.getStats();
		var loader2St = loader2.getStats();

		var rest1St = rest1.getStats();
		var rest2St = rest2.getStats();

		var truck1St = truck1.getStats();
		var truck2St = truck2.getStats();
		var truck3St = truck3.getStats();
		var truck4St = truck4.getStats();

		var rest11St = rest11.getStats();
		var rest12St = rest12.getStats();
		var rest13St = rest13.getStats();
		var rest14St = rest14.getStats();

		double mean_loader_utilization = 
			(loader1St.getBusyTime() + loader2St.getBusyTime() + rest1St.getBusyTime() + rest2St.getBusyTime()) / 
			2 / loader1St.getTotalTime();

		double mean_truck_utilization = 
			((truck1St.getBusyTime() + truck2St.getBusyTime() + truck3St.getBusyTime() + truck4St.getBusyTime()) + 
			(rest11St.getBusyTime() + rest12St.getBusyTime() + rest13St.getBusyTime() + rest14St.getBusyTime()) +
			(loader1St.getBusyTime() + loader2St.getBusyTime())) / 
			4 / (truck1St.getTotalTime());

		System.out.println(mean_loader_utilization);
		System.out.println(mean_truck_utilization);

		model.getStats().forEach((stats) -> System.out.println(stats));
	}
}
