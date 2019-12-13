package fil.algorithm.SFCCMOP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import fil.resource.substrate.Rpi;
import fil.resource.substrate.SubstrateLink;
import fil.resource.virtual.*;
import fil.topology.routing.NetworkRouting;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SFCCMOP  {
	final static int NUM_PI = 300;
	final static int REQUEST = 24;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int ARRAY_SIZE = 100; //100
	final static double LIVE_TIME = 0.08;

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
	public static int totalChainActive_temp = 0;
	public static int totalDecOffLoad_temp = 0;
	public static int totalDenOffLoad_temp = 0;
	public static int totalChainReject_temp = 0;
	public static int numSFCTotal = 0;
	public static int finalNumChain = 0;
	public static int finalChainReject = 0;
	public static int finalOffDecode = 0;
	public static int finalOffDensity = 0;
	public static int numChainLeave = 0;
	public static double totalPowerSystem_temp = 0; 
	public static double totalPowerEdge_temp = 0;
	public static double totalPowerServer_temp = 0;
	public static double finalUsedPowerPi = 0;
	public static double finalBandWidth = 0;
	public static double finalCPUPi = 0;
	
	
	
	
	public SFCCMOP() {;
		//numSFCTotal = 0;
	}
	
	public static void sfccmMapping(int numRequest, int i, int numChain, Topology topo, LinkedList<Rpi> listRpi, 
			Map<Rpi, LinkedList<SFC>> listRpiSFC, MappingServer mappingServer,
			LinkedList<SFC> listSFCFinal, NetworkRouting networkRouting) {
		rejectServer = 0;
		rejectPi = 0;
		
		finalUsedPowerPi = 0.0;
		finalBandWidth = 0.0;
		finalCPUPi = 0.0;
		finalChainReject = 0;
		finalNumChain = 0;
		finalOffDecode = 0;
		finalOffDensity = 0;
		////////////////////////////////////////////////////////////////
		boolean doneFlag = false;
			
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		ReceiveDensity receive = new ReceiveDensity();
		
		MAP_LOOP:
		while (doneFlag == false) {//////////////////////////////////////////////////////TRIAL LOOP////////////////////////
			
			System.out.println("MAP_LOOP with numChain " + numChain);
			
			if (piState[i] == false) { // this Pi cannot map more chain
				System.out.println("Pi number "+(i+1)+" is out of order ...\n");
				rejectPi = 1;
				break;
			}
			
			if(numChain <= 0)  {
				System.out.println("numChain == 0 for some reason ");
				break MAP_LOOP; // if no chain need to be mapped then no need to run MAP 
			}
			
			LinkedList<SFC> listSFCTemp = new LinkedList<>();
					
			for(int numSFC = 0; numSFC < numChain; numSFC++ ) { // initialize SFC list
				SFC sfc = new SFC(String.valueOf(numSFCTotal), i); //sai ten khi remap
				sfc.setSfcID(i); // belong to which Rpi?
				sfc.setStartTime(numRequest);
				sfc.setEndTime(StdRandom.exp(LIVE_TIME)); // leave after 2 hours
				sfc.setServicePosition(capture, true);
				sfc.setServicePosition(decode, true);
				sfc.setServicePosition(density, true);
				sfc.setServicePosition(receive, false);
				listSFCTemp.add(sfc);
				numSFCTotal++;
				
			}
					
			double totalBandwidth = numChain*density.getBandwidth();
			double cpuPi = numChain*capture.getCpu_pi() + numChain*decode.getCpu_pi() + numChain*density.getCpu_pi();
					
					
			/* check Pi resource pool ************************************************/
//			if(totalBandwidth > listRpi.get(i).getRemainBandwidth()) {
//				System.out.println("bw is not enough");
//				if (numChain < finalNumChain || finalNumChain > 0) { // prevent system continue loop even final result has been selected
//					System.out.println("inside breakmap_loop bw problem");
//					break MAP_LOOP;
//					} else {
//						numChain --;
//						if(numChain == 0) {
//							piState[i] = false;
//							rejectPi = 1;
//							break MAP_LOOP;
//						}
//						doneFlag = false;
//					}
//				} else 
				if (cpuPi > listRpi.get(i).getRemainCPU()){
					System.out.println("cpu pi is not enough asking is " + cpuPi +" while cpu remain is "+ listRpi.get(i).getRemainCPU());
					numChain --;
					if(numChain == 0) {
						piState[i] = false;
						rejectPi = 1;
						break MAP_LOOP;
					}
					doneFlag = false;
					continue;
				}else {
					System.out.println("Thoa man gan het dieu kien! \n");
					System.out.println("CPU Pi ask is " + cpuPi + " remain " + listRpi.get(i).getRemainCPU() + " !!! \n");
					System.out.println("CPU Pi used is " + (cpuPi + listRpi.get(i).getUsedCPU()) + " bandwidth used is " + (totalBandwidth + listRpi.get(i).getUsedBandwidth()) + " !!! \n");
					System.out.println("Bandwidth remain is " + listRpi.get(i).getRemainBandwidth() + "\n");
						
					finalUsedPowerPi = numChain*capture.getPower() + numChain*decode.getPower() + numChain*density.getPower();						System.out.println(" One method has been selected!!!");
					finalBandWidth = totalBandwidth;						
					finalCPUPi = cpuPi;
					doneFlag = true; // used to break MAP_LOOP
					
					listSFCFinal.clear();
					for(int index = 0; index < listSFCTemp.size(); index++) {
						listSFCFinal.add(listSFCTemp.get(index));
					}

					finalNumChain = numChain;
					finalChainReject = requestNumChain - finalNumChain;
				} 
				
			}
		}

	
//	public static double calculatePowerServer(double cpuServer) {
//		double numServer = Math.floor(cpuServer/100);
//		double cpuFragment = cpuServer - 100*numServer;
//		 return numServer*powerServer(100) + powerServer(cpuFragment);
//	}
	
//	public static double powerServer(double cpu) {
//		return (120 + 380*cpu/100);
//	}
	
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
	
	public void run(ArrayList<Integer> listRequest) {
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
		
		//double [] totalPowerServer = new double [100];
		//double [] totalPowerSystem = new double [100];
		Arrays.fill(piState, true);
		Arrays.fill(totalChainMap, 0);
		Arrays.fill(totalChainRequest, 0);


		ArrayList<Double> totalPowerSystem = new ArrayList<Double>();
		ArrayList<Double> totalEdgePowerSystem = new ArrayList<Double>();
		ArrayList<Double> serverUtilization = new ArrayList<Double>();
		ArrayList<Double> totalChainAcceptance = new ArrayList<Double>();
		ArrayList<Integer> listServerUsed = new ArrayList<Integer>();
		ArrayList<Integer> totalChainSystem = new ArrayList<Integer>();
		ArrayList<Integer> totalChainActive = new ArrayList<Integer>();
		ArrayList<Integer> totalDecOffload = new ArrayList<Integer>();
		ArrayList<Integer> totalDenOffload = new ArrayList<Integer>();
		ArrayList<Integer> totalChainReject = new ArrayList<Integer>();
		ArrayList<Double> totalLoadEdge = new ArrayList<Double>();
		ArrayList<Double> totalBwEdge = new ArrayList<Double>();
		ArrayList<Integer> totalChainLeave = new ArrayList<Integer>();
		ArrayList<Integer> numChainRequest = new ArrayList<>();
		ArrayList<Integer> numChainAccept = new ArrayList<>();
		
		double [] sumLoadNumPi= new double [REQUEST];  ///for storing load of numPi
		double [] sumBwNumPi = new double [REQUEST];
		Arrays.fill(sumLoadNumPi, 0);
		Arrays.fill(sumBwNumPi, 0);
	

		double acceptance = 0;
		int numRequest = 0; // number of request
		
		LinkedList<Rpi> listRpi = new LinkedList<Rpi>(); //create  a number of Pi
		for(int i = 0; i < NUM_PI; i++ ) {
			Rpi rpi = new Rpi();
			listRpi.add(rpi);
			listRpiSFC.put(rpi, new LinkedList<SFC>());
		}
		
//		GenRequest genRequest = new GenRequest();
		
		double cpuUtilization = 0;
		ArrayList<Integer> requestRandomReceive = new ArrayList<>();
		
		REQUEST_LOOP:
		while (numRequest < REQUEST ) { //////////////////////////////////////////////////////////////////////////////////////////
			
			int numRequestReceive = listRequest.get(numRequest);
//			do {
//				numRequestReceive = genRequest.joinRequest(numRequest);	
//			}while (numRequestReceive == 0);
			
			numChainRequest.add(numRequestReceive);
			
			sumAllRequest = 0;
			sumMapRequest = 0;
			
			double sumLoadPi = 0;
			double sumBwPi = 0;
			
			///////////////////////////////////////////////////
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
				} else if(sumAllRequest > numRequestReceive) {
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
				LinkedList<SFC> listRpiSFCLeave = new LinkedList<SFC>();
				LinkedList<SFC> listSFCFinal = new LinkedList<SFC>();
				
				//boolean networkSuccess = false;
				System.out.println("this belongs to request number " + numRequest);
				System.out.println("Pi number " + (i+1)+ " with " +numChain+ " chains need to be mapped \n");
				System.out.println("this pi has mapped  "+ listRpiSFC.get(listRpi.get(i)).size());
				
				//END--OF--SERVICE--PROCESS //////////////////////////////////////////////////////////////
				
				LinkedList <SFC> listSFCLeave = listRpiSFC.get(listRpi.get(i));
				if(listSFCLeave.size() != 0) {
					
					Iterator<SFC> iter = listSFCLeave.iterator();
					SFC sfc = new SFC();
					int flagLeave = 0;
					while(iter.hasNext()) {
						sfc = iter.next();
						if(sfc.getEndTime() <= numRequest) {
							if(flagLeave == 0) { // if there exists a chain runs out of time
								double a = listRpi.get(i).getCurrentPower();
								mappingServer.getServiceMapping().resetRpiSFC(listSFCLeave, topo.getLinkBandwidth()); // reset at server
								listRpi.get(i).reset(); // reset rpi
								piState[i] = true;
								flagLeave ++;
							}
							iter.remove();
							numChainLeave++;
						}
					}
					
					if(flagLeave != 0) {
						int numChainRemap = listSFCLeave.size();
						listRpiSFC.get(listRpi.get(i)).clear();
						sfccmMapping(numRequest, i, numChainRemap, topo, listRpi, listRpiSFC, mappingServer, listRpiSFCLeave, networkRouting);
						listRpi.get(i).setUsedCPU(finalCPUPi); // change CPU pi-server
						listRpi.get(i).setUsedBandwidth(finalBandWidth); //change Bandwidth used by Pi
						listRpi.get(i).setCurrentPower(finalUsedPowerPi);
						double a = listRpi.get(i).getCurrentPower();
						mappingServer.runMapping(listRpiSFCLeave, topo);
						LinkedList<SFC> listSFC1 = new LinkedList<SFC>();
						LinkedList<SFC> listTemp = mappingServer.getListSFC();
						for(int element = 0; element < listTemp.size();element++) {
							listSFC1.add(listTemp.get(element));
						}
						
						listRpiSFC.put(listRpi.get(i), listSFC1);
						
						if(listRpiSFC.get(listRpi.get(i)).size() > 6){
							throw new java.lang.Error("FNumber of Pi chain exceeds 7");
						}
					}
				}
				else ;
				///JOIN --PROCESS ////////////////////////////////////////////////////////////////////////
				sfccmMapping(numRequest, i, numChain, topo, listRpi, listRpiSFC, mappingServer, listSFCFinal, networkRouting);
				///////////////////////////////////////////////////////////////////////////////////
//				System.out.println("finalnumchain outside function equals "+ finalNumChain);
//				System.out.println("finalCPUPi outside function equals "+ finalCPUPi);
				
				if(finalNumChain != 0 ) { // new set of chain has been mapped
					System.out.println("inside finalnumchain !=0");
					
					listRpi.get(i).setUsedCPU(finalCPUPi); // change CPU pi-server
					listRpi.get(i).setUsedBandwidth(finalBandWidth); //change Bandwidth used by Pi
					listRpi.get(i).setCurrentPower(finalUsedPowerPi);
					
//					run mapping server for final listSFC
					
					int position = rand.nextInt(4); // random a position where request comes from
					networkRouting.NetworkRun(edgePosition.get(position), listSFCFinal, listRpi.get(i));
					
					mappingServer.runMapping(listSFCFinal, topo);
					
//					double finalPowerServer = mappingServer.getPower();
//							.getServiceMapping().getPowerServer();
					
					finalNumChain = mappingServer.getListSFC().size();
					
					totalChainMap[i] += finalNumChain; // for remapping purpose
					totalOffDecPi[i] += finalOffDecode; //
					totalOffDenPi[i] += (finalOffDensity+finalNumChain); // 
					totalChainRequest[i] += requestNumChain; //
					totalPowerEdge[i] += finalUsedPowerPi;
					
					totalDecOffLoad_temp += finalOffDecode;
					totalDenOffLoad_temp += (finalOffDensity+finalNumChain);
										
					sumMapRequest += finalNumChain; //num of accepted request of a Pi
					
					System.out.println("cpuUtilization " + cpuUtilization);
					
					int bwtotal = 0;
					System.out.println(networkRouting.listBW.size());
					for (int h = 0; h < networkRouting.listBW.size(); h++)
						bwtotal += networkRouting.listBW.get(h);
					System.out.println("bwtotal da dung la " + bwtotal);
					//topoTemp = deepClone(topoFinal);
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
					if(listRpiSFC.get(listRpi.get(i)).size() > 6){
						throw new java.lang.Error("FNumber of Pi chain exceeds 7");
					}
				}
				
				totalPowerServer_temp = mappingServer.getPower();
				totalPowerEdge_temp = 0;
				
				for (int j = 0; j < NUM_PI; j++) {
					totalPowerEdge_temp += listRpi.get(j).getCurrentPower(); //calculate System Power
				}
				
				totalPowerSystem_temp = totalPowerEdge_temp + totalPowerServer_temp;
				
				totalChainActive_temp = 0;
				for (int j = 0; j < listRpiSFC.size(); j++) {
					totalChainActive_temp += listRpiSFC.get(listRpi.get(j)).size();
				}
				//totalChainActive_temp = totalChainSystem_temp - numChainLeave;
//				totalChainActive_temp = 0;
//				for(LinkedList<SFC> list : listRpiSFC.values()) {
//					totalChainActive_temp += list.size();
//				}
				
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
//				totalPowerSystem.add(count, totalPowerSystem_temp);
				totalEdgePowerSystem.add(count, totalPowerEdge_temp);
				totalChainLeave.add(count, numChainLeave);
//				totalChainSystem.add(count, totalChainSystem_temp);
//				totalChainActive.add(count, totalChainActive_temp);
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
				sumLoadNumPi[numRequest] += (loadEdgeNumPi.get(index));
				sumBwNumPi[numRequest] += (bwEdgeNumPi.get(index));
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
			
			cpuUtilization = mappingServer.getServiceMapping().getCpuUtilization();
			serverUtilization.add(cpuUtilization);
			int serverUsed = mappingServer.getServiceMapping().getServerUsed();
			listServerUsed.add(serverUsed);
			
//			int totalChainActive=0;
//			for(int i = 0; i < totalChainSystem.size(); i++) {
//				totalChainActive += totalChainSystem.get(i);
//			}
			numChainAccept.add(totalChainSystem_temp);
			totalChainActive.add(totalChainActive_temp);
			totalPowerSystem.add(totalPowerSystem_temp);
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
//		
		
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
		
		ArrayList<Double> linkBandwidth = new ArrayList<>();
		for(int index = 0; index < topo.getLinkBandwidth().size(); index++) {
			if(topo.getLinkBandwidth().get(index).getBandwidth() < 1000)
			linkBandwidth.add(topo.getLinkBandwidth().get(index).getBandwidth());
		}
		
		ArrayList<Double> linkPhyEdge = new ArrayList<>();
		for(int index = 0; index < topo.getListLinkPhyEdge().size(); index++) {
			if(topo.getListLinkPhyEdge().get(index).getBandwidth() < 1000)
			linkPhyEdge.add(topo.getListLinkPhyEdge().get(index).getBandwidth());
		}
		
		ArrayList<Double> listCoreAgg = new ArrayList<>();
		for(int index = 0; index < mappingServer.getLinkMapping().getListLinkCore().size(); index++) {
			listCoreAgg.add(mappingServer.getLinkMapping().getListLinkCore().get(index).getBandwidth());
		}
		ArrayList<Double> listAggEdge = new ArrayList<>();
		for(int index = 0; index < mappingServer.getLinkMapping().getListLinkAgg().size(); index++) {
			listAggEdge.add(mappingServer.getLinkMapping().getListLinkAgg().get(index).getBandwidth());
		}
 	 
		try {
			
			write_double("./PlotSFCCMOP/linklistCoreAggSFCCMOP.txt",listCoreAgg);
			write_double("./PlotSFCCMOP/linklistAggEdgeSFCCMOP.txt",listAggEdge);
			write_double("./PlotSFCCMOP/linklinkPhyEdgeSFCCMOP.txt",linkPhyEdge);
			write_double("./PlotSFCCMOP/linkBandwidthSFCCMOP.txt",linkBandwidth);
			write_double("./PlotSFCCMOP/serverUtilizationSFCCMOP.txt",serverUtilization);
			write_integer("./PlotSFCCMOP/totalChainLeaveSFCCMOP.txt",totalChainLeave);
			write_integer("./PlotSFCCMOP/listServerUsedSFCCMOP.txt",listServerUsed);
			write_integer("./PlotSFCCMOP/requestRandomSFCCMOP.txt",requestRandomReceive);
			write_integer("./PlotSFCCMOP/totalDecOffloadSFCCMOP.txt",totalDecOffload);
			write_integer("./PlotSFCCMOP/totalDenOffloadSFCCMOP.txt",totalDenOffload);
			write_double("./PlotSFCCMOP/totalPowerSystemSFCCMOP.txt",totalPowerSystem);
			write_double("./PlotSFCCMOP/totalEdgePowerSystemSFCCMOP.txt", totalEdgePowerSystem);
			write_double("./PlotSFCCMOP/totalLoadEdgeSFCCMOP.txt",totalLoadEdge);
			write_double("./PlotSFCCMOP/totalBwEdgeSFCCMOP.txt",totalBwEdge);
			write_double("./PlotSFCCMOP/totalChainAcceptanceSFCCMOP.txt",totalChainAcceptance);
			write_double("./PlotSFCCMOP/sumLoadNumPiSFCCMOP.txt", sumLoadNumPi);
			write_double("./PlotSFCCMOP/sumBwNumPiSFCCMOP.txt", sumBwNumPi);
			write_integer("./PlotSFCCMOP/totalChainSystemSFCCMOP.txt",totalChainSystem);
			write_integer("./PlotSFCCMOP/totalChainActiveSFCCMOP.txt",totalChainActive);
			write_integer("./PlotSFCCMOP/totalChainRejectSFCCMOP.txt",totalChainReject);
			write_integer("./PlotSFCCMOP/numChainRequestSFCCMOP.txt",numChainRequest);
			write_integer("./PlotSFCCMOP/numChainAcceptSFCCMOP.txt",numChainAccept);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
//	System.out.println("Server remain " + physicalServer.getRemainCPU() + " \n");
	}
	

}