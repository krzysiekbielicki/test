package pl.skyman.autobuser;

public class Departure {
	public int minute;
	public boolean isLowFloor;
	public char modifier;
	
	public Departure(int minute, boolean isLowFloor, char modifier) {
		this.minute = minute;
		this.isLowFloor = isLowFloor;
		this.modifier = (char) (modifier<97?(modifier>='A'?modifier+32:'\0'):modifier);
	}
	
	public Departure(Departure dep) {
		this.minute = dep.minute;
		this.isLowFloor = dep.isLowFloor;
		this.modifier = dep.modifier;
	}
}