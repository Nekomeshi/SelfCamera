package com.nekomeshi312.selfcamera;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

public class SelfCameraSettingActivity extends PreferenceActivity{
	public static class NameList implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = -1088486254802182596L;
		public ArrayList<String> list;
	}
	private static final String LOG_TAG = "SelfCameraSettingActivity";

	public static final int SHUTTER_NONE = 0x00;
	public static final int SHUTTER_TOUCH = 0x01;
	public static final int SHUTTER_OK_BUTTON = 0x02;
	public static final int SHUTTER_VOL_PLUS_BUTTON = 0x04;
	public static final int SHUTTER_VOL_MINUS_BUTTON = 0x08;
	public static final int SHUTTER_SEARCH_BUTTON = 0x10;
	public static final int SHUTTER_FRAMED = 0x20;//被写体が良い位置に来たとき
	
	private ArrayList<String> mAngleList = null;
	private ArrayList<String> mAngleEntryValueList = null;
	private ArrayList<String> mVoiceNameList = null;
	private ArrayList<String> mVoiceEntryValueList = null;
	private ArrayList<String> mVoiceCommentList = null;
	private ArrayList<String> mEngineNameList = null;
	private ArrayList<String> mEngineEntryValueList = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "onCreate");
		super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        NameList data = (NameList)intent.getSerializableExtra(getString(R.string.extra_angle_entry_value_list));
        mAngleEntryValueList = data.list;
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_angle_name_list));
        mAngleList = data.list;
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_voice_entry_value_list));
        mVoiceEntryValueList = data.list;
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_voice_name_list));
        mVoiceNameList = data.list;
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_voice_comment_list));
        mVoiceCommentList = data.list;
       
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_engine_name));
        mEngineNameList = data.list;
        data = (NameList)intent.getSerializableExtra(getString(R.string.extra_engine_entry_value));
        mEngineEntryValueList = data.list;
        
        setPreferenceScreen(createPreferenceHierarchy());
    	
	}
	//設定されている撮影人数の名前を取得する
	public static String getAngle(Context context, ArrayList<String>entryList){
		//デフォルトは一人
		String s = PreferenceManager.getDefaultSharedPreferences(context).
					getString(context.getString(R.string.mAngle), context.getString(R.string.face_mark_single_entry_value));
		for(String l:entryList){
			if(l.equals(s)) return s;
		}
		return context.getString(R.string.face_mark_single_entry_value);
	}

	//設定されている撮影人数の名前を書きこむ
	public static void setAngle(Context context, String name){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putString(context.getString(R.string.mAngle), name);
		ed.commit();
	}
	/**
	 * 設定されている認識エンジン名を取得する
	 * @param context
	 * @return
	 */
	public static String getEngine(Context context, ArrayList<String>entryList){
		String s = PreferenceManager.getDefaultSharedPreferences(context).
		getString(context.getString(R.string.mEngineName), context.getString(R.string.OpenCVEngineEntryValue));	
		for(String l:entryList){
			if(l.equals(s))return s;
		}
		return entryList.get(0);
	}
	/**
	 * 顔認識エンジン名を書き込む
	 * @param context
	 * @param name
	 */
	public static void setEngine(Context context, String name){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putString(context.getString(R.string.mEngineName), name);
		ed.commit();
	}
	/**
	 * 設定されている音声名を取得する
	 * @param context
	 * @return
	 */
	public static String getVoice(Context context, ArrayList<String>entryList){
		//デフォルトは女性
		String s = PreferenceManager.getDefaultSharedPreferences(context).
					getString(context.getString(R.string.mVoiceName), context.getString(R.string.female_voice_entry_value));	
		for(String l:entryList){
			if(l.equals(s))return s;
		}
		return context.getString(R.string.female_voice_entry_value);
	}
	
	/**
	 * 音声名を書き込む
	 * @param context
	 * @param name
	 */
	public static void setVoice(Context context, String name){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
    	ed.putString(context.getString(R.string.mVoiceName), name);
		ed.commit();
		
	}
	
	//シャッタボタンを取得する
	public static int getShutterButton(Context context){
		int ans = SHUTTER_NONE;
		//デフォルトはタッチ&被写体が良い位置に来たとき
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		try{
			if(pref.getBoolean(context.getString(R.string.mShutterTouch), true)) ans |= SHUTTER_TOUCH;
			if(pref.getBoolean(context.getString(R.string.mShutterOK), false)) ans |= SHUTTER_OK_BUTTON;
			if(pref.getBoolean(context.getString(R.string.mShutterVolPlus), false)) ans |= SHUTTER_VOL_PLUS_BUTTON;
			if(pref.getBoolean(context.getString(R.string.mShutterVolMinus), false)) ans |= SHUTTER_VOL_MINUS_BUTTON;
			if(pref.getBoolean(context.getString(R.string.mShutterSearch), false)) ans |= SHUTTER_SEARCH_BUTTON;
			if(pref.getBoolean(context.getString(R.string.mShutterFramed), true)) ans |= SHUTTER_FRAMED;
		}
		catch(Exception e){
			e.printStackTrace();
			ans = (SHUTTER_TOUCH | SHUTTER_FRAMED);
		}
		return ans;
	}
	//シャッタボタンを設定する
	public static void setShutterButton(Context context, int shutter){
	    Editor ed = PreferenceManager.getDefaultSharedPreferences(context).edit();
	    boolean ans;
	    ans = (SHUTTER_TOUCH & shutter) == SHUTTER_TOUCH;
    	ed.putBoolean(context.getString(R.string.mShutterTouch), ans);
    	ans = (SHUTTER_OK_BUTTON & shutter) == SHUTTER_OK_BUTTON;
    	ed.putBoolean(context.getString(R.string.mShutterOK), ans);
    	ans = (SHUTTER_VOL_PLUS_BUTTON & shutter) == SHUTTER_VOL_PLUS_BUTTON;
    	ed.putBoolean(context.getString(R.string.mShutterVolPlus), ans);
    	ans = (SHUTTER_VOL_MINUS_BUTTON & shutter) == SHUTTER_VOL_MINUS_BUTTON;
    	ed.putBoolean(context.getString(R.string.mShutterVolMinus), ans);
    	ans = (SHUTTER_SEARCH_BUTTON & shutter) == SHUTTER_SEARCH_BUTTON;
    	ed.putBoolean(context.getString(R.string.mShutterSearch), ans);
    	ans = (SHUTTER_FRAMED & shutter) == SHUTTER_FRAMED;
    	ed.putBoolean(context.getString(R.string.mShutterFramed), ans);
		ed.commit();
	}
	

	
	private String [] stringList2Array(List<String>list){
		int size = list.size();
		if(0 == size) return null;
        String [] tmp = new String[size];
        for(int i = 0;i < size;i++){
        	tmp[i] = list.get(i);
        }
        return tmp;
	}
	private PreferenceScreen createPreferenceHierarchy() {
		// Root
	    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
    	PreferenceCategory prefCat = new PreferenceCategory(this);
    	prefCat.setTitle(getString(R.string.setting_category_title_picture));
    	root.addPreference(prefCat);
    	
	    //被写体の人数
	    ListPreference listAnglePref = new ListPreference(this);
	    listAnglePref.setEntries(stringList2Array(mAngleList));
	    listAnglePref.setEntryValues(stringList2Array(mAngleEntryValueList));
	    listAnglePref.setDialogTitle(getString(R.string.self_camera_setting_dialog_title_angle));
	    listAnglePref.setKey(getString(R.string.mAngle));
	    listAnglePref.setTitle(getString(R.string.self_camera_setting_title_angle));
	    listAnglePref.setSummary(getString(R.string.self_camera_setting_summary_angle));
	    listAnglePref.setDefaultValue(getString(R.string.face_mark_single_entry_value));
	    prefCat.addPreference(listAnglePref);

	    //シャッタボタン
        PreferenceScreen shutterPref = getPreferenceManager().createPreferenceScreen(this);
        shutterPref.setKey(getString(R.string.mShutter));
        shutterPref.setTitle(getString(R.string.self_camera_setting_title_shutter));
        shutterPref.setSummary(getString(R.string.self_camera_setting_summary_shutter));
        prefCat.addPreference(shutterPref);
        {
        	//touch
            CheckBoxPreference checkBoxPref1 = new CheckBoxPreference(this);
            checkBoxPref1.setKey(getString(R.string.mShutterTouch));
            checkBoxPref1.setTitle(getString(R.string.self_camera_setting_shutter_touch));
            checkBoxPref1.setSummary("");
            checkBoxPref1.setDefaultValue(true);
            shutterPref.addPreference(checkBoxPref1);
            //OKbutton
            CheckBoxPreference checkBoxPref2 = new CheckBoxPreference(this);
            checkBoxPref2.setKey(getString(R.string.mShutterOK));
            checkBoxPref2.setTitle(getString(R.string.self_camera_setting_shutter_OK));
            checkBoxPref2.setSummary("");
            checkBoxPref2.setDefaultValue(false);
            shutterPref.addPreference(checkBoxPref2);
            //Vol+
            CheckBoxPreference checkBoxPref3 = new CheckBoxPreference(this);
            checkBoxPref3.setKey(getString(R.string.mShutterVolPlus));
            checkBoxPref3.setTitle(getString(R.string.self_camera_setting_shutter_vol_plus));
            checkBoxPref3.setSummary("");
            checkBoxPref3.setDefaultValue(false);
            shutterPref.addPreference(checkBoxPref3);
            //Vol-
            CheckBoxPreference checkBoxPref4 = new CheckBoxPreference(this);
            checkBoxPref4.setKey(getString(R.string.mShutterVolMinus));
            checkBoxPref4.setTitle(getString(R.string.self_camera_setting_shutter_vol_minus));
            checkBoxPref4.setSummary("");
            checkBoxPref4.setDefaultValue(false);
            shutterPref.addPreference(checkBoxPref4);
            //Search
            CheckBoxPreference checkBoxPref5 = new CheckBoxPreference(this);
            checkBoxPref5.setKey(getString(R.string.mShutterSearch));
            checkBoxPref5.setTitle(getString(R.string.self_camera_setting_shutter_search));
            checkBoxPref5.setSummary("");
            checkBoxPref5.setDefaultValue(false);
            shutterPref.addPreference(checkBoxPref5);
            //Framed
            CheckBoxPreference checkBoxPref6 = new CheckBoxPreference(this);
            checkBoxPref6.setKey(getString(R.string.mShutterFramed));
            checkBoxPref6.setTitle(getString(R.string.self_camera_setting_shutter_framed));
            checkBoxPref6.setSummary("");
            checkBoxPref6.setDefaultValue(true);
            shutterPref.addPreference(checkBoxPref6);
        }
        //音声名
	    ListPreference listVoicePref = new ListPreference(this);
	    listVoicePref.setEntries(stringList2Array(mVoiceNameList));
	    listVoicePref.setEntryValues(stringList2Array(mVoiceEntryValueList));
	    listVoicePref.setDialogTitle(getString(R.string.self_camera_setting_dialog_title_voice_name));
	    listVoicePref.setKey(getString(R.string.mVoiceName));
	    listVoicePref.setTitle(getString(R.string.self_camera_setting_title_voice_name));
	    listVoicePref.setSummary(getString(R.string.self_camera_setting_summary_voice_name));
	    listVoicePref.setDefaultValue(getString(R.string.female_voice_entry_value));
	    prefCat.addPreference(listVoicePref);
 
        //認識エンジン
	    ListPreference listEnginePref = new ListPreference(this);
	    listEnginePref.setEntries(stringList2Array(mEngineNameList));
	    listEnginePref.setEntryValues(stringList2Array(mEngineEntryValueList));
	    listEnginePref.setDialogTitle(getString(R.string.self_camera_setting_dialog_title_engine_name));
	    listEnginePref.setKey(getString(R.string.mEngineName));
	    listEnginePref.setTitle(getString(R.string.self_camera_setting_title_engine_name));
	    listEnginePref.setSummary(getString(R.string.self_camera_setting_summary_engine_name));
	    listEnginePref.setDefaultValue(getEngine(this, mEngineEntryValueList));
	    prefCat.addPreference(listEnginePref);
	    
	    return root;
	}
}