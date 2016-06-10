package com.example.rubicscube;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.opengl.*;
import android.os.SystemClock;


public class MyGLRenderer  implements GLSurfaceView.Renderer{
	//debuging reasons 
	private final Context mActivityContext;
	boolean texture = true; //used for touch event to vary between drawing texture and not .. 
	private float mscaleSize = 0.5f;
	private boolean fixedLight = true; //use to toggle between a moving light and not. (False means the light is fixed on identity matrix).
	private float mZoom = -7.0f;
	private float[][] switchColors;

	public float mAngle = 0;
	private float[] mModelMatrix = new float[16];
	private float[] mViewMatrix = new float[16];
	private float[] mProjectionMatrix = new float[16];
	private float[] mMVPMatrix = new float[16];
	private float[] mLightModelMatrix = new float[16];	

	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private  FloatBuffer mCubeColors;
	private final FloatBuffer mCubeNormals;
	private final FloatBuffer mCubeTextureCoordinates;

	/*Handles for the shader code*/

	private int mMVPMatrixHandle;
	private int mMVMatrixHandle;
	private int mLightPosHandle;
	private int mPositionHandle;
	private int mTextureUniformHandle;
	private int mColorHandle;
	private int mNormalHandle;

	private int mTextureCoordinateHandle;
	private final int mTextureCoordinateDataSize = 2;



	private final int mBytesPerFloat = 4;	
	private final int mPositionDataSize = 3;	


	private final int mColorDataSize = 4;	

	private final int mNormalDataSize = 3;


	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	private final float[] mLightPosInWorldSpace = new float[4];

	/** hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];

	//handle drawing our cube
	private int mProgramHandle;
	//handle to our light point program
	private int mPointProgramHandle;	

	private int mTextureDataHandle;


	//rotation of cube
	public volatile float mDeltaX;
	public volatile float mDeltaY;

	/** Store the accumulated rotation. */
	private final float[] mAccumulatedRotation = new float[16];

	/** Store the current rotation. */
	private final float[] mCurrentRotation = new float[16];

	/** A temporary matrix. */
	private float[] mTemporaryMatrix = new float[16];

	/** Additional info for cube generation. */



