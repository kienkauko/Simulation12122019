package fil.algorithm.overallPower;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
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

public class OverallPower  {
	final static int NUM_PI = 300;
	final static int REQUEST = 24;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int ARRAY_SIZE = 100; //100
	final static double LIVETIME = 0.25; 
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
	
	
	
	
	public OverallPower() {;
		//numSFCTotal = 0;
	}
	

	public static void edgeMapping(int numRequest, int i, int numChain, Topology topo, LinkedList<Rpi> listRpi, 
			Map<Rpi, LinkedList<SFC>> listRpiSFC, MappingServer mappingServer,
			LinkedList<SFC> listSFCFinal, NetworkRouting networkRouting) {
		///variable init////////////////////////////////////////////////
		rejectServer = 0;
		rejectPi = 0;
		
		listSFCFinal.clear();
		
		finalBandWidth = 0.0;
		finalCPUPi = 0.0;
		finalChainReject = 0;
		finalNumChain = 0;
		finalOffDecode = 0;
		finalOffDensity = 0;
		////////////////////////////////////////////////////////////////
		boolean doneFlag = false;
		boolean remapping = false;
		boolean doneMap = false;
		
		int remapLoopCount = 0;
		int numChain_temp = 0;
		int totalNumChain = 0;
	
		double minPower = Integer.MAX_VALUE;
		
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		ReceiveDensity receive = new ReceiveDensity();
		
		MAP_LOOP:
			while (doneFlag == false) {//////////////////////////////////////////////////////TRIAL LOOP////////////////////////
				
				System.out.println("vong MAP_LOOP with numChain " + numChain);
				if (piState[i] == false) { // this Pi cannot map more chain
					System.out.println("Pi number "+(i+1)+" is out of order ...\n");
					rejectPi = 1;
					break;
				}
				
				if (remapping == true && doneMap == false){ //remapping
					System.out.println("Inside remapping ");			
					if (remapLoopCount == 0) {
						numChain_temp = totalChainMap[i];
						numChain += totalChainMap[i]; //sum of all previous successful mapping chain
						requestNumChain += totalChainRequest[i]; // sum of all request mapping chain
						sumMapRequest -= totalChainMap[i];
						if (sumMapRequest < 0) {
							System.out.println("abc");
						}
						totalChainSystem_temp -= totalChainMap[i];
						totalDecOffLoad_temp -= totalOffDecPi[i];
						totalDenOffLoad_temp -= totalOffDenPi[i];						
						totalOffDecPi[i] =0; // sai
						totalOffDenPi[i] =0;
						totalChainMap[i] = 0; // reset all mapped chains before
					}
					
					//LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
					if(!listRpiSFC.get(listRpi.get(i)).isEmpty()) {
						System.out.println("ListRPi.get(i) != null with size = " + listRpiSFC.get(listRpi.get(i)).size());
						mappingServer.getServiceMapping().resetRpiSFC(listRpiSFC.get(listRpi.get(i)), topo.getLinkBandwidth()); // reset mapped chains of Pi_index
						listRpiSFC.get(listRpi.get(i)).clear();
						//networkRouting.NetworkReset(listRpi.get(i)); // reset network topology
					}
					else System.out.println("ListRPi.get(i) size equals 0 ");
		
					listRpi.get(i).reset(); //reset RPI
					doneMap = true;
					remapLoopCount ++; // count number of remapping times in MAP_LOOP
				}
				
				if(numChain == 0)  {
					System.out.println("numChain == 0 nhe ban ");
					break MAP_LOOP; // if no chain need to be mapped then no need to run MAP 
				}
				
				int offDecode = 0;
				int offDensity = 0;
				
				OFFDECODE_LOOP:
				for (offDecode = 0; offDecode <= numChain; offDecode++) {
					System.out.println("inside offdecode");
					OFFDENSITY_LOOP:
					for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numChain; offDensity++) {
						System.out.println("inside offdensity");
//						mappingServerTemp = deepClone(mappingServer);
//						topoTemp = deepClone(topo);
//						networkRoutingTemp = deepClone(networkRouting);
//						set position for all chains of a pi
						int numOffDecode = 0;
						int numOffDensity = 0;
						LinkedList<SFC> listSFCTemp = new LinkedList<>();
						
						for(int numSFC = 0; numSFC < numChain; numSFC++ ) { // initialize SFC list
							SFC sfc = new SFC(String.valueOf(numSFCTotal), i); //sai ten khi remap
							sfc.setSfcID(i); // belong to which Rpi?
							sfc.setStartTime(numRequest);
							sfc.setEndTime(StdRandom.exp(LIVETIME)); // leave after 2 hours,
							sfc.setServicePosition(capture, true);
							
							if(numOffDecode < offDecode) {
								sfc.setServicePosition(decode, false);
							}
							else sfc.setServicePosition(decode, true);
							if(numOffDensity < offDensity) {
								sfc.setServicePosition(density, false);
							}
							else sfc.setServicePosition(density, true);
							sfc.setServicePosition(receive, false);
							listSFCTemp.add(sfc);
							numSFCTotal++;
							numOffDecode++;
							numOffDensity++;
						}
						
						double totalBandwidth = (offDecode)*capture.getBandwidth() + (offDensity - offDecode)*decode.getBandwidth() + (numChain - offDensity)*density.getBandwidth();
						double cpuPi = numChain*capture.getCpu_pi() + (numChain - offDecode)*decode.getCpu_pi() + (numChain - offDensity)*density.getCpu_pi();
						
						
						/* check Pi resource pool ************************************************/
						if(totalBandwidth > listRpi.get(i).getRemainBandwidth()) {
							System.out.println("bw is not enough");
							if (remapping == true ) {
								numChain --; // remapping is not working then reduce number of chain
								System.out.println("numChain --");
								if (numChain < finalNumChain) { // prevent system continue loop even final result has been selected
									System.out.println("inside breakmap_loop bw problem");
									break MAP_LOOP;
								} else {
									doneMap = false;
									break OFFDECODE_LOOP; // try to map with lower num of chain
								}
							} else if (finalNumChain == 0) {
								remapping = true;
								doneMap = false;
//								System.out.println("Trying to remap !!! \n ");
								break OFFDECODE_LOOP; // try to remap
							} else {
								//rejectBandwidth = 1;
								break MAP_LOOP; // ofload ngu, out mapping
							}

						} else if(cpuPi > listRpi.get(i).getRemainCPU()){
							System.out.println("cpu pi is not enough asking is " + cpuPi +" while cpu remain is "+ listRpi.get(i).getRemainCPU());
							if (offDecode == numChain && remapping == false ) {
								remapping = true; //turn off remap
								break OFFDECODE_LOOP;
							} else
							continue; // try to offload service  
						/*check Pi resource done***************************************************/
						} else {
							System.out.println("Thoa man gan het dieu kien! \n");
							System.out.println("CPU Pi ask is " + cpuPi + " remain " + listRpi.get(i).getRemainCPU() + " !!! \n");
							System.out.println("CPU Pi used is " + (cpuPi + listRpi.get(i).getUsedCPU()) + " bandwidth used is " + (totalBandwidth + listRpi.get(i).getUsedBandwidth()) + " !!! \n");
							System.out.println("Bandwidth remain is " + listRpi.get(i).getRemainBandwidth() + "\n");
							//Routing algorithm
							//latency
							double powerChainPi = numChain*capture.getPower() +(numChain - offDecode)*decode.getPower() + (numChain - offDensity)*density.getPower();
							double powerChainServer = calculatePowerServer(numChain*receive.getCpu_server() + offDecode*decode.getCpu_server() + offDensity*density.getCpu_server());
							System.out.println("Power server "+ powerChainServer +" Power Pi: " +powerChainPi+ "\n");
							double totalPower = powerChainPi + powerChainServer;
							
							System.out.println("Total sysem power is " + totalPower + " \n");
							if (numChain >= totalNumChain && totalPower <= minPower) { //QoS and acceptance rate priority
								System.out.println(" One method has been selected!!!");
								totalNumChain = numChain;
								doneFlag = true; // used to break MAP_LOOP
								minPower = totalPower;
									
								if (totalNumChain <= numChain_temp) { // case after remap nothing changes
									piState[i] = false;
								}
								
								//add to listSFCFinal
								listSFCFinal.clear();
								for(int index = 0; index < listSFCTemp.size(); index++) {
									listSFCFinal.add(listSFCTemp.get(index));
								}
		
//								networkRoutingFinal = deepClone(networkRoutingTemp);
								finalNumChain = totalNumChain;
								System.out.println("finalnumchain inside function equals "+ finalNumChain);
								finalChainReject = requestNumChain - finalNumChain;
//								finalCPUServer = cpuServer;
								finalUsedPowerPi = powerChainPi;
								if(finalUsedPowerPi < 0) {
									System.out.println("Power pi smaller than 0: " + finalUsedPowerPi);
									return;
								}
								finalBandWidth = totalBandwidth;
//								finalPowerServer = powerChainServer;
//								finalPower = totalPower;
								finalCPUPi = cpuPi;
								finalOffDecode = offDecode;
								finalOffDensity = offDensity;
			
							} else {
								System.out.println("Maploop has gone so wrong, stop! \n");
								if (offDecode == numChain ) { // last loop
									break MAP_LOOP;
								}
								continue;
							}
						}
					}	 // OFF_DENSITY LOOP
				} // OFF_DECODE LOOP
				
			} 
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
	
