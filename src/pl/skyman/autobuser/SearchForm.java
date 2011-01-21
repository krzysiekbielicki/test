package pl.skyman.autobuser;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.CursorToStringConverter;

public class SearchForm extends Activity implements AutobuserActivity {

		@Override
	protected void onResume() {
		getParent().setTitle(R.string.search);
		refreshHistory();
		refreshAutocomplete();
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.searchform);
		((ListView)findViewById(R.id.history)).setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
				String q = ((Cursor)((ListView)findViewById(R.id.history)).getAdapter().getItem(arg2)).getString(1).toUpperCase();
				findZiL(q);
			}
		});
		((ImageButton)findViewById(R.id.search)).setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				int pos = ((AutoCompleteTextView) findViewById(R.id.atv)).getListSelection();
				String q;
				if(pos == ListView.INVALID_POSITION)
					q = ((AutoCompleteTextView) findViewById(R.id.atv)).getText().toString();
				else
					q = ((Cursor)((AutoCompleteTextView) findViewById(R.id.atv)).getAdapter().getItem(pos)).getString(1);
				findZiL(q);
			}
		});
	}
	
	private void refreshAutocomplete() {
		final AutoCompleteTextView atv = (AutoCompleteTextView) findViewById(R.id.atv);
		atv.requestFocus();
		Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, null, null, null);
		SimpleCursorAdapter acAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_dropdown_item_1line, c, new String[] {ZiL.SQL_KEY_NAME}, new int[] {android.R.id.text1});
        c.close();
        acAdapter.setFilterQueryProvider(new FilterQueryProvider() {
        	public Cursor runQuery(CharSequence constraint) {
                String like = "";
                if (constraint != null) {
                	like+=constraint;
                }
                like = like.toUpperCase();
                Cursor c = getContentResolver().query(DatabaseProvider.CONTENT_URI_ZIL, new String[] {ZiL.SQL_KEY_ID, ZiL.SQL_KEY_NAME}, "(name) LIKE ('"+like+"%') OR (name) LIKE ('% "+like+"%') OR (name2) LIKE ('"+like+"%') OR (name2) LIKE ('% "+like+"%') OR (name) LIKE ('%."+like+"%') OR (name2) LIKE ('%."+like+"%')", null, null);
                startManagingCursor(c);
                return c;
            }
        });
        acAdapter.setCursorToStringConverter(new CursorToStringConverter() {

			public CharSequence convertToString(Cursor c) {
				return c.getString(1);
			}
        	
        });
		atv.setAdapter(acAdapter);
		atv.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> adapter, View arg1, int i,	long arg3) {
				String q = atv.getText().toString();
				findZiL(q);
			}
		});
		atv.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					int pos = atv.getListSelection();
					String q;
					if(pos == ListView.INVALID_POSITION)
						q = atv.getText().toString();
					else
						q = ((Cursor)atv.getAdapter().getItem(pos)).getString(1);
					findZiL(q);
					return true;
				}
				return false;
			}
		});
	}
	
	private void refreshHistory() {
		Cursor c = managedQuery(DatabaseProvider.CONTENT_URI_SEARCHHISTORY, new String[] {Search.SQL_KEY_ID, Search.SQL_KEY_VALUE}, null, null, Search.SQL_KEY_TIME+" DESC");
        if(c.moveToFirst()) {
	        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.searchhistoryrecord, c, new String[] {Search.SQL_KEY_VALUE}, new int[] {android.R.id.text1});
	        ((ListView)findViewById(R.id.history)).setAdapter(adapter);
	        ((View)findViewById(R.id.history)).setVisibility(View.VISIBLE);
	        ((View)findViewById(R.id.empty)).setVisibility(View.GONE);
        } else {
        	((View)findViewById(R.id.history)).setVisibility(View.GONE);
	        ((View)findViewById(R.id.empty)).setVisibility(View.VISIBLE);
        }
	}
	
	protected void findZiL(String q) {
		if(this.getCurrentFocus() != null) {
			((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			this.getCurrentFocus().setSelected(false);
		}
		((ImageButton)findViewById(R.id.search)).setSelected(true);
		((ImageButton)findViewById(R.id.search)).requestFocus();
		Intent i = new Intent(SearchForm.this, Search.class);
		i.putExtra("q", q);
		getParent().startActivity(i);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return getParent().onKeyDown(keyCode, event);
	}
	
	public void locationChanges(Location newLocation) {
	}

	public void minuteChanges() {
	}

	public void orientationChanges(float[] newOrientation) {
	}

}