	public MyGLRenderer(final Context activityContext, MyGLSurfaceView myGLView){

		mActivityContext = activityContext;

		final float[] cubePositionData =
			{
				// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
				// if the points are counter-clockwise we are looking at the "front". If not we are looking at
				// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
				// usually represent the backside of an object and aren't visible anyways.

				// Front face
				-1.0f, 1.0f, 1.0f,				
				-1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, 1.0f, 				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, 1.0f,

				// Right face
				1.0f, 1.0f, 1.0f,				
				1.0f, -1.0f, 1.0f,
				1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, 1.0f,				
				1.0f, -1.0f, -1.0f,
				1.0f, 1.0f, -1.0f,

				// Back face
				1.0f, 1.0f, -1.0f,				
				1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,
				1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, -1.0f,

				// Left face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, -1.0f, -1.0f,
				-1.0f, 1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,				
				-1.0f, -1.0f, 1.0f, 
				-1.0f, 1.0f, 1.0f, 

				// Top face
				-1.0f, 1.0f, -1.0f,				
				-1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f, 
				-1.0f, 1.0f, 1.0f, 				
				1.0f, 1.0f, 1.0f, 
				1.0f, 1.0f, -1.0f,

				// Bottom face
				1.0f, -1.0f, -1.0f,				
				1.0f, -1.0f, 1.0f, 
				-1.0f, -1.0f, -1.0f,
				1.0f, -1.0f, 1.0f, 				
				-1.0f, -1.0f, 1.0f,
				-1.0f, -1.0f, -1.0f,
			};	

		// R, G, B, A
		switchColors =  initColors();

		// X, Y, Z
		// The normal is used in light calculations and is a vector which points
		// orthogonal to the plane of the surface. For a cube model, the normals
		// should be orthogonal to the points of each face.
		final float[] cubeNormalData =
			{												
				// Front face
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,				
				0.0f, 0.0f, 1.0f,
				0.0f, 0.0f, 1.0f,

				// Right face 
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,				
				1.0f, 0.0f, 0.0f,
				1.0f, 0.0f, 0.0f,

				// Back face 
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,				
				0.0f, 0.0f, -1.0f,
				0.0f, 0.0f, -1.0f,

				// Left face 
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,				
				-1.0f, 0.0f, 0.0f,
				-1.0f, 0.0f, 0.0f,

				// Top face 
				0.0f, 1.0f, 0.0f,			
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,				
				0.0f, 1.0f, 0.0f,
				0.0f, 1.0f, 0.0f,

				// Bottom face 
				0.0f, -1.0f, 0.0f,			
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f,				
				0.0f, -1.0f, 0.0f,
				0.0f, -1.0f, 0.0f
			};
		// S, T Texture coordinate data.
		// OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
		// Same Texture coordinate for every face..
		final float[] cubeTextureCoordinateData =
			{												
				// Front face
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,				

				// Right face 
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,	

				// Back face 
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,	

				// Left face 
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,	

				// Top face 
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f,	

				// Bottom face 
				0.0f, 0.0f, 				
				0.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f,
				1.0f, 0.0f
			};
		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(cubePositionData).position(0);		

		mCubeColors = ByteBuffer.allocateDirect(switchColors[1].length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeColors.put(switchColors[1]).position(0);

		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeNormals.put(cubeNormalData).position(0);

		mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);


	}
	//initializes the colors
	private float[][] initColors() {
		float[][] temp = {	
				 {
					 
			     //First Square
					 
			     // Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				 },
				{

				//Second Square
					 
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},
				{
					
				//Third Square
					
				// Front face 	
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},
				{
					
				//Fourth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Fifth Square
					
				// Front face 	
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Sixth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Seventh Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Eighth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
				
				//Ninth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,1.0f,1.0f,0.0f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Tenth Square
					
				// Front face 	
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},
				{
					
				//Eleventh Square
					
				// Front face 		
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f ,
				},
				{
					
				//Twelfth Square
					
				// Front face 
					
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f ,
				// Back face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Left face
				 0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},
				{
					
				//Thirteenth Square
					
				// Front face 
					
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Fourteenth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				},
				{
					
				//Fifteenth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Left face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				},
				{
					
				//Sixteenth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Back face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				// Left face
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f ,
				// Top face
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f ,
				// Bottom face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f ,
				},{
					
				//Seventeenth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Eighteenth Square
					
				// Front face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Nineteenth Square
					
				// Front face
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f, 
				// Right face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f, 
				// Back face
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f, 
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f, 
				},
				{

				//Twentieth Square
				
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},{
					
				//Twenty first Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,
				},{
					
				//Twenty Second Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},
				{
					
				//Twenty Third Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},{
					
				//Twenty Fourth Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},{
					
				//Twenty Fifth Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,0.0f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},{
					
				//Twenty Sixth Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face 
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				},{
				//Twenty Seventh Square
					
				// Front face 
				1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,
				// Right face 
				1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,1.0f,0.5f,0.0f,1.0f,
				// Back face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Left face 
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				// Top face
				0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,0.0f,0.0f,1.0f,1.0f,
				// Bottom face  
				0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,0.5f,0.5f,0.5f,1.0f,
				}};
		return temp;
	}

	protected String getShader(int shaderType) 
	{
		return com.example.helper.RawResourceReader.readTextFileFromRawResource(mActivityContext, shaderType);
	}


	@Override
	public void onDrawFrame(GL10 gl) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);			        



		//per fragment lighting and texture  program
		GLES20.glUseProgram(mProgramHandle);

		// Set program handles for cube drawing.
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
		mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix"); 
		mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");

		if(texture){
			mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
		}

		mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
		mColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
		mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");

		if(texture){
			mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

			// Set the active texture unit to texture unit 0.
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

			// Bind the texture to this unit.
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);

			// Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
			GLES20.glUniform1i(mTextureUniformHandle, 0);  
		}



		// Do a complete rotation every 10 seconds.
		long time = SystemClock.uptimeMillis() % 10000L;        
		float angleInDegrees = (360.0f / 10000.0f) * ((int) time);
		
		// Calculate position of the light. Rotate and then push into the distance.
		Matrix.setIdentityM(mLightModelMatrix, 0);
		if(fixedLight){
			Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, mZoom);      
			Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
			Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 3.0f);
		}
		Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
		Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);  

		int colorOffset = 0;
		
		//initializes the cubes 
		for (int z = -1; z < 2;z++){
			for(int y = -1; y < 2; y++){
				for(int x = -1; x < 2; x++){
					//the cube 
					Matrix.setIdentityM(mModelMatrix, 0);
					Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, mZoom);


					// Set a matrix that contains the current rotation.
					Matrix.setIdentityM(mCurrentRotation, 0);        
					Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f);
					Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f);


					// Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
					Matrix.multiplyMM(mTemporaryMatrix, 0, mCurrentRotation, 0, mAccumulatedRotation, 0);
					System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16);

					// Rotate the cube taking the overall rotation into account.     	
					Matrix.multiplyMM(mTemporaryMatrix, 0, mModelMatrix, 0, mAccumulatedRotation, 0);
					System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16);

					Matrix.translateM(mModelMatrix, 0, x*1.3f, y*1.3f, z*1.3f);

					//Scale the Matrix
					Matrix.scaleM(mModelMatrix, 0, mscaleSize, mscaleSize, mscaleSize);
					mCubeColors = ByteBuffer.allocateDirect(switchColors[colorOffset].length * mBytesPerFloat)
							.order(ByteOrder.nativeOrder()).asFloatBuffer();							
					mCubeColors.put(switchColors[colorOffset]).position(0);
				
					drawCube(); 
					colorOffset++;
				}
			}
			
		}
	
		mDeltaX = 0.0f;
		mDeltaY = 0.0f;



		// Draw a point to indicate the light.
		GLES20.glUseProgram(mPointProgramHandle);        
		if(fixedLight)
			drawLight();        
		Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
		Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0); 
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {

		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);
		final float ratio = (float) width / height;
		Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 10.0f);	//uses perspective	
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set the background clear color to black.
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Use culling to remove back faces.
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);



		Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f, -7.0f, 0.0f, 1.0f, 0.0f);		

		final String vertexShader = getShader(R.raw.per_pixel_vertex_shader_tex_and_light);   		
		final String fragmentShader = getShader(R.raw.per_pixel_fragment_shader_tex_and_light);			

		final int vertexShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
		final int fragmentShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		

		mProgramHandle = com.example.helper.ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
				new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});								                                							       

		// Define a simple shader program for our point light source.
		final String pointVertexShader = getShader(R.raw.point_vertex_shader);        	       
		final String pointFragmentShader = getShader(R.raw.point_fragment_shader);


		final int pointVertexShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
		final int pointFragmentShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);

		mPointProgramHandle = com.example.helper.ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle, 
				new String[] {"a_Position"}); 
		// Load the texture
		mTextureDataHandle = com.example.helper.TextureHelper.loadTexture(mActivityContext, R.drawable.rough_wood);
		// Initialize the accumulated rotation matrix
		Matrix.setIdentityM(mAccumulatedRotation, 0);

	}
	private void drawCube()
	{		
		// Pass in the position information
		mCubePositions.position(0);		
		GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
				0, mCubePositions);        

		GLES20.glEnableVertexAttribArray(mPositionHandle);        

		// Pass in the color information
		mCubeColors.position(0);
		GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
				0, mCubeColors);        

		GLES20.glEnableVertexAttribArray(mColorHandle);

		// Pass in the normal information
		mCubeNormals.position(0);
		GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
				0, mCubeNormals);

		GLES20.glEnableVertexAttribArray(mNormalHandle);

		if(texture){
			// Pass in the texture coordinate information
			mCubeTextureCoordinates.position(0);
			GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
					0, mCubeTextureCoordinates);

			GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
		}
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
		// (which currently contains model * view).
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   

		// Pass in the modelview matrix.
		GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                

		// This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
		// (which now contains model * view * projection).CHANGED***
		Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

		// Pass in the combined matrix.
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Pass in the light position in eye space.        
		GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);

		// Draw the cube.
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);                               
	}	

	/**
	 * Draws a point representing the position of the light.
	 */
	private void drawLight()
	{
		final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
		final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

		// Pass in the position.
		GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

		// Since we are not using a buffer object, disable vertex arrays for this attribute.
		GLES20.glDisableVertexAttribArray(pointPositionHandle);  

		// Pass in the transformation matrix.
		Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
		Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
		GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

		// Draw the point.
		GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
	}
	public void zoomIn(){
		mZoom += 0.5f;
		Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f, mZoom, 0.0f, 1.0f, 0.0f);	
		
	}
	
	public void zoomOut(){
		mZoom = mZoom - 0.5f;
		Matrix.setLookAtM(mViewMatrix, 0, 0.0f, 0.0f, -0.5f, 0.0f, 0.0f, mZoom, 0.0f, 1.0f, 0.0f);	
	}
	public void toggleLight(){
		fixedLight = !fixedLight;
	}
	public void switchLeft(){
		
	}
	public void switchRight(){
		
	}

	public void toggleTexture()
	{		
		//*changing the shader and updating the program for enabling texture and disabling .. 
		texture = !texture;

		if(texture){
			final String vertexShader = getShader(R.raw.per_pixel_vertex_shader);   //change this variable when playing with light..		
			final String fragmentShader = getShader(R.raw.per_pixel_fragment_shader);			

			final int vertexShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
			final int fragmentShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		

			mProgramHandle = com.example.helper.ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
					new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});
		}else if(!texture) {
			final String vertexShader = getShader(R.raw.per_pixel_vertex_shader_no_tex);   		
			final String fragmentShader = getShader(R.raw.per_pixel_fragment_shader_no_tex);			

			final int vertexShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);		
			final int fragmentShaderHandle = com.example.helper.ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);		

			mProgramHandle = com.example.helper.ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, 
					new String[] {"a_Position",  "a_Color", "a_Normal"});

		}





	}




}
