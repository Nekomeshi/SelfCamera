package com.nekomeshi312.selfcamera;

import java.util.ArrayList;

import com.nekomeshi312.selfcamera.jni.FaceDetect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Surface;

public class FaceMarkDouble  implements FaceMarkView.FaceAreaDraw{
	private static final String LOG_TAG = "FaceMarkDouble";
	private float mFacePosRX;
	private float mFacePosRY;
	private float mFacePosLX;
	private float mFacePosLY;
	private float mFaceRadius;
	private float mBodyRadius;
	
	private Path mLinePath = new Path();
	private Paint mLinePaint = new Paint();

	public static final int ROTATION_UNKNOWN = 0xffffffff;
	private  int mRotation = ROTATION_UNKNOWN;
	private Context mContext = null;
	private int mWidth = 0;
	private int mHeight = 0;
	@Override
	public void drawFaceArea(Canvas canvas) {
		// TODO Auto-generated method stub
		canvas.drawPath(mLinePath, mLinePaint);
	}

	@Override
	public String getFacePosScreenName() {
		// TODO Auto-generated method stub
		return mContext.getString(R.string.face_mark_double_name);
	}

	@Override
	public String getFacePosEntryValue() {
		// TODO Auto-generated method stub
		return mContext.getString(R.string.face_mark_double_entry_value);
	}

	
	/**
	 * 上下方向は、二人のうち上にある方が適正エリアの高さにあり、
	 * 左右方向は二人が対称位置になるように合わせる
	 */
	@Override
	public int isFacePosGood(ArrayList<Rect> facePos, ArrayList<Float> delta) {
		// TODO Auto-generated method stub
		if(facePos.size() == 0){
			return FaceMarkView.FACE_POS_NOT_DETECTED;
		}
		if(facePos.size() == 1){
			return FaceMarkView.FACE_POS_NOT_ENOUGH;
		}
		
		Rect rct0 = facePos.get(0);
		Rect rct1 = facePos.get(1);
		float rx = mFacePosRX;
		float ry = mFacePosRY;
		float lx = mFacePosLX;
		float ly = mFacePosLY;
		//画面の回転を戻してしゃべらせるために回転させる。
		if(FaceDetect.ROTATION_90 == mRotation){
			
		}
		else if(FaceDetect.ROTATION_270 == mRotation){
			rct0.top = -rct0.top;
			rct0.bottom = -rct0.bottom;
			rct0.left = -rct0.left;
			rct0.right = -rct0.right;
			rct1.top = -rct1.top;
			rct1.bottom = -rct1.bottom;
			rct1.left = -rct1.left;
			rct1.right = -rct1.right;
			rx = -rx;
			ry = -ry;
			lx = -lx;
			ly = -ly;
		}
		else if(FaceDetect.ROTATION_0 == mRotation){
			int tmp = rct0.left;
			rct0.left = -rct0.top;
			rct0.top = tmp;
			tmp = rct0.right;
			rct0.right = -rct0.bottom;
			rct0.bottom = tmp;

			tmp = rct1.left;
			rct1.left = -rct1.top;
			rct1.top = tmp;
			tmp = rct1.right;
			rct1.right = -rct1.bottom;
			rct1.bottom = tmp;
			
			float tmp2 = rx;
			rx = -ry;
			ry = tmp2;
			tmp2 = lx;
			lx = -ly;
			ly = tmp2;
		}
		else if(FaceDetect.ROTATION_180 == mRotation){

			int tmp = rct0.left;
			rct0.left = rct0.top;
			rct0.top = -tmp;
			tmp = rct0.right;
			rct0.right = rct0.bottom;
			rct0.bottom = -tmp;

			tmp = rct1.left;
			rct1.left = rct1.top;
			rct1.top = -tmp;
			tmp = rct1.right;
			rct1.right = rct1.bottom;
			rct1.bottom = -tmp;
			
			float tmp2 = rx;
			rx = ry;
			ry = -tmp2;
			tmp2 = lx;
			lx = ly;
			ly = -tmp2;
		}		

		if((rct0.top + rct0.bottom) > (rct1.top + rct1.bottom)){//rct0が必ず高い位置になるようにする。
			Rect r = rct0;
			rct0 = rct1;
			rct1 = r;
		}
		float dY;
		if(rct0.left+rct0.right > rct1.left+rct1.right){//rct0が右側の場合
			dY = (rct0.top + rct0.bottom)/2f - ry;
		}
		else{
			dY = (rct0.top + rct0.bottom)/2f - ly;			
		}
		
		float dX = (rct0.left+rct0.right + rct1.left+rct1.right)/4f - (rx + lx)/2f;
		
		

		
		delta.add(dX);
		delta.add(dY);
		return FaceMarkView.FACE_POS_DETECTED;
	}

