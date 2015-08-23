package com.blogspot.defik.zxandroid;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class Spectrum extends Activity {


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		findViewById(R.id.spectrum);
		findViewById(R.id.text);
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
	}

	@Override
	protected void onPause() {
		super.onPause();
		// TODO:implement
		// spectrum.pause(); // pause game when Activity pauses
	}
}