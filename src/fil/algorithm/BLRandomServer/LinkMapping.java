package fil.algorithm.BLRandomServer;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import fil.resource.substrate.*;
import fil.resource.virtual.*;



@SuppressWarnings("serial")
public class LinkMapping implements java.io.Serializable{
	private boolean isSuccess;
	private LinkedList<VirtualLink> listVirLink;
	private Map<LinkedList<SubstrateSwitch>, Double> resultsLinkMapping;
	private Map<VirtualLink, LinkedList<SubstrateSwitch>> listPathMapped;
	private Map<VirtualLink, LinkedList<LinkPhyEdge>> listPhyEdgeMapped;
	private Map<LinkPhyEdge, Double> listBandwidthPhyEdge;
	private LinkedList<SubstrateLink> listLinkCore;
	private LinkedList<SubstrateLink> listLinkAgg;
	private Map<SFC, LinkedList<SubstrateLink>> listSFCVLink;
	private int numLinkSuccess;
	private double powerConsumed;
	private Map<Service, Map<LinkedList<SubstrateSwitch>, Double>> listServicePath;

	public LinkMapping() {
		isSuccess = false;
		listVirLink = new LinkedList<>();
		resultsLinkMapping = new HashMap<>();
		listPathMapped = new HashMap<>();
		listBandwidthPhyEdge = new HashMap<>();
		listPhyEdgeMapped = new HashMap<>();
		powerConsumed =0;
		numLinkSuccess=0;
		listLinkCore = new LinkedList<>();
		listLinkAgg = new LinkedList<>();
		listSFCVLink = new HashMap<>();
		listServicePath = new HashMap<>();
	}
	
