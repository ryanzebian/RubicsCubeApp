package com.example.rubicscube;


import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class MyGLSurfaceView extends GLSurfaceView{

	private MyGLRenderer mRenderer;

	// Offsets for touch events	 
	private float mPreviousX;
	private float mPreviousY;

	private float mDensity;



	public  MyGLSurfaceView(Context context){
		super(context);

	}

	public MyGLSurfaceView (Context context, AttributeSet attrs) 
	{
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) 
	{
		if (event != null)
		{			
			float x = event.getX();
			float y = event.getY();

			if (event.getAction() == MotionEvent.ACTION_MOVE)
			{
				if (mRenderer != null)
				{
					float deltaX = (x - mPreviousX) / mDensity / 2f;
					float deltaY = (y - mPreviousY) / mDensity / 2f;

					mRenderer.mDeltaX += deltaX;
					mRenderer.mDeltaY += deltaY;												
				}
			}	

			mPreviousX = x;
			mPreviousY = y;

			return true;
		}
		else
		{
			return super.onTouchEvent(event);
		}		
	}


	// Hides superclass method.
	public void setRenderer(MyGLRenderer renderer, float density) 
	{
		mRenderer = renderer;
		mDensity = density;
		super.setRenderer(renderer);
	}


}
