package com.example.rubicscube;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;




public class OpenGLES20 extends Activity {
	
	private MyGLSurfaceView myGLView;
	private MyGLRenderer mRenderer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.opengles20);
		
		
		myGLView = (MyGLSurfaceView)findViewById(R.id.gl_surface_view);
		
		
		myGLView.setEGLContextClientVersion(2);
		
		//get the size of the xml window
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		

		mRenderer = new MyGLRenderer(this, myGLView);
		myGLView.setRenderer(mRenderer, displayMetrics.density*9f);//increase the density of the touch so rubics rotates slower
		
		findViewById(R.id.button_texture).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleTexture();
			}
		});
		
		findViewById(R.id.button_decrease).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoomOut();
			}
		});
		findViewById(R.id.button_increase).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				zoomIn();
			}
		});
		findViewById(R.id.button_light).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleLight();
			}
		});

		

	}
	
	private void toggleTexture(){
		myGLView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.toggleTexture();
			}
		});
		
		
	}
	private void toggleLight(){//switches between a rotational light and an Identity
		myGLView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.toggleLight();
			}
		});
		
	}
	

	
	private void zoomIn(){
		myGLView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.zoomIn();
			}
		});
		
	}
	private void zoomOut(){
		myGLView.queueEvent(new Runnable() {
			@Override
			public void run() {
				mRenderer.zoomOut();
			}
		});
		
	}
	   @Override
	    protected void onPause() {
	        super.onPause();
	        // The following call pauses the rendering thread.
	        // If your OpenGL application is memory intensive,
	        // you should consider de-allocating objects that
	        // consume significant memory here.
	        myGLView.onPause();
	    }
	   @Override
	    protected void onResume() {
	        super.onResume();
	        // The following call resumes a paused rendering thread.
	        // If you de-allocated graphic objects for onPause()
	        // this is a good place to re-allocate them.
	        myGLView.onResume();
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.open_gles20, menu);
		return true;
	}

}




