package fil.resource.virtual;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import fil.resource.substrate.LinkPhyEdge;
import fil.resource.substrate.PhysicalServer;
import fil.resource.substrate.SubstrateLink;
import fil.resource.substrate.SubstrateSwitch;

import static java.util.Map.Entry.*;

/**
 * This class helps user to build new substrate network
 * 
 * @author Van Huynh Nguyen
 *
 */
public class Topology implements Serializable {
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> map;
	private LinkedList<SubstrateLink> linkBandwidth; // bandwidth of all links
	private LinkedList<SubstrateLink> linkBandwidthOriginal;
	private Map<PhysicalServer, SubstrateSwitch> listLinksServer;
	private Map<Integer, PhysicalServer> listPhyServers;
	private LinkedList<LinkPhyEdge> listLinkPhyEdge; // bandwidth of all Phy->Edge switch link
	private LinkedList<LinkPhyEdge> listLinkPhyEdgeOriginal;
	private LinkedList<SubstrateSwitch> listSwitch;
	private LinkedList<SubstrateSwitch> listSwitchUsed;
	
	
	//for link mapping
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge; // list agg switch connect to edge switch
	
	private Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg; // list core switch connect to agg switch
	
	private Map<Integer, LinkedList<SubstrateSwitch>> listEdgeSwitchInPod; // list edge switch in pod
	private Map<Integer, LinkedList<SubstrateSwitch>> listAggSwitchInPod; // list agg switch in pod
	private LinkedList<SubstrateSwitch> listPhySwitch; // gia physical server la mot switch
	private Map<Integer, SubstrateSwitch> listAggSwitch;
	private Map<Integer, SubstrateSwitch> listCoreSwitch;
	private Map<Integer, SubstrateSwitch> listEdgeSwitch;
	
	/**
	 * Constructs new topology
	 */
	public Topology() {
		map = new HashMap<>();
		listLinksServer = new HashMap<PhysicalServer, SubstrateSwitch>();
		listPhyServers = new HashMap<>();
		linkBandwidth = new LinkedList<>();
		linkBandwidthOriginal = new LinkedList<>();
		listLinkPhyEdge = new LinkedList<>();
		listLinkPhyEdgeOriginal = new LinkedList<>();
		listSwitch = new LinkedList<>();
		listAggConnectEdge = new HashMap<>();
		listCoreConnectAgg = new HashMap<>();
		listEdgeSwitchInPod = new HashMap<>();
		listAggSwitchInPod = new HashMap<>();
		listPhySwitch = new LinkedList<>();
		listAggSwitch = new HashMap<>();
		listCoreSwitch = new HashMap<>();
		listEdgeSwitch = new HashMap<>();
		listSwitchUsed = new LinkedList<>();
	}
	public Topology(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> mapInput, LinkedList<SubstrateLink> linkBandwidthInput, Map<PhysicalServer, 
			SubstrateSwitch> listLinksServerInput, Map<Integer, PhysicalServer> listPhyServersInput, LinkedList<LinkPhyEdge> listLinkPhyEdgeInput, 
			LinkedList<SubstrateSwitch> listSwitchInput, Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdgeInput, Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAggInput
			, Map<Integer, LinkedList<SubstrateSwitch>> listEdgeSwitchInPodInput, Map<Integer, LinkedList<SubstrateSwitch>> listAggSwitchInPodInput, LinkedList<SubstrateSwitch> listPhySwitchInput
			, Map<Integer, SubstrateSwitch> listAggSwitchInput, Map<Integer, SubstrateSwitch> listCoreSwitchInput, Map<Integer, SubstrateSwitch> listEdgeSwitchInput)
	{
		map = mapInput;
		listLinksServer = listLinksServerInput;
		listPhyServers = listPhyServersInput;
		linkBandwidth = linkBandwidthInput;
		listLinkPhyEdge = listLinkPhyEdgeInput;
		listSwitch =listSwitchInput;
		listAggConnectEdge = listAggConnectEdgeInput;
		listCoreConnectAgg = listCoreConnectAggInput;
		listEdgeSwitchInPod = listEdgeSwitchInPodInput;
		listAggSwitchInPod = listAggSwitchInPodInput;
		listPhySwitch = listPhySwitchInput;
		listAggSwitch = listAggSwitchInput;
		listCoreSwitch = listCoreSwitchInput;
		listEdgeSwitch = listEdgeSwitchInput;
	}
	
	
	public void addEdge(SubstrateSwitch node1, SubstrateSwitch node2, double bandwidth) {
		linkBandwidth.add(new SubstrateLink(node1, node2, bandwidth));
		linkBandwidthOriginal.add(new SubstrateLink(node1, node2, bandwidth));
		LinkedList<SubstrateSwitch> neighbor = map.get(node1);
		if (neighbor == null) {
			neighbor = new LinkedList<>();
			map.put(node1, neighbor);
		}
		neighbor.add(node2);
		if(!listSwitch.contains(node1))
		{
			listSwitch.add(node1);	
		}
		if(!listSwitch.contains(node2))
		{
			listSwitch.add(node2);
		}
	}

