package fil.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fil.algorithm.BLRandomServer.BaselineRandom;
import fil.algorithm.MrPhuoc.BaselineMRP;
import fil.algorithm.SFCCM.SFCCM;
import fil.algorithm.SFCCMOP.SFCCMOP;
import fil.algorithm.overallPower.Baseline;
import fil.algorithm.overallPower.OverallPower;
import fil.resource.virtual.GenRequest;

public class AllAlgorithm {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		GenRequest genRequest = new GenRequest();
		BaselineRandom blRD = new BaselineRandom();
		OverallPower op = new OverallPower();
		Baseline blOP = new Baseline();
		BaselineMRP blMRP = new BaselineMRP();
		SFCCM sfccmRD = new SFCCM();
		SFCCMOP sfccmOP = new SFCCMOP();
		
				
		Map<Integer,HashMap<Integer,Integer>> allRequest = genRequest.joinRequest();	
		ArrayList<Integer> numChainPoisson = genRequest.getNumChainPoisson();

		System.out.println("Before run");
		//blRD.run(allRequest,numChainPoisson);
		System.out.println("Done blrd");
		//blMRP.run(allRequest);
		System.out.println("Done blMRP");
		op.run(allRequest, numChainPoisson);
		System.out.println("done op");
		//blOP.run(allRequest);
		System.out.println("done baselineOP");
		//sfccmRD.run(allRequest);
		System.out.println("done sfccmRd");
		//sfccmOP.run(allRequest);
		System.out.println("done sfccmOP");
	}
}
