package pl.skyman.autobuser;

public class Waypoint {
	String stopname = "";
	String stopno = "";
	String linename = "";
	String direction = "";
	double lat, lon;
	int travelTime;
	int stopsNumber;
	boolean end;
	boolean start;
	int departure;
	
	public String toString() {
		return stopname+" "+linename;
	}
}