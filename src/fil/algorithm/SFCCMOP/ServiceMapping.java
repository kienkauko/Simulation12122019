package fil.algorithm.SFCCMOP;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import fil.resource.substrate.*;
import fil.resource.virtual.*;


@SuppressWarnings("serial")
public class ServiceMapping implements java.io.Serializable{
	public LinkedList<SFC> listSFC;
	private boolean isSuccess;
	private Map<PhysicalServer, SubstrateSwitch> listLinksServer;
	private Map<SubstrateSwitch, LinkedList<PhysicalServer>> listPhysConEdge; // list Physical server connected to physical machine
	private Map<SFC, PhysicalServer> listSFCServer;
	private Map<PhysicalServer, LinkedList<SFC>> listServerSFC = new ConcurrentHashMap<>();
	private Map<Integer, Map<Service, PhysicalServer>> needLinkMapping;
	private Map<Integer, Map<Service, PhysicalServer>> needLinkMappingCopy;
	private LinkedList<PhysicalServer> listServer2Core;
	private Map<Integer, PhysicalServer> listServerUsed;
	private Map<PhysicalServer, LinkedList<SFC>> numReceiveServer = new ConcurrentHashMap<>();
	private boolean isSatisfiedCPU;
	private int numService;
	private double powerServer;
	private boolean separateService;
	private double cpuUtilization;
	private Topology topo;
	private int K_PORT_SWITCH = 10;
	private LinkedList<SFC> totalSFCServer;
	
	public ServiceMapping() {
		isSuccess = false;
		listSFC = new LinkedList<SFC>();
		listServer2Core = new LinkedList<>();
		listLinksServer = new HashMap<>();
		listPhysConEdge = new HashMap<>();
		listServerUsed = new HashMap<>();
		listSFCServer = new ConcurrentHashMap<>();
		this.setListServerSFC(listServerSFC);
		this.setNumReceiveServer(numReceiveServer);
		needLinkMapping = new HashMap<>();
		needLinkMappingCopy = new HashMap<>();
		isSatisfiedCPU = false;
		numService = 0;
		powerServer = 0;
		separateService = false;
		topo = new Topology();
		totalSFCServer = new LinkedList<>();
	}
	
