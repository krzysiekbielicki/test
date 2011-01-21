package pl.skyman.autobuser;

import java.util.Comparator;

public class StopsNameComparator implements Comparator<MyStop> {
	public int compare(MyStop stop1, MyStop stop2) {
		if(stop1.getStopName().equals(stop2.getStopName()))
			return stop1.getLineName().compareTo(stop2.getLineName());
		else
			return stop1.getStopName().compareTo(stop2.getStopName());
	}
}
