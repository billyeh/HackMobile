package com.hackmobile.fridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Files.FileColumns;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CameraActivity extends Activity {

	private Camera mCamera;
	private CameraPreview mPreview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_camera);
		super.onCreate(savedInstanceState);

		// Create an instance of Camera
		mCamera = getCameraInstance();

		// Create our Preview view and set it as the content of our activity.
		mPreview = new CameraPreview(this, mCamera);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);

		Button captureButton = (Button) findViewById(R.id.button_capture);
		mCamera.setDisplayOrientation(90);
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mCamera.takePicture(null, null, mPicture);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mCamera.startPreview();
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera(); // release the camera immediately on pause event
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {

			new ProcessImage(getApplicationContext()).execute(data);

		}
	};

	/**
	 * Utility
	 */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			Log.d("Fridge", "No camera available");
		}
		return c;
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				"MyCameraApp");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d("MyCameraApp", "failed to create directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		File mediaFile;
		if (type == FileColumns.MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} else if (type == FileColumns.MEDIA_TYPE_VIDEO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "VID_" + timeStamp + ".mp4");
		} else {
			return null;
		}

		return mediaFile;
	}
}

/** A basic Camera preview class */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;

	public CameraPreview(Context context, Camera camera) {
		super(context);
		mCamera = camera;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d("Fridge", "Error setting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.d("Fridge", "Error starting camera preview: " + e.getMessage());
		}
	}
}

class ProcessImage extends AsyncTask<byte[], Integer, int[][]> {

	private Context activity;
	private String imageFileName;

	public ProcessImage(Context context) {
		this.activity = context;
	}

	protected int[][] doInBackground(byte[]... params) {
		File pictureFile = CameraActivity
				.getOutputMediaFile(FileColumns.MEDIA_TYPE_IMAGE);
		this.imageFileName = pictureFile.getAbsolutePath();

		try {
			FileOutputStream fos = new FileOutputStream(pictureFile);
			fos.write(params[0]);
			fos.close();
		} catch (FileNotFoundException e) {
			Log.d("Fridge", "File not found: " + e.getMessage());
		} catch (IOException e) {
			Log.d("Fridge", "Error accessing file: " + e.getMessage());
		}

		// Turn bytes from camera into Bitmap
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 8;
		Bitmap bmp = BitmapFactory.decodeByteArray(params[0], 0,
				params[0].length, options);

		// Turn Bitmap into bytes
		int numBuckets = 6;
		int picw = bmp.getWidth();
		int pich = bmp.getHeight();
		int[] pix = new int[picw * pich];
		int[][] hist = new int[3][numBuckets];
		bmp.getPixels(pix, 0, picw, 0, 0, picw, pich);
		int R, G, B, RBucket, GBucket, BBucket;
		for (int y = 0; y < pich; y++) {
			for (int x = 0; x < picw; x++) {
				int index = y * picw + x;
				R = (pix[index] >> 16) & 0xff;
				RBucket = (int) (R / (255 / numBuckets));
				hist[0][RBucket]++;
				G = (pix[index] >> 8) & 0xff;
				GBucket = (int) (G / (255 / numBuckets));
				hist[1][GBucket]++;
				B = pix[index] & 0xff;
				BBucket = (int) (B / (255 / numBuckets));
				hist[2][BBucket]++;
			}
		}

		return hist;
	}

	protected void onProgressUpdate(Integer... progress) {
		return;
	}

	protected void onPostExecute(int[][] result) {
		// save int[][]
		for (int j = 0; j < 6; j++) {
			for (int i = 0; i < 3; i++) {
				String color;
				if (i == 0) {
					color = "red";
				} else if (i == 1) {
					color = "green";
				} else {
					color = "blue";
				}
				Log.d("Fridge",
						"Color: " + color + " j: " + Integer.toString(j) + " "
								+ Integer.toString(result[i][j]));
			}
		}
		String filename = activity.getString(R.string.local_items);
		String json = readFromFile(filename);
		JsonParser jsonParser = new JsonParser();
		JsonObject obj = (JsonObject) jsonParser.parse(json);
		Time t = new Time();
		t.setToNow();
		Item item = new Item("", result, this.imageFileName, t);
		InputStream is = null;
		try {
			is = activity.getAssets().open("valid.json");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuilder buf = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String str;
		try {
			while ((str = in.readLine()) != null) {
				buf.append(str);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String stock = buf.toString();
		String nameMatched = item.matchItem(stock);
		if (nameMatched != null && !"".equals(nameMatched)) {
			Toast.makeText(activity, "Matched item to " + nameMatched, 0)
					.show();
		}
		String newItem = new Gson().toJson(item);
		JsonObject i = (JsonObject) jsonParser.parse(newItem);
		obj.getAsJsonArray("items").add(i);
		Log.d("Fridge", "JSON: " + obj.toString());
		writeToFile(filename, obj.toString());
	}

	private String readFromFile(String FILENAME) {
		String ret = "";

		try {
			InputStream inputStream = activity.openFileInput(FILENAME);
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

	private void writeToFile(String filename, String data) {
		FileOutputStream fos = null;
		try {
			fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
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