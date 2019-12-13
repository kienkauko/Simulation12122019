package fil.resource.virtual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fil.resource.virtual.PoissonDistribution;

public class GenRequest {
	private PoissonDistribution number;
//	private int [] LUT = {32,26,22,20,20,21,23,27,37,55,89,143,211,284,335,350,337,308,282,267,263,264,269,273,
//			276,280,287,296,313,330,350,372,375,356,304,284,269,206,151,125,110,96,80,59}; //30 min/a request
	private int [] LUT = {32,22,20,23,37,89,211,335,337,282,263,269,
			276,287,313,350,375,304,269,151,110,80}; //30 min/a request
	private ArrayList<Integer> numChainPoisson;
	
	public GenRequest() {
		this.number = new PoissonDistribution();
		this.numChainPoisson = new ArrayList<>();
		
	}
	

	public Map<Integer,HashMap<Integer,Integer>> joinRequest() {
		int size = LUT.length;
		Map<Integer,HashMap<Integer,Integer>> allRequest = new HashMap<>();
		
		for(int i = 0; i < size; i++) { // size = 44 requests
			int numChain = number.sample(LUT[i]);
			this.numChainPoisson.add(numChain);
			Random rand = new Random();
			HashMap<Integer, Integer> request = new HashMap<>();
			for(int j = 0; j < 300; j++) {
				request.put(j, 0);
			}
			
			while(numChain > 0) {
				int device = rand.nextInt(300);
				int newNumChain = request.get(device) + 1;
				request.put(device, newNumChain);
				numChain --;
			}
			allRequest.put(i, request);
		}
		return allRequest;
	}
	
	public int leaveRequest(int time) {
		double lamdaTemp = (-9.5)*time + 290;
		if(lamdaTemp < 0) {
			return 0;
		}
		double lamda = Math.floor(lamdaTemp);
		return number.sample(lamda);
	}
	
	public int receiveRequestJoin(int totalRequestRemain, double cpu, double bw) {
		double lamda;
		double request  = 0;
		double resource_condition = (cpu + bw)/200;
		if (resource_condition >= 0 && resource_condition <= 0.13) lamda = 0;
		else if (resource_condition > 0.13 && resource_condition <= 0.25) lamda = 1;
		else if (resource_condition > 0.25 && resource_condition <= 0.5) lamda = 2;
		else if (resource_condition > 0.5 && resource_condition <= 1.0) lamda = 3;
		else {
			throw new java.lang.Error("Error occurs at lamda process");
		}
		do {
			request = number.sample(lamda);
		} while (request > 3 || request > totalRequestRemain);
		return (int) request;
	}
	
	public int receiveRequestLeave(int totalRequestRemain, double cpu, double bw) {
		double lamda;
		double request  = 0;
		double resource_condition = (cpu + bw)/200;
		if (resource_condition >= 0 && resource_condition <= 0.13) lamda = 3;
		else if (resource_condition > 0.13 && resource_condition <= 0.25) lamda = 2;
		else if (resource_condition > 0.25 && resource_condition <= 0.5) lamda = 1;
		else if (resource_condition > 0.5 && resource_condition <= 1.0) lamda = 0;
		else {
			throw new java.lang.Error("Error occurs at lamda process");
		}
		do {
			request = number.sample(lamda);
		} while (request > 3 || request > totalRequestRemain);
		return (int) (request + 1);
	}
	
	public ArrayList<Integer> getNumChainPoisson() {
		return numChainPoisson;
	}
}