	@Override
	public boolean setOrientation(int rot) {
		// TODO Auto-generated method stub
		
		//向きが変わらないときはなにもしない
		if(rot == mRotation)return true;

		mRotation = rot;
		if(FaceDetect.ROTATION_90 == mRotation){
			mFacePosRX = mWidth*2f/3f;
			mFacePosRY = mHeight*1f/3f;
			mFacePosLX = mWidth*1f/3f;
			mFacePosLY = mHeight*1f/3f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/6f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosRX, mFacePosRY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosRX - mBodyRadius,
											mFacePosRY + mFaceRadius,
											mFacePosRX + mBodyRadius,
											mHeight),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			mLinePath.addCircle(mFacePosLX, mFacePosLY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosLX - mBodyRadius,
											mFacePosLY + mFaceRadius,
											mFacePosLX + mBodyRadius,
											mHeight),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
		}
		else if(FaceDetect.ROTATION_270 == mRotation){
			mFacePosRX = mWidth*1f/3f;
			mFacePosRY = mHeight*2f/3f;
			mFacePosLX = mWidth*2f/3f;
			mFacePosLY = mHeight*2f/3f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/6f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosRX, mFacePosRY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosRX - mBodyRadius,
											0,
											mFacePosRX + mBodyRadius,
											mFacePosRY - mFaceRadius),
											mWidth / 10f,
											mHeight/10f, 
											Path.Direction.CCW);
			mLinePath.addCircle(mFacePosLX, mFacePosLY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosLX - mBodyRadius,
											0,
											mFacePosLX + mBodyRadius,
											mFacePosLY - mFaceRadius),
											mWidth / 10f,
											mHeight/10f, 
											Path.Direction.CCW);
			return true;
		}
		else if(FaceDetect.ROTATION_0 == mRotation){
			mFacePosRX = mWidth*1f/3f;
			mFacePosRY = mHeight*7f/24f;
			mFacePosLX = mWidth*1f/3f;
			mFacePosLY = mHeight*17f/24f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/6f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosRX, mFacePosRY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosRX + mFaceRadius,
											mFacePosRY - mBodyRadius ,
											mWidth,
											mFacePosRY + mBodyRadius ),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			mLinePath.addCircle(mFacePosLX, mFacePosLY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(mFacePosLX + mFaceRadius,
											mFacePosLY - mBodyRadius ,
											mWidth,
											mFacePosLY + mBodyRadius ),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			return true;
		}
		else if(FaceDetect.ROTATION_180 == mRotation){

			mFacePosRX = mWidth*2f/3f;
			mFacePosRY = mHeight*17f/24f;
			mFacePosLX = mWidth*2f/3f;
			mFacePosLY = mHeight*7f/24f;
			mFaceRadius = mWidth/8f;
			mBodyRadius = mWidth/6f;
			mLinePaint.setStyle(Paint.Style.STROKE);
			mLinePaint.setColor(FRAME_COLOR);
			mLinePaint.setStrokeWidth(mWidth/64f);
			mLinePath.reset();
			mLinePath.addCircle(mFacePosRX, mFacePosRY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(0,
											mFacePosRY - mBodyRadius ,
											mFacePosRX - mFaceRadius ,
											mFacePosRY + mBodyRadius ),
									mWidth / 10f,
									mHeight/10f, 
									Path.Direction.CCW);
			mLinePath.addCircle(mFacePosLX, mFacePosLY, mFaceRadius, Path.Direction.CW);
			mLinePath.addRoundRect(new RectF(0,
											mFacePosLY - mBodyRadius ,
											mFacePosLX - mFaceRadius ,
											mFacePosLY + mBodyRadius ),
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
		return 2;
	}


}
