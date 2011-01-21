package pl.skyman.autobuser;

import java.util.Comparator;

public class ZiLStopComparator implements Comparator<ZiLStop> {
	public int compare(ZiLStop arg0, ZiLStop arg1) {
		return arg0.linia.compareToIgnoreCase(arg1.linia);
	}
}
