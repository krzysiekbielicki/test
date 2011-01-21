package pl.skyman.autobuser;


public class ZiL {
	
	public static final String SQL_TABLE_NAME = "zil";
	public static final String SQL_KEY_ID = "_id";
	public static final String SQL_KEY_NAME = "name";
	public static final String SQL_KEY_NAME2 = "name2";
	public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
														SQL_TABLE_NAME + " (" + 
														SQL_KEY_ID + " integer primary key autoincrement, " +
														SQL_KEY_NAME + " text not null," +
														SQL_KEY_NAME2 + " text);";
														//SQL_KEY_NAME + " text);";
}