	public Topology linkMappingOurAlgorithm(Topology topo, LinkedList<SFC> listSFCMap, Map<Service, PhysicalServer> listServiceServer, LinkedList<SFC> listSFCReject) {
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();
		LinkedList<SubstrateSwitch> listPhySwitch = topo.getListPhySwitch();
		Map<PhysicalServer, SubstrateSwitch> listLinkServers = topo.getListLinksServer();
		
		Map<Integer, SubstrateSwitch> listCoreSwitch = topo.getListCoreSwitch();
		
		listBandwidthPhyEdge = new HashMap<>();
		resultsLinkMapping = new HashMap<>();
		listVirLink.clear();
		numLinkSuccess=0;
		
		LinkedList<SFC> remainCopy = new LinkedList<>();
		
		remainCopy.addAll(listSFCMap);
		
		if(remainCopy.isEmpty()) {
			System.out.println("remainCopy is empty");
			throw new java.lang.Error(" error inside linkmapping");
		}
		
		boolean checkLinkCore = false;
		boolean checkLinkService = false;
		
			
		for(SFC sfc : remainCopy) {
			boolean finalServiceCloud = false;
			
			int serviceCount = 0;
			
			SERVICE_LOOP:
			for(int i = 4; i>= 2; i--) {
				
				Service service1 = sfc.getService(i);
				serviceCount++;
				if(sfc.getService(i-1).getBelongToEdge()) {
					finalServiceCloud = true;
				}
				
				double bandwidth=0;
				bandwidth = sfc.getService(i-1).getBandwidth();
				
				if(bandwidth < 0) {
					throw new java.lang.Error("bandwidth am");
				}
				
				if(finalServiceCloud == true) {
					//noi ra core
					PhysicalServer phy = new PhysicalServer();
					phy = listServiceServer.get(service1);
					SubstrateSwitch edgeSwitch = new SubstrateSwitch();
					edgeSwitch = listLinkServers.get(phy);
					
					if(!check1PhyEdge(phy, edgeSwitch, listPhyEdge, bandwidth)) {
						checkLinkCore = false;
						break;
					} else {
						//map den core
						for(SubstrateSwitch sw : listCoreSwitch.values()) {
							
							BFSNew bfs = new BFSNew(edgeSwitch, sw);
							
							LinkedList<SubstrateSwitch> shortestPath = bfs.getShortestPathGH(topo);
							if (bandwidth > getBanwidthOfPath(shortestPath, listLinkBandwidth)) {
								checkLinkCore = false;
								break;
							}
							
							VirtualLink vLink = new VirtualLink(service1, sfc.getService(i-1), bandwidth);
							
							LinkPhyEdge phy2Edge1 = new LinkPhyEdge();
							
							for(int index = 0; index < listPhyEdge.size();index++) {
								LinkPhyEdge link = listPhyEdge.get(index);
								if(link.getEdgeSwitch().equals(edgeSwitch)&&link.getPhysicalServer().equals(phy)) {
									phy2Edge1 = link;
									break;
								}
							}
								
							if (shortestPath.size()>0) {
								double temp =0;
								if(resultsLinkMapping.containsKey(shortestPath))
									temp = resultsLinkMapping.get(shortestPath);
								resultsLinkMapping.put(shortestPath, bandwidth+temp);
								
								Map<LinkedList<SubstrateSwitch>, Double> linkTemp = new HashMap<>();
								linkTemp.put(shortestPath, bandwidth+temp);
								
								listServicePath.put(service1, linkTemp);
																
								listLinkBandwidth = MapLink(shortestPath, listLinkBandwidth, bandwidth);
								numLinkSuccess++;
								
								listPathMapped.put(vLink, shortestPath);
								
								LinkedList<LinkPhyEdge> phyEdge = new LinkedList<>();
								phyEdge.add(phy2Edge1);
								listPhyEdgeMapped.put(vLink, phyEdge);
								phy2Edge1.setBandwidth(phy2Edge1.getBandwidth()-bandwidth);
								edgeSwitch.setPort(getSwitchFromID(listPhySwitch, phy.getName()), bandwidth);
								if(listBandwidthPhyEdge.containsKey(phy2Edge1))
									listBandwidthPhyEdge.put(phy2Edge1, listBandwidthPhyEdge.get(phy2Edge1) + bandwidth);
								else
									listBandwidthPhyEdge.put(phy2Edge1, bandwidth);
								
								checkLinkCore = true;
								break SERVICE_LOOP;
							}
						}
						
						break;
					}
					
				} else {
					//tao link giua service i va service i-1
					Service service2 = sfc.getService(i-1);
					
					PhysicalServer phy1 = new PhysicalServer();
					PhysicalServer phy2 = new PhysicalServer();
					
					SubstrateSwitch edgeSwitch1 = new SubstrateSwitch();
					SubstrateSwitch edgeSwitch2 = new SubstrateSwitch();
					
					phy1 = listServiceServer.get(service1);
					phy2 = listServiceServer.get(service2);
					
					String namePhy1 = phy1.getName();
					String namePhy2 = phy2.getName();
					
					if(phy1.equals(phy2)) { // both services belong to a server
						checkLinkService = true;
						continue;
					}
					
					edgeSwitch1 = listLinkServers.get(phy1);
					edgeSwitch2 = listLinkServers.get(phy2);
					
					
					VirtualLink vLink = new VirtualLink(service2, service1, service2.getBandwidth());
					
					
					if(!checkPhyEdge(phy1, edgeSwitch1, phy2, edgeSwitch2, bandwidth, listPhyEdge)) {
						break;
					}
					
					
					LinkPhyEdge phy2Edge1 = new LinkPhyEdge();
					LinkPhyEdge phy2Edge2 = new LinkPhyEdge();
					
					int countP2E = 0;
					for(int index = 0; index < listPhyEdge.size();index++) {
						LinkPhyEdge link = listPhyEdge.get(index);
						if(link.getEdgeSwitch().equals(edgeSwitch1)&&link.getPhysicalServer().equals(phy1)) {
							phy2Edge1 = link;
							countP2E++;
						}
						if(link.getEdgeSwitch().equals(edgeSwitch2)&&link.getPhysicalServer().equals(phy2)) {
							phy2Edge2 = link;
							countP2E++;
						}
						if(countP2E==2)
							break;
					}
					
					
					// near groups
					if (edgeSwitch1.equals(edgeSwitch2)) {
						LinkedList<LinkPhyEdge> phyEdge = new LinkedList<>();
						phyEdge.add(phy2Edge1);
						phyEdge.add(phy2Edge2);
						listPhyEdgeMapped.put(vLink, phyEdge);
						phy2Edge1.setBandwidth(phy2Edge1.getBandwidth()-bandwidth);
						phy2Edge2.setBandwidth(phy2Edge2.getBandwidth()-bandwidth);
						edgeSwitch1.setPort(getSwitchFromID(listPhySwitch, phy1.getName()), bandwidth);
						edgeSwitch1.setPort(getSwitchFromID(listPhySwitch, phy2.getName()), bandwidth);
						if(listBandwidthPhyEdge.containsKey(phy2Edge1))
							listBandwidthPhyEdge.put(phy2Edge1, listBandwidthPhyEdge.get(phy2Edge1) + bandwidth);
						else
							listBandwidthPhyEdge.put(phy2Edge1, bandwidth);
						
						if(listBandwidthPhyEdge.containsKey(phy2Edge2))
							listBandwidthPhyEdge.put(phy2Edge2, listBandwidthPhyEdge.get(phy2Edge2) + bandwidth);
						else
							listBandwidthPhyEdge.put(phy2Edge2, bandwidth);
						
						LinkedList<SubstrateSwitch> list = new LinkedList<>();
						list.add(edgeSwitch1);
						listPathMapped.put(vLink, list);
						// check if path already exits
						double temp =0;
						if(resultsLinkMapping.containsKey(list))
							temp = resultsLinkMapping.get(list);
						resultsLinkMapping.put(list, bandwidth+temp);
						
						Map<LinkedList<SubstrateSwitch>, Double> linkTemp = new HashMap<>();
						linkTemp.put(list, bandwidth+temp);
						
						listServicePath.put(service1, linkTemp);
						
						numLinkSuccess++;
						
						checkLinkService = true;
						continue;
					}
					
					BFSNew bfs = new BFSNew(edgeSwitch1, edgeSwitch2);
					LinkedList<SubstrateSwitch> shortestPath = bfs.getShortestPathGH(topo);
					if (bandwidth > getBanwidthOfPath(shortestPath, listLinkBandwidth)) {
						checkLinkService = false;
						break;
					}
						
					if (shortestPath.size()>0) {
						double temp =0;
						if(resultsLinkMapping.containsKey(shortestPath))
							temp = resultsLinkMapping.get(shortestPath);
						resultsLinkMapping.put(shortestPath, bandwidth+temp);
						
						Map<LinkedList<SubstrateSwitch>, Double> linkTemp = new HashMap<>();
						linkTemp.put(shortestPath, bandwidth+temp);
						
						listServicePath.put(service1, linkTemp);
						
						listLinkBandwidth = MapLink(shortestPath, listLinkBandwidth, bandwidth);
						numLinkSuccess++;
						
						listPathMapped.put(vLink, shortestPath);
						LinkedList<LinkPhyEdge> phyEdge = new LinkedList<>();
						phyEdge.add(phy2Edge1);
						phyEdge.add(phy2Edge2);
						listPhyEdgeMapped.put(vLink, phyEdge);
						phy2Edge1.setBandwidth(phy2Edge1.getBandwidth()-bandwidth);
						phy2Edge2.setBandwidth(phy2Edge2.getBandwidth()-bandwidth);
						edgeSwitch1.setPort(getSwitchFromID(listPhySwitch, phy1.getName()), bandwidth);
						edgeSwitch2.setPort(getSwitchFromID(listPhySwitch, phy2.getName()), bandwidth);
						if(listBandwidthPhyEdge.containsKey(phy2Edge1))
							listBandwidthPhyEdge.put(phy2Edge1, listBandwidthPhyEdge.get(phy2Edge1) + bandwidth);
						else
							listBandwidthPhyEdge.put(phy2Edge1, bandwidth);
						
						if(listBandwidthPhyEdge.containsKey(phy2Edge2))
							listBandwidthPhyEdge.put(phy2Edge2, listBandwidthPhyEdge.get(phy2Edge2) + bandwidth);
						else
							listBandwidthPhyEdge.put(phy2Edge2, bandwidth);
					
						checkLinkService = true;
					}
				}
			}
			
			if(serviceCount == 1 && checkLinkCore == true) {
				isSuccess = true;
				powerConsumed = getPower(topo);
			} else if(checkLinkCore == true && checkLinkService == true) {
				isSuccess = true;
				powerConsumed = getPower(topo);
			} else {
				
				topo = reverseLinkMapping(topo, resultsLinkMapping);
				reversePhyLinkMapping(topo);
				listSFCReject.add(sfc);
				listSFCMap.remove(sfc);
			}
		}
		
		topo.setLinkBandwidth(listLinkBandwidth);
		topo.setListLinkPhyEdge(listPhyEdge);
		
		return topo;
	}
	