	public void addPhysicalServer(SubstrateSwitch edgeSwitch, PhysicalServer physicalServer, double bandwidth) {
		listLinksServer.put(physicalServer, edgeSwitch);
		//1: up, 0:down
		listLinkPhyEdge.add(new LinkPhyEdge(physicalServer, edgeSwitch, bandwidth));
		listLinkPhyEdgeOriginal.add(new LinkPhyEdge(physicalServer, edgeSwitch, bandwidth));
		if(!listPhyServers.containsValue(physicalServer))
			listPhyServers.put(Integer.parseInt(physicalServer.getName()), physicalServer);
		// add physical serer switch - gia switch
		
		SubstrateSwitch s = new SubstrateSwitch(physicalServer.getName(), 0, false);
		listPhySwitch.add(s);
	}

	public void addNeighbor(SubstrateSwitch node1, SubstrateSwitch node2) {
		if (node1.getNameSubstrateSwitch().equals(node2.getNameSubstrateSwitch()))
			return;
		LinkedList<SubstrateSwitch> neighbor = map.get(node1);
		if (neighbor == null) {
			neighbor = new LinkedList<SubstrateSwitch>();
			map.put(node1, neighbor);
		}
		neighbor.add(node2);
	}

	public LinkedList<SubstrateSwitch> adjacentNodes(SubstrateSwitch node) {
		LinkedList<SubstrateSwitch> adjacent = map.get(node);
		if (adjacent == null) {
			return new LinkedList<SubstrateSwitch>();
		}
		return new LinkedList<SubstrateSwitch>(adjacent);
	}

	public int nNeighbors(SubstrateSwitch node) {
		LinkedList<SubstrateSwitch> adjacent = map.get(node);
		if (adjacent == null) {
			return 0;
		}
		return map.get(node).size();
	}

//	public LinkedList<String> getForgetLink() {
//		return forgetLink;
//	}
	// show all informations of Topology
	public void showInfo() {
		for (Entry<SubstrateSwitch, LinkedList<SubstrateSwitch>> entry : map.entrySet()) {
			System.out.println("Node " + entry.getKey());
			for (SubstrateSwitch node : entry.getValue()) {
				System.out.print(node.getNameSubstrateSwitch()+" ");
			}
			System.out.println();
		}
		System.out.println("server");
		for (Entry<PhysicalServer, SubstrateSwitch> entry : listLinksServer.entrySet()) {
			System.out.println(
					"Server " + entry.getKey().getName() + " edgeSwitch " + entry.getValue().getNameSubstrateSwitch());

		}
	}
	
	public void addListEdge(int index, SubstrateSwitch edge) {
		
		this.listEdgeSwitch.put(index, edge);
	}
	
	public void addListAgg(int index, SubstrateSwitch agg) {
		this.listAggSwitch.put(index, agg);
	}
	
	public void addListCore(int index, SubstrateSwitch core) {
		this.listCoreSwitch.put(index, core);
	}
	
	public LinkedList<SubstrateSwitch> getMinimunSpanningTree() {
		
		LinkedList<SubstrateSwitch> listSwitchON = new LinkedList<SubstrateSwitch>();
		
		Map<Integer, SubstrateSwitch> listEdgeSwitchON = this.getListEdgeSwitch();
		Map<Integer, SubstrateSwitch> listAggSwitch = new HashMap<>();
		listAggSwitch = this.getListAggSwitch();
		Map<Integer, SubstrateSwitch> listCoreSwitch = this.getListCoreSwitch();
				
		
		if(!listEdgeSwitchON.isEmpty()) {
			Collection<SubstrateSwitch> listEdgeSwitch = listEdgeSwitchON.values();
			listSwitchON.addAll(listEdgeSwitch);
		} else {
			System.out.println("List edge switch get from topo is empty!");
		}
		
		Map<Integer, SubstrateSwitch> sortListAgg = listAggSwitch
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(
				Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
				(e1, e2) -> e2, LinkedHashMap::new));
		
