package fil.resource.virtual;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class SFC implements java.io.Serializable{
	private ArrayList<Boolean> servicePosition;
	private int sfcID;
	private String name;
	private double totalChainCpu;
	private double totalChainBandwidth;
	
	private Capture capture;
	private Decode decode ;
	private Density density;
	private ReceiveDensity receive;
	
	
	private double startTime;
	private double endTime;
	
	public SFC(String name, int sfcID) {
		servicePosition = new ArrayList<Boolean>();
		capture = new Capture(name, sfcID);
		decode = new Decode(name, sfcID);
		density = new Density(name, sfcID);
		receive = new ReceiveDensity(name, sfcID);
		totalChainBandwidth = 0;
		this.setSfcID(sfcID);
		this.setName(name);
		setStartTime(0.0);
		setEndTime(0.0);
	}
	
	public SFC() {
		// TODO Auto-generated constructor stub
	}

	public ArrayList<Boolean> getServicePosition() {
		return servicePosition;
	}
	
	public void setServicePosition (Service service, boolean position) {
		String type = service.getServiceType();
		switch(type) {
		case "capture": this.capture.setBelongToEdge(position);servicePosition.add(position);break;
		case "decode": this.decode.setBelongToEdge(position);servicePosition.add(position);break;
		case "density": this.density.setBelongToEdge(position);servicePosition.add(position);break;
		case "receive": this.receive.setBelongToEdge(position);servicePosition.add(position);break;
		default: System.out.println("error at set position"); break;
		}
	}
	
	public boolean getServicePosition (Service service) {
		String type = service.getServiceType();
		switch(type) {
		case "decode": return this.decode.getBelongToEdge();
		case "density": return this.density.getBelongToEdge();
		default: System.out.println("error at set position"); return false;
		}
	}

//	public void setServicePosition(Capture capture, boolean position) {
//		this.capture = capture;
//		this.capture.setBelongToEdge(position);
//	}
//	public void setServicePosition(Decode decode, boolean position) {
//		this.decode = decode;
//		this.decode.setBelongToEdge(position);
//	}
//	public void setServicePosition(Density density, boolean position) {
//		this.density = density;
//		this.density.setBelongToEdge(position);
//	}
//	public void setServicePosition(ReceiveDensity receive, boolean position) {
//		this.receive = receive;
//		this.receive.setBelongToEdge(position);
//	}
	
	public double getCpuDD(int dec, int den) {

		totalChainCpu = dec*decode.getCpu_pi() + den*density.getCpu_pi()
		+ capture.getCpu_pi() + receive.getCpu_pi();
		
		return totalChainCpu;
	}
	
	public double getBandwidthDD(int dec, int den) {
		
		if(dec != den)
			totalChainBandwidth = dec*this.decode.getBandwidth();
		else if(dec*den == 1)
			totalChainBandwidth = this.density.getBandwidth();
		else
			totalChainBandwidth = this.capture.getBandwidth();
		
		return totalChainBandwidth;
	}
	
	public double getBandwidth() {
		for (int i = (servicePosition.size() - 1); i >=0 ; i--) {
			if (servicePosition.get(i) == false) {
				switch(i) {
				case 0: System.out.println("impposible");break;
				case 1: this.totalChainBandwidth = capture.getBandwidth();break;
				case 2: this.totalChainBandwidth = decode.getBandwidth();break;
				case 3: this.totalChainBandwidth = density.getBandwidth();break;
				}
			}
		}
		return this.totalChainBandwidth;
	}
	public Service getService(int number) {
		if(number == 1) return capture;
		if(number == 2) return decode;
		if(number == 3) return density;
		if(number == 4) return receive;
		else return null;
	}

	public int getSfcID() {
		return sfcID;
	}

	public void setSfcID(int sfcID) {
		this.sfcID = sfcID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double liveTime) {
		this.endTime = this.startTime + liveTime;
	}
}
