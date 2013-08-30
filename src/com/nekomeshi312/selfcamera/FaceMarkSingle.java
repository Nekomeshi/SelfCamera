package com.nekomeshi312.selfcamera;

import java.util.ArrayList;

import com.nekomeshi312.selfcamera.jni.FaceDetect;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Surface;

public class FaceMarkSingle implements FaceMarkView.FaceAreaDraw{
	private static final String LOG_TAG = "FaceMarkSingle";
	public static final int ROTATION_UNKNOWN = 0xffffffff;
	private  int mRotation = ROTATION_UNKNOWN;
	private Context mContext = null;
	private int mWidth = 0;
	private int mHeight = 0;
	
	private float 	mFacePosX = 0f;
	private float	mFacePosY = 0f;
	private float	mFaceRadius = 0f;
	private float 	mBodyRadius = 0f;
	
	private Path mLinePath = new Path();
	private Paint mLinePaint = new Paint();
	
	@Override
	public void drawFaceArea(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawPath(mLinePath, mLinePaint);
	}


	@Override
	public String getFacePosScreenName() {
		// TODO Auto-generated method stub
		return mContext.getString(R.string.face_mark_single_name);
	}
	@Override
	public String getFacePosEntryValue() {
		// TODO Auto-generated method stub
		return mContext.getString(R.string.face_mark_single_entry_value);
	}
	@Override
	public int isFacePosGood(ArrayList<Rect> facePos, ArrayList<Float> delta) {
		// TODO Auto-generated method stub
		if(facePos.size() == 0){
			return FaceMarkView.FACE_POS_NOT_DETECTED;
		}
		Rect rct = facePos.get(0);
		float dX = (rct.left + rct.right)/2f - mFacePosX;
		float dY = (rct.top + rct.bottom)/2f - mFacePosY;


		//画面の回転を戻してしゃべらせるために回転させる。
		if(FaceDetect.ROTATION_90 == mRotation){
		}
		else if(FaceDetect.ROTATION_270 == mRotation){
			dX = -dX;
			dY = -dY;			
		}
		else if(FaceDetect.ROTATION_0 == mRotation){
			float tmp = dX;
			dX = -dY;
			dY = tmp;
		}
		else if(FaceDetect.ROTATION_180 == mRotation){
			float tmp = dX;
			dX = dY;
			dY = -tmp;
		}
			
		delta.add(dX);
		delta.add(dY);
		return FaceMarkView.FACE_POS_DETECTED;
	}

	@Override
	public boolean setOrientation(int rot) {
		// TODO Auto-generated method stub
		if(rot == mRotation)return true;//向きが変わらないときはなにもしない
		mRotation = rot;
		
		if(FaceDetect.ROTATION_90 == mRotation){
			mFacePosX = mWidth/2f;
			mFacePosY = mHeight/3f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/4f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosX, mFacePosY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosX - mBodyRadius,
											mFacePosY + mFaceRadius,
											mFacePosX + mBodyRadius,
											mHeight),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
		}
		else if(FaceDetect.ROTATION_270 == mRotation){
			mFacePosX = mWidth/2f;
			mFacePosY = mHeight*2f/3f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/4f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosX, mFacePosY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosX - mBodyRadius,
											0,
											mFacePosX + mBodyRadius,
											mFacePosY - mFaceRadius),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
		}
		else if(FaceDetect.ROTATION_0 == mRotation){
			mFacePosX = mWidth/3f;
			mFacePosY = mHeight/2f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/4f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosX, mFacePosY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosX + mFaceRadius,
											mFacePosY - mBodyRadius ,
											mWidth,
											mFacePosY + mBodyRadius ),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
			
		}
		else if(FaceDetect.ROTATION_180 == mRotation){
			mFacePosX = mWidth*2f/3f;
			mFacePosY = mHeight/2f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/4f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosX, mFacePosY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(0,
											mFacePosY - mBodyRadius ,
											mFacePosX - mFaceRadius ,
											mFacePosY + mBodyRadius ),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
		}
		return false;
	}

	@Override
	public void init(Context context) {
		// TODO Auto-generated method stub
		mContext = context;
	}

	@Override
	public void setScreenSize(int width, int height, int rotation) {
		// TODO Auto-generated method stub
		mWidth = width;
		mHeight = height;
		setOrientation(rotation);
	}


	@Override
	public int getFaceNum() {
		// TODO Auto-generated method stub
		return 1;
	}
}
