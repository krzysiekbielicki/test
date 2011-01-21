package pl.skyman.autobuser;

public class ZiLStop {

	protected String linia;
	protected ZilStopDir dir1 = new ZilStopDir();
	protected ZilStopDir dir2 = new ZilStopDir();
	protected String nazwa;
	
	class ZilStopDir {
		protected String kierunek;
		protected String id_przystanku;
		protected String numer;
		//protected String numer_przystanku;
	}
}
