package pl.skyman.autobuser;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Contacts;

public class AutobuserQSBProvider extends ContentProvider {
	/**
     * The columns we'll include in our search suggestions.  There are others that could be used
     * to further customize the suggestions, see the docs in {@link SearchManager} for the details
     * on additional columns that are supported.
     */
    private static final String[] COLUMNS = {
            "_id",  // must include this column
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_QUERY,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            };

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String like = uri.getLastPathSegment().toUpperCase().trim();
		Resources r = getContext().getResources();
		MatrixCursor cursor = new MatrixCursor(COLUMNS);
		Cursor c;
		boolean found = false;
		if(like.startsWith(r.getString(R.string.setRoute_From_hint).toUpperCase()) || like.indexOf(" "+r.getString(R.string.setRoute_To_hint).toUpperCase()+" ") > 0) {
			String[] data = like.split(r.getString(R.string.setRoute_From_hint).toUpperCase()+" ", 2);
			data = data[data.length-1].split(" "+r.getString(R.string.setRoute_To_hint).toUpperCase()+" ", 2);
			like = data[0];
			if(data.length == 2)
				like = data[1];
			
			c = getContext().getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_NAME}, "(name) LIKE ('"+like+"%') OR (name) LIKE ('% "+like+"%') OR (name2) LIKE ('"+like+"%') OR (name2) LIKE ('% "+like+"%') OR (name) LIKE ('%."+like+"%') OR (name2) LIKE ('%."+like+"%')", null, null);
			if(!c.moveToFirst()) {
				c.close();
				like = data[0];
				c = getContext().getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_NAME}, "(name) LIKE ('"+like+"%') OR (name) LIKE ('% "+like+"%') OR (name2) LIKE ('"+like+"%') OR (name2) LIKE ('% "+like+"%') OR (name) LIKE ('%."+like+"%') OR (name2) LIKE ('%."+like+"%')", null, null);
			} else {
				c.moveToPrevious();
			}
			String text;
			while (c.moveToNext()) {
	        	text = c.getString(0);
	        	if(text.equals(like))
	        		found = true;
	        	if(!text.matches("[0-9NZ-]{1,3}")) {
	        		if(data.length == 2)
	        			cursor.addRow(new String[] {text, String.format("%s > %s", data[0], text), String.format(r.getString(R.string.routeFromTo), data[0], text), String.format(r.getString(R.string.QrouteFromTo), data[0], text), "route/"+data[0]+"/"+text});
	        		else
	        			cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.routeFrom), text), String.format(r.getString(R.string.QrouteFrom), text), "route/"+text+"/@GPS"});
	        	}
	        }
			Cursor contacts = getContext().getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('"+like+"%') OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('% "+like+"%')) AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            //getContext().startManagingCursor(contacts);
			while (contacts.moveToNext()) {
	        	text = contacts.getString(1);
	        	if(text.equals(like))
	        		found = true;
	        	if(data.length == 2)
        			cursor.addRow(new String[] {text, String.format("%s > %s", data[0], text), String.format(r.getString(R.string.routeFromTo), data[0], text), String.format(r.getString(R.string.QrouteFromTo), data[0], contacts.getString(2)), "route/"+data[0]+"/"+contacts.getString(2)});
	        	else
        			cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.routeFrom), text), String.format(r.getString(R.string.QrouteFrom), contacts.getString(2)), "route/"+contacts.getString(2)+"/@GPS"});
	        }
	        
			if(!found)
				if(data.length == 2)
        			cursor.addRow(new String[] {like, String.format("%s > %s", data[0], data[1]), String.format(r.getString(R.string.routeFromTo), data[0], data[1]), String.format(r.getString(R.string.QrouteFromTo), data[0], data[1]), "route/"+data[0]+"/"+data[1]});
        		else
        			cursor.addRow(new String[] {like, like, String.format(r.getString(R.string.routeFrom), like), String.format(r.getString(R.string.QrouteFrom), like), "route/"+like+"/@GPS"});
		} else if(like.startsWith(r.getString(R.string.setRoute_To_hint).toUpperCase())) {
			like = like.split(" ", 2)[1];
			c = getContext().getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_NAME}, "(name) LIKE ('"+like+"%') OR (name) LIKE ('% "+like+"%') OR (name2) LIKE ('"+like+"%') OR (name2) LIKE ('% "+like+"%') OR (name) LIKE ('%."+like+"%') OR (name2) LIKE ('%."+like+"%')", null, null);
			String text;
			while (c.moveToNext()) {
	        	text = c.getString(0);
	        	if(text.equals(like))
	        		found = true;
	        	if(!text.matches("[0-9NZ-]{1,3}")) {
	        		cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.routeTo), text), String.format(r.getString(R.string.QrouteTo), text), "route/@GPS/"+text});
	        	}
	        }
			Cursor contacts = getContext().getContentResolver().query(Contacts.ContactMethods.CONTENT_URI, new String[] {Contacts.ContactMethods.PERSON_ID, Contacts.PeopleColumns.DISPLAY_NAME, Contacts.ContactMethodsColumns.DATA}, "(UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('"+like+"%') OR UPPER("+Contacts.PeopleColumns.DISPLAY_NAME+") LIKE ('% "+like+"%')) AND "+Contacts.ContactMethods.KIND + " == " + Contacts.KIND_POSTAL, null, null);
            //getContext().startManagingCursor(contacts);
			while (contacts.moveToNext()) {
	        	text = contacts.getString(1);
	        	if(text.equals(like))
	        		found = true;
	        	cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.routeTo), text), String.format(r.getString(R.string.QrouteTo), contacts.getString(2)), "route/@GPS/"+contacts.getString(2)});
	        }
			if(!found)
        		cursor.addRow(new String[] {like, like, String.format(r.getString(R.string.routeTo), like), String.format(r.getString(R.string.QrouteTo), like), "route/@GPS/"+like});
		} else {
			c = getContext().getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_NAME}, "(name) LIKE ('"+like+"%') OR (name) LIKE ('% "+like+"%') OR (name2) LIKE ('"+like+"%') OR (name2) LIKE ('% "+like+"%') OR (name) LIKE ('%."+like+"%') OR (name2) LIKE ('%."+like+"%')", null, null);
			String text;
			while (c.moveToNext()) {
	        	text = c.getString(0);
	        	if(text.length() <= 3 && text.matches("[0-9NZ-]{1,3}")) {
	        		cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.line), text), text, "search/"+text});
	        	} else {
	        		cursor.addRow(new String[] {text, text, String.format(r.getString(R.string.group), text), text, "search/"+text});
	        	}
	        }
		}
		c.close();
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
