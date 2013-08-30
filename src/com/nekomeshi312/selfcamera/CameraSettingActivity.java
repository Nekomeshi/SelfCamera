package com.nekomeshi312.selfcamera;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

public class CameraSettingActivity extends PreferenceActivity {
	private static final String LOG_TAG = "CameraSettingActivity";
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onPause");
		super.onPause();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onResume");
		super.onResume();
	}
	@Override
    public void onCreate(Bundle savedInstanceState) {
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
        setPreferenceScreen(createPreferenceHierarchy());
    }
	private void setNoInfoPref(PreferenceCategory parent){
        // Toggle preference
        Preference pref = new Preference(this);
        pref.setTitle(getString(R.string.setting_no_info_pref_title));
        parent.addPreference(pref);
		
	}
    private PreferenceScreen createPreferenceHierarchy() {
        // Root
    	PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    	//画像設定
    	int count = 0;
    	PreferenceCategory prefCatPicture = new PreferenceCategory(this);
    	prefCatPicture.setTitle(getString(R.string.setting_category_title_picture));
    	root.addPreference(prefCatPicture);
    	if(null != SelfCameraActivity.mSettingInfo.mColorEffect && 
    			true == SelfCameraActivity.mSettingInfo.mColorEffect.setPreference(prefCatPicture, this)) count++;
    	if(null != SelfCameraActivity.mSettingInfo.mSceneMode && 
    			true == SelfCameraActivity.mSettingInfo.mSceneMode.setPreference(prefCatPicture, this)) count++;
    	if(0 == count){
    		setNoInfoPref( prefCatPicture);
    	}
    	
    	//画質設定
    	count = 0;
    	PreferenceCategory prefCatQuality = new PreferenceCategory(this);
    	prefCatQuality.setTitle(getString(R.string.setting_category_title_quality));
    	root.addPreference(prefCatQuality);
    	if(null != SelfCameraActivity.mSettingInfo.mPictureSize && 
    			true == SelfCameraActivity.mSettingInfo.mPictureSize.setPreference(prefCatQuality, this)) count++;
//    	if(true == SelfCameraActivity.mSettingInfo.mVideoSize.setPreference(prefCatQuality, this)) count++;
    	if(null != SelfCameraActivity.mSettingInfo.mJpegQuality &&
    			true == SelfCameraActivity.mSettingInfo.mJpegQuality.setPreference(prefCatQuality, this)) count++;
//    	if(true == SelfCameraActivity.mSettingInfo.mJpegThumbnailSize.setPreference(prefCatQuality, this)) count++;;
    	if(0 == count){
    		setNoInfoPref(prefCatQuality);
    	}
        
    	//撮影設定
    	count = 0;
    	PreferenceCategory prefCatImaging= new PreferenceCategory(this);
    	prefCatImaging.setTitle(getString(R.string.setting_category_title_imaging));
    	root.addPreference(prefCatImaging);
    	if(null != SelfCameraActivity.mSettingInfo.mZoom &&
    			true == SelfCameraActivity.mSettingInfo.mZoom.setPreference(prefCatImaging, this)) count++;
    	if(null != SelfCameraActivity.mSettingInfo.mExposurecompensation &&
    			true == SelfCameraActivity.mSettingInfo.mExposurecompensation.setPreference(prefCatImaging, this)) count++;
    	if(null != SelfCameraActivity.mSettingInfo.mFlashMode &&
    			true == SelfCameraActivity.mSettingInfo.mFlashMode.setPreference(prefCatImaging, this)) count++;
    	
       	if(null != SelfCameraActivity.mSettingInfo.mFocusMode &&
        			true == SelfCameraActivity.mSettingInfo.mFocusMode.setPreference(prefCatImaging, this)) count++;
    	if(null != SelfCameraActivity.mSettingInfo.mWhiteBalance &&
    			true == SelfCameraActivity.mSettingInfo.mWhiteBalance.setPreference(prefCatImaging, this)) count++;
//    	if(true == SelfCameraActivity.mSettingInfo.mAntibanding.setPreference(prefCatImaging, this)) count++;
    	if(0 == count){
    		setNoInfoPref(prefCatImaging);
    	}
    	return root;
    }

}