		int countAgg = 0;
		if(!listAggSwitch.isEmpty()) {
			for(Entry<Integer, SubstrateSwitch> substrateAgg : sortListAgg.entrySet()) {
				if(countAgg == 0) {
					countAgg = substrateAgg.getKey();
				}
				if(listAggSwitch.get(countAgg) == null) {
					break;
				}
				listSwitchON.add(listAggSwitch.get(countAgg));
				countAgg+=10;
			}
		} else {
			System.out.println("List aggregation switch get from topo is empty!");
		}
		
		Map<Integer, SubstrateSwitch> sortListCore = listCoreSwitch
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(
				Collectors.toMap(e -> e.getKey(), e -> e.getValue(),
				(e1, e2) -> e2, LinkedHashMap::new));
		
		if(!listCoreSwitch.isEmpty()) {
			for(Entry<Integer, SubstrateSwitch> substrateCore : sortListCore.entrySet()) {
				listSwitchON.add(substrateCore.getValue());
				break;
			}
		}
		return listSwitchON;
	}

	public Map<PhysicalServer, SubstrateSwitch> getListLinksServer() {
		return listLinksServer;
	}

	public Map<Integer, PhysicalServer> getListPhyServers() {
		return listPhyServers;
	}

	public void setListPhyServers(Map<Integer, PhysicalServer> listPhyServers) {
		this.listPhyServers = listPhyServers;
	}
	public LinkedList<SubstrateLink> getLinkBandwidth()
	
	{
		return linkBandwidth;
	}
	public void setLinkBandwidth(LinkedList<SubstrateLink> linkBandwidth)
	{
		this.linkBandwidth = linkBandwidth;
	}

	public LinkedList<LinkPhyEdge> getListLinkPhyEdge() {
		return listLinkPhyEdge;
	}

	public void setListLinkPhyEdge(LinkedList<LinkPhyEdge> listLinkPhyEdge) {
		this.listLinkPhyEdge = listLinkPhyEdge;
	}
	public void removeEdge(SubstrateSwitch egde1, SubstrateSwitch edge2)
	{
		//get list Edge switch
		LinkedList<SubstrateSwitch> listEdgeSwitch = new LinkedList<>();
		for(Entry<PhysicalServer, SubstrateSwitch> entry: listLinksServer.entrySet())
		{
			if(entry.getValue().equals(egde1) || entry.getValue().equals(edge2))
				continue;
			else
			{
			if(!listEdgeSwitch.contains(entry.getValue()))
				listEdgeSwitch.add(entry.getValue());
			}
		}
		
		System.out.println("List edge switch");
		for(SubstrateSwitch s: listEdgeSwitch)
			System.out.println(s.getNameSubstrateSwitch());
		Iterator<Map.Entry<SubstrateSwitch,LinkedList<SubstrateSwitch>>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<SubstrateSwitch,LinkedList<SubstrateSwitch>> entry = iter.next();
			SubstrateSwitch sKey = entry.getKey();
			LinkedList<SubstrateSwitch> listNeighbor = entry.getValue();
			for(SubstrateSwitch edge: listEdgeSwitch)
			{
				for(int i=0; i<listNeighbor.size(); i++)
					if(listNeighbor.get(i).equals(edge))
						listNeighbor.remove(i);
			}
			
			map.put(sKey, listNeighbor);
		}
		for(SubstrateSwitch s: listEdgeSwitch)
			map.remove(s);
	}

	public LinkedList<SubstrateSwitch> getListSwitch() {
		return listSwitch;
	}

	public Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> getListAggConnectEdge() {
		return listAggConnectEdge;
	}

	public void setListAggConnectEdge(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listAggConnectEdge) {
		this.listAggConnectEdge = listAggConnectEdge;
	}

	public Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> getListCoreConnectAgg() {
		return listCoreConnectAgg;
	}

	public void setListCoreConnectAgg(Map<SubstrateSwitch, LinkedList<SubstrateSwitch>> listCoreConnectAgg) {
		this.listCoreConnectAgg = listCoreConnectAgg;
	}

	public Map<Integer, LinkedList<SubstrateSwitch>> getListEdgeSwitchInPod() {
		return listEdgeSwitchInPod;
	}

	public void setListEdgeSwitchInPod(Map<Integer, LinkedList<SubstrateSwitch>> listEdgeSwitchInPod) {
		this.listEdgeSwitchInPod = listEdgeSwitchInPod;
	}

	public Map<Integer, LinkedList<SubstrateSwitch>> getListAggSwitchInPod() {
		return listAggSwitchInPod;
	}

	public void setListAggSwitchInPod(Map<Integer, LinkedList<SubstrateSwitch>> listAggSwitchInPod) {
		this.listAggSwitchInPod = listAggSwitchInPod;
	}

	public void setListSwitch(LinkedList<SubstrateSwitch> listSwitch) {
		this.listSwitch = listSwitch;
	}

	public LinkedList<SubstrateSwitch> getListPhySwitch() {
		return listPhySwitch;
	}
