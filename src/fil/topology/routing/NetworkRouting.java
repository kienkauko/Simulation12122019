package fil.topology.routing;



import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fil.resource.substrate.Rpi;
import fil.resource.virtual.SFC;

@SuppressWarnings("serial")
public class NetworkRouting implements java.io.Serializable{
	private NetworkTopology topo;
	private LinkedList<LinkedList<NetworkSwitch>> listPath;
	//private  Map<SFC,LinkedList<NetworkSwitch>> pathOfRequest;
	private Map<Rpi, Map<SFC,LinkedList<NetworkSwitch>>> pathOfRequestOfPi;
	public LinkedList<Double> listBW;
	//private LinkedList<NetworkSwitch> listSwitch;
	public static int SERVER_POSITION = 1;
	
	public NetworkRouting() {
		this.topo = new NetworkTopology();
		this.topo.initTopo();
		this.listPath = new LinkedList<>();
		//this.pathOfRequest = new HashMap<>();
		this.pathOfRequestOfPi = new HashMap<>();
		this.listBW = new LinkedList<>();
	}
	
	public boolean NetworkRun(int start, LinkedList<SFC> sfc, Rpi rpi) {
		//this.pathOfRequest.clear();
		Map<SFC,LinkedList<NetworkSwitch>> pathOfRequest = new HashMap<>();
		LinkedList<LinkedList<NetworkSwitch>> listPath = new LinkedList<LinkedList<NetworkSwitch>>();
		LinkedList<NetworkSwitch> pathChosen = new  LinkedList<NetworkSwitch>();
		NetworkSwitch startNode = topo.getListSwitch().get(start);
		NetworkSwitch endNode = topo.getListSwitch().get(SERVER_POSITION);
		
		int size = sfc.size();
		for (int i =0; i < size; i++) {
			double bwDemand = sfc.get(i).getBandwidth();
			System.out.println("bw demand for this sfc is: " + bwDemand);
			listBW.add(bwDemand);
			
			BFS testBFS = new BFS(startNode, endNode, bwDemand);
			pathChosen = testBFS.run(topo, pathChosen);
			
			if(pathChosen.isEmpty()) {
				System.out.println("No network link found for sfc num: " + i);
				return false;
			}
			listPath.add(pathChosen);
			//Done path finding .................................//
			System.out.println("SFC number " + i + " travels through " + pathChosen.size() + " switches \n");
			for (int j = 0; j < pathChosen.size(); j++) {
				System.out.println("Switch get through is: " + pathChosen.get(j).getNameNetworkSwitch() + " \n");
			}
			//set bw for each sfc//////////////////////////////////////////////
        	System.out.println("Setting used bandwidth for network topology... \n");
        	for ( int j = 0; j < (pathChosen.size() - 1); j++) {
        		String a = pathChosen.get(j).getNameNetworkSwitch();
            	String b = pathChosen.get(j+1).getNameNetworkSwitch();
            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
            	// print result////////////////////////////////////////////////
            	System.out.println("Link " + (a+b) + " has been changed to " + this.topo.getListLink().get(b+a).getUsedBandwidth());
            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
        	}
        	pathOfRequest.put(sfc.get(i), listPath.get(i));
		}
    	this.pathOfRequestOfPi.put(rpi, pathOfRequest);
//		System.out.println(listBW);
		return true;
	}
	
	public void  NetworkReset(Rpi piIndex) {
		Map<SFC,LinkedList<NetworkSwitch>> listSFCPi = this.pathOfRequestOfPi.get(piIndex);
		if(listSFCPi == null) {
			throw new java.lang.Error("abc");
		}
		for ( SFC sfc : listSFCPi.keySet()) {
			LinkedList<NetworkSwitch> path = listSFCPi.get(sfc);
			double bwDemand = (sfc.getBandwidth()*(-1.0));
			for (int i = 0; i < (path.size() - 1); i++) {
				String a = path.get(i).getNameNetworkSwitch();
            	String b = path.get(i+1).getNameNetworkSwitch();
            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
			}
		}
		this.pathOfRequestOfPi.remove(piIndex); //delete all chain of this pi
	}

	public void  NetworkReset(Rpi piIndex, LinkedList <SFC> listSFCLeave) {
		Map<SFC,LinkedList<NetworkSwitch>> listSFCPi = new HashMap<>();
		listSFCPi = this.pathOfRequestOfPi.get(piIndex);
		Iterator<Map.Entry<SFC,LinkedList<NetworkSwitch>>> iterator = listSFCPi.entrySet().iterator();
		
		while(iterator.hasNext()) {
			HashMap.Entry<SFC,LinkedList<NetworkSwitch>> entry = iterator.next();
			SFC sfc = entry.getKey();
			if (sfc == listSFCLeave.getFirst()) {
				listSFCLeave.removeFirst();
				LinkedList<NetworkSwitch> path = listSFCPi.get(sfc);
				double bwDemand = (sfc.getBandwidth()*(-1.0));
				for (int i = 0; i < (path.size() - 1); i++) {
					String a = path.get(i).getNameNetworkSwitch();
	            	String b = path.get(i+1).getNameNetworkSwitch();
	            	this.topo.getListLink().get(a+b).setUsedBandwidth(bwDemand);
	            	this.topo.getListLink().get(b+a).setUsedBandwidth(bwDemand);
				}
			}
		}
	}

	public NetworkTopology getTopo() {
		return topo;
	}

	public void setTopo(NetworkTopology topo) {
		this.topo = topo;
	}

	public LinkedList<LinkedList<NetworkSwitch>> getListPath() {
		return listPath;
	}

	public void setListPath(LinkedList<LinkedList<NetworkSwitch>> listPath) {
		this.listPath = listPath;
	}

//	public Map<SFC, LinkedList<NetworkSwitch>> getPathOfRequest() {
//		return pathOfRequest;
//	}
//
//	public void setPathOfRequest(Map<SFC, LinkedList<NetworkSwitch>> pathOfRequest) {
//		this.pathOfRequest = pathOfRequest;
//	}

	public Map<Rpi, Map<SFC, LinkedList<NetworkSwitch>>> getPathOfRequestOfPi() {
		return pathOfRequestOfPi;
	}

	public void setPathOfRequestOfPi(Map<Rpi, Map<SFC, LinkedList<NetworkSwitch>>> pathOfRequestOfPi) {
		this.pathOfRequestOfPi = pathOfRequestOfPi;
	}
	
}
