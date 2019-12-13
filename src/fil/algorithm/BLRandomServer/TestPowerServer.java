package fil.algorithm.BLRandomServer;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import fil.resource.virtual.*;

public class TestPowerServer {
	public static int K_PORT_SWITCH = 10;
	public static int TRIAL = 10;
	public static int NUM_PI = 300;
	
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
	
	public static void main(String[] args) {
		GenRequest genRequest = new GenRequest();
		
		int numRequest = 0;
		int totalRequestReceive = 0;
		int numMapped = 0;
		int totalChainMap = 0;
		//double acceptance= 0;
		ArrayList<Double> acceptanceRatio = new ArrayList<>();
		ArrayList<Double> loadServer = new ArrayList<>();
		ArrayList<Integer> numMappedServer = new ArrayList<>();
		ArrayList<Integer> totalRequestServer = new ArrayList<>();
		ArrayList<Double> totalPowerServer = new ArrayList<>();
		ArrayList<Integer> totalChain = new ArrayList<>();
		
		Random rand = new Random();
		int numSFCTotal = 0;
		
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		ReceiveDensity receive = new ReceiveDensity();
		
		int [] numMappedPi = new int[NUM_PI];
		int countFull = 0;
		
		int min = 200;
		int max = 600;
		
		int numChainMap = 0;
		
		MappingServer mappingServer = new MappingServer();
		Topology topo = new Topology();
		FatTree fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH);
		
		while(numRequest < 20) { // CPU UTILIZATION
			int numSmallRequest = 0;
			double [] acceptance = new double [TRIAL];
			// set CPU server//////////////////////
			
			///////////////////////////////////////
//			while (numSmallRequest < TRIAL) {
				
				
				
//				MappingServer mappingServer = new MappingServer();
//				topo.setCPUServer(numRequest);
				
				
				int numRequestReceive = 0;
				
				numRequestReceive = 100;
				
				
				totalRequestReceive += numRequestReceive;
				boolean doneThisRequest = false;
				LinkedList<Integer> checkPi = new LinkedList<>();
				LinkedList<SFC> listSFCTemp = new LinkedList<>();
				double totalbw = 0;
				
				for(int numSFC = 0; numSFC < numRequestReceive; numSFC++ ) { // initialize SFC list
						SFC sfc = new SFC(String.valueOf(numSFCTotal), rand.nextInt(NUM_PI)); //sai ten khi remap
//						sfc.setSfcID(rand.nextInt(NUM_PI)); // belong to which Rpi?
						double bw = 0;
						sfc.setServicePosition(capture, true);
						sfc.setServicePosition(receive, false);
						int randomPosition = rand.nextInt(3);
						if (randomPosition == 0) {
							sfc.setServicePosition(decode, true);
							sfc.setServicePosition(density, false);
						}
						else if(randomPosition == 1) {
							sfc.setServicePosition(decode, true);
							sfc.setServicePosition(density, false);
						}
						else {
							sfc.setServicePosition(decode, false);
							sfc.setServicePosition(density, false);
						}
						listSFCTemp.add(sfc);
						bw = density.getBandwidth();
						numSFCTotal++;
				}
				
				System.out.println("listSFCTemp " + listSFCTemp.size());
				mappingServer.runMapping(listSFCTemp, topo);
					
				int numMappedReally = mappingServer.getListSFC().size();
				//numChainMap = numMappedReally;
				
				//double loadServerTemp = mappingServer.getServiceMapping().getCpuUtilization();
//						getServiceMapping().getNumChainMapped();
				totalChainMap += numMappedReally;
				totalChain.add(totalChainMap);
				
				acceptance[numSmallRequest] = (numMappedReally*1.0)/numRequestReceive;
				
				numSmallRequest++;
//			}
			double finalAccept = 0;
			for(int i = 0; i < TRIAL; i++) {
				finalAccept += acceptance[i];
			}
			finalAccept = (finalAccept*1.0)/ TRIAL;
			
			
			double power = mappingServer.getPower();
			totalPowerServer.add(power);
//			loadServer.add(loadServerTemp);
			acceptanceRatio.add(finalAccept);
			//numMappedServer.add(numChainMap);
			//totalRequestServer.add(totalRequestReceive);
			numRequest++;
		}
			
		try {
//			write_double("./Plot2108/loadServer.txt", loadServer);
			write_double("./PlotOverall/serverAcceptanceOP.txt",acceptanceRatio);
			write_double("./PowerServer/totalPowerServerOP.txt", totalPowerServer);
			write_integer("./PowerServer/totalChainServerOP.txt",totalChain);
			//write_integer("./Plot2108/totalRequestServer.txt",totalRequestServer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