	public LinkedList<SubstrateLink> MapLink(LinkedList<SubstrateSwitch> path, LinkedList<SubstrateLink> listLinkBandwidthTemp, double bandwidth)
	{
		for (int i = 0; i < path.size() - 1; i++) {
		
			SubstrateSwitch switch1 = path.get(i);
			SubstrateSwitch switch2 = path.get(i + 1);
			for (int j = 0; j < listLinkBandwidthTemp.size(); j++) {
				SubstrateLink link = listLinkBandwidthTemp.get(j);
				// update bandwidth, two-direction
				if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
					link.setBandwidth(link.getBandwidth() - bandwidth);
					listLinkBandwidthTemp.set(j, link);
					// break;
				}
				//Vm1-> Vm2 == Vm2-Vm1
				if (link.getStartSwitch().equals(switch2) && link.getEndSwitch().equals(switch1)) {
					link.setBandwidth(link.getBandwidth() - bandwidth);
					listLinkBandwidthTemp.set(j, link);
					// break;
				}
			}
			switch1.setPort(switch2, bandwidth);
			switch2.setPort(switch1, bandwidth);
		}
		return listLinkBandwidthTemp;
	}
	
	// sort List switch in increasing order by ID
	public LinkedList<SubstrateSwitch> sortListSwitch(LinkedList<SubstrateSwitch> list) {
		Collections.sort(list, new Comparator<SubstrateSwitch>() {
			@Override
			public int compare(SubstrateSwitch o1, SubstrateSwitch o2) {
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) < Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return -1;
				}
				if (Integer.parseInt(o1.getNameSubstrateSwitch()) > Integer.parseInt(o2.getNameSubstrateSwitch())) {
					return 1;
				}
				return 0;
			}
		});
		return list;
	}

	public double getBanwidthOfPath(LinkedList<SubstrateSwitch> path, LinkedList<SubstrateLink> listLinkBandwidth) {
		double bandwidth = Integer.MAX_VALUE;
		for (int i = 0; i < path.size() - 1; i++) {
			SubstrateSwitch switch1 = path.get(i);
			SubstrateSwitch switch2 = path.get(i + 1);
			for (int j = 0; j < listLinkBandwidth.size(); j++) {
				SubstrateLink link = listLinkBandwidth.get(j);
				if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
					if (link.getBandwidth() < bandwidth)
						bandwidth = link.getBandwidth();
					break;
				}
			}
		}
		return bandwidth;
	}
	
	public boolean check1PhyEdge(PhysicalServer phy, SubstrateSwitch edgeSwitch, LinkedList<LinkPhyEdge> listPhyEdgeTemp, Double bandwidth) {
		boolean Satisfied = false;
		for (LinkPhyEdge link : listPhyEdgeTemp) {
			if ((link.getPhysicalServer().equals(phy) && link.getEdgeSwitch().equals(edgeSwitch)) && link.getBandwidth() >= bandwidth) {
					System.out.println("link.getBandwidth() 1 : " + link.getBandwidth());
					Satisfied = true;
					break;
			}
		}
		return Satisfied;
	}
	
	public boolean checkPhyEdge(PhysicalServer phy1, SubstrateSwitch edge1, PhysicalServer phy2,
			SubstrateSwitch edge2, double bandwidth, LinkedList<LinkPhyEdge> listPhyEdgeTemp) {
		boolean check = false;
		boolean Satisfied = false;
		int count=0;
		System.out.println("bandwidth " + bandwidth);
		for (LinkPhyEdge link : listPhyEdgeTemp) {
			if ((link.getPhysicalServer().equals(phy1) && link.getEdgeSwitch().equals(edge1)) && link.getBandwidth() >= bandwidth) {
					System.out.println("link.getBandwidth() 1 : " + link.getBandwidth());
					Satisfied = true;
					count++;
			}
			if ((link.getPhysicalServer().equals(phy2) && link.getEdgeSwitch().equals(edge2)) && link.getBandwidth() >= bandwidth) {
					System.out.println("link.getBandwidth() 2 : " + link.getBandwidth());
					check = true;
					count++;
			}
			if(count == 2) {
				break;
			}
		}
		
		return (Satisfied&&check);
	}
	
	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
		SubstrateSwitch s= new SubstrateSwitch();
		for(SubstrateSwitch sw: listSwitch)
			if(sw.getNameSubstrateSwitch().equals(id))
			{
				s= sw;
				break;
			}
		return s;
	}
	
	public void reversePhyLinkMapping(Topology topo) {
		LinkedList<SubstrateSwitch> phySwitch = topo.getListPhySwitch();		
		for (LinkPhyEdge link : listBandwidthPhyEdge.keySet()) {
			link.setBandwidth(link.getBandwidth()+listBandwidthPhyEdge.get(link));
			link.getEdgeSwitch().setPort(getSwitchFromID(phySwitch, link.getPhysicalServer().getName()), -listBandwidthPhyEdge.get(link));
		}
	}
	
	public Topology reverseLinkMapping(Topology topo, Map<LinkedList<SubstrateSwitch>, Double> resultsLinkMapping) {

		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
		for (Entry<LinkedList<SubstrateSwitch>, Double> entry : resultsLinkMapping.entrySet()) {
			LinkedList<SubstrateSwitch> path = entry.getKey();
			double bandwidth = entry.getValue();
			if(path.size()<=1)
				continue;
			for (int i = 0; i < path.size() - 1; i++) {
				
				SubstrateSwitch switch1 = path.get(i);
				SubstrateSwitch switch2 = path.get(i + 1);
				for (int j = 0; j < listLinkBandwidth.size(); j++) {
					SubstrateLink link = listLinkBandwidth.get(j);
					double bw = link.getBandwidth();
					// update bandwidth, two-direction
					//Vm1-> Vm2
					if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
						
						link.setBandwidth(bw + bandwidth);
						listLinkBandwidth.set(j, link);
						// break;
					}
					//Vm2-> Vm1
					if (link.getStartSwitch().equals(switch2) && link.getEndSwitch().equals(switch1)) {
						link.setBandwidth(bw + bandwidth);
						listLinkBandwidth.set(j, link);
						// break;
					}
				}
				switch1.setPort(switch2, -bandwidth);
				switch2.setPort(switch1, -bandwidth);
			}
		}
		topo.setLinkBandwidth(listLinkBandwidth);
		topo.setListSwitch(listSwitch);
		return topo;
	}
	
	public Topology reverseLinkMapping(Topology topo, LinkedList<SFC> listSFC) {

		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
		
		
		for(SFC sfc : listSFC) {
			for(int index = 4; index >= 2; index-- ) {
				if(!sfc.getService(index).getBelongToEdge()) {
					Map<LinkedList<SubstrateSwitch>, Double> resultsLinkMapping = new HashMap<>();
					resultsLinkMapping = listServicePath.get(sfc.getService(index));
					
					if(resultsLinkMapping == null) {
						continue;
					}
		
					for (Entry<LinkedList<SubstrateSwitch>, Double> entry : resultsLinkMapping.entrySet()) {
						LinkedList<SubstrateSwitch> path = entry.getKey();
						double bandwidth = entry.getValue();
						if(path.size()<=1)
							continue;
						for (int i = 0; i < path.size() - 1; i++) {
							
							SubstrateSwitch switch1 = path.get(i);
							SubstrateSwitch switch2 = path.get(i + 1);
							for (int j = 0; j < listLinkBandwidth.size(); j++) {
								SubstrateLink link = listLinkBandwidth.get(j);
								double bw = link.getBandwidth();
								// update bandwidth, two-direction
								//Vm1-> Vm2
								if (link.getStartSwitch().equals(switch1) && link.getEndSwitch().equals(switch2)) {
									
									link.setBandwidth(bw + bandwidth);
									listLinkBandwidth.set(j, link);
									// break;
								}
								//Vm2-> Vm1
								if (link.getStartSwitch().equals(switch2) && link.getEndSwitch().equals(switch1)) {
									link.setBandwidth(bw + bandwidth);
									listLinkBandwidth.set(j, link);
									// break;
								}
							}
							switch1.setPort(switch2, -bandwidth);
							switch2.setPort(switch1, -bandwidth);
						}
					}
				}
				
			}
			 
		}
		topo.setLinkBandwidth(listLinkBandwidth);
		topo.setListSwitch(listSwitch);
		return topo;
	}
	
	public double getPower(Topology topo)
	{
		double power = 0;
		modelHP HP = new modelHP();
		LinkedList<SubstrateSwitch> listSwitch = topo.getListSwitch();
//		for(SubstrateLink link: topo.getLinkBandwidth())
//		{
//			double bw = link.getBandwidth();
//			SubstrateSwitch s = link.getStartSwitch();
//			if(listSwitch.containsKey(s.getNameSubstrateSwitch()))
//			{
//				SubstrateSwitch sw = listSwitch.get(s.getNameSubstrateSwitch());
//				sw.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), sw);
//			}
//			else				
//			{
//				s.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), s);
//			}
//			
//		}
		for(SubstrateSwitch entry: listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}
	
	public double getPower(LinkedList<SubstrateSwitch> listSwitch)
	{
		double power = 0;
		modelHP HP = new modelHP();
//		for(SubstrateLink link: topo.getLinkBandwidth())
//		{
//			double bw = link.getBandwidth();
//			SubstrateSwitch s = link.getStartSwitch();
//			if(listSwitch.containsKey(s.getNameSubstrateSwitch()))
//			{
//				SubstrateSwitch sw = listSwitch.get(s.getNameSubstrateSwitch());
//				sw.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), sw);
//			}
//			else				
//			{
//				s.setPort(link.getEndSwitch(), 1000-bw);
//				listSwitch.put(s.getNameSubstrateSwitch(), s);
//			}
//			
//		}
		for(SubstrateSwitch entry : listSwitch)
		{
			power+= HP.getPowerOfSwitch(entry);
		}
			
		return power;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public int getNumLinkSuccess() {
		return numLinkSuccess;
	}

	public void setNumLinkSuccess(int numLinkSuccess) {
		this.numLinkSuccess = numLinkSuccess;
	}

	public double getPowerConsumed() {
		return powerConsumed;
	}

	public void setPowerConsumed(double powerConsumed) {
		this.powerConsumed = powerConsumed;
	}

	public Map<LinkPhyEdge, Double> getListBandwidthPhyEdge() {
		return listBandwidthPhyEdge;
	}

	public void setListBandwidthPhyEdge(Map<LinkPhyEdge, Double> listBandwidthPhyEdge) {
		this.listBandwidthPhyEdge = listBandwidthPhyEdge;
	}

	public LinkedList<SubstrateLink> getListLinkCore() {
		return listLinkCore;
	}

	public void setListLinkCore(LinkedList<SubstrateLink> listLinkCore) {
		this.listLinkCore = listLinkCore;
	}

	public LinkedList<SubstrateLink> getListLinkAgg() {
		return listLinkAgg;
	}

	public void setListLinkAgg(LinkedList<SubstrateLink> listLinkAgg) {
		this.listLinkAgg = listLinkAgg;
	}
}
