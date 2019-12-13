package fil.algorithm.MrPhuoc;


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
	}
	
	public Topology linkMappingOurAlgorithm(Topology topo,
			Map<Integer, Map<Service, PhysicalServer>> resultsServiceMapping, ServiceMapping serviceMapping) {
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();
		LinkedList<SubstrateSwitch> listPhySwitch = topo.getListPhySwitch();
		LinkedList<SubstrateSwitch> listSwitchUsed = new LinkedList<SubstrateSwitch>();
		listVirLink.clear();
		numLinkSuccess=0;
		
		Map<Integer, Map<Service, PhysicalServer>> remainCopy = new HashMap<>();
		
		remainCopy.putAll(resultsServiceMapping);
		
		if(remainCopy.isEmpty()) {
			System.out.println("remainCopy is empty");
		}
		
		Service serviceA = new Service();
		Service serviceB = new Service();
		int serviceCount = 0;
		
		Integer type1 = null;
		Integer type2 = null;
		
		PhysicalServer phy1 = new PhysicalServer();
		PhysicalServer phy2 = new PhysicalServer();
		
		double bandwidth=0;
		
		while(!remainCopy.isEmpty()) {
			
			for(Integer serviceType : remainCopy.keySet()) {
				
				if(serviceType.equals(4)) {
					Map<Service, PhysicalServer> service = remainCopy.get(serviceType);
					for(Service sv : service.keySet()) {
						bandwidth = 0.6;
						serviceA = sv;
						phy1 = service.get(sv);
						type1 = serviceType;
					}
					serviceCount++;
				}
				if(serviceType.equals(3)) {
					Map<Service, PhysicalServer> service = remainCopy.get(serviceType);
					for(Service sv : service.keySet()) {
						serviceB = sv;
						phy2 = service.get(sv);
						type2 = serviceType;
					}
					serviceCount++;
				}
			}
			
			if(serviceCount != 2) {
				serviceCount = 0;
				for(Integer serviceType : remainCopy.keySet()) {
					
					if(serviceType.equals(3)) {
						Map<Service, PhysicalServer> service = remainCopy.get(serviceType);
						for(Service sv : service.keySet()) {
							bandwidth = 16.32;
							serviceA = sv;
							phy1 = service.get(sv);
							type1 = serviceType;
						}
						serviceCount++;
					}
					if(serviceType.equals(2)) {
						Map<Service, PhysicalServer> service = remainCopy.get(serviceType);
						for(Service sv : service.keySet()) {
							serviceB = sv;
							phy2 = service.get(sv);
							type2 = serviceType;
						}
						serviceCount++;
					}
				}
			}
			
			if(serviceCount == 2 && type1 != null && type2 != null) {
				remainCopy.remove(type1);
				remainCopy.remove(type2);
			} else System.out.println("service can map link khong du 2");
				
			VirtualLink vLink = new VirtualLink(serviceB, serviceA, bandwidth);
			listVirLink.add(vLink);
			
	
			String namePhy1 = phy1.getName();
			String namePhy2 = phy2.getName();
			
			SubstrateSwitch edgeSwitch1 = new SubstrateSwitch();
			SubstrateSwitch edgeSwitch2 = new SubstrateSwitch();
			
			LinkPhyEdge phy2Edge1=null, phy2Edge2=null;
			int countP2E = 0;
			for(LinkPhyEdge linkPhyEdge : listPhyEdge) {
				PhysicalServer phy = linkPhyEdge.getPhysicalServer();
				if(phy.getName().equals(namePhy1)) {
					edgeSwitch1 = linkPhyEdge.getEdgeSwitch();
					phy1 = phy;
					phy2Edge1 = linkPhyEdge;
					countP2E++;
				} else if (phy.getName().equals(namePhy2)) {
					edgeSwitch2 = linkPhyEdge.getEdgeSwitch();
					phy2 = phy;
					phy2Edge2 = linkPhyEdge;
					countP2E++;
				}
				if(countP2E == 2) {
					break;
				}
			}
			
			if(phy2Edge1.getBandwidth() <= bandwidth || phy2Edge2.getBandwidth() <= bandwidth) {
				System.out.println("check bandwidth faild");
				break;
			}
			
//			if (!checkPhyEdge(phy1, edgeSwitch1, phy2, edgeSwitch2, bandwidth, listPhyEdge)) {
//				break;
//			}
			
			Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
			LinkedList<SubstrateSwitch> listAggConnectStartEdge = new LinkedList<>();
			LinkedList<SubstrateSwitch> listAggConnectEndEdge = new LinkedList<>();
			
			// near groups
			if (edgeSwitch1.equals(edgeSwitch2)) {
				System.out.println("Near group!!");
				///////////////////////////////////////////////////////
				LinkedList<LinkPhyEdge> phyEdge = new LinkedList<>();
				phyEdge.add(phy2Edge1);
				phyEdge.add(phy2Edge2);
				listPhyEdgeMapped.put(vLink, phyEdge);
				phy2Edge1.setBandwidth(phy2Edge1.getBandwidth() - vLink.getBandwidthRequest());
				phy2Edge2.setBandwidth(phy2Edge2.getBandwidth() - vLink.getBandwidthRequest());
				edgeSwitch1.setPort(getSwitchFromID(listPhySwitch, phy1.getName()), vLink.getBandwidthRequest());
				edgeSwitch1.setPort(getSwitchFromID(listPhySwitch, phy2.getName()), vLink.getBandwidthRequest());
				
				boolean check1 = false;
				boolean check2 = false;
				
				if(!listBandwidthPhyEdge.isEmpty()) {
				
					for(LinkPhyEdge linkPhyEdge : listBandwidthPhyEdge.keySet()) {
						if(linkPhyEdge.getPhysicalServer().getName().equals(namePhy1) && linkPhyEdge.getEdgeSwitch().getNameSubstrateSwitch().equals(edgeSwitch1.getNameSubstrateSwitch())) {
							listBandwidthPhyEdge.put(linkPhyEdge, listBandwidthPhyEdge.get(linkPhyEdge) + vLink.getBandwidthRequest());
							check1 = true;
						} else if(linkPhyEdge.getPhysicalServer().getName().equals(namePhy2) && linkPhyEdge.getEdgeSwitch().getNameSubstrateSwitch().equals(edgeSwitch2.getNameSubstrateSwitch())) {
							listBandwidthPhyEdge.put(linkPhyEdge, listBandwidthPhyEdge.get(linkPhyEdge) + vLink.getBandwidthRequest());
							check2 = true;
						}
					}
				}
				if(check1 == false) {
					listBandwidthPhyEdge.put(phy2Edge1, vLink.getBandwidthRequest());
				}
				if(check2 == false) {
					listBandwidthPhyEdge.put(phy2Edge2, vLink.getBandwidthRequest());
				}
				
				LinkedList<SubstrateSwitch> list = new LinkedList<>();
				list.add(edgeSwitch1);
				double temp=0;
				if(resultsLinkMapping.containsKey(list))
					temp= resultsLinkMapping.get(list);

				resultsLinkMapping.put(list, vLink.getBandwidthRequest()+temp);
				listPathMapped.put(vLink, list);
				numLinkSuccess++;
			} else {
				///check if aggregation or core
				System.out.println("check if aggregation or core");
				int count = 0;
				for (Entry<SubstrateSwitch, LinkedList<SubstrateSwitch>> entry1 : listAggConnectEdge.entrySet()) {
					if (entry1.getKey().getNameSubstrateSwitch().equals(edgeSwitch1.getNameSubstrateSwitch())) {
						listAggConnectStartEdge = entry1.getValue();
						count++;
					}
						
					if (entry1.getKey().getNameSubstrateSwitch().equals(edgeSwitch2.getNameSubstrateSwitch())) {
						listAggConnectEndEdge = entry1.getValue();
						count++;
					}
					
					if(count == 2) {
						break;
					}
				}
				// sort list Agg
				listAggConnectStartEdge = sortListSwitch(listAggConnectStartEdge);
				listAggConnectEndEdge = sortListSwitch(listAggConnectEndEdge);

				// check middle groups
				if (listAggConnectStartEdge.equals(listAggConnectEndEdge)) {
					System.out.println("aggr group");
					if(serviceMapping.remappingAggrFarGroup(vLink)) {
						isSuccess = true;
						numLinkSuccess++;
					} else {
						isSuccess = linkMappingAggSeparate(phy1, edgeSwitch1, phy2, edgeSwitch2, bandwidth, listPhyEdge, listAggConnectStartEdge, listLinkBandwidth, topo);
						if(isSuccess == true) {
							numLinkSuccess++;
						}
					}
				} else {
					//far group
					System.out.println("far group");
					if(serviceMapping.remappingAggrFarGroup(vLink)) {
						isSuccess = true;
						numLinkSuccess++;
					} else {
						isSuccess = linkMappingCoreSeparate(phy1, edgeSwitch1, phy2, edgeSwitch2, bandwidth, listPhyEdge, topo);
						if(isSuccess == true) {
							numLinkSuccess++;
						}
					}
				}
				
			}
		}
		
		topo.setLinkBandwidth(listLinkBandwidth);
		topo.setListLinkPhyEdge(listPhyEdge);
		if (numLinkSuccess == listVirLink.size()) {
			isSuccess = true;
			powerConsumed = getPower(listSwitchUsed);
		} else {
			isSuccess = false;
			topo= reverseLinkMapping(topo, resultsLinkMapping);
			reversePhyLinkMapping(topo);
		}
		return topo;
	}
	
	private boolean linkMappingCoreSeparate(PhysicalServer phy1, SubstrateSwitch edgeSwitch1, PhysicalServer phy2,
			SubstrateSwitch edgeSwitch2, double bandwidth, LinkedList<LinkPhyEdge> listPhyEdge, Topology topo) {
		// TODO Auto-generated method stub
		boolean success = true;
		
		
		
		return success;
	}

	private boolean linkMappingAggSeparate(PhysicalServer phy1, SubstrateSwitch edgeSwitch1, PhysicalServer phy2,
			SubstrateSwitch edgeSwitch2, double bandwidth, LinkedList<LinkPhyEdge> listPhyEdge, 
			LinkedList<SubstrateSwitch> listAggConnectStartEdge, LinkedList<SubstrateLink> listLinkBandwidth, Topology topo) {
		// TODO Auto-generated method stub
		boolean success = false;
		boolean checkAgg = false;
		boolean checkEdge = false;
		
		SubstrateLink linkAggEdge01 = new SubstrateLink();
		SubstrateLink linkAggEdge10 = new SubstrateLink();
		SubstrateLink linkAggEdge02 = new SubstrateLink();
		SubstrateLink linkAggEdge20 = new SubstrateLink();
		
		LinkPhyEdge linkPhyEdge1 = new LinkPhyEdge();
//		SubstrateLink linkPhyEdge10 = new SubstrateLink();
		LinkPhyEdge linkPhyEdge2 = new LinkPhyEdge();
//		SubstrateLink linkPhyEdge20 = new SubstrateLink();
		
		SubstrateSwitch aggSW = new SubstrateSwitch();
		
		
		for(SubstrateSwitch sw : listAggConnectStartEdge) {
			int count = 0;
			for(SubstrateLink link : listLinkBandwidth) {
				if(link.getStartSwitch().getNameSubstrateSwitch().equals(sw.getNameSubstrateSwitch()) && link.getEndSwitch().getNameSubstrateSwitch().equals(edgeSwitch1.getNameSubstrateSwitch()) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge01 = link;
				} else if(link.getStartSwitch().getNameSubstrateSwitch().equals(edgeSwitch1.getNameSubstrateSwitch()) && link.getEndSwitch().getNameSubstrateSwitch().equals(sw.getNameSubstrateSwitch()) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge10 = link;
				}
				
				if(link.getStartSwitch().getNameSubstrateSwitch().equals(sw.getNameSubstrateSwitch()) && link.getEndSwitch().getNameSubstrateSwitch().equals(edgeSwitch2.getNameSubstrateSwitch()) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge02 = link;
				} else if(link.getStartSwitch().getNameSubstrateSwitch().equals(edgeSwitch2.getNameSubstrateSwitch()) && link.getEndSwitch().getNameSubstrateSwitch().equals(sw.getNameSubstrateSwitch()) && link.getBandwidth() >= bandwidth) {
					count++;
					linkAggEdge20 = link;
				}
				
				if(count == 4) {
					aggSW = sw;
					checkAgg = true;
					break;
				}
			}
		}
		
		for(LinkPhyEdge link : listPhyEdge) {
			int count = 0;
			if(link.getEdgeSwitch().getNameSubstrateSwitch().equals(edgeSwitch1.getNameSubstrateSwitch()) && link.getPhysicalServer().getName().equals(phy1.getName()) && link.getBandwidth() >= bandwidth) {
				linkPhyEdge1 = link;
				count++;
			}
			
			if(link.getEdgeSwitch().getNameSubstrateSwitch().equals(edgeSwitch2.getNameSubstrateSwitch()) && link.getPhysicalServer().getName().equals(phy2.getName()) && link.getBandwidth() >= bandwidth) {
				linkPhyEdge2 = link;
				count++;
			}
			
			if(count == 2) {
				checkEdge = true;
				break;
			}
		}
		
		if(checkAgg == true && checkEdge == true) {
			
			success = true;
			
			LinkedList<SubstrateSwitch> listSWUsed = topo.getListSwitchUsed();
			boolean checkContain = false;
			for(SubstrateSwitch sw : listSWUsed) {
				if(sw.getNameSubstrateSwitch().equals(aggSW.getNameSubstrateSwitch())) {
					checkContain = true;
					break;
				}
			}
			
			if(checkContain == false) {
				listSWUsed.add(aggSW);
				topo.setListSwitchUsed(listSWUsed);
			}
			
			linkAggEdge01.setBandwidth(linkAggEdge01.getBandwidth() - bandwidth);
			linkAggEdge10.setBandwidth(linkAggEdge10.getBandwidth() - bandwidth);
			linkAggEdge02.setBandwidth(linkAggEdge02.getBandwidth() - bandwidth);
			linkAggEdge20.setBandwidth(linkAggEdge20.getBandwidth() - bandwidth);
			
			linkPhyEdge1.setBandwidth(linkPhyEdge1.getBandwidth() - bandwidth);
			linkPhyEdge2.setBandwidth(linkPhyEdge2.getBandwidth() - bandwidth);
		} else {
			success = false;
		}
		
		return success;
	}
	
	
	public Topology linkMappingCoreServer(Topology topo, Map<Integer, PhysicalServer> listPhy, Map<PhysicalServer, LinkedList<SFC>> listServerSFC) {
		
		topo.resetLinkBandWidth();
		listLinkCore = new LinkedList<>();
		listLinkAgg = new LinkedList<>();
		listSFCVLink = new HashMap<>();
		
		LinkedList<SubstrateSwitch> listSwitchON = new LinkedList<SubstrateSwitch>();
		LinkedList<SubstrateSwitch> listSwitchUsed = new LinkedList<SubstrateSwitch>();
		listSwitchUsed = topo.getListSwitchUsed();
		listSwitchON = topo.getMinimunSpanningTree();
		
		if(listSwitchON.size() < listSwitchUsed.size()) {
			listSwitchON.clear();
			listSwitchON.addAll(listSwitchUsed);
		}
		
		LinkedList<SubstrateLink> listLinkBandwidth = topo.getLinkBandwidth();
		LinkedList<LinkPhyEdge> listPhyEdge = topo.getListLinkPhyEdge();
		Map<PhysicalServer, SubstrateSwitch> listLinksServer = topo.getListLinksServer();
		LinkedList<LinkPhyEdge> listLinkPhyEdge = topo.getListLinkPhyEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge = topo.getListAggConnectEdge();
		Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggMap = topo.getListCoreConnectAgg();
		
		
		boolean check = false;
		
		
		
		Collection<PhysicalServer> listPhysical = listPhy.values();
		for(PhysicalServer phy : listPhysical) {			//get edge switch connect to server
			String namePhy = phy.getName();
			SubstrateSwitch edgeSwitch1 = new SubstrateSwitch();
			for(PhysicalServer phyInLinksServer : listLinksServer.keySet()) {
				if(phyInLinksServer.getName().equals(namePhy)) {
					edgeSwitch1 = listLinksServer.get(phyInLinksServer);
					break;
				}
			}
			
			double bandwidthDemandOfServer = 0;
			for(PhysicalServer phyInListServerSFC : listServerSFC.keySet()) {
				if(phyInListServerSFC.getName().equals(namePhy)) {
					LinkedList<SFC> listSFC = listServerSFC.get(phyInListServerSFC);
					for(int index = 0; index < listSFC.size(); index++) {
						bandwidthDemandOfServer += listSFC.get(index).getBandwidth();
					}
					break;
				}
			}
			
			if(edgeSwitch1 == null) 
				System.out.println("abcdef");
			SubstrateSwitch substrateAggr = new SubstrateSwitch();
			SubstrateSwitch substrateCore = new SubstrateSwitch();
			
			
			
			boolean checkAgg = false;
			boolean checkCore = false;
			
			
			//find aggregation switch
			LinkedList<SubstrateSwitch> listAgg = listAggConnectEdge.get(edgeSwitch1);
			if(listAgg == null) 
				System.out.println("cuong oc ");
			
			FIND_AGG_SW:
			for(SubstrateSwitch aggSwitch : listAgg) {
				for(SubstrateLink link : listLinkBandwidth) {
					if(link.getStartSwitch().equals(aggSwitch) && link.getEndSwitch().equals(edgeSwitch1) && link.getBandwidth() >= bandwidthDemandOfServer) {
						substrateAggr = aggSwitch;
						checkAgg = true;
						break FIND_AGG_SW;
					} else if(link.getStartSwitch().equals(edgeSwitch1) && link.getEndSwitch().equals(aggSwitch) && link.getBandwidth() >= bandwidthDemandOfServer) {
						substrateAggr = aggSwitch;
						checkAgg = true;
						break FIND_AGG_SW;
					}
				}
			}
			if(checkAgg == true) {
				LinkedList<SubstrateSwitch> listCore = listCoreConnectAggMap.get(substrateAggr);
				if(listCore == null) 
					System.out.println("null here");
				FIND_CORE_SW:
				for(SubstrateSwitch coreSwitch : listCore) {
					for(SubstrateLink link : listLinkBandwidth) {
						if(link.getStartSwitch().equals(substrateAggr) && link.getEndSwitch().equals(coreSwitch) && link.getBandwidth() >= bandwidthDemandOfServer) {
							substrateCore = coreSwitch;
							checkCore = true;
							break FIND_CORE_SW;
						} else if(link.getStartSwitch().equals(coreSwitch) && link.getEndSwitch().equals(substrateAggr) && link.getBandwidth() >= bandwidthDemandOfServer) {
							substrateCore = coreSwitch;
							checkCore = true;
							break FIND_CORE_SW;
						}
					}
				}
					
					//set bandwidth core to aggregation
				if(checkCore == true && checkAgg == true) {
					
					for(int i = 0; i < listLinkBandwidth.size(); i++) {
						SubstrateLink link = listLinkBandwidth.get(i);
						
						if(link.getStartSwitch().equals(substrateCore) && link.getEndSwitch().equals(substrateAggr)) {
//							SubstrateLink linkAdd = new SubstrateLink();
//							linkAdd = link;
//							listLinkBandwidth.remove(link);
							link.setBandwidth(link.getBandwidth() - bandwidthDemandOfServer);
							
							if(!listLinkCore.contains(link)) {
								listLinkCore.add(link);
							}
							if(link.getBandwidth() < 0 )
								System.out.println();
//							listLinkBandwidth.add(linkAdd);
						} else if(link.getStartSwitch().equals(substrateAggr) && link.getEndSwitch().equals(substrateCore)) {
//							SubstrateLink linkAdd = new SubstrateLink();
//							linkAdd = link;
//							listLinkBandwidth.remove(link);
							link.setBandwidth(link.getBandwidth() - bandwidthDemandOfServer);
							if(link.getBandwidth() < 0 )
								System.out.println();
							
							if(!listLinkCore.contains(link)) {
								listLinkCore.add(link);
							}
//							listLinkBandwidth.add(link);
						}
						
						if(link.getStartSwitch().equals(edgeSwitch1) && link.getEndSwitch().equals(substrateAggr)) {
//							SubstrateLink linkAdd = new SubstrateLink();
//							linkAdd = link;
//							listLinkBandwidth.remove(link);
							link.setBandwidth(link.getBandwidth() - bandwidthDemandOfServer);
							
							
							if(!listLinkAgg.contains(link)) {
								listLinkAgg.add(link);
							}
//							listLinkBandwidth.add(link);
							if(link.getBandwidth() < 0 )
								System.out.println();
						} else if(link.getStartSwitch().equals(substrateAggr) && link.getEndSwitch().equals(edgeSwitch1)) {
//							SubstrateLink linkAdd = new SubstrateLink();
//							linkAdd = link;
//							listLinkBandwidth.remove(link);
							link.setBandwidth(link.getBandwidth() - bandwidthDemandOfServer);
							if(!listLinkAgg.contains(link)) {
								listLinkAgg.add(link);
							}
							if(link.getBandwidth() < 0 )
								System.out.println();
//							listLinkBandwidth.add(linkAdd);
						}
					}
					//set bandwidth agg to edge
					
					//set bandwidth edge switch to physical server
					
					for(int i = 0; i < listLinkPhyEdge.size(); i++) {
						LinkPhyEdge link = listLinkPhyEdge.get(i);
						if(link.getEdgeSwitch().equals(edgeSwitch1) && link.getPhysicalServer().equals(phy)) {
							link.setBandwidth(link.getBandwidth() - bandwidthDemandOfServer);
						}
					}
					
					LinkedList<SubstrateSwitch> list = new LinkedList<>();
					list.add(edgeSwitch1);
					double temp=0;
					if(resultsLinkMapping.containsKey(list))
						temp= resultsLinkMapping.get(list);

					resultsLinkMapping.put(list, bandwidthDemandOfServer+temp);
//					listPathMapped.put(bandwidthDemandOfServer, list);
					
					
					topo.setLinkBandwidth(listLinkBandwidth);
					topo.setListLinkPhyEdge(listPhyEdge);
					check = true;
				}
			}
			//add list switch are ON_STATE
			if(!listSwitchON.contains(edgeSwitch1)) {
				listSwitchON.add(edgeSwitch1);
			}
			if(!listSwitchON.contains(substrateAggr)) {
				listSwitchON.add(substrateAggr);
			}
			if(!listSwitchON.contains(substrateCore)) {
				listSwitchON.add(substrateCore);
			}
		}
			//find core switch
		
		if(check == false) {
			isSuccess = false;
			topo= reverseLinkMapping(topo, resultsLinkMapping);
			reversePhyLinkMapping(topo);
			powerConsumed = getPower(listSwitchON);
		} else {
			isSuccess = true;
			powerConsumed = getPower(listSwitchON);
		}
		
		topo.setListSwitchUsed(listSwitchON);
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
