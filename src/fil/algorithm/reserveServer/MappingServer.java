package fil.algorithm.reserveServer;

import java.util.LinkedList;
import java.util.Map;

import fil.resource.substrate.PhysicalServer;
import fil.resource.virtual.SFC;
import fil.resource.virtual.Service;
import fil.resource.virtual.Topology;



@SuppressWarnings("serial")
public class MappingServer implements java.io.Serializable{
	private double power;
	private ServiceMapping serviceMapping;
	private LinkMapping linkMapping;
	private boolean isSuccess;
	private LinkedList<SFC> listSFC;
	//private Topology topo;
	
	public MappingServer() {
		serviceMapping = new ServiceMapping();
		linkMapping = new LinkMapping();
		this.setPower(0);
		isSuccess = false;
		listSFC = new LinkedList<SFC>();
		//topo = new Topology();
	}
	
	public void runMapping(LinkedList<SFC> listSFC, Topology topo) {
//		topo = fatTree.genFatTree(K);
		this.listSFC.clear();
		System.out.println("list sfc dau vao size " + listSFC.size());
		LinkedList<SFC> listSFCMap = new LinkedList<>();
		listSFCMap = serviceMapping.run(listSFC, topo, false);
		
		System.out.println(" list SFC size " + listSFCMap.size());
		this.setListSFC(listSFCMap);
		System.out.println("Size of this.listSFC " + this.listSFC.size());
		Map<Integer, Map<Service, PhysicalServer>> resultsServiceMapping = serviceMapping.getNeedLinkMapping();
		if(serviceMapping.isSuccess()) {
			System.out.println("Success service mapping! \n");
			isSuccess = true;
			Map<Integer, PhysicalServer> listPhy = serviceMapping.getListServerUsed();
			Map<PhysicalServer, LinkedList<SFC>> listServerSFC = serviceMapping.getListServerSFC();
			linkMapping.linkMappingCoreServer(topo, listPhy, listServerSFC);
			
			System.out.println(" list SFC size " + listSFCMap.size());
			
			double power = linkMapping.getPowerConsumed();
			if(!resultsServiceMapping.isEmpty()) {
				linkMapping.linkMappingOurAlgorithm(topo, resultsServiceMapping, serviceMapping);
				if(linkMapping.isSuccess()) {
					setPower(serviceMapping.getPowerServer() + power);
					isSuccess = true;
				} else {
					setPower(serviceMapping.getPowerServer() + power);
					isSuccess = false;
				}
			} else {
				setPower(serviceMapping.getPowerServer() + linkMapping.getPowerConsumed());
			}
		}
		else {
			System.out.println("failed cmm \n");
			isSuccess = false;
		}
		
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public LinkMapping getLinkMapping() {
		return linkMapping;
	}

	public void setLinkMapping(LinkMapping linkMapping) {
		this.linkMapping = linkMapping;
	}

	public ServiceMapping getServiceMapping() {
		return serviceMapping;
	}

	public void setServiceMapping(ServiceMapping serviceMapping) {
		this.serviceMapping = serviceMapping;
	}

	public LinkedList<SFC> getListSFC() {
		return listSFC;
	}

	public void setListSFC(LinkedList<SFC> listSFC) {
		for(int i = 0; i < listSFC.size(); i++) {
			this.listSFC.add(listSFC.get(i));
		}
	}
}