	public void run(Map<Integer,HashMap<Integer,Integer>> listRequest, ArrayList<Integer> numChainPoisson){
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
		
		Arrays.fill(piState, true);
		Arrays.fill(totalChainMap, 0);
		Arrays.fill(totalChainRequest, 0);

		ArrayList<Integer> edgePosition = new ArrayList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		
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
		
		
		double [] sumLoadNumPi= new double [numChainPoisson.size()];  ///for storing load of numPi
		double [] sumBwNumPi = new double [numChainPoisson.size()];
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
			
		while (numRequest < numChainPoisson.size()) { //////////////////////////////////////////////////////////////////////////////////////////
			HashMap<Integer,Integer> listRequestDetail = listRequest.get(numRequest);
			int numPiReceive = listRequestDetail.size(); // number of Pi receives request
			int numRequestReceive = numChainPoisson.get(numRequest);
			int piReceive = 0;
			sumAllRequest = 0;
			sumMapRequest = 0;
			
			double sumLoadPi = 0;
			double sumBwPi = 0;
			
			///////////////////////////////////////////////////
			LinkedList<Double> loadEdgeNumPi = new LinkedList<>();
			LinkedList<Double> bwEdgeNumPi = new LinkedList<>();
			LinkedList<Integer> checkPi = new LinkedList<>();
			
			

		
			for (Entry<Integer, Integer> entry : listRequestDetail.entrySet()) { //change i < 1 to i < num_pi for mapping every pi/////////////////////////////////
				//////////////////////Identify which Pi will receive request////////////////////////////////////////////
				int pi = entry.getKey();
				requestNumChain = entry.getValue();
				sumAllRequest += requestNumChain;
				requestRandomReceive.add(requestNumChain);
				int numChain = requestNumChain;
				/////////////////////////////////////////////////////////////////////////////////////////
			
				//sfc request to server
				LinkedList<SFC> listSFC = new LinkedList<SFC>(); //create  a number of Pi
				LinkedList<SFC> listSFCFinal = new LinkedList<SFC>();
				
				//boolean networkSuccess = false;
				System.out.println("this belongs to request number " + numRequest);
				System.out.println("Pi number " + (pi+1)+ " with " +numChain+ " chains need to be mapped \n");
				System.out.println("this pi has mapped  "+ listRpiSFC.get(listRpi.get(pi)).size());
				//END--OF--SERVICE--PROCESS //////////////////////////////////////////////////////////////
				
				LinkedList <SFC> listSFCRemain = listRpiSFC.get(listRpi.get(pi));
				//LinkedList <SFC> listSFCLeave = new LinkedList<>();
				
				if(listSFCRemain.size() != 0 && listSFCRemain != null) {
					
					Iterator<SFC> iter = listSFCRemain.iterator();
					SFC sfc = new SFC();
					boolean flagLeave = false;
					while(iter.hasNext()) {
						sfc = iter.next();
						if(sfc.getEndTime() <= numRequest) {
							flagLeave = true;
							//listSFCLeave.add(sfc);
							iter.remove();
							numChainLeave++;
						}
					}
					
					if(flagLeave == true) {
						mappingServer.getServiceMapping().resetRpiSFC(listRpiSFC.get(listRpi.get(pi)), topo.getLinkBandwidth()); // reset at server
						networkRouting.NetworkReset(listRpi.get(pi));
						listRpi.get(pi).reset(); // reset rpi
						piState[pi] = true;
						int numChainRemap = listSFCRemain.size();
						listRpiSFC.get(listRpi.get(pi)).clear();
						edgeMapping(numRequest, pi, numChainRemap, topo, listRpi, listRpiSFC, mappingServer, listSFCFinal, networkRouting);
						mappingServer.runMapping(listSFCFinal, topo);
						
						LinkedList<SFC> listSFC1 = new LinkedList<SFC>();
						LinkedList<SFC> listTemp = mappingServer.getListSFC();
						for(int element = 0; element < listTemp.size();element++) {
							listSFC1.add(listTemp.get(element));
						}
						
						listRpiSFC.put(listRpi.get(pi), listSFC1);
						
						if(listRpiSFC.get(listRpi.get(pi)).size() > 7){
							throw new java.lang.Error("FNumber of Pi chain exceeds 7");
						}
						
						listRpi.get(pi).setUsedCPU(finalCPUPi); // change CPU pi-server
						listRpi.get(pi).setUsedBandwidth(finalBandWidth); //change Bandwidth used by Pi
						listRpi.get(pi).setCurrentPower(finalUsedPowerPi);
					}
				}
				else ;
				///JOIN --PROCESS ////////////////////////////////////////////////////////////////////////
				edgeMapping(numRequest, pi, numChain, topo, listRpi, listRpiSFC, mappingServer, listSFCFinal, networkRouting);
			
				///////////////////////////////////////////////////////////////////////////////////
//				System.out.println("finalnumchain outside function equals "+ finalNumChain);
//				System.out.println("finalCPUPi outside function equals "+ finalCPUPi);
				
				if((finalNumChain != 0 || piState[pi] == false) && rejectPi != 1) { // new set of chain has been mapped
					System.out.println("inside finalnumchain !=0");
					
					listRpi.get(pi).setUsedCPU(finalCPUPi); // change CPU pi-server
					listRpi.get(pi).setUsedBandwidth(finalBandWidth); //change Bandwidth used by Pi
					listRpi.get(pi).setCurrentPower(finalUsedPowerPi);
					
//					run mapping server for final listSFC
					
					Random rand = new Random();
					int position = rand.nextInt(4); // random a position where request comes from
					networkRouting.NetworkRun(edgePosition.get(position), listSFCFinal, listRpi.get(pi));
					
					mappingServer.runMapping(listSFCFinal, topo);
					
					
		//////////////thay doi finalNumchain de tinh lai accpeptance ratio

					finalNumChain = mappingServer.getListSFC().size();
					
					
					totalOffDecPi[pi] += finalOffDecode; //
					totalOffDenPi[pi] += (finalOffDensity+finalNumChain); // 
					totalChainRequest[pi] += requestNumChain; //
					
					totalDecOffLoad_temp += finalOffDecode;
					totalDenOffLoad_temp += (finalOffDensity+finalNumChain);
					
					
					
					sumMapRequest += finalNumChain; //num of accepted request of a Pi
					
					System.out.println("cpuUtilization " + cpuUtilization);
					
					int bwtotal = 0;
					System.out.println(networkRouting.listBW.size());
					for (int h = 0; h < networkRouting.listBW.size(); h++)
						bwtotal += networkRouting.listBW.get(h);
					System.out.println("bwtotal da dung la " + bwtotal);
				
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
					
					if(!listRpiSFC.get(listRpi.get(pi)).isEmpty()) {
						System.out.println("num chain ban dau la: " + listRpiSFC.get(listRpi.get(pi)).size());
						LinkedList<SFC> listSFCTemp = new LinkedList<>();
						listSFCTemp = listRpiSFC.get(listRpi.get(pi));
						listSFC.addAll(listSFCTemp);
						listRpiSFC.put(listRpi.get(pi), listSFC);
						System.out.println("num chain cuoi cung  la: " + listRpiSFC.get(listRpi.get(pi)).size());
					} else listRpiSFC.put(listRpi.get(pi), listSFC);
					if(listRpiSFC.get(listRpi.get(pi)).size() > 7){
						throw new java.lang.Error("FNumber of Pi chain exceeds 7");
					}
					totalChainMap[pi] = listRpiSFC.get(listRpi.get(pi)).size(); // for remapping purpose
				}
				totalPowerServer_temp = mappingServer.getPower();
				totalPowerEdge_temp = 0;
				
				for (int j = 0; j < NUM_PI; j++) {
					totalPowerEdge_temp += listRpi.get(j).getCurrentPower(); //calculate System Power
				}
							
				totalPowerSystem_temp = totalPowerEdge_temp + totalPowerServer_temp;
				//totalChainActive_temp = totalChainSystem_temp - numChainLeave;
				totalChainActive_temp = 0;
				for (int j = 0; j < listRpiSFC.size(); j++) {
					totalChainActive_temp += listRpiSFC.get(listRpi.get(j)).size();
				}
				
				if (rejectServer == 1) {
					finalChainReject = requestNumChain; // reject all chain
					totalChainReject_temp += finalChainReject;
				}
				if (rejectPi == 1) { // this means CPU is also not enough
					System.out.println(" Raspberry Pi number " + (pi+1) + " is out of resource \n");
					System.out.println(" Raspberry Pi number " + (pi+1) + " CPU Pi " + listRpi.get(pi).getRemainCPU() +"\n");
					finalChainReject = requestNumChain; // reject all chain
					totalChainReject_temp += finalChainReject;
				}
				
				totalDecOffload.add(count, totalDecOffLoad_temp); // sum of all case offloading decode
				totalDenOffload.add(count, totalDenOffLoad_temp);
//				totalPowerSystem.add(count, totalPowerSystem_temp);
//				totalEdgePowerSystem.add(count, totalPowerEdge_temp);
//				totalChainSystem.add(count, totalChainSystem_temp);
				
				totalChainReject.add(count, totalChainReject_temp);
				
				
				count++;
				
	//			calculate total CPU, bandwidth for number of Pi
	//			array for saving
				loadEdgeNumPi.add(listRpi.get(pi).getUsedCPU());
				bwEdgeNumPi.add(listRpi.get(pi).getUsedBandwidth());		
				System.out.println("Pi number " + (pi+1) + " is out of duty \n");
				
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
			numChainRequest.add(numRequestReceive);
			
//			if (totalChainSystem_temp > 1900) {
//				break;
//			}
			
			cpuUtilization = mappingServer.getServiceMapping().getCpuUtilization();
			serverUtilization.add(cpuUtilization);
			int serverUsed = mappingServer.getServiceMapping().getServerUsed();
			listServerUsed.add(serverUsed);
			
			numChainAccept.add(totalChainSystem_temp);
			totalChainLeave.add(numChainLeave);
			totalChainActive.add(totalChainActive_temp);
			totalEdgePowerSystem.add(totalPowerEdge_temp);
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
		
		ArrayList<Double> cpuServerUsed = new ArrayList<>();
		int numServerUsed = 0;
		for(Integer phy : mappingServer.getServiceMapping().getListServerUsed().keySet()) {
			numServerUsed++;
			cpuServerUsed.add(mappingServer.getServiceMapping().getListServerUsed().get(phy).getUsedCPUServer());
		}
		
		System.out.println("numServerUsed" + numServerUsed);
		try {
			write_double("./PlotOverall/cpuServerUsedOP.txt",cpuServerUsed);
			write_double("./PlotOverall/linklistCoreAggOP.txt",listCoreAgg);
			write_double("./PlotOverall/linklistAggEdgeOP.txt",listAggEdge);
			write_double("./PlotOverall/linklinkPhyEdgeOP.txt",linkPhyEdge);
			write_double("./PlotOverall/linkBandwidthOP.txt",linkBandwidth);
			write_double("./PlotOverall/serverUtilizationOP.txt",serverUtilization);
			write_integer("./PlotOverall/totalChainLeaveOP.txt",totalChainLeave);
			write_integer("./PlotOverall/listServerUsedOP.txt",listServerUsed);
			write_integer("./PlotOverall/requestRandomOP.txt",requestRandomReceive);
			write_integer("./PlotOverall/totalDecOffloadOP.txt",totalDecOffload);
			write_integer("./PlotOverall/totalDenOffloadOP.txt",totalDenOffload);
			write_double("./PlotOverall/totalPowerSystemOP.txt",totalPowerSystem);
			write_double("./PlotOverall/totalEdgePowerSystemOP.txt", totalEdgePowerSystem);
			write_double("./PlotOverall/totalLoadEdgeOP.txt",totalLoadEdge);
			write_double("./PlotOverall/totalBwEdgeOP.txt",totalBwEdge);
			write_double("./PlotOverall/totalChainAcceptanceOP.txt",totalChainAcceptance);
			write_double("./PlotOverall/sumLoadNumPiOP.txt", sumLoadNumPi);
			write_double("./PlotOverall/sumBwNumPiOP.txt", sumBwNumPi);
			write_integer("./PlotOverall/totalChainSystemOP.txt",totalChainSystem);
			write_integer("./PlotOverall/totalChainActiveOP.txt",totalChainActive);
			write_integer("./PlotOverall/totalChainRejectOP.txt",totalChainReject);
			write_integer("./PlotOverall/numChainRequestOP.txt",numChainRequest);
			write_integer("./PlotOverall/numChainAcceptOP.txt",numChainAccept);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
//	System.out.println("Server remain " + physicalServer.getRemainCPU() + " \n");
	}
	

}