//	public double getRAMRes()
//	{
//		double totalRam = 0;
//		for(Entry<Integer, PhysicalServer> entry: listPhyServers.entrySet())
//		{
//			totalRam += entry.getValue().getRam();
//		}
//		return totalRam;
//	}
	
	public double getCPURes()
	{
		double totalCPU = 0;
		for(Entry<Integer, PhysicalServer> entry: listPhyServers.entrySet())
		{
			totalCPU += entry.getValue().getUsedCPUServer(); //get usedCPU
		}
		return totalCPU;
	}
	public double getLinkBandwidthTopo()
	{
		
		double totalBW =0;
		for(SubstrateLink link: linkBandwidth)
		{
			totalBW+= link.getBandwidth();
		}
		for(LinkPhyEdge link: listLinkPhyEdge)
		{
			totalBW+=link.getBandwidth();
		}
		return totalBW;
	}
	
	public Object clone(){
		Topology t = new Topology(map, linkBandwidth, listLinksServer, listPhyServers, listLinkPhyEdge, listSwitch, listAggConnectEdge, listCoreConnectAgg, listEdgeSwitchInPod, listAggSwitchInPod, listPhySwitch,listAggSwitch,listCoreSwitch, listEdgeSwitch);
		return t;		
	}
	public Map<Integer, SubstrateSwitch> getListAggSwitch() {
		return listAggSwitch;
	}
	public void setListAggSwitch(Map<Integer, SubstrateSwitch> listAggSwitch) {
		this.listAggSwitch = listAggSwitch;
	}
	public Map<Integer, SubstrateSwitch> getListCoreSwitch() {
		return listCoreSwitch;
	}
	public void setListCoreSwitch(Map<Integer, SubstrateSwitch> listCoreSwitch) {
		this.listCoreSwitch = listCoreSwitch;
	}
	public Map<Integer, SubstrateSwitch> getListEdgeSwitch() {
		return listEdgeSwitch;
	}
	public void setListEdgeSwitch(Map<Integer, SubstrateSwitch> listEdgeSwitch) {
		this.listEdgeSwitch = listEdgeSwitch;
	}
	public LinkedList<SubstrateSwitch> getListSwitchUsed() {
		return listSwitchUsed;
	}
	public void setListSwitchUsed(LinkedList<SubstrateSwitch> listSwitchUsed) {
		this.listSwitchUsed = listSwitchUsed;
	}
	public void resetLinkBandWidth() {
		for(int index = 0; index < this.linkBandwidth.size(); index++) {
			this.linkBandwidth.get(index).setBandwidth(1000);;
		}
		
		for(int index = 0; index < this.listLinkPhyEdge.size(); index++) {
			this.listLinkPhyEdge.get(index).setBandwidth(1000);;
		}
	}
	
	public void setCPUServer(int i) {
		for(PhysicalServer phy : this.listPhyServers.values()) {
			//phy.resetCPU();
			phy.setUsedCPUServer(i*5);
		}
	}
	public double getCPUServer() {
		double average = 0.0;
		for(PhysicalServer phy : this.listPhyServers.values()) {
			//phy.resetCPU();
			average += phy.getUsedCPUServer();
		}
		return (average*1.0)/listPhyServers.size();
	}
}