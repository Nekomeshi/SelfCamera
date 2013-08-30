package com.nekomeshi312.selfcamera;

import java.io.File;


import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;


import com.nekomeshi312.selfcamera.jni.FaceDetect;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;


public class SelfCameraActivity extends Activity
								implements SensorEventListener{

	private static final String LOG_TAG = "SelfCameraActivity";
	public static CameraSetting mSettingInfo = null;
	
	private SelfPreview mPreview = null;
	private FaceMarkView mFaceMarkView = null;
	private boolean mSettingActivityOpen = false;
	private static final int ACTIVIY_REQUEST_FINISH_SETTING = 0;
	private static final int ACTIVIY_REQUEST_FINISH_SELF_CAMERA_SETTING = 1;
	private static final int ACTIVIY_REQUEST_FINISH_PIC_VIEWER = 2;
	private static final int ACTIVIY_REQUEST_FINISH_GALLERY_PIC_SELECT = 3;	

	private static final float ROTATION_CHATA_SIZE = (float)(10.0/180.0*Math.PI);//単位はrad
	private int mOrientation = FaceDetect.ROTATION_UNKNOWN;
	private SensorManager mSensorManager = null;

	private SeekBar2 mZoomBar = null;
	private Button mZoomDownButton = null;
	private Button mZoomUpButton = null;

	private VerticalSeekBar2 mExposureBar = null;
	private Button mExposureDownButton = null;
	private Button mExposureUpButton = null;

	private Button mResolutionButton = null;
	private Button mFlashModeButton = null;
	private Button mWhiteBalanceButton = null;
	private Button mAngleButton = null;

    private PowerManager.WakeLock mWakeLock = null;
	private int mSensorOrientation;
    
    private static Field  mCameraOrientation = null;
    private static Class<?> clazzCameraInfo = null;
    private static Method mGetCameraInfo = null;
	private ProgressDialog mProgressDialog = null;
	
	//リフレクション
    static {  
        initCompatibility();  
    };  
    private static void initCompatibility() {  
        try{//Camera.CameraInfo, getCameraInfo, orientationのリフレクション API Level 9から
           	clazzCameraInfo = Class.forName("android.hardware.Camera$CameraInfo");
        	mCameraOrientation = clazzCameraInfo.getDeclaredField("orientation");
        	mGetCameraInfo = Camera.class
        			.getMethod("getCameraInfo", new Class[]{
        											int.class,
        											clazzCameraInfo});        	 
        }
        catch(Exception e){
        	mCameraOrientation = null;
        	mGetCameraInfo = null;
        	clazzCameraInfo = null;
        }

    }
	
    private void dismissProgresDialog(){
		if (null != mProgressDialog && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
    	
    }
	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		class FileDeleteRunnable implements Runnable{
			File file = null;
			private void setFile(String fn){
				file = new File(fn);
			}
			@Override
			public void run() {
				// TODO Auto-generated method stub
				file.delete();
				dismissProgresDialog();
			}
		}

		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onActivityResult requestCode = " + requestCode +
										" resultCode = " + resultCode);

		
		switch(requestCode){
		 	case ACTIVIY_REQUEST_FINISH_SETTING://CameraSettingActivityから戻った場合
		 		Log.i(LOG_TAG, "ACTIVIY_REQUEST_FINISH_SETTING");
		 		mSettingActivityOpen = false;
		 		break;
		 	case ACTIVIY_REQUEST_FINISH_SELF_CAMERA_SETTING:
		 		break;
		 	case ACTIVIY_REQUEST_FINISH_PIC_VIEWER:
	 			SharedPreferences pref = getSharedPreferences(getString(R.string.pic_pref), MODE_PRIVATE);
	 			String fn = pref.getString(getString(R.string.pic_pref_filename), null);
	 			if(MyDebug.DEBUG)Log.i(LOG_TAG, "saved fn = " + fn);
	 			if(null == fn){
	 				break;
	 			}
	 			else{
	 				if(RESULT_OK == resultCode){//OKの場合は写真をandroidギャラリーへ登録
	 					try{
	 						//フルパスからファイル名のみ取得
	 					    String fnm = "";
	 					    String w1  = "";
	 					    String w2  = "/";    //区切り文字
	 					    String w3  = ".";    //拡張子判定
	 					    int    pw3 = 0;      //拡張子の位置
	 					    for(int i=1; i<=fn.length(); i++){
	 					    	w1 = fn.substring(fn.length()-i,fn.length()-i+1);
	 					    	if(w1.equals(w3)){
	 					    		pw3 = i;
	 					    	}
	 					    	else if(w1.equals(w2)){
	 					    		fnm = fn.substring(fn.length()-i+1,fn.length()-pw3);
	 					    		break;
	 					    	}
	 					    }
	 						
		 					long	nDate;
		 					ContentValues values = new ContentValues();
		 					nDate = System.currentTimeMillis();
		 					values.put(Images.Media.MIME_TYPE,"image/jpeg");			//必須
		 					values.put(Images.Media.DATA,fn);						//必須：ファイルパス（uriからストリーム作るなら不要）
		 					values.put(Images.Media.SIZE,new File(fn).length()); 	//必須：ファイルサイズ（同上）
		 					values.put(Images.Media.TITLE,fnm);
		 					values.put(Images.Media.DISPLAY_NAME,fnm);
		 					values.put(Images.Media.DATE_ADDED,nDate);
		 					values.put(Images.Media.DATE_TAKEN,nDate);
		 					values.put(Images.Media.DATE_MODIFIED,nDate);
//		 					values.put(Images.Media.DESCRIPTION,"");
//		 					values.put(Images.Media.LATITUDE,0.0);
//		 					values.put(Images.Media.LONGITUDE,0.0);
//		 					values.put(Images.Media.ORIENTATION,"");
		 					
		 					ContentResolver	contentResolver = getContentResolver();
		 					contentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
		 					SDCardAccess.pictureNumberCountUp(this);	 						
	 					}
	 					catch(Exception e){
	 						e.printStackTrace();
	 						Toast.makeText(this, R.string.pic_gallery_set_error, Toast.LENGTH_SHORT).show();
	 					}

	 				}
	 				else{//撮影がキャンセルされたので保存してあった画像は削除する
	 					//ファイルを削除するところでANRがでる？？？？
	 					//信じられないけど一応別スレッドにする
	 					mProgressDialog = new ProgressDialog(this);
	 					mProgressDialog.setTitle(R.string.progress_title_wait);
	 					mProgressDialog.setMessage("");
	 					mProgressDialog.setIndeterminate(false);
	 					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	 					mProgressDialog.show();
	 					FileDeleteRunnable r = new FileDeleteRunnable();
	 					r.setFile(fn);
	 					new Thread(r).start();
	 				}
	 			}
		 		break;
		 	case ACTIVIY_REQUEST_FINISH_GALLERY_PIC_SELECT:
 				if(RESULT_OK == resultCode){
 					if(null != data){//選択された画像をギャラリーで全画面表示
 						Log.i(LOG_TAG, "URI = " + data.getDataString());
 						Intent intent = new Intent(Intent.ACTION_VIEW); 
 						try{
 							intent.setData(data.getData()); 
 							startActivity(intent); 
 						}
 						catch(Exception e){//errorがthrowされたときは次のonResumeで表示するエラーメッセージをセットする
 							Log.w(LOG_TAG, e.getMessage());
 							e.printStackTrace();
 							Toast.makeText(this, getString(R.string.pic_gallery_img_open_error), Toast.LENGTH_SHORT).show();
 						}
 					}
 				}
 				else{
 					mPreview.invalidate();
 				}
		 		break;
		 	default:
		 		if(RESULT_OK == resultCode){
		 		}
		 		break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
	/**
	 * preview Viewを撮影する解像度のアスペクト比に合わせてリサイズする
	 */
	public void resizePreview(){
		if(null == mPreview){
			Log.w(LOG_TAG, "mPreview == NULL");
			return;
		}
		if(null == mSettingInfo){
			Log.w(LOG_TAG, "mSettingInfo == NULL");
			return;
		}
		if(null == mSettingInfo.mPictureSize){
			Log.w(LOG_TAG, "mPirctureSize == NULL");
			return;
		}
		WindowManager windowmanager = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = windowmanager.getDefaultDisplay();
		int width = disp.getWidth();
		int height = disp.getHeight();
		double aspect = (double)width/(double)height;
		Camera.Size size = mSettingInfo.mPictureSize.getValue();
		double aspectPic = (double)size.width/(double)size.height;
		int newWidth;
		int newHeight;
		int leftMargin;
		int topMargin;
		if(aspect > aspectPic){//画面のほうが横長
			newWidth = (int)((double)height*aspectPic + 0.5);
			newHeight = height;
			leftMargin = (width - newWidth)/2;
			topMargin = 0;
		}
		else{//撮影フォーマットのほうが横長
			newWidth = width;
			newHeight = (int)((double)width/aspectPic + 0.5);
			leftMargin = 0;
			topMargin = (height - newHeight) /2;
		}
		
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "Org View Width = " + width +
										" Height = " + height + 
										"Pic Width = " + size.width +
										" Height = " + size.height);
		if(MyDebug.DEBUG)Log.i(LOG_TAG, 	"preview Width = " + newWidth + 
										" Hight = " + newHeight +
										" Left = " + leftMargin +
										" Top = " + topMargin);
		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(newWidth, newHeight, leftMargin, topMargin);
		mPreview.setLayoutParams(lp);
		mFaceMarkView.setLayoutParams(lp);
		mPreview.invalidate();
	}
	private void setExposurecompensationBar(){
		if(null == mPreview) return;
		if(null == mSettingInfo) return;
		if(null == mSettingInfo.mExposurecompensation) return;
		LinearLayout exposureLayout = (LinearLayout)findViewById(R.id.ExposurecompensationBarLayout);
		if(!mSettingInfo.mExposurecompensation.isSupported()){
			exposureLayout.setVisibility(View.INVISIBLE);
		}
		else{
			exposureLayout.setVisibility(View.VISIBLE);
			//露出補正エリアを触ったときに背景を触ったようにシャッタボタンが押されないようにする
			exposureLayout.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					return true;
				}
			});
			int min = mSettingInfo.mExposurecompensation.getMinValue();
			int max = mSettingInfo.mExposurecompensation.getMaxValue();
			int cur = mSettingInfo.mExposurecompensation.getValue();
	    	mExposureBar.setRange(min, max);
	    	mExposureBar.setProgressF(cur);
			if(MyDebug.DEBUG)Log.i(LOG_TAG, "ExposureRange min = " + min + " max = " + max + " current = " + cur);
			mExposureBar.setOnSeekBarChangeListener(new VerticalSeekBar2.OnSeekBarChangeListener() {
				

				@Override
				public void onProgressChanged(VerticalSeekBar seekBar,
						int progress, boolean fromUser) {
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mExposurecompensation) return;
					VerticalSeekBar2 sk = (VerticalSeekBar2)seekBar;
					mSettingInfo.mExposurecompensation.setValue((int)(sk.getProgressF()+0.5f), true);//露出補正バーでの位置をカメラにセットする
					if(MyDebug.DEBUG) Log.i(LOG_TAG, "ExposureCompensation = " + (int)(sk.getProgressF()+0.5f));
				}

				@Override
				public void onStartTrackingTouch(VerticalSeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onStopTrackingTouch(VerticalSeekBar seekBar) {
					// TODO Auto-generated method stub
				}
			});
			mExposureDownButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mExposurecompensation) return;
					int cur =  mSettingInfo.mExposurecompensation.getValue();//現在のズームの設定をpreferenceから取得
					Log.i(LOG_TAG, "zoomDownButton " + cur);
					cur--;//一段下げる
					mSettingInfo.mExposurecompensation.setValue(cur, true);//新しいズームの値を設定する。range外の処理はmSettingInfoの中で最小値になるように修正している		
					mExposureBar.setProgressF(cur);//新しいズームの値をズームバーに設定する。 range外の処理はズームバーの表示でやっている
				}
			});
			mExposureUpButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mExposurecompensation) return;
					int cur =  mSettingInfo.mExposurecompensation.getValue();
					cur++;
					mSettingInfo.mExposurecompensation.setValue(cur, true);
					mExposureBar.setProgressF(cur);
				}
			});
		}
	}
	/**
	 * 画面のズームバーとボタンの表示設定
	 */
	private void setZoomBar(){
		if(null == mPreview) return;
		if(null == mSettingInfo) return;
		if(null == mSettingInfo.mZoom) return;
		LinearLayout zoomLayout = (LinearLayout)findViewById(R.id.ZoomBarLayout);
		if(!mSettingInfo.mZoom.isSupported()){//zoomがサポートされていないときは非表示にする
			zoomLayout.setVisibility(View.INVISIBLE);
		}
		else{
			zoomLayout.setVisibility(View.VISIBLE);
			//ズームエリアを触ったときに背景を触ったようにシャッタボタンが押されないようにする
			zoomLayout.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					return true;
				}
			});
			int min =  mSettingInfo.mZoom.getMinValue();
			int max =  mSettingInfo.mZoom.getMaxValue();
			int cur =  mSettingInfo.mZoom.getValue();
			mZoomBar.setRange(min, max);
			mZoomBar.setProgressF(cur);
			if(MyDebug.DEBUG)Log.i(LOG_TAG, "ZoomRange min = " + min + " max = " + max + " current = " + cur);
			mZoomBar.setOnSeekBarChangeListener(new SeekBar2.OnSeekBarChangeListener() {//ズームバーが操作されたとき
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {//ズームバーが操作されたとき
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mZoom) return;

					SeekBar2 sk = (SeekBar2)seekBar;
					mSettingInfo.mZoom.setValue((int)(sk.getProgressF()+0.5f), true);//ズームバーでの位置をカメラにセットする
					if(MyDebug.DEBUG){
						Camera.Size current = mSettingInfo.mPictureSize.getValue();
						Log.i(LOG_TAG, "zoom = " + (int)(sk.getProgressF()+0.5f) +
									"PicSize = " + current.width + ":" + current.height);
					}
				}
			});
			mZoomDownButton.setOnClickListener(new View.OnClickListener() {//ズームボタン（Wide側)が押されたとき
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mZoom) return;

					int cur =  mSettingInfo.mZoom.getValue();//現在のズームの設定をpreferenceから取得
					Log.i(LOG_TAG, "zoomDownButton " + cur);
					cur--;//一段下げる
					mSettingInfo.mZoom.setValue(cur, true);//新しいズームの値を設定する。range外の処理はmSettingInfoの中で最小値になるように修正している
					mZoomBar.setProgressF(cur);//新しいズームの値をズームバーに設定する。 range外の処理はズームバーの表示でやっている
				}
			});
			mZoomUpButton.setOnClickListener(new View.OnClickListener() {//ズームボタン(Tele側)が押されたとき
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					if(null == mSettingInfo) return;
					if(null == mSettingInfo.mZoom) return;

					int cur =  mSettingInfo.mZoom.getValue();
					cur++;
					mSettingInfo.mZoom.setValue(cur, true);
					mZoomBar.setProgressF(cur);
				}
			});
		}
	}
	/**
	 * ホワイトバランスボタンの表示設定
	 */
	private void setWhiteBalanceButton(){
		if(null == mPreview) return;
		if(null == mSettingInfo) return;
		if(null == mSettingInfo.mWhiteBalance) return;
		if(!mSettingInfo.mWhiteBalance.isSupported()){
			mWhiteBalanceButton.setVisibility(View.INVISIBLE);
		}
		else{
			mWhiteBalanceButton.setVisibility(View.VISIBLE);
			mWhiteBalanceButton.setText(mSettingInfo.mWhiteBalance.getValueName());
			mWhiteBalanceButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					List<String> listName = mSettingInfo.mWhiteBalance.getSupportedListName();
					final Dialog dlg = new Dialog(SelfCameraActivity.this);
					dlg.setContentView(R.layout.pic_list_select_dlg);
					dlg.setTitle(R.string.dialog_white_balance_title);//タイトル設定
					RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
					for(String s:listName){
						RadioButton rb = new RadioButton(SelfCameraActivity.this);
						rb.setText(s);
						rg.addView(rb);
						if(s.equals(mSettingInfo.mWhiteBalance.getValueName())){
							rb.setChecked(true);
						}
						else{
							rb.setChecked(false);
						}
					}
					//OKボタンが押された
					dlg.findViewById(R.id.DlgButtonOK).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
							for(int i = 0;i < rg.getChildCount();i++){
								RadioButton rb = (RadioButton)rg.getChildAt(i);
								if(rb.isChecked() == true){//チェックされたラジオボタン
									List<String> list = mSettingInfo.mWhiteBalance.getSupportedList();
									mSettingInfo.mWhiteBalance.setValue(list.get(i), true); //同じ位置の画像解像度をセットする
									mWhiteBalanceButton.setText(mSettingInfo.mWhiteBalance.getValueName());//新しい解像度をボタンに反映
									if(MyDebug.DEBUG){
										String name = (String) rb.getText();
										Log.i(LOG_TAG, "setWhiteBalanceButton Selected Resolution = " + name);
									}
									break;
								}
							}
							dlg.dismiss();
						}
					});
					dlg.findViewById(R.id.DlgButtonCancel).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							if(MyDebug.DEBUG) Log.i(LOG_TAG, "setWhiteBalanceButton canceled");
							dlg.dismiss();
						}
					});
					dlg.show();
				}
			});
		}
	}
	/**
	 * フラッシュモードボタンの表示設定
	 */
	private void setFlashModeButton(){
		if(null == mPreview) return;
		if(null == mSettingInfo) return;
		if(null == mSettingInfo.mFlashMode) return;
		if(!mSettingInfo.mFlashMode.isSupported()){
			mFlashModeButton.setVisibility(View.INVISIBLE);
		}
		else{
			mFlashModeButton.setVisibility(View.VISIBLE);
			mFlashModeButton.setText(mSettingInfo.mFlashMode.getValueName());
			mFlashModeButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					List<String> listName = mSettingInfo.mFlashMode.getSupportedListName();
					final Dialog dlg = new Dialog(SelfCameraActivity.this);
					dlg.setContentView(R.layout.pic_list_select_dlg);
					dlg.setTitle(R.string.dialog_flash_mode_title);//タイトル設定
					RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
					for(String s:listName){
						RadioButton rb = new RadioButton(SelfCameraActivity.this);
						rb.setText(s);
						rg.addView(rb);
						if(s.equals(mSettingInfo.mFlashMode.getValueName())){
							rb.setChecked(true);
						}
						else{
							rb.setChecked(false);
						}
					}
					//OKボタンが押された
					dlg.findViewById(R.id.DlgButtonOK).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
							for(int i = 0;i < rg.getChildCount();i++){
								RadioButton rb = (RadioButton)rg.getChildAt(i);
								if(rb.isChecked() == true){//チェックされたラジオボタン
									List<String> list = mSettingInfo.mFlashMode.getSupportedList();
									mSettingInfo.mFlashMode.setValue(list.get(i), true); //同じ位置の画像解像度をセットする
									mFlashModeButton.setText(mSettingInfo.mFlashMode.getValueName());//新しい解像度をボタンに反映
									if(MyDebug.DEBUG){
										String name = (String) rb.getText();
										Log.i(LOG_TAG, "setFlashModeButton Selected Resolution = " + name);
									}
									break;
								}
							}
							dlg.dismiss();
						}
					});
					dlg.findViewById(R.id.DlgButtonCancel).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							if(MyDebug.DEBUG) Log.i(LOG_TAG, "setFocusModeButton canceled");
							dlg.dismiss();
						}
					});
					dlg.show();
				}
			});
		}
	}
	/**
	 * 画面上の解像度ボタンの表示設定
	 */
	private void setPictureSizeButton(){
		if(null == mPreview) return;
		if(null == mSettingInfo) return;
		if(null == mSettingInfo.mPictureSize) return;
		if(!mSettingInfo.mPictureSize.isSupported()){//解像度変更ができない場合は表示しない
			mResolutionButton.setVisibility(View.INVISIBLE);
		}
		else{
			mResolutionButton.setVisibility(View.VISIBLE);
			mResolutionButton.setText(mSettingInfo.mPictureSize.getValueName());
			mResolutionButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					List<String> list = mSettingInfo.mPictureSize.getSupportedListName();
					List<Camera.Size> sizeList = mSettingInfo.mPictureSize.getSupportedList();
					Camera.Size current = mSettingInfo.mPictureSize.getValue();
					//解像度変更ダイアログを準備
					final Dialog dlg = new Dialog(SelfCameraActivity.this);
					dlg.setContentView(R.layout.pic_list_select_dlg);
					dlg.setTitle(R.string.dialog_picture_size_title);//タイトル設定
					RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
					for(int i = 0;i < list.size();i++){//解像度ラジオボタンを準備
						RadioButton rb = new RadioButton(SelfCameraActivity.this);
						rb.setText(list.get(i));
						rg.addView(rb);
						//addViewする前にcheckを入れると、ラジオボタンを操作してもチェックが消えないので
						//addViewのあとでセットする
						if(sizeList.get(i).width == current.width && 
								sizeList.get(i).height == current.height){//現在の解像度と一致するときはチェックを入れる
							rb.setChecked(true);
						}
						else{
							rb.setChecked(false);
						}
					}
					//OKボタンが押された
					dlg.findViewById(R.id.DlgButtonOK).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
							for(int i = 0;i < rg.getChildCount();i++){
								RadioButton rb = (RadioButton)rg.getChildAt(i);
								if(rb.isChecked() == true){//チェックされたラジオボタン
									List<Camera.Size> list = mSettingInfo.mPictureSize.getSupportedList();
									mSettingInfo.mPictureSize.setValue(list.get(i), true); //同じ位置の画像解像度をセットする
									resizePreview();//プレビューをリサイズ
									mResolutionButton.setText(mSettingInfo.mPictureSize.getValueName());//新しい解像度をボタンに反映
									if(MyDebug.DEBUG){
										String name = (String) rb.getText();
										Log.i(LOG_TAG, "setPictureSizeButton Selected Resolution = " + name);
									}
									break;
								}
							}
							dlg.dismiss();
						}
					});
					dlg.findViewById(R.id.DlgButtonCancel).setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							if(MyDebug.DEBUG) Log.i(LOG_TAG, "setPictureSizeButton canceled");
							dlg.dismiss();
						}
					});
					dlg.show();
				}
			});
		}
	}
	/**
	 * 撮影人数ボタンの設定
	 */
	private void setAngleButton(){
		mAngleButton.setVisibility(View.VISIBLE);
		String angle = SelfCameraSettingActivity.getAngle(this, mFaceMarkView.getAngleEntryValue());
		ArrayList<String>list = mFaceMarkView.getAngleName();
		ArrayList<String>entry = mFaceMarkView.getAngleEntryValue();
		for(int i = 0;i < list.size();i++){
			if(entry.get(i).equals(angle)){
				mAngleButton.setText(list.get(i));
			}
		}
		mFaceMarkView.setAngleEntryValue(angle);
		mAngleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				final Dialog dlg = new Dialog(SelfCameraActivity.this);
				dlg.setContentView(R.layout.pic_list_select_dlg);
				dlg.setTitle(R.string.dialog_angle_title);//タイトル設定
				RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
				ArrayList<String>list = mFaceMarkView.getAngleName();
				ArrayList<String>entry = mFaceMarkView.getAngleEntryValue();
				for(int i = 0;i < list.size();i++){
					RadioButton rb = new RadioButton(SelfCameraActivity.this);
					rb.setText(list.get(i));
					rg.addView(rb);
					Log.i(LOG_TAG, list.get(i) + ":"+ SelfCameraSettingActivity.getAngle(SelfCameraActivity.this, 
																						mFaceMarkView.getAngleEntryValue()));
					if(entry.get(i).equals(SelfCameraSettingActivity.getAngle(SelfCameraActivity.this, 
																		mFaceMarkView.getAngleEntryValue()))){//現在の撮影人数と一致するときはチェックを入れる
						rb.setChecked(true);
					}
					else{
						rb.setChecked(false);
					}
				}
				for(String s:list){//人数ボタンを準備
				}					
				//OKボタンが押された
				dlg.findViewById(R.id.DlgButtonOK).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						RadioGroup rg = (RadioGroup)dlg.findViewById(R.id.SettingRadioGroup);
						for(int i = 0;i < rg.getChildCount();i++){
							RadioButton rb = (RadioButton)rg.getChildAt(i);
							if(rb.isChecked() == true){//チェックされたラジオボタン
								String name = (String)rb.getText();

								ArrayList<String>list = mFaceMarkView.getAngleName();
								ArrayList<String>entry = mFaceMarkView.getAngleEntryValue();
								String n = null;
								for(int j = 0;j < list.size();j++){
									if(list.get(j).equals(name)){
										n = entry.get(j);
										break;
									}
								}
								if(null == n) n = entry.get(0);
								mFaceMarkView.setAngleEntryValue(n);
								mAngleButton.setText(name);//新しい人数をボタンに反映
								SelfCameraSettingActivity.setAngle(SelfCameraActivity.this, n);//Preferenceに書き込み
								if(MyDebug.DEBUG){
									Log.i(LOG_TAG, "setPictureSizeButton Selected anglen = " + name);
								}
								break;
							}
						}
						dlg.dismiss();
					}
				});
				//キャンセルボタンが押された
				dlg.findViewById(R.id.DlgButtonCancel).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(MyDebug.DEBUG) Log.i(LOG_TAG, "setAngleButton canceled");
						dlg.dismiss();
					}
				});
				dlg.show();
				
			}
		});
	}
	private void setupViews(){
		resizePreview();
		setZoomBar();
		setExposurecompensationBar();
		setPictureSizeButton();
		setFlashModeButton();
		setWhiteBalanceButton();
		setAngleButton();
		mFaceMarkView.setVoiceEntryValue(
				SelfCameraSettingActivity.getVoice(this, mFaceMarkView.getVoiceEntryValues()));

	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		int shutter = SelfCameraSettingActivity.getShutterButton(this);
		boolean capture = false;
		switch(keyCode){
			case KeyEvent.KEYCODE_DPAD_CENTER:
				if(null != mPreview && 
						(shutter & SelfCameraSettingActivity.SHUTTER_OK_BUTTON) == SelfCameraSettingActivity.SHUTTER_OK_BUTTON){
					capture = true;
				}

				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if(null != mPreview && 
						(shutter & SelfCameraSettingActivity.SHUTTER_VOL_PLUS_BUTTON) == SelfCameraSettingActivity.SHUTTER_VOL_PLUS_BUTTON){
					capture = true;
				}
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if(null != mPreview && 
						(shutter & SelfCameraSettingActivity.SHUTTER_VOL_MINUS_BUTTON) == SelfCameraSettingActivity.SHUTTER_VOL_MINUS_BUTTON){
					capture = true;
				}
				break;
			case KeyEvent.KEYCODE_SEARCH:
				if(null != mPreview && 
						(shutter & SelfCameraSettingActivity.SHUTTER_SEARCH_BUTTON) == SelfCameraSettingActivity.SHUTTER_SEARCH_BUTTON){
					capture = true;
				}
				break;
				
		}
		if(true == capture){
			takePicture();				
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			if(null != mPreview){
				int shutter = SelfCameraSettingActivity.getShutterButton(this);
				if( (shutter & SelfCameraSettingActivity.SHUTTER_TOUCH) == SelfCameraSettingActivity.SHUTTER_TOUCH){
					takePicture();				
				}
			}
		}
		return false;
//		return super.onTouchEvent(event);
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG) Log.i(LOG_TAG, "onDestroy");

		if(null != mWakeLock) mWakeLock.release();
		super.onDestroy();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		if(MyDebug.DEBUG) Log.i(LOG_TAG, "onPause");
		// TODO Auto-generated method stub
		if(false == mSettingActivityOpen){//SettingActivityが開いているときはカメラをリリースしない。中でカメラ設定をよんでいるため。
			try {
				mPreview.stopPreview();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mSettingInfo.releaseCamera();
			mPreview.setVisibility(View.GONE);

		}
		if(null != mSensorManager){
			mSensorManager.unregisterListener(this);			
		}
		dismissProgresDialog();
		if(null != mPreview){
			mPreview.dismissCameraOpenDialog();
		}
		super.onPause();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG) Log.i(LOG_TAG, "onResume");
		
		//加速度センサ　初期化
		//ホントは傾きセンサを使いたいがSO-01Bがなにやらへんな値を返すので加速度センサから変換して検出する
		mOrientation = FaceDetect.ROTATION_90;
		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		if(null == mSensorManager){//センサマネージャがうまく取れないときはrotation_90 = landscapeに固定
		}
		else{
			List<Sensor>sensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
			for(Sensor s:sensors){//一応ループで回して設定しておく
				mSensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
			}
			//if(sensors.size() == 0){//加速度センサがない場合は　rotation_90=landscapeに固定
			//}
		}
		//カメラオープン
		if(!mSettingInfo.isCameraOpen() && false == mSettingInfo.openCamera()){
			Toast.makeText(this, R.string.ErrorCantOpenCamera, Toast.LENGTH_SHORT).show();
		}
		//SDチェック
		int errorMsgID = SDCardAccess.checkSDCard(this);
		if(0 != errorMsgID){
			Toast.makeText(this, 
		    		errorMsgID, 
		            Toast.LENGTH_LONG).show();
		}
		int shutter = SelfCameraSettingActivity.getShutterButton(this);
		boolean framed = (shutter & SelfCameraSettingActivity.SHUTTER_FRAMED) == SelfCameraSettingActivity.SHUTTER_FRAMED;
		mPreview.setShootOnFramed(framed);
		setupViews();

		mPreview.setVisibility(View.VISIBLE);

		super.onResume();
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		if(MyDebug.DEBUG) Log.i(LOG_TAG, "onCreate");
		
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		mSettingInfo = new CameraSetting(this);
		
        //画面のデフォルトの向きを取得する
		try{
			Object cameraInfo = clazzCameraInfo.newInstance();
			mGetCameraInfo.invoke(null, 0, cameraInfo);
			mSensorOrientation = (Integer) mCameraOrientation.get(cameraInfo);
		}
		catch(Exception e){
			mSensorOrientation = 90;
		}
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "orientation = " + mSensorOrientation);
        
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        mWakeLock.acquire();



		mPreview = (SelfPreview)findViewById(R.id.PreviewSurfaceView);
		mPreview.setMainActivity(this);
		mFaceMarkView = (FaceMarkView)findViewById(R.id.FaceMarkView);
        mPreview.setFaceMarkView(mFaceMarkView);
        
    	mZoomBar = (SeekBar2)findViewById(R.id.ZoomBar);
    	mZoomDownButton = (Button)findViewById(R.id.ZoomButtonDown);
    	mZoomUpButton = (Button)findViewById(R.id.ZoomButtonUp);
    	
    	mExposureBar = (VerticalSeekBar2)findViewById(R.id.ExposurecompensationBar);
    	mExposureDownButton = (Button)findViewById(R.id.ExposurecompensationButtonDown);
    	mExposureUpButton = (Button)findViewById(R.id.ExposurecompensationButtonUp);
    	
    	mResolutionButton = (Button)findViewById(R.id.ResolutionButton);
    	mFlashModeButton = (Button)findViewById(R.id.FlashModeButton);
    	mWhiteBalanceButton = (Button)findViewById(R.id.WhiteBalanceButton);
    	mAngleButton = (Button)findViewById(R.id.AngleButton);
    	

    }
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option, menu);
		return true;
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		Intent i;
		switch(item.getItemId()){
			case R.id.menu_setting:
				if(null == mSettingInfo){
					Toast.makeText(this, R.string.ErrorCameraNotOpen, Toast.LENGTH_SHORT).show();
					break;
				}
				mSettingActivityOpen = true;
				i = new Intent(SelfCameraActivity.this, 
						CameraSettingActivity.class);
				startActivityForResult(i, ACTIVIY_REQUEST_FINISH_SETTING);
				break;
			case R.id.menu_sefl_camera_setting:
				//人数、シャッタボタン、音声・声、
				i = new Intent(SelfCameraActivity.this, 
						SelfCameraSettingActivity.class);
				//人数情報を渡すために、シリアライズされたクラスに一回データを移す
				//人数情報のentryvalue
				SelfCameraSettingActivity.NameList angleEntryValueList = 
					new SelfCameraSettingActivity.NameList();
				angleEntryValueList.list = mFaceMarkView.getAngleEntryValue();
				i.putExtra(getString(R.string.extra_angle_entry_value_list), angleEntryValueList);
				//人数名
				SelfCameraSettingActivity.NameList angleList = 
							new SelfCameraSettingActivity.NameList();
				angleList.list = mFaceMarkView.getAngleName();
				i.putExtra(getString(R.string.extra_angle_name_list), angleList);

				//声のEntryValue
				SelfCameraSettingActivity.NameList voiceEntryValueList = 
					new SelfCameraSettingActivity.NameList();
				voiceEntryValueList.list = mFaceMarkView.getVoiceEntryValues();
				i.putExtra(getString(R.string.extra_voice_entry_value_list), voiceEntryValueList);
				//音声名一覧
				SelfCameraSettingActivity.NameList voiceNameList = 
							new SelfCameraSettingActivity.NameList();
				voiceNameList.list = mFaceMarkView.getVoiceName();
				i.putExtra(getString(R.string.extra_voice_name_list), voiceNameList);
				//音声コメント
				SelfCameraSettingActivity.NameList voiceCommentList = 
							new SelfCameraSettingActivity.NameList();
				voiceCommentList.list = mFaceMarkView.getVoiceComment();
				i.putExtra(getString(R.string.extra_voice_comment_list), voiceCommentList);
				
				//認識エンジンentryvalue一覧
				SelfCameraSettingActivity.NameList engineEntryValueList = 
							new SelfCameraSettingActivity.NameList();
				engineEntryValueList.list = mPreview.getEngineEntryValue();
				i.putExtra(getString(R.string.extra_engine_entry_value), engineEntryValueList);
				
				//認識エンジン名一覧
				SelfCameraSettingActivity.NameList engineList = 
							new SelfCameraSettingActivity.NameList();
				engineList.list = mPreview.getEngineName();
				i.putExtra(getString(R.string.extra_engine_name), engineList);
		
				
				startActivityForResult(i, ACTIVIY_REQUEST_FINISH_SELF_CAMERA_SETTING);
				break;
			case R.id.menu_gallery:
				i = new Intent();
				i.setType("image/*");
				i.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(i, ACTIVIY_REQUEST_FINISH_GALLERY_PIC_SELECT);
				break;
			default:
				break;
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		return super.onPrepareOptionsMenu(menu);
	}
	/**
	 * 写真撮影が実行されたときに呼び出される。
	 * @param fn
	 * 撮影されたjpeg画像が保存されたファイル名
	 * @param width
	 * 撮影された画像のプレビュー幅
	 * @param height
	 * 高さ
	 */
	public void pictureTaken(String fn, int width, int height){
		Log.i(LOG_TAG, "pictureTaken width = " + width + " height = " + height);
		Intent i = new Intent(SelfCameraActivity.this, 
				PictureViewerActivity.class);
		i.putExtra(getString(R.string.pic_tmp_file), fn);
		i.putExtra(getString(R.string.pic_width), width);
		i.putExtra(getString(R.string.pic_height), height);
		
		startActivityForResult(i, ACTIVIY_REQUEST_FINISH_PIC_VIEWER);
	}

	/**
	 * 撮影を実行する
	 */
	public void takePicture(){
    	//撮影
		mPreview.capture();
	}
	static class SDCardAccess{
		private static final int INITIAL_FOLDER_NUMBER = 100;
		private static final int INITIAL_PICTURE_NUMBER = 1;
		private static final String PICTURE_NAME = "/DSC_";
		


		/**
		 * 写真の番号をカウントアップする
		 */
		public static void pictureNumberCountUp(Context context){
	    	int  dataFolderNumber = INITIAL_FOLDER_NUMBER;
	    	int	 pictureNumber = INITIAL_PICTURE_NUMBER;
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			dataFolderNumber = pref.getInt(context.getString(R.string.mFolderNumber), INITIAL_FOLDER_NUMBER);
			pictureNumber = pref.getInt(context.getString(R.string.mPictureNumber), INITIAL_PICTURE_NUMBER);

			//撮影に成功したらファイル番号をインクリメント
			if(9999 == pictureNumber){
				pictureNumber = INITIAL_PICTURE_NUMBER;
				if(999 == dataFolderNumber){
					dataFolderNumber = INITIAL_FOLDER_NUMBER;
				}
				else{
					dataFolderNumber++;
				}
			}
			else{
				pictureNumber++;
			}
			//新しいファイル番号とフォルダ番号を保存
		    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
	    	ed.putInt(context.getString(R.string.mFolderNumber), dataFolderNumber);
	    	ed.putInt(context.getString(R.string.mPictureNumber), pictureNumber);
			ed.commit();
		}

		public static String createPicturePath(Context context){
			String dataPath = null;
			File file = Environment.getExternalStorageDirectory();
	    	int  dataFolderNumber = INITIAL_FOLDER_NUMBER;
	    	int	 pictureNumber = INITIAL_PICTURE_NUMBER;
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			dataFolderNumber = pref.getInt(context.getString(R.string.mFolderNumber), INITIAL_FOLDER_NUMBER);
			pictureNumber = pref.getInt(context.getString(R.string.mPictureNumber), INITIAL_PICTURE_NUMBER);
			//データフォルダ名作成
	    	dataPath = file.getPath() + "/DCIM"
	    				+ "/" + dataFolderNumber + context.getString(R.string.data_folder_name);
	    	//データフォルダがなければ作成する
	    	File fl = new File(dataPath);
			if(true != fl.exists() && true != fl.mkdirs()){
				Toast.makeText(context, R.string.PictureFolderMakeNG, Toast.LENGTH_SHORT).show();
				return null;
			}
	    	//データファイル名を追加
	    	dataPath += PICTURE_NAME + String.format("%04d", pictureNumber) + ".jpg";
	    	return dataPath;

		}
		/**
		 * SDカードの状態を調べる。書き込み不可の場合はエラーメッセージをToastに表示する
		 * @param context
		 * @return
		 * 0:書き込み可能　
		 * それ以外：エラーメッセージを示すリソースID
		 */
		public static int checkSDCard(Context context){
			String status = Environment.getExternalStorageState();
			int errorMsgID;
			if (status.equalsIgnoreCase(Environment.MEDIA_MOUNTED)){
				return 0;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_MOUNTED_READ_ONLY)){
				errorMsgID = R.string.SDCardReadOnly;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_REMOVED)){
				errorMsgID = R.string.SDCardRemoved;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_SHARED)){
				errorMsgID = R.string.SDCardShared;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_BAD_REMOVAL)){
				errorMsgID = R.string.SDCardBadRemoval;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_CHECKING)){
				errorMsgID = R.string.SDCardChecking;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_NOFS)){
				errorMsgID = R.string.SDCardNOFS;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_UNMOUNTABLE)){
				errorMsgID = R.string.SDCardUnMountable;
			}
			else if (status.equalsIgnoreCase(Environment.MEDIA_UNMOUNTED)){
				errorMsgID = R.string.SDCardUnMounted;
			}
			else{
				errorMsgID = R.string.SDCardUnknownError;
			}
			return errorMsgID;
//		    Toast.makeText(context, 
//		    		errorMsgID, 
//		            Toast.LENGTH_LONG).show();
//		    return false;
		}	
	}
	
	/**
	 * 現在の画面の傾きを取得する
	 * @return
	 * FaceDetect.ROTATION_0
	 * FaceDetect.ROTATION_90
	 * FaceDetect.ROTATION_180
	 * FaceDetect.ROTATION_270
	 * FaceDetect.ROTATION_UNKNOWNのどれか
	 */
	public int getOrientation(){
		return mOrientation;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	
	private long mSensorTimestamp = 0;
	private float mAccX = 0f;
	private float mAccY = 0f;
	private float mAccZ = 0f;
	private static final float PI_P_2 = (float) (Math.PI/2.0);
	private static final float PI = (float) (Math.PI);
	private static final int SENSOR_UPDATE_TIME = 500000000;//0.5秒毎にセンサを更新
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if(event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)return;
		
		mAccX += event.values[SensorManager.DATA_X];
		mAccY += event.values[SensorManager.DATA_Y];
		mAccZ += event.values[SensorManager.DATA_Z];
		if( event.timestamp > mSensorTimestamp &&//タイムスタンプがオーバーフローしたときの対応
				(event.timestamp - mSensorTimestamp) < SENSOR_UPDATE_TIME){
			return;
		}
		mSensorTimestamp = event.timestamp;
		

		float pitch = (float)- Math.atan2((double)mAccY, (double)mAccZ);
		if(pitch < -PI_P_2) pitch = -PI - pitch;//プラマイ９０度に変換
		if(pitch > PI_P_2) pitch = PI - pitch;
		float roll = (float) Math.atan2((double)mAccX, (double)mAccZ);
		if(roll < -PI_P_2) roll = -PI - roll;
		if(roll > PI_P_2) roll =  PI - roll;
		mAccX = 0f;
		mAccY = 0f;
		mAccZ = 0f;
		
		//デフォルトの向きで加速度センサの軸が異なるため補正
		float tmp;
		switch(mSensorOrientation){
			case 0:
				tmp = roll;
				roll = -pitch;
				pitch = tmp;
				break;
			case 90:break;
			case 180:
				tmp = roll;
				roll = pitch;
				pitch = -tmp;
				break;
			case 270:
				roll *= -1f;
				pitch *= -1f;
				break;
		}
		if(MyDebug.DEBUG){
			Log.i(LOG_TAG,	" roll = " + String.valueOf(roll) + 
							" pitch = " + String.valueOf(pitch));
		}
		// 数はPortraitがdefaultの場合
		//        ->    pitch 0->90->180   rot_180
		//        |
		//     |-----|
		//     |     | roll 0->-90->0 rot_270
		//  |--|     |--|
		//  V  |○　○ ○|  V
		//     |-----|
		//        |
		//        -> pitch 0->-90->-180 rot_0
		///roll 0->90->0  rot_90
//		float azimus = event.values[SensorManager.DATA_X];
//		float pitch = event.values[SensorManager.DATA_Y];
//		if(pitch < -90f) pitch = -180f - pitch;
//		if(pitch > 90f) pitch = 180f - pitch;  // rollにあわせて±90に変換
//		float roll = event.values[SensorManager.DATA_Z];
		final int oldOrientation = mOrientation;
		
		if(FaceDetect.ROTATION_UNKNOWN == mOrientation){
			if(Math.abs(pitch) > Math.abs(roll)){
				if(pitch < 0.0){
					mOrientation = FaceDetect.ROTATION_0;
				}
				else{
					mOrientation = FaceDetect.ROTATION_180;						
				}
			}
			else{
				if(roll > 0.0){
					mOrientation = FaceDetect.ROTATION_90;						
				}
				else{
					mOrientation = FaceDetect.ROTATION_270;						
				}
			}
		}
		else if(FaceDetect.ROTATION_0 == mOrientation){
			if(Math.abs(pitch) > (Math.abs(roll)-ROTATION_CHATA_SIZE)){
				if(pitch < ROTATION_CHATA_SIZE){
					//なにもしない
					//mOrientation = FaceDetect.ROTATION_0;
				}
				else{
					mOrientation = FaceDetect.ROTATION_180;
				}
			}
			else{
				if(roll < 0){
					mOrientation = FaceDetect.ROTATION_270;						
				}
				else{
					mOrientation = FaceDetect.ROTATION_90;
				}
			}
		}
		else if(FaceDetect.ROTATION_90 == mOrientation){
			if(Math.abs(roll) > (Math.abs(pitch)-ROTATION_CHATA_SIZE)){
				if(roll > -ROTATION_CHATA_SIZE){
					//なにもしない
					//mOrientation = FaceDetect.ROTATION_90;
				}
				else{
					mOrientation = FaceDetect.ROTATION_270;
				}
			}
			else{
				if(pitch < 0){
					mOrientation = FaceDetect.ROTATION_0;						
				}
				else{
					mOrientation = FaceDetect.ROTATION_180;
				}
			}
		}
		else if(FaceDetect.ROTATION_180 == mOrientation){
			if(Math.abs(pitch) > (Math.abs(roll)-ROTATION_CHATA_SIZE)){
				if(pitch > -ROTATION_CHATA_SIZE){
					//なにもしない
					//mOrientation = FaceDetect.ROTATION_180;
				}
				else{
					mOrientation = FaceDetect.ROTATION_0;
				}
			}
			else{
				if(roll < 0){
					mOrientation = FaceDetect.ROTATION_270;						
				}
				else{
					mOrientation = FaceDetect.ROTATION_90;
				}
			}
		}
		else if(FaceDetect.ROTATION_270 == mOrientation){
			if(Math.abs(roll) > (Math.abs(pitch)-ROTATION_CHATA_SIZE)){
				if(roll < ROTATION_CHATA_SIZE){
					//なにもしない
					//mOrientation = FaceDetect.ROTATION_270;
				}
				else{
					mOrientation = FaceDetect.ROTATION_90;
				}
			}
			else{
				if(pitch < 0){
					mOrientation = FaceDetect.ROTATION_0;						
				}
				else{
					mOrientation = FaceDetect.ROTATION_180;
				}
			}
		}
		if(mOrientation != oldOrientation){
			Log.i(LOG_TAG, "Orientation Old = " + oldOrientation + " New = " + mOrientation);
		}
	}
}