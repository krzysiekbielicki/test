package pl.skyman.autobuser;

public class WDeparture extends Departure {
	String name;
	public WDeparture(Departure dep, String name) {
		super(dep);
		this.name = name;
	}
	
}