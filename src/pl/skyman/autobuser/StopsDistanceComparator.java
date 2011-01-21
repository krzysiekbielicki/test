package pl.skyman.autobuser;

import java.util.Comparator;

public class StopsDistanceComparator implements Comparator<MyStop> {
	public int compare(MyStop stop1, MyStop stop2) {
	//	if(stop1.getDistance()==stop2.getDistance())
			
		return stop1.getDistance()>stop2.getDistance()?1:-1;
	}

}
