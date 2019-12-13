package fil.algorithm.BLRandomServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.*;

import fil.resource.substrate.Rpi;
import fil.resource.substrate.SubstrateLink;
import fil.resource.virtual.*;
import fil.topology.routing.NetworkRouting;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class EdgeOptimal  {
	final static int NUM_PI = 200;
	final static int REQUEST = 22;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int ARRAY_SIZE = 100; //100
	final static double limitLatency = 50;

	public static boolean [] piState = new boolean [NUM_PI];
	public static int [] totalChainMap = new int [NUM_PI];
	public static int [] totalChainRequest = new int [NUM_PI];
	public static int [] totalOffDecPi = new int [NUM_PI];
	public static int [] totalOffDenPi = new int [NUM_PI];
	public static double [] totalPowerEdge = new double [NUM_PI];
	
	public static int count = 0;
	public static int rejectServer = 0;
	public static int rejectPi = 0;
	public static int requestNumChain = 0;
	
	public static int sumAllRequest = 0;
	public static int sumMapRequest = 0;
	public static int totalChainSystem_temp = 0;
	public static int totalDecOffLoad_temp = 0;
	public static int totalDenOffLoad_temp = 0;
	public static int totalChainReject_temp = 0;
	public static int numSFCTotal = 0;
	public static int finalNumChain = 0;
	public static int finalChainReject = 0;
	public static int finalOffDecode = 0;
	public static int finalOffDensity = 0;
	
	public static double totalPowerSystem_temp = 0; 
	public static double totalPowerEdge_temp = 0;
	public static double finalUsedPowerPi = 0;
	public static double finalBandWidth = 0;
	public static double finalCPUPi = 0;
	
	public static int maxChain = 0;
	
	
	
	public EdgeOptimal() {;
		//numSFCTotal = 0;
	}
	
	@SuppressWarnings("deprecation")
	public static double [] solveModel() {
		double [] result = new double [3];
		try {
			// Instantiate an empty model
			int n = 3;
			int m = 4;
			double [] c = {1, 0, 0};
			double[][]A = {{3, 8, 13.6}, {47.3, -31, -16.3}, {-1, 1, 0}, {0, -1, 1}};
			double[] b = {100, 100, 0, 0}; // capacity constraints
			
			IloCplex model = new IloCplex();
			
			// Define an array of decision variables
			IloNumVar[] x = new IloNumVar[n];
			for(int i = 0; i < n; i++) {
				// Define each variable's range from 0 to +Infinity
//				x[i] = model.numVar(0, Double.MAX_VALUE);
				x[i] = model.intVar(0, Integer.MAX_VALUE);
			}
			
			// Define the objective function
			IloLinearNumExpr obj = model.linearNumExpr();
			// Add expressions to the objective function
			for(int i = 0; i < n; i++) {
				obj.addTerm(c[i], x[i]);
			}
			// Define a maximization problem
			model.addMaximize(obj);
			
			// Define the constraints
			for(int i = 0; i < m; i++) { // for each constraint
				IloLinearNumExpr constraint = model.linearNumExpr();
				for(int j = 0; j < n; j++) { // for each variable
					constraint.addTerm(A[i][j], x[j]);
				}
				// Define the RHS of the constraint
				model.addLe(constraint, b[i]);
			}
			
			// Suppress the auxiliary output printout
			model.setParam(IloCplex.IntParam.SimDisplay, 0);
			
			// Solve the model and print the output
			boolean isSolved = model.solve();
			if(isSolved) {
				double objValue = model.getObjValue();
				System.out.println("obj_val = " + objValue);
				
				for(int i = 0; i < n; i++) {
					System.out.println("x[" + (i+1) + "] = " + model.getValue(x[i]));
					result[i] = model.getValue(x[i]);
				}
			} else {
				System.out.println("Model not solved :(");
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	public static void edgeMapping(int i, int numChain, double [] service, Topology topo, LinkedList<Rpi> listRpi, Map<Rpi, LinkedList<SFC>> listRpiSFC, MappingServer mappingServer,
		LinkedList<SFC> listSFCFinal, NetworkRouting networkRouting) {
		///variable init////////////////////////////////////////////////
		rejectServer = 0;
		rejectPi = 0;
		
		finalBandWidth = 0.0;
		finalCPUPi = 0.0;
		finalChainReject = 0;
		finalNumChain = 0;
		finalOffDecode = 0;
		finalOffDensity = 0;
		////////////////////////////////////////////////////////////////
		int numChainRequest = numChain;
		
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		ReceiveDensity receive = new ReceiveDensity();
			
		System.out.println("Start mapping with numChain " + numChain);
		
		LinkedList<SFC> listSFCTemp = new LinkedList<>();
		///////////////Fetch input for ILP solver//////////////////////////////////////////
		int numCapture = (int) service[0];
		int numDecode = (int) service[1];
		int numDensity = (int) service[2];
				
		while (numChain > numCapture) {
			numChain --;
		}
		
		if(numChain < 0) throw new java.lang.Error("numChain < 0??");
		
		if(numChain == 0) {
			System.out.println("numChain == 0 nhe ban ");
			finalChainReject = numChainRequest; 
			return; // if no chain need to be mapped then no need to run MAP 
		}
		int notOffDecode = 0;
		int notOffDensity = 0;
		///////////////////////////////////////////////////////////////////////////////////
		for(int numSFC = 0; numSFC < numChain; numSFC++ ) { // initialize SFC list
			SFC sfc = new SFC(String.valueOf(numSFCTotal), i); //sai ten khi remap
			sfc.setSfcID(i); // belong to which Rpi?
			sfc.setServicePosition(capture, true);
			sfc.setServicePosition(receive, false);
			
			if(numDecode > 0) {
				sfc.setServicePosition(decode, true);
				numDecode --;
				notOffDecode ++;
			}
			else sfc.setServicePosition(decode, false);
			
			if(numDensity > 0) {
				sfc.setServicePosition(density, true);
				numDensity --;
				notOffDensity ++;
			}
			else sfc.setServicePosition(density, false);
			
			listSFCTemp.add(sfc);
			numSFCTotal++;
		}
	
		/////////////////////////////////////////////////////////////////////////////////////
		double totalBandwidth = (numChain - notOffDecode)*capture.getBandwidth() + (notOffDecode - notOffDensity)*decode.getBandwidth() + (notOffDensity)*density.getBandwidth();
		double cpuPi = numChain*capture.getCpu_pi() + notOffDecode*decode.getCpu_pi() + notOffDensity*density.getCpu_pi();
				
		System.out.println("CPU Pi after this process is " + (cpuPi + listRpi.get(i).getUsedCPU()) + " bandwidth after is " + (totalBandwidth + listRpi.get(i).getUsedBandwidth()) + " !!! \n");
		double powerChainPi = numChain*capture.getPower() + notOffDecode*decode.getPower() + notOffDensity*density.getPower();
					
		listSFCFinal.clear();
		for(int index = 0; index < listSFCTemp.size(); index++) {
			listSFCFinal.add(listSFCTemp.get(index));
		}
		
		service[0] -= numChain;
		if(service[0] == 0) piState[i] = false;
		else if(service[0] < 0) throw new java.lang.Error("number mapped service greater than number can be mapped ");
		else ;
		service[1] -= notOffDecode;
		service[2] -= notOffDensity;
		
		finalNumChain = numChain;
		System.out.println("finalnumchain inside function equals "+ finalNumChain);
		finalChainReject = requestNumChain - finalNumChain;
		finalUsedPowerPi = powerChainPi;
		
		if(finalUsedPowerPi < 0) throw new java.lang.Error("Power pi smaller than 0 ");
		
		finalBandWidth = totalBandwidth;
		finalCPUPi = cpuPi;
		finalOffDecode = numChain - notOffDecode;
		finalOffDensity = numChain - notOffDensity;
		
		if(finalOffDecode < 0 || finalOffDensity < 0)
			throw new java.lang.Error("finalOffDecode or Density lower than 0");
	}///end mapping function
	
	public static double calculatePowerServer(double cpuServer) {
		double numServer = Math.floor(cpuServer/100);
		double cpuFragment = cpuServer - 100*numServer;
		 return numServer*powerServer(100) + powerServer(cpuFragment);
	}
	
	public static double powerServer(double cpu) {
		return (120 + 380*cpu/100);
	}
	
	public static void write_integer (String filename, ArrayList<Integer> x) throws IOException{ //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush();  
		outputWriter.close();  
	}
	
	public static void write_integer (String filename, int [] x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush();  
		outputWriter.close();  
	}

	public static void write_double (String filename, ArrayList<Double> x) throws IOException { //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static void write_double (String filename, double [] x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static  MappingServer deepClone(MappingServer object){
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			ObjectInputStream objectInputStream = new ObjectInputStream(bais);
			return (MappingServer) objectInputStream.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static  Topology deepClone(Topology object){
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			ObjectInputStream objectInputStream = new ObjectInputStream(bais);
			return (Topology) objectInputStream.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static  NetworkRouting deepClone(NetworkRouting object){
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			ObjectInputStream objectInputStream = new ObjectInputStream(bais);
			return (NetworkRouting) objectInputStream.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static  LinkedList deepClone(LinkedList object){
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(object);
			ByteArrayInputStream bais = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
			ObjectInputStream objectInputStream = new ObjectInputStream(bais);
			return (LinkedList) objectInputStream.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		 // Creating a File object that represents the disk file. 
//        PrintStream out = new PrintStream(new File("./Plot/output.txt")); 
//        System.setOut(out); 
//		
		Map<Rpi, LinkedList<SFC>> listRpiSFC = new HashMap<>(NUM_PI);
		Topology topo = new Topology();
		FatTree fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH);
		MappingServer mappingServer = new MappingServer();
		NetworkRouting networkRouting = new NetworkRouting();
		
		ArrayList<Integer> edgePosition = new ArrayList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		
		ArrayList<double[]> serviceInPi = new ArrayList<>(NUM_PI);
		
		Arrays.fill(piState, true);
		Arrays.fill(totalChainMap, 0);
		Arrays.fill(totalChainRequest, 0);


		ArrayList<Double> totalPowerSystem = new ArrayList<Double>();
		ArrayList<Double> totalEdgePowerSystem = new ArrayList<Double>();
		ArrayList<Double> serverUtilization = new ArrayList<Double>();
		ArrayList<Double> totalChainAcceptance = new ArrayList<Double>();
		ArrayList<Integer> listServerUsed = new ArrayList<Integer>();
		ArrayList<Integer> totalChainSystem = new ArrayList<Integer>();
		ArrayList<Integer> totalDecOffload = new ArrayList<Integer>();
		ArrayList<Integer> totalDenOffload = new ArrayList<Integer>();
		ArrayList<Integer> totalChainReject = new ArrayList<Integer>();
		ArrayList<Double> totalLoadEdge = new ArrayList<Double>();
		ArrayList<Double> totalBwEdge = new ArrayList<Double>();
	
		
		double [] sumLoadNumPi= new double [REQUEST];  ///for storing load of numPi
		double [] sumBwNumPi = new double [REQUEST];
		Arrays.fill(sumLoadNumPi, 0);
		Arrays.fill(sumBwNumPi, 0);
	

		double acceptance = 0;
		int numRequest = 0; // number of request
	////Run ILP to calculate maximum number of chain a Pi can run///
		/*
		 * n: number of decision variable
		 * m: number of constraints
		 * c: objective function coefficients
		 * A: constraints' coefficients
		 * b: cost coefficients of contraints
		 */
		double [] result = solveModel();
		
		////////////////////////////////////////////////////////////////
		LinkedList<Rpi> listRpi = new LinkedList<Rpi>(); //create  a number of Pi
		for(int i = 0; i < NUM_PI; i++ ) {
			double [] resultTemp = new double [result.length];
			for (int k = 0; k < resultTemp.length; k++) {
		         resultTemp[k] = result[k];
		      }
			Rpi rpi = new Rpi();
			listRpi.add(rpi);
			listRpiSFC.put(rpi, new LinkedList<SFC>());
			serviceInPi.add(i, resultTemp);
		}
		////////////////////////////////////////////////////////////////
		GenRequest genRequest = new GenRequest();
		
		double cpuUtilization = 0;
		ArrayList<Integer> requestRandomReceive = new ArrayList<>();
		
		REQUEST_LOOP:
		while (numRequest < REQUEST ) { //////////////////////////////////////////////////////////////////////////////////////////
			
			int numRequestReceive = 0;
			do {
				numRequestReceive = genRequest.joinRequest(numRequest);	
			}while (numRequestReceive == 0);
			
			sumAllRequest = 0;
			sumMapRequest = 0;
			
			double sumLoadPi = 0;
			double sumBwPi = 0;
			
			
			LinkedList<Double> loadEdgeNumPi = new LinkedList<>();
			LinkedList<Double> bwEdgeNumPi = new LinkedList<>();
			LinkedList<Integer> checkPi = new LinkedList<>();
			
			boolean doneThisRequest = false;
			int numPiReceive = 0; // number of Pi receives request

		
			while (doneThisRequest == false) { //change i < 1 to i < num_pi for mapping every pi/////////////////////////////////
				//////////////////////Identify which Pi will receive request////////////////////////////////////////////
//				count_map++;
				Random rand = new Random();
				int i = rand.nextInt(NUM_PI); // choose specific Pi that will receive request
				int flag = 0;
				//check random number
				if(checkPi.size() == NUM_PI) {
					checkPi.clear();
				}
				for(int checkpi = 0; checkpi < checkPi.size(); checkpi++) {
					if(checkPi.get(checkpi) == i) {
						flag = 1;
						break;
					}
				}
				
				if(flag == 1) {
					continue;
				}
				else {
					checkPi.add(i);
					numPiReceive++;
				}
								
				/////////////////////Identify number of chain that Pi will receive///////////////////////////////////////
				
				requestNumChain = 0;
				requestNumChain = rand.nextInt(3) + 1;
				sumAllRequest += requestNumChain;
				while(sumAllRequest > numRequestReceive) {
					sumAllRequest -= requestNumChain;
					requestNumChain = rand.nextInt(3) + 1;
					sumAllRequest += requestNumChain;
					System.out.println("tim kiem request tot nhat");
				}
				if(sumAllRequest == numRequestReceive) {
					System.out.println("this is the final request of this set");
					doneThisRequest = true;
				}
				else if(sumAllRequest > numRequestReceive) {
					throw new java.lang.Error("Fatal error: sumAllRequest has exceeded numRequestReceive");
				}
				else {
					;
				}
				requestRandomReceive.add(requestNumChain);
				int numChain = requestNumChain;
				/////////////////////////////////////////////////////////////////////////////////////////
			
				//sfc request to server
				LinkedList<SFC> listSFC = new LinkedList<SFC>(); //create  a number of Pi
				LinkedList<SFC> listSFCFinal = new LinkedList<SFC>();
				
				//boolean networkSuccess = false;
				System.out.println("this belongs to request number " + numRequest);
				System.out.println("Pi number " + (i+1)+ " with " +numChain+ " chains need to be mapped \n");
				System.out.println("this pi has mapped  "+ listRpiSFC.get(listRpi.get(i)).size());
				
				if (piState[i] == false) { // this Pi cannot map more chain
					System.out.println("Pi number "+(i+1)+" is out of order ...\n");
					rejectPi = 1;
				}
				else edgeMapping(i, numChain, serviceInPi.get(i), topo, listRpi, listRpiSFC, mappingServer, listSFCFinal, networkRouting);
				
				//////////////////////////////////////////////////////////////////////////////////////////
//				System.out.println("finalnumchain outside function equals "+ finalNumChain);
//				System.out.println("finalCPUPi outside function equals "+ finalCPUPi);
				
				if(finalNumChain != 0) { // new set of chain has been mapped
					System.out.println("inside finalnumchain !=0");
					
					listRpi.get(i).setUsedCPU(finalCPUPi); // change CPU pi-server
					listRpi.get(i).setUsedBandwidth(finalBandWidth); //change Bandwidth used by Pi
					listRpi.get(i).setCurrentPower(finalUsedPowerPi);
					
//					run mapping server for final listSFC
					
					int position = rand.nextInt(4); // random a position where request comes from
					networkRouting.NetworkRun(edgePosition.get(position), listSFCFinal, listRpi.get(i));
					
					mappingServer.runMapping(listSFCFinal, topo);
					
					double finalPowerServer = mappingServer.getPower();
					double finalPower = finalUsedPowerPi + finalPowerServer;
					
					totalChainMap[i] += finalNumChain; // for remapping purpose
					totalOffDecPi[i] += finalOffDecode; //
					totalOffDenPi[i] += (finalOffDensity+finalNumChain); // 
					totalChainRequest[i] += requestNumChain; //
					totalPowerEdge[i] += finalUsedPowerPi;
					
					totalDecOffLoad_temp += finalOffDecode;
					totalDenOffLoad_temp += (finalOffDensity+finalNumChain);
					
					totalPowerEdge_temp += finalUsedPowerPi; //calculate System Power
					totalPowerSystem_temp = totalPowerEdge_temp + finalPowerServer;
					
					sumMapRequest += finalNumChain; //num of accepted request of a Pi
					
	//				gan lai mapping server
	//				System.out.println("gan lai mappingserver with power server final is: " + mappingServerFinal.getPower());
//					mappingServer = deepClone(mappingServerFinal);
	//				System.out.println("mappingserver da duoc gan lai with power server official is: " + mappingServer.getPower());
	//				System.out.println("mappingserver da duoc gan lai with power server official is: " + mappingServer.getServiceMapping().getPowerServer());
//					topo = deepClone(topoFinal);
//					networkRouting = deepClone(networkRoutingFinal);
										
					int bwtotal = 0;
					System.out.println(networkRouting.listBW.size());
					for (int h = 0; h < networkRouting.listBW.size(); h++)
						bwtotal += networkRouting.listBW.get(h);
					System.out.println("bwtotal da dung la " + bwtotal);
					//topoTemp = deepClone(topoFinal);
					
					System.out.println("Final power finalPower " + finalPower);
	//				System.out.println("sumLoadPi get Used "+ listRpi.get(i).getUsedCPU() + " \n");
	//				System.out.println("sumLoadPi ..." +sumLoadPi+" finalCpuPi " + finalCPUPi + "\n");
					totalChainSystem_temp += finalNumChain; // num of accepted request all over the systen
					totalChainReject_temp += finalChainReject;
					
					listSFC.clear();
					LinkedList<SFC> listTemp = mappingServer.getListSFC();
					for(int element = 0; element < listTemp.size();element++) {
						listSFC.add(listTemp.get(element));
					}
					
					
					if(listSFC.size() > 7) {
						throw new java.lang.Error("Number of Mapped chain exceeds 7");
					}
					
					if(!listRpiSFC.get(listRpi.get(i)).isEmpty()) {
						System.out.println("num chain ban dau la: " + listRpiSFC.get(listRpi.get(i)).size());
						LinkedList<SFC> listSFCTemp = new LinkedList<>();
						listSFCTemp = listRpiSFC.get(listRpi.get(i));
						listSFC.addAll(listSFCTemp);
						listRpiSFC.put(listRpi.get(i), listSFC);
						System.out.println("num chain cuoi cung  la: " + listRpiSFC.get(listRpi.get(i)).size());
					} else listRpiSFC.put(listRpi.get(i), listSFC);
					if(listRpiSFC.get(listRpi.get(i)).size() > 7){
						throw new java.lang.Error("FNumber of Pi chain exceeds 7");
					}
				}
				
				if (rejectServer == 1) {
					finalChainReject = requestNumChain; // reject all chain
					totalChainReject_temp += finalChainReject;
				}
				if (rejectPi == 1) { // this means CPU is also not enough
					System.out.println(" Raspberry Pi number " + (i+1) + " is out of resource \n");
					System.out.println(" Raspberry Pi number " + (i+1) + " CPU Pi " + listRpi.get(i).getRemainCPU() +"\n");
					finalChainReject = requestNumChain; // reject all chain
					totalChainReject_temp += finalChainReject;
				}
				
				totalDecOffload.add(count, totalDecOffLoad_temp); // sum of all case offloading decode
				totalDenOffload.add(count, totalDenOffLoad_temp);
				totalPowerSystem.add(count, totalPowerSystem_temp);
				totalEdgePowerSystem.add(count, totalPowerEdge_temp);
				totalChainSystem.add(count, totalChainSystem_temp);
				totalChainReject.add(count, totalChainReject_temp);
				
				count++;
				
	//			calculate total CPU, bandwidth for number of Pi
	//			array for saving
				loadEdgeNumPi.add(listRpi.get(i).getUsedCPU());
				bwEdgeNumPi.add(listRpi.get(i).getUsedBandwidth());		
				System.out.println("Pi number " + (i+1) + " is out of duty \n");
				
			} //end Rpi for loop
			
			//a for loop to get result of CPU, bw of number of Pis
			for(int index = 0; index < numPiReceive; index++) {
				sumLoadNumPi[numRequest] += (loadEdgeNumPi.get(index)/numPiReceive);
				sumBwNumPi[numRequest] += (bwEdgeNumPi.get(index)/numPiReceive);
			}
				
				//a for loop to get result total cpu bw of all Pi
			for (int index = 0; index < NUM_PI; index++) {
				sumLoadPi += listRpi.get(index).getUsedCPU();
				sumBwPi += listRpi.get(index).getUsedBandwidth();
			}
			
		//calculate power and number of chain relation
			totalLoadEdge.add(numRequest,(sumLoadPi/(NUM_PI)));
			totalBwEdge.add(numRequest,(sumBwPi/NUM_PI));
			System.out.println("sumLoadPi ..." + sumLoadPi + "\n");
			acceptance = (sumMapRequest*1.0)/sumAllRequest; //after a request
			totalChainAcceptance.add(numRequest, acceptance);
			
			if (totalChainSystem_temp > 1300) {
				break;
			}
			
			cpuUtilization = mappingServer.getServiceMapping().getCpuUtilization();
			serverUtilization.add(cpuUtilization);
			int serverUsed = mappingServer.getServiceMapping().getServerUsed();
			listServerUsed.add(serverUsed);
			numRequest++;
		} // end while loop (request)
		////////////////////////LEAVE PROCESS???????????????????????????????????
//		Map<Integer, Boolean> piStateLeave = new HashMap<>(NUM_PI);
//		for (int index = 0; index < NUM_PI; index++) {
//			System.out.println(" chay vao day lan thu " + index);
//			if (listRpiSFC.get(listRpi.get(index)).size() != 0) { // check if this pi has been used to map chain be4
//				System.out.println("this Pi has chain with num = " + listRpiSFC.get(listRpi.get(index)).size());
//				piStateLeave.put(index, true);
//			}
//			else {
//				System.out.println("No chain in this Pi? wtf?");
//				piStateLeave.put(index, false);
//			}
//			
//		}
//		Arrays.fill(piState, true);
		
//		LEAVE_LOOP:
//		while(numRequest <= 31) {////////////////////////////////////////////////////////////////
//			numRequest = 20;
//			System.out.println("Start leaving process with totalchain now equals " + totalChainSystem_temp);
//			int numRequestLeave;
//			int sumAllRequestLeave = 0;
//			//ArrayList<Integer> checkPi = new ArrayList<>();
//			do {
//				numRequestLeave = genRequest.leaveRequest(numRequest);	
//			}while (numRequestLeave == 0);
//			boolean doneThisRequest = false;
//			
//			while (doneThisRequest == false) {
//				System.out.println("Identify which Pi will receive request");
//				//////////////////////Identify which Pi will receive request////////////////////////////////////////////
////				count_map++;
//				Random rand = new Random();
//				int i = rand.nextInt(NUM_PI); // choose specific Pi that will receive request
//				
//				if(piStateLeave.containsValue(true)) {
//					//just continue;
//				}
//				else {
//					System.out.println("No chain left to leave");
//					break LEAVE_LOOP;
//				}
//				
//				if(piStateLeave.get(i) == false) {
//					continue;
//				}
//				
//				if(listRpiSFC.get(listRpi.get(i)).size() == 0){
//					System.out.println("this Pi has no chain left to leave");
//					piStateLeave.put(i, false);
//					continue;
//				}
//				
//				System.out.println("Pi num " + i + " is true with size = " + listRpiSFC.get(listRpi.get(i)).size() );				
//				/////////////////////Identify number of chain that Pi will receive///////////////////////////////////////
//				double cpu_temp = listRpi.get(i).getRemainCPU();
//				double bw_temp = listRpi.get(i).getRemainBandwidth();
//				
//				int requestRemain = numRequestLeave - sumAllRequestLeave;
//				int numChainLeave = genRequest.receiveRequestLeave(requestRemain, cpu_temp, bw_temp);
//				
//				while(listRpiSFC.get(listRpi.get(i)).size() < numChainLeave || requestRemain < numChainLeave) {
//					numChainLeave --;
//				}
//				
//				if(listRpiSFC.get(listRpi.get(i)).size() == numChainLeave) {
//					piStateLeave.put(i, false); // indicate that this Pi has no chain to leave
//				}
//				
//				sumAllRequestLeave += numChainLeave;
//				
//				if(sumAllRequestLeave == numRequestLeave) {
//					System.out.println("this is the final request of this set");
//					doneThisRequest = true;
//				}
//				else if(sumAllRequestLeave > numRequestLeave) {
//					throw new java.lang.Error("Fatal error: sumAllRequest has exceeded numRequestReceive");
//				}
//				else {
//					;
//				}
//				
//				LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
//				LinkedList<SFC> listPossibleLeave = new LinkedList<>();
//					
//				int numChainRemain = listRpiSFC.get(listRpi.get(i)).size() - numChainLeave; //  remained chains
//				
//				mappingServer.getServiceMapping().resetRpiSFC(listRpiSFC.get(listRpi.get(i)), listLinkBandwidth); // reset mapped chains of Pi_index
//				
//				for (int index = 0; index < numChainLeave; index++) {
//					listPossibleLeave.add(listRpiSFC.get(listRpi.get(i)).getFirst());
//					listRpiSFC.get(listRpi.get(i)).removeFirst();
//				}
//									
//				//now remap////////////////////////////////
//				LinkedList<SFC> listSFCFinal = new LinkedList<SFC>();
//				listRpi.get(i).reset();//reset Rpi
//				//networkRouting.NetworkReset(listRpi.get(i), numChainLeave); // reset at topology
//				edgeMapping(i, numChainRemain, topo, listRpi, listRpiSFC, mappingServer, listSFCFinal, networkRouting); //  re-map at edge
//					
//				if(listSFCFinal.isEmpty()) {
//					continue;
//				}
//				else {
//					mappingServer.runMapping(listSFCFinal, topo); // map at server
//				}
//					
//				///////////////////////////////////////////	
//				System.out.println("total chain system has been reduced before is  " + totalChainSystem_temp);
//				totalChainSystem_temp -= numChainLeave;
//				System.out.println("total chain system has been reduced after is  " + totalChainSystem_temp);
//				
//				if(totalChainSystem_temp <= 300) {
//					System.out.println("system has reached limit number of chains");
//					break LEAVE_LOOP;
//				}
//			}
//			mappingServer.getServiceMapping().leavingRemapServer(); //consolidationServer
//			int serverUsed = mappingServer.getServiceMapping().getServerUsed();
//			double utilization = mappingServer.getServiceMapping().getCpuUtilization();
//			serverUtilization.add(utilization);
//			listServerUsed.add(serverUsed);
//			numRequest++;
//		}/////////////////////////////////////////////////////////////////////////////////////////////////////
		
	
		try {
			write_double("./PlotOverall/EdgeOptimal/serverUtilizationOP.txt",serverUtilization);
			write_integer("./PlotOverall/EdgeOptimal/listServerUsedOP.txt",listServerUsed);
			write_integer("./PlotOverall/EdgeOptimal/requestRandomOP.txt",requestRandomReceive);
			write_integer("./PlotOverall/EdgeOptimal/totalDecOffloadOP.txt",totalDecOffload);
			write_integer("./PlotOverall/EdgeOptimal/totalDenOffloadOP.txt",totalDenOffload);
			write_double("./PlotOverall/EdgeOptimal/totalPowerSystemOP.txt",totalPowerSystem);
			write_double("./PlotOverall/EdgeOptimal/totalEdgePowerSystemOP.txt", totalEdgePowerSystem);
			write_double("./PlotOverall/EdgeOptimal/totalLoadEdgeOP.txt",totalLoadEdge);
			write_double("./PlotOverall/EdgeOptimal/totalBwEdgeOP.txt",totalBwEdge);
			write_double("./PlotOverall/EdgeOptimal/totalChainAcceptanceOP.txt",totalChainAcceptance);
			write_double("./PlotOverall/EdgeOptimal/sumLoadNumPiOP.txt", sumLoadNumPi);
			write_double("./PlotOverall/EdgeOptimal/sumBwNumPiOP.txt", sumBwNumPi);
			write_integer("./PlotOverall/EdgeOptimal/totalChainSystemOP.txt",totalChainSystem);
			write_integer("./PlotOverall/EdgeOptimal/totalChainRejectOP.txt",totalChainReject);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
//	System.out.println("Server remain " + physicalServer.getRemainCPU() + " \n");
	}
	

}