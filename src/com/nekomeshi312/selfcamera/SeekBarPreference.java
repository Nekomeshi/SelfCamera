package com.nekomeshi312.selfcamera;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class SeekBarPreference extends DialogPreference
								implements SeekBar.OnSeekBarChangeListener{
	private float mRangeMin;
	private float mRangeMax;
	private float mRangeDefault;
    private SeekBar bar;
    private Context mContext;
    private SharedPreferences sp;
    private TextView mDispValTextView = null;
    private static final int LAYOUT_PADDING = 10;
    
    private float seekVal2RealVal(int seekVal){
    	return (seekVal/(float)bar.getMax()*(mRangeMax - mRangeMin) + mRangeMin);
    }
    private int realVal2SeekVal(float realVal){
    	return (int)((realVal- mRangeMin)/(mRangeMax - mRangeMin)*(float)bar.getMax() + 0.5);
    }
    public SeekBarPreference(Context context) {
        super(context, null);
        mContext = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void onProgressChanged(SeekBar seekBar, int progress,
            boolean fromUser) {
        // TODO Auto-generated method stub
    	if(null == mDispValTextView)return;
        DecimalFormat df=new DecimalFormat();
       	// パターンを設定
        df.applyPattern("0");
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);

    	float val = seekVal2RealVal(progress);
        Double mid=new Double(val);
        mDispValTextView.setText(df.format(mid));
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // TODO Auto-generated method stub
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout layout = new LinearLayout(mContext);
        layout.setPadding(LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING);
        layout.setOrientation(LinearLayout.VERTICAL);
        bar = new SeekBar(mContext);
        bar.setOnSeekBarChangeListener(this);
        float val = getValue();
        bar.setProgress(realVal2SeekVal(val));
        layout.addView(bar, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        
        DecimalFormat df=new DecimalFormat();
   	// パターンを設定
        df.applyPattern("0");
        df.setMaximumFractionDigits(0);
        df.setMinimumFractionDigits(0);

        //seek barの最大／最小／現在値を表示
        RelativeLayout l = new RelativeLayout(mContext);
        //最小値
        TextView str = new TextView(mContext);
        Double min=new Double(mRangeMin);
        str.setText(df.format(min));
        RelativeLayout.LayoutParams pmin = 
            new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            								RelativeLayout.LayoutParams.WRAP_CONTENT);
        pmin.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        str.setLayoutParams(pmin);
        l.addView(str);

        //現在値
        str = new TextView(mContext);
        Double mid=new Double(getValue());
        str.setText(df.format(mid));
        RelativeLayout.LayoutParams pmid = 
            new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            								RelativeLayout.LayoutParams.WRAP_CONTENT);
        pmid.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        str.setLayoutParams(pmid);
        l.addView(str);
        mDispValTextView = str;
        
        //最大値
        str = new TextView(mContext);
        Double max=new Double(mRangeMax);
        str.setText(df.format(max));
        RelativeLayout.LayoutParams pmax = 
            new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            								RelativeLayout.LayoutParams.WRAP_CONTENT);
        pmax.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        str.setLayoutParams(pmax);

        l.addView(str);
        layout.addView(l, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
        	float val = seekVal2RealVal(bar.getProgress());
            setValue(val);
        }
    }
	public void setRange(float min, float max, float def){
		mRangeMax = max;
		mRangeMin = min;
		mRangeDefault = def;
	}
	public float getRangeMax(){
		return mRangeMax;
	}
	public float getRangeMin(){
		return mRangeMin;
	}

    private void setValue(float value) {
        Editor ed = sp.edit();
        ed.putFloat(getKey(), value);
        ed.commit();
    }

    private float getValue() {
        return sp.getFloat(getKey(), mRangeDefault);
    }
}
