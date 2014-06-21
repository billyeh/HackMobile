package com.hackmobile.fridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FridgeActivity extends Activity {

	static final int REQUEST_IMAGE_CAPTURE = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fridge);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		/**
		 * Set up JSON file
		 */
		String filename = getString(R.string.local_items);
		if (fileExistence(filename)) {
			Log.d("Fridge", "JSON file already created!");
			return;
		}
		final SharedPreferences prefs = getSharedPreferences(
				getString(R.string.local_prefs), 0);
		String json = "{\"user\": \"" + prefs.getString("username", "anon")
				+ "\", \"items\": []}";
		writeToFile(filename, json);
		Log.d("Fridge", "User file successfully made!");
	}

	protected void onResume() {
		super.onResume();
		setUpUser();

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				String filename = getString(R.string.local_items);
				String json = ImageAdapter.readFromFile(filename);
				JsonParser jsonParser = new JsonParser();
				JsonObject obj = (JsonObject) jsonParser.parse(json);
				JsonArray items = obj.getAsJsonArray("items");
				JsonObject time = items.get(position).getAsJsonObject().getAsJsonObject("dateOfCreation");
				Time creation = new Time();
				Time now = new Time();
				creation.set(time.get("monthDay").getAsInt(), time.get("month").getAsInt(), time.get("year").getAsInt());
				now.setToNow();
				long age = now.toMillis(true) - creation.toMillis(true);
				Time oldness = new Time();
				oldness.set(age);
				Toast.makeText(getBaseContext(), "This thing is " + oldness.format("%e days, %l hours old!"), Toast.LENGTH_LONG).show();
			}
		});
		gridview.setAdapter(new ImageAdapter(this));
	}

	private void setUpUser() {
		/*
		 * Set up username if not registered yet
		 */
		final SharedPreferences prefs = getSharedPreferences(
				getString(R.string.local_prefs), 0);
		if (prefs.contains("username")
				&& !(prefs.getString("username", "anon").equals("anon"))) {
			return; // no need to register new user
		}
		final EditText input = new EditText(this);
		DialogInterface.OnClickListener okClicked = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("username", input.getText().toString());
				editor.commit();
				// TODO register new user in database
			}
		};
		new AlertDialog.Builder(this).setTitle("Welcome!")
				.setMessage("What's your name?").setView(input)
				.setPositiveButton("Done", okClicked).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fridge, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			//Intent takePictureIntent = new Intent(this, TextReco.class);
		}
		return true;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_fridge,
					container, false);

			/*
			 * Register new item button listener
			 */
			Button addItemBtn = (Button) rootView
					.findViewById(R.id.main_add_button);
			Button.OnClickListener addClicked = new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(getActivity()
							.getApplicationContext(), CameraActivity.class);
					Log.d("Fridge", "Starting camera");
					startActivity(intent);
				}
			};
			addItemBtn.setOnClickListener(addClicked);

			return rootView;
		}
	}

	/**
	 * Utility functions
	 */

	private boolean fileExistence(String fname) {
		File file = getBaseContext().getFileStreamPath(fname);
		return file.exists();
	}

	private void writeToFile(String filename, String data) {
		FileOutputStream fos = null;
		try {
			fos = openFileOutput(filename, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.d("Fridge", "Error creating JSON file!");
		}
		try {
			fos.write(data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class ImageAdapter extends BaseAdapter {
	private static Context mContext;

	public ImageAdapter(Context c) {
		mContext = c;
	}

	public int getCount() {
		return getFileNames().size();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return 0;
	}

	// create a new ImageView for each item referenced by the Adapter
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
		if (convertView == null) { // if it's not recycled, initialize some
									// attributes
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new GridView.LayoutParams(200, 200));
			imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			imageView.setPadding(1, 1, 1, 1);
		} else {
			imageView = (ImageView) convertView;
		}

		imageView.setImageBitmap(BitmapFactory.decodeFile(getFileNames().get(
				position)));
		return imageView;
	}

	public static String readFromFile(String FILENAME) {
		String ret = "";

		try {
			InputStream inputStream = mContext.openFileInput(FILENAME);
			ArrayList<String> bandWidth = new ArrayList<String>();

			if (inputStream != null) {
				InputStreamReader inputStreamReader = new InputStreamReader(
						inputStream);
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				String receiveString = "";
				StringBuilder stringBuilder = new StringBuilder();

				while ((receiveString = bufferedReader.readLine()) != null) {
					bandWidth.add(receiveString);
					if (bandWidth.size() == 10)
						bandWidth.remove(0);
				}

				for (String str : bandWidth)
					stringBuilder.append(str + "\n");

				ret = stringBuilder.toString();
				inputStream.close();
			}
		} catch (FileNotFoundException e) {
			Log.i("File not found", e.toString());
		} catch (IOException e) {
			Log.i("Can not read file:", e.toString());
		}
		return ret;
	}

	// references to our images
	private List<String> getFileNames() {
		List<String> filenames = new ArrayList<String>();
		String filename = mContext.getString(R.string.local_items);
		String json = readFromFile(filename);
		JsonParser jsonParser = new JsonParser();
		JsonObject obj = (JsonObject) jsonParser.parse(json);
		JsonArray items = obj.getAsJsonArray("items");
		for (JsonElement item : items) {
			filenames.add(item.getAsJsonObject()
					.getAsJsonPrimitive("imageFileName").getAsString());
		}
		return filenames;
	}
}
