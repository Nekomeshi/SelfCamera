package com.nekomeshi312.selfcamera;

import android.content.Context;
import android.util.AttributeSet;

public class VerticalSeekBar2 extends VerticalSeekBar {
	private float mRangeMax = getMax();
	private float mRangeMin = 0f;
	
	public VerticalSeekBar2(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	public VerticalSeekBar2(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.seekBarStyle);
		// TODO Auto-generated constructor stub
	}
	public VerticalSeekBar2(Context context) {
        this(context, null);
		// TODO Auto-generated constructor stub
	}
	public void setRange(float min, float max){
		mRangeMax = max;
		mRangeMin = min;
	}
	public float getRangeMax(){
		return mRangeMax;
	}
	public float getRangeMin(){
		return mRangeMin;
	}

	public synchronized void shiftProgressF(float val){
		float currentPos = getProgressF();
		currentPos += val;
		setProgressF(currentPos);
	}
	public synchronized void setProgressF(float val){
		if(val < mRangeMin){
			val = mRangeMin;
		}
		if(val > mRangeMax){
			val = mRangeMax;
		}
		setProgress((int)((val- mRangeMin)/(mRangeMax - mRangeMin)*(float)getMax() + 0.5));
	}

	public float getProgressF(){
		return (float)(super.getProgress()/(float)getMax()*(mRangeMax - mRangeMin) + mRangeMin);
	}

}