	public LinkedList<SFC> run(LinkedList<SFC> listSFC, Topology topo, boolean leaving) {
		
		this.topo = topo;
		// getNumService
		needLinkMapping = new HashMap<>();
		this.listLinksServer = this.topo.getListLinksServer();
		Map<Integer, PhysicalServer> listPhysical = this.topo.getListPhyServers();  //get list physical server
		LinkedList<SFC> listRemainSFC = new LinkedList<>();  // save SFC not be mapped
		
		if(leaving == false)
			totalSFCServer.addAll(listSFC);
		
		LinkedList<SFC> listSFCTemp = new LinkedList<>();
		listSFCTemp.addAll(listSFC);
		
		//get switch connect to server
		
//		for (Entry<PhysicalServer, SubstrateSwitch> entry : listLinksServer.entrySet()) {
//			// stupid, get physical server
//			PhysicalServer phy = listPhysical.get(Integer.parseInt(entry.getKey().getName()));
//			// System.out.println("Physical server "+phy.getName()+" CPU
//			// "+phy.getCpu()+" RAM "+phy.getRam());
//			SubstrateSwitch edge = entry.getValue();
//			if (listPhysConEdge.containsKey(edge)) {
//				LinkedList<PhysicalServer> listPhy = listPhysConEdge.get(edge);
//				listPhy.add(phy);
//				listPhysConEdge.put(edge, listPhy);
//			} else {
//				LinkedList<PhysicalServer> listPhy = new LinkedList<>();
//				listPhy.add(phy);
//				listPhysConEdge.put(edge, listPhy);
//			}
//		}
//		for(int i = 1; i <= 128; i++ ) {
//			listServerSFC.put(new PhysicalServer(String.valueOf(i)), new LinkedList<SFC>());
//			numReceiveServer.put(new PhysicalServer(String.valueOf(i)), new LinkedList<SFC>());
//		}
		
		
		LinkedList<SFC> listMappedSFCperRequest = new LinkedList<SFC>();
		LinkedList<SFC> listMappedSFC = new LinkedList<SFC>();
		int previousService = 0;
		for (Entry<Integer, PhysicalServer> entry : listPhysical.entrySet()) {
			
			String namePhy = entry.getValue().getName();
			
			listMappedSFC.clear();
			
			if(entry.getValue().getUsedCPUServer() > 98.6) {
				continue;
			}
			
			while (!listSFCTemp.isEmpty()) {
				SFC sfc = listSFCTemp.get(0);
				//compare cpu chain with remain cpu server
				double cpuSFC = 0;
				int serviceCount = 0; // number of service inside SFC belongs to cloud
				for(int i = 4; i >= 2; i--) {
					if(!sfc.getService(i).getBelongToEdge()) {
						cpuSFC += sfc.getService(i).getCpu_server();  //calculate all cpu used by chain
						serviceCount++;
					}
				}
				
				
				
				if(cpuSFC <= entry.getValue().getRemainCPU()) { // enough cpu to map
					
//					if(!listMappedSFCperRequest.contains(sfc)) {
					listMappedSFCperRequest.add(sfc);
					System.out.println("sfc nam tren may " + entry.getValue().getName() + " with CPU " + cpuSFC + " remain Cpu " + entry.getValue().getRemainCPU());
//					}
					
					
					
					if(!listMappedSFC.contains(sfc)) {
						listMappedSFC.add(sfc);
						System.out.println("listMappedSFC nhay vao day !!!");
					}
					//add to list sfc belong to a server
					entry.getValue().setUsedCPUServer(cpuSFC);
					listSFCServer.put(sfc, entry.getValue());
					numService += serviceCount; // total services run on cloud
					if(!listRemainSFC.isEmpty()) {
						listRemainSFC.remove(0);
					}
					//add service need link mapping
					if(separateService == true) {
						Map<Service, PhysicalServer> needLink = new HashMap<>();
						needLink.put(sfc.getService(previousService), entry.getValue());
						needLinkMapping.put(previousService, needLink);
						separateService = false;
					}
					
					listSFCTemp.remove(0);
				} else {
					if(!listRemainSFC.contains(sfc)){
						listRemainSFC.add(sfc); //list not mapping
					}
						listSFCTemp.remove(0);
				}
				
				if(serviceCount == 1 && !sfc.getService(4).isBelongToEdge()) { // only Receive is mapped to cloud
					
					//lap di lap lai nhieu lan sau moi lan thu map
					
					LinkedList<SFC> listNumReceive = new LinkedList<>();
					
					for(PhysicalServer phy : numReceiveServer.keySet()) {
						if(phy.getName().equals(namePhy)) {
							listNumReceive = numReceiveServer.get(phy);
							listNumReceive.add(sfc);
							numReceiveServer.put(phy, listNumReceive); // number of independent receive in a server
							break;
						}
//						else {
//							listNumReceive.add(sfc);
//							numReceiveServer.put(entry.getValue(), listNumReceive);
//						}
					}
				}
			}
			
//			if(!listMappedSFC.isEmpty()) {
			
			if(entry.getValue().getUsedCPUServer() > 100) {
				System.out.println("cpuUsedServer > 100");
			}
			
				for(PhysicalServer phy : listServerSFC.keySet()) {
					if(phy.getName().equals(namePhy)){
					LinkedList<SFC> sfcTemp = new LinkedList<SFC>();
					sfcTemp = listServerSFC.get(phy);
					for(int index = 0; index < listMappedSFC.size(); index++) {
						if(!sfcTemp.contains(listMappedSFC.get(index))) {
							sfcTemp.add(listMappedSFC.get(index));
						}
					}
//					sfcTemp.addAll(listMappedSFC);
//					}
//					listServerSFC.put(phy, sfcTemp);
					
					if(sfcTemp.size() > 20) {
						System.out.println("sfcTemp size = " + sfcTemp.size());
					}
					
					int index = Integer.parseInt(entry.getValue().getName());
					listServerUsed.put(index, entry.getValue());
					break;
					}
				}
//			}
			
			///not done all map to next server
			if(listRemainSFC.isEmpty()) {
				isSuccess = true;
				
				break;
			} else {
				/*Kien change 13-09*/
//				int index = Integer.parseInt(entry.getValue().getName());
//				listServerUsed.put(index, entry.getValue());
			
				//divide sfc to map
				
				SFC sfcA = new SFC();
				
				sfcRemain_LOOP:
				for(SFC sfcRemain : listRemainSFC) {
					boolean separateServiceTemp = false;
					double cpuSFCRemain = 0;
					if(sfcRemain.getService(3).getBelongToEdge()) {
						continue;
					}
					for(int i = 4; i >= 2; i--) {
						if(!sfcRemain.getService(i).getBelongToEdge()) {
							cpuSFCRemain = sfcRemain.getService(i).getCpu_server();  //calculate all cpu used by chain
							if(cpuSFCRemain > entry.getValue().getRemainCPU() || cpuSFCRemain <= 0) {

								break;
							}
							else {
								//co the map dc
								separateServiceTemp = true;
								separateService = true;
								numService++;
								sfcA = new SFC(sfcRemain.getName(), 0);
								sfcA.getService(i).setBelongToEdge(true);
								//setCPu Server
								entry.getValue().setUsedCPUServer(cpuSFCRemain);
								//change position state
								Service service = new Service();
								service = sfcRemain.getService(i);
								sfcRemain.setServicePosition(service, true);
								previousService = i-1;   //service truoc no
								//set sfc belong to server
//								listSFCServer.put(sfcRemain, entry.getValue());
								needLinkMapping.clear();
								Map<Service, PhysicalServer> needLink = new HashMap<>();
								needLink.put(sfcA.getService(i), entry.getValue());
								needLinkMapping.put(i, needLink);
							}
						}
					}
					if (separateServiceTemp == true) {
//						listMappedSFCperRequest.add(sfcA);
						LinkedList<SFC> listMap = new LinkedList<>();
						
						listSFCServer.put(sfcA, entry.getValue());
						
						//them listMap vao server
						for(PhysicalServer phy : listServerSFC.keySet()) {
							if(phy.getName().equals(namePhy)){
								listMap = listServerSFC.get(phy);
								
								for(int index = 0; index < listMappedSFC.size(); index++) {
									if(!listMap.contains(listMappedSFC.get(index))) {
										listMap.add(listMappedSFC.get(index));
									}
								}
								listMap.add(sfcA);
								
								if(listMap.size() > 20) {
									System.out.println("listMap > 20");;
								}
								
								break;
							}
						}
						
//						int index = Integer.parseInt(entry.getValue().getName());
//						listServerUsed.put(index, entry.getValue());
					} else break sfcRemain_LOOP;
				}
			}
			
			for(int index=0; index < listRemainSFC.size(); index++) {
				listSFCTemp.add(listRemainSFC.get(index));
			}
		} // end physical server loop
		
		
		
		
//		if(!needLinkMapping.isEmpty()) {
//			needLinkMappingCopy.putAll(needLinkMapping);
//		}
		return listMappedSFCperRequest;
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
	
	public void resetRpiSFC(LinkedList<SFC> listSFC, LinkedList<SubstrateLink> listLinkBandwidth) {
		
		int count = 0;
		int reduceNumSFC = 0;
		int reduceCpu = 0;
		
//		lay listPhysicalServer tu topo
		Map<Integer, PhysicalServer> listPhysicalServer = this.topo.getListPhyServers();
		
		for(SFC sfc : listSFC) {
			
			totalSFCServer.remove(sfc);
			
			double cpuSFC = 0;
			for(int i = 4; i >= 2; i--) {
				if(!sfc.getService(i).getBelongToEdge()) {
					cpuSFC += sfc.getService(i).getCpu_server();
				}
			}

			
			boolean check = false;
			String sfcName = sfc.getName();
			PhysicalServer phy = new PhysicalServer();
			String namePhy = "";
			for(SFC sfcSet : listSFCServer.keySet()) {
				if(sfcSet.getName().equals(sfcName)) {
					phy = listSFCServer.get(sfcSet);
					namePhy = phy.getName();
					check = true;
					
					listSFCServer.remove(sfcSet);
					count++;
					
//					for(Integer server : numSFCinServer.keySet()) {
//						if(Integer.parseInt(namePhy) >= server && Integer.parseInt(namePhy) < (server + 5)) {
//							int numChain = numSFCinServer.get(server);
//							numChain--;
//							numSFCinServer.put(server, numChain);
//							reduceNumSFC++;
//							break;
//						}
//					}
					
					listServerSFC_LOOP:
					for(PhysicalServer phyInListServerSFC : listServerSFC.keySet()) {
						if(phyInListServerSFC.getName().equals(namePhy)) {
							LinkedList<SFC> listSFCInListServerSFC = listServerSFC.get(phyInListServerSFC);
							for(int i = 0; i < listSFCInListServerSFC.size(); i++) {
								SFC sfcInlistSFCInListServerSFC = listSFCInListServerSFC.get(i);
								if(sfcInlistSFCInListServerSFC.getName().equals(sfcName)) {
									listSFCInListServerSFC.remove(sfcInlistSFCInListServerSFC);
//									listServerSFC.put(phyInListServerSFC, listSFCInListServerSFC);
									
//									phy = listPhysicalServer.get(Integer.parseInt(phy.getName()));
									phy.setUsedCPUServer(-cpuSFC);
									reduceCpu++;
//									if(phy.getUsedCPUServer() <= 0) {
//										int server = Integer.parseInt(namePhy);
//										listServerUsed.remove(server);
//									}
//										gan lai cho listServer
									listPhysicalServer.put(Integer.parseInt(phy.getName()), phy);
//										numReceiveServer.put(phy, listSFCInListServerSFC);
									break listServerSFC_LOOP;
								}
							}
							break;
						}
					}
					
					//xoa receive doc lap trong numReceiveServer
					numReceiveServer_LOOP:
					for(PhysicalServer phyInNumReceiveServer : numReceiveServer.keySet()) {
						if(phyInNumReceiveServer.getName().equals(namePhy)) {
							LinkedList<SFC> listSFCInNumReceiveServer = numReceiveServer.get(phyInNumReceiveServer);
							for(int i = 0; i < listSFCInNumReceiveServer.size(); i++) {
								SFC sfcInNumReceiveServer = listSFCInNumReceiveServer.get(i);
								if(sfcInNumReceiveServer.getName().equals(sfcName)) {
									listSFCInNumReceiveServer.remove(sfcInNumReceiveServer);
									numReceiveServer.put(phyInNumReceiveServer, listSFCInNumReceiveServer);
									break numReceiveServer_LOOP;
								}
							}
						}
					}
					
//					break;
				}
			}
			
			//xoa sfc trong listServerSFC, tra lai cpu trong listPhysicalServer cua topo
			
			
			if(check == false) continue;
			
			
////			tra lai link mapping
//			Set<Service> services =  needLinkMappingCopy.keySet();
//			for(Service service : services) {
//				if(sfc.getName().equals(service.getNameService())) {
//					LinkedList<SubstrateSwitch> phySwitch = this.topo.getListPhySwitch();
//					
//					for (LinkPhyEdge link : listBandwidthPhyEdge.keySet()) {
//						link.setBandwidth(link.getBandwidth()+listBandwidthPhyEdge.get(link));
//						link.getEdgeSwitch().setPort(getSwitchFromID(phySwitch, link.getPhysicalServer().getName()), -listBandwidthPhyEdge.get(link));
//					}
		}
		System.out.println("count == " + count);
		System.out.println("reduceCpu " + reduceCpu);
	}
	
	public void leavingRemapServer() {
//		LinkedList<SFC> listSFCRemap = new LinkedList<>();
		Map<Integer, PhysicalServer> listPhysicalServer = this.topo.getListPhyServers();
		LinkedList<SubstrateLink> linkBandwidth = this.topo.getLinkBandwidth();
		
//		for(PhysicalServer phy : listServerUsed.values()) {
//			String namePhy = phy.getName();
//			
//			for(PhysicalServer phyInListServerSFC : listServerSFC.keySet()) {
//				if(phyInListServerSFC.getName().equals(namePhy)) {
//					LinkedList<SFC> listSFCTemp = listServerSFC.get(phyInListServerSFC);
//					if(!listSFCTemp.isEmpty()) {
//						listSFCRemap.addAll(listSFCTemp);
//					}
//					break;
//				}
//			}
		for(PhysicalServer phyInListPhysicalServer : listPhysicalServer.values()) {
//				if(phyInListPhysicalServer.getName().equals(namePhy)) {
			phyInListPhysicalServer.resetCPU();
//					break;
//				}
		}
//		}
		
		//reset linkBandwidth
		for(int index = 0; index < linkBandwidth.size(); index++){
			linkBandwidth.get(index).setBandwidth(1000);
		}
		
		this.topo.resetLinkBandWidth();
		
		this.setListServerSFC(new ConcurrentHashMap<>());
		this.setListServerUsed(new ConcurrentHashMap<>());
		//remap all remain sfc
		System.out.println("size "+ totalSFCServer.size());
		this.run(totalSFCServer, this.topo, true);
	}
	
	public boolean remappingAggrFarGroup(VirtualLink vLink) {
		
		boolean isSuccess = false;
		//remapping if 2 service connect through core switch
		
		Service sService = vLink.getsService();
		Service dService = vLink.getdService();
		
		Map<Integer, PhysicalServer> listPhysicalServer = this.topo.getListPhyServers();
//		sService.getNameService();
//		sService.getSfcID();
		SFC sfc = new SFC(sService.getNameService(), sService.getSfcID());
		
		Map<Integer, Map<Service, PhysicalServer>> remainCopy = new HashMap<>();
		remainCopy.putAll(needLinkMapping);
		
		PhysicalServer phyA = new PhysicalServer();
		PhysicalServer phyB = new PhysicalServer();
		
		
		for(Integer serviceType : remainCopy.keySet()) {
			int serviceCount = 0;
			Map<Service, PhysicalServer> service = remainCopy.get(serviceType);
			for(Service sv : service.keySet()) {
				if(sv.getServiceType().equals(sService.getServiceType())) {
					phyB = service.get(sv);
					serviceCount++;
				} else if(sv.getServiceType().equals(dService.getServiceType())) {
					phyA = service.get(sv);
					serviceCount++;
				}
				if(serviceCount == 2) {
					break;
				}
			}
		}
		
		LinkedList<SFC> listSFCA = new LinkedList<>();
		LinkedList<SFC> listSFCB = new LinkedList<>();
		int phyCount = 0;
		for(PhysicalServer phy : listServerSFC.keySet()) {
			if(phy.getName().equals(phyA.getName())) {
				listSFCA = listServerSFC.get(phy);
				phyCount++;
			} else if(phy.getName().equals(phyB.getName())) {
				listSFCB = listServerSFC.get(phy);
				phyCount++;
			}
			if(phyCount == 2) {
				break;
			}
		}
		
		int numReceiveA = 0;
		int numReceiveB = 0;
		
		phyCount = 0;
		for(PhysicalServer phy : numReceiveServer.keySet()) {
			if(phy.getName().equals(phyA.getName())) {
				numReceiveA = numReceiveServer.get(phy).size();
				phyCount++;
			} else if(phy.getName().equals(phyB.getName())) {
				numReceiveB = numReceiveServer.get(phy).size();
				phyCount++;
			}
			if(phyCount == 2) {
				break;
			}
		}
		
		double cpuDemand = 0;
		SFC sfcB = new SFC();
		SFC sfcA = new SFC();
		int numServiceA=0;
		int element = 0;
		
		System.out.println("listSFCA.size" + listSFCA.size());
		System.out.println("listSFCB.size" + listSFCB.size());
		
//		if(listSFCA.size() < listSFCB.size()) {
//			PhysicalServer phyTemp = phyA;
//			phyA = phyB;
//			phyB = phyTemp;
//			LinkedList<SFC> listSFCTemp = new LinkedList<>();
//			listSFCTemp = listSFCA;
//			listSFCA = listSFCB;
//			listSFCB = listSFCTemp;
//		}
		
		SFCA_LOOP:
		for(int index = 0; index < listSFCA.size(); index++) {
			SFC sfc1 = listSFCA.get(index);
			for(SFC sfc2 : listSFCB) {
				if(sfc1.getService(1).getNameService().equals(sfc2.getService(1).getNameService())) {
					sfcB = sfc2;
					sfcA = sfc1;
					element = index;
					//xet cpu demand cho viec di chuyen service tu B sang A
					for(int i = 4; i >= 2; i--) {
						if(!sfc2.getService(i).isBelongToEdge()) {
							cpuDemand += sfc2.getService(i).getCpu_server();
						}
						if(!sfc1.getService(i).isBelongToEdge()) {
							numServiceA++;
						}
					}
					break SFCA_LOOP;
				}
			}
		}
		if(numServiceA == 1) {
			numReceiveA--;
		}
		
		if((numReceiveA)*5 >= cpuDemand) { //neu demand nho hon so receive doc lap thi tien hanh chuyen
			double numReceiveEvacuate = Math.ceil(cpuDemand/5);
			
			listSFCServer.remove(sfcB); // xoa sfc khoi may B
			for(int i=4; i>=2; i--) {
				if(sfcB.getService(i).isBelongToEdge()) {
					sfcA.setServicePosition(sfcB.getService(i), false);
				} else {
					sfcA.setServicePosition(sfcB.getService(i), false);
					break;
				}
			}
			listSFCServer.put(sfcA, phyA); //set sfcA thuoc may phyA
			//set lai listSFCA may A
			listSFCA.add(element, sfcA);
//			listServerSFC.put(phyA, listSFCA);
			isSuccess = true;
			
			//set lai cpu hai may A, B
			
			int countSV = 0;
			for(PhysicalServer phy : listPhysicalServer.values()) {
				if(phy.getName().equals(phyA.getName())) {
					/* Kien fix 11-09-2019*/
					phy.setUsedCPUServer(-(numReceiveEvacuate)*5 + cpuDemand);
					
					if(phy.getUsedCPUServer() < 0) throw new java.lang.Error("cpuServer wrong value");
					countSV++;
				} else if(phy.getName().equals(phyB.getName())) {
					phy.setUsedCPUServer(-cpuDemand);
					countSV++;
				}
				if(countSV == 2) {
					break;
				}
			}
			
			LinkedList<SFC> sfcEvacuate = new LinkedList<>();
			LinkedList<SFC> receiveInA = new LinkedList<>();
			
			for(PhysicalServer phyInNumReceiveServer : numReceiveServer.keySet()) {
				if(phyInNumReceiveServer.getName().equals(phyA.getName())) {
					receiveInA = numReceiveServer.get(phyInNumReceiveServer);
					break;
				}
			}
			
			
			int numReceiveEvacuateCount = 0;
			for(SFC sfcTemp : receiveInA) {
				sfcEvacuate.add(sfcTemp);
				
				listSFCA.remove(sfcTemp);
				numReceiveEvacuateCount++;
				if(numReceiveEvacuateCount > numReceiveEvacuate) {
					break;
				}
			}
			
			
			
			
			run(sfcEvacuate, this.topo, true); //chay ham run() de map listSFC
			if(this.isSuccess()) {
				isSuccess = true;
				sService.setBelongToEdge(false);
				System.out.println("Remap Aggr / Core successfully");
			} else {
				isSuccess = false;
			}
		}
		return isSuccess;
	}
	
	
	public HashMap<Integer, PhysicalServer> sortListServer(HashMap<Integer, PhysicalServer> list) {
		
		//create a list from elements of HashMap
		LinkedList<Map.Entry<Integer, PhysicalServer> > listMap = new LinkedList<Map.Entry<Integer, PhysicalServer>>(list.entrySet());
		
		//sort the list
		Collections.sort(listMap, new Comparator<Map.Entry<Integer, PhysicalServer>>() {
			@Override
			public int compare(Map.Entry<Integer, PhysicalServer> o1, Map.Entry<Integer, PhysicalServer> o2) {
				if (o1.getValue().getUsedCPUServer() > o2.getValue().getUsedCPUServer()) {
					return -1;
				}
				if (o1.getValue().getUsedCPUServer() < o2.getValue().getUsedCPUServer()) {
					return 1;
				}
				return 0;
			}
		});
		
		//put data from sorted list to HashMap
		HashMap<Integer, PhysicalServer> temp = new LinkedHashMap<Integer, PhysicalServer>();
		for(Map.Entry<Integer, PhysicalServer> aa : listMap) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}

	public Map<SFC, PhysicalServer> getListSFCServer() {
		return listSFCServer;
	}

	public void setListSFCServer(Map<SFC, PhysicalServer> listSFCServer) {
		this.listSFCServer = listSFCServer;
	}

	public boolean isSeparateService() {
		return separateService;
	}

	public void setSeparateService(boolean separateService) {
		this.separateService = separateService;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public double getPowerServer() {
		double power = 0;
		for(PhysicalServer phy : listServerUsed.values()) {
			double cpuServer = phy.getUsedCPUServer();
			if(cpuServer < 1) continue;
			if (cpuServer > 100) throw new java.lang.Error("cpuServer > 100");
			double powerTemp = phy.calculatePowerServer(cpuServer);
			power += powerTemp;
		}
		return power;
	}

	public void setPowerServer(double powerServer) {
		this.powerServer = powerServer;
	}

	public Map<Integer, PhysicalServer> getListServerUsed() {
		return listServerUsed;
	}

	public void setListServerUsed(Map<Integer, PhysicalServer> listServerUsed) {
		this.listServerUsed = listServerUsed;
	}

	public Map<PhysicalServer, LinkedList<SFC>> getNumReceiveServer() {
		return numReceiveServer;
	}

	public void setNumReceiveServer(Map<PhysicalServer, LinkedList<SFC>> numReceiveServer) {
		for(int i = 1; i <= (K_PORT_SWITCH*K_PORT_SWITCH*K_PORT_SWITCH/4) ; i++) {
			numReceiveServer.put(new PhysicalServer(String.valueOf(i)), new LinkedList<SFC>());
		}
		this.numReceiveServer = numReceiveServer;
	}

	public Map<PhysicalServer, LinkedList<SFC>> getListServerSFC() {
		return listServerSFC;
	}

	public void setListServerSFC(Map<PhysicalServer, LinkedList<SFC>> listServerSFC) {
		for(int i = 1; i <= (K_PORT_SWITCH*K_PORT_SWITCH*K_PORT_SWITCH/4) ; i++) {
			listServerSFC.put(new PhysicalServer(String.valueOf(i)), new LinkedList<SFC>());
		}
		this.listServerSFC = listServerSFC;
	}

	public Map<Integer, Map<Service, PhysicalServer>> getNeedLinkMapping() {
		return needLinkMapping;
	}

	public void setNeedLinkMapping(Map<Integer, Map<Service, PhysicalServer>> needLinkMapping) {
		this.needLinkMapping = needLinkMapping;
	}

	public double getCpuUtilization() {
		double cpuServer=0;
		for(PhysicalServer phy : listServerUsed.values()) {
			cpuServer += phy.getUsedCPUServer();
		}
		if(listServerUsed.size() == 0) throw new java.lang.Error(" Used server = 0 ");
		double utilization = cpuServer/(listServerUsed.size()*100);
		return utilization;
	}
	
	public int getServerUsed() {
		return listServerUsed.size();
	}

	public void setCpuUtilization(double cpuUtilization) {
		this.cpuUtilization = cpuUtilization;
	}
	
	public int getNumChainMapped() {
		int numChain = 0;
		for(PhysicalServer phyUsed : this.listServerUsed.values()) {
			String namePhy = phyUsed.getName();
			for(PhysicalServer phy : this.listServerSFC.keySet()) {
				if(phy.getName().equals(namePhy)) {
					numChain += this.listServerSFC.get(phy).size();
					break;
				}
			}
		}
		return numChain;
	}

	public LinkedList<SFC> getTotalSFCServer() {
		return totalSFCServer;
	}

	public void setTotalSFCServer(LinkedList<SFC> totalSFCServer) {
		this.totalSFCServer = totalSFCServer;
	}
}
