package com.blogspot.defik.zxandroid;

import org.razvan.jzx.BaseSpectrum;
import org.razvan.jzx.Z80Loader;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SpectrumView extends SurfaceView implements SurfaceHolder.Callback {
	private Context context;

	public SpectrumView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setFocusable(true);
		bringToFront();
		setDrawingCacheEnabled(true);
		requestFocus();
		BaseSpectrum.setRomIS(context.getResources().openRawResource(
				R.raw.sinclair48));
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		this.context = context;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return BaseSpectrum.doKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return BaseSpectrum.doKeyUp(keyCode, event);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		int scale = 1;

		BaseSpectrum.setRomIS(context.getResources().openRawResource(
				R.raw.sinclair48));
		BaseSpectrum.setScale(scale);
		BaseSpectrum.init(holder, context.getResources().openRawResource(
				R.raw.sinclair48));
		BaseSpectrum.reset();
		Z80Loader load = new Z80Loader();
		try {
			load.load(context.getResources().openRawResource(R.raw.knightlore));
		} catch (Exception e) {
			Log.e(this.getClass().getName(), "Error loading file: "
					+ e.toString(), e);
		}
		BaseSpectrum.load(load);
		new Thread(new BaseSpectrum()).start();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	public void surfaceDestroyed(SurfaceHolder arg0) {
	}
}
