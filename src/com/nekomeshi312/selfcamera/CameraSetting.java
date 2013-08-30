package com.nekomeshi312.selfcamera;


import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;

public class CameraSetting {
	public interface SetPreference{
		/**
		 * PreferenceActivityに表示する設定情報を設定する
		 * @param pref
		 * 親preference
		 * @param context
		 * 親のコンテキスト
		 * @return
		 */
		boolean setPreference(PreferenceCategory pref, Context context);
	}
	public abstract class CameraSettingBase{
		protected String mKey;
		protected String mDialogTitle;
		protected String mTitle;
		protected String mSummary;
		
		/**
		 * @return the mKey
		 */
		@SuppressWarnings("unused")
		public String getKey() {
			return mKey;
		}
		public abstract boolean isSupported();
		public abstract void setValue();
		protected abstract int getMinAPILevel();
		protected  String [] stringList2Array(List<String>list){
			int size = list.size();
			if(0 == size) return null;
	        String [] tmp = new String[size];
	        for(int i = 0;i < size;i++){
	        	tmp[i] = list.get(i);
	        }
	        return tmp;
		}
	}
	private abstract class CameraSettingString extends CameraSettingBase{

		protected String mDefault;

		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        listPref.setEntryValues(stringList2Array(getSupportedList()));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(mDefault);
	        pref.addPreference(listPref);
	        return true;
		}
		/**
		 * サポートされているかどうか
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		/**
		 * サポートされている項目の名称
		 * @return
		 */
		public abstract List<String> getSupportedList();
		/**
		 * サポートされている項目の設定画面に表示される名所
		 * @return
		 */
		public abstract List<String> getSupportedListName();
		/**
		 * Preferenceに登録されている値を取得する
		 * @return
		 */
		@SuppressWarnings("unused")
		public String getValue(){
			String val = mSharedPref.getString(mKey, mDefault);
			List<String> lst = getSupportedList();
			if(null == lst)return val;
			for(String s:lst){
				if(s.equals(val))return val;
			}
			return  lst.get(0);
		}
		
		@SuppressWarnings("unused")
		public String getValueName(){
			String cur = getValue();
			List<String> list = getSupportedList();
			for(int i = 0;i < list.size();i++){
				if(list.get(i).equals(cur)){
					return getSupportedListName().get(i);
				}
			}
			return null;
		}
		/**
		 * Preferenceに登録されている値を端末のカメラに設定する
		 */
		@SuppressWarnings("unused")
		public void setValue(){
			setValue(getValue());
		}
		/**
		 * 指定された値を端末のカメラに設定する
		 * @param value
		 */
		protected abstract void setValue(String value);
		@SuppressWarnings("unused")
		public boolean setValue(String value, boolean setNow){
			String res = null;
			if(false != isSupported()){
				for(String s:getSupportedList()){
					if(s.equals(value)){
						res = s;
						break;
					}
				}
			}
		    Editor ed = mSharedPref.edit();
	    	ed.putString(mKey, res == null ? NOT_SUPPORTED_STRING:res);
			ed.commit();
			if(true == setNow && null != res){
				setValue(res);
			}
			return res != null;
		}
	}
	//チラツキ防止
	public class CameraSettingAntibanding extends CameraSettingString
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingAntibanding";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingAntibanding() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mAntibanding));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle = mContext.getString(R.string.anti_banding_dialog_title);
			mTitle = mContext.getString(R.string.anti_banding_title);
			mSummary = mContext.getString(R.string.anti_banding_summary);
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}

		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedAntibanding();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.ANTIBANDING_AUTO)){
					name = mContext.getString(R.string.anti_banding_auto);
				}
				else if(s.equals(Parameters.ANTIBANDING_50HZ)){
					name = mContext.getString(R.string.anti_banding_50Hz);
				}
				else if(s.equals(Parameters.ANTIBANDING_60HZ)){
					name = mContext.getString(R.string.anti_banding_60Hz);
				}
				else if(s.equals(Parameters.ANTIBANDING_OFF)){
					name = mContext.getString(R.string.anti_banding_off);			
				}
				else{
					name = mContext.getString(R.string.anti_banding_etc) + "(" + s + ")";					
				}
				ans.add(name);
			}
			return ans;
		}


		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setAntibanding(value);
			mCamera.setParameters(param);
		}
	}
	
	//カラーエフェクト
	public class CameraSettingColorEffect extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingColorEffect";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingColorEffect() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mColorEffect));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle = mContext.getString(R.string.color_effect_dialog_title);
			mTitle = mContext.getString(R.string.color_effect_title);
			mSummary = mContext.getString(R.string.color_effect_summary);
		}

		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}
		
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			return param.getSupportedColorEffects();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.EFFECT_NONE)){
					name =  mContext.getString(R.string.color_effect_none);
				}
				else if(s.equals(Parameters.EFFECT_MONO)){
					name =  mContext.getString(R.string.color_effect_mono);
				}
				else if(s.equals(Parameters.EFFECT_NEGATIVE)){
					name =  mContext.getString(R.string.color_effect_negative);
				}
				else if(s.equals(Parameters.EFFECT_SOLARIZE)){
					name =  mContext.getString(R.string.color_effect_solarize);
				}
				else if(s.equals(Parameters.EFFECT_SEPIA)){
					name =  mContext.getString(R.string.color_effect_sepia);
				}
				else if(s.equals(Parameters.EFFECT_POSTERIZE)){
					name =  mContext.getString(R.string.color_effect_posterize);
				}
				else if(s.equals(Parameters.EFFECT_WHITEBOARD)){
					name =  mContext.getString(R.string.color_effect_whiteboard);
				}
				else if(s.equals(Parameters.EFFECT_BLACKBOARD)){
					name =  mContext.getString(R.string.color_effect_blackboard);
				}
				else if(s.equals(Parameters.EFFECT_AQUA)){
					name =  mContext.getString(R.string.color_effect_aqua);
				}
				else{
					name =  mContext.getString(R.string.color_effect_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setColorEffect(value);
			mCamera.setParameters(param);
		}


	}
	
	//フラッシュモード
	public class CameraSettingFlashMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingFlashMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingFlashMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mFlashMode));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle =  mContext.getString(R.string.flash_mode_dialog_title);
			mTitle =  mContext.getString(R.string.flash_mode_title);
			mSummary =  mContext.getString(R.string.flash_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
	        return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedFlashModes();
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.FLASH_MODE_OFF)){
					name =  mContext.getString(R.string.flash_mode_off);
				}
				else if(s.equals(Parameters.FLASH_MODE_AUTO)){
					name =  mContext.getString(R.string.flash_mode_auto);
				}
				else if(s.equals(Parameters.FLASH_MODE_ON)){
					name =  mContext.getString(R.string.flash_mode_on);
				}
				else if(s.equals(Parameters.FLASH_MODE_RED_EYE)){
					name =  mContext.getString(R.string.flash_mode_red_eye);
				}
				else if(s.equals(Parameters.FLASH_MODE_TORCH)){
					name = mContext.getString(R.string.flash_mode_torch);
				}
				else{
					name =  mContext.getString(R.string.flash_mode_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		
		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setFlashMode(value);
			mCamera.setParameters(param);
		}
	}

	//フォーカスモード
	public class CameraSettingFocusMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingFocusMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingFocusMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mFocusMode));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle =  mContext.getString(R.string.focus_mode_dialog_title);
			mTitle =  mContext.getString(R.string.focus_mode_title);
			mSummary =  mContext.getString(R.string.focus_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();
			if(!VersionCheck.isICS()){
				return param.getSupportedFocusModes();
			}
			else{//ICSから(GNだけ？)AutoFocus or Macro 以外ではonAutoFocus()でtrueが帰らなくなったのでどちらかしか選べなくしておく
				ArrayList<String>tmp = new ArrayList<String>();
				for(String s:param.getSupportedFocusModes()){
					if(s.equals(Parameters.FOCUS_MODE_AUTO) ||
							s.equals(Parameters.FOCUS_MODE_MACRO)){
						tmp.add(s);
					}
				}
				return tmp;
			}
		}

		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				Log.w(LOG_TAG, s);
				if(s.equals(Parameters.FOCUS_MODE_AUTO)){
					name =  mContext.getString(R.string.focus_mode_auto);
				}
				else if(s.equals(Parameters.FOCUS_MODE_INFINITY)){
					name = mContext.getString(R.string.focus_mode_infinity);
				}
				else if(s.equals(Parameters.FOCUS_MODE_MACRO)){
					name =  mContext.getString(R.string.focus_mode_macro);
				}
				else if(s.equals(Parameters.FOCUS_MODE_FIXED)){
					name =  mContext.getString(R.string.focus_mode_fixed);
				}
				else{
					name =  mContext.getString(R.string.focus_mode_etc) + "(" + s + ")";
					Class<?> clazzCameraParam;
					try {
						clazzCameraParam = Class.forName("android.hardware.Camera$Parameters");
						try{
							Field f = clazzCameraParam.getDeclaredField("FOCUS_MODE_EDOF");
							if(s.equals((String)f.get(null))){
								name =  mContext.getString(R.string.focus_mode_EDOF);
							}
						}
						catch(Exception e){
							e.printStackTrace();
						}
						try{
							Field f = clazzCameraParam.getDeclaredField("FOCUS_MODE_CONTINUOUS_VIDEO");
							if(s.equals((String)f.get(null))){
								name =  mContext.getString(R.string.focus_mode_continuous_video);
							}
						}
						catch(Exception e){
							e.printStackTrace();
						}
						try{
							Field f = clazzCameraParam.getDeclaredField("FOCUS_MODE_CONTINUOUS_PICTURE");
							if(s.equals((String)f.get(null))){
								name =  mContext.getString(R.string.focus_mode_continuous_pic);
							}
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				ans.add(name);
			}
			return ans;
		}
	
		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setFocusMode(value);
			mCamera.setParameters(param);
		}
	}

	
	//シーンモード
	public class CameraSettingSceneMode extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingSceneMode";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingSceneMode() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mSceneMode));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle =  mContext.getString(R.string.scene_mode_dialog_title);
			mTitle = mContext.getString(R.string.scene_mode_title);
			mSummary = mContext.getString(R.string.scene_mode_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedSceneModes();
		}
		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.SCENE_MODE_AUTO)){
					name = mContext.getString(R.string.scene_mode_auto);
				}
				else if(s.equals(Parameters.SCENE_MODE_ACTION)){
					name = mContext.getString(R.string.scene_mode_action);
				}
				else if(s.equals(Parameters.SCENE_MODE_PORTRAIT)){
					name = mContext.getString(R.string.scene_mode_portrait);
				}
				else if(s.equals(Parameters.SCENE_MODE_LANDSCAPE)){
					name = mContext.getString(R.string.scene_mode_landscape);
				}
				else if(s.equals(Parameters.SCENE_MODE_NIGHT)){
					name = mContext.getString(R.string.scene_mode_night);
				}
				else if(s.equals(Parameters.SCENE_MODE_NIGHT_PORTRAIT)){
					name = mContext.getString(R.string.scene_mode_night_portrait);
				}
				else if(s.equals(Parameters.SCENE_MODE_THEATRE)){
					name = mContext.getString(R.string.scene_mode_theatre);
				}
				else if(s.equals(Parameters.SCENE_MODE_BEACH)){
					name = mContext.getString(R.string.scene_mode_beach);
				}
				else if(s.equals(Parameters.SCENE_MODE_SNOW)){
					name = mContext.getString(R.string.scene_mode_snow);
				}
				else if(s.equals(Parameters.SCENE_MODE_SUNSET)){
					name = mContext.getString(R.string.scene_mode_sunset);
				}
				else if(s.equals(Parameters.SCENE_MODE_STEADYPHOTO)){
					name = mContext.getString(R.string.scene_mode_steadyphoto);
				}
				else if(s.equals(Parameters.SCENE_MODE_FIREWORKS)){
					name = mContext.getString(R.string.scene_mode_fireworks);
				}
				else if(s.equals(Parameters.SCENE_MODE_SPORTS)){
					name = mContext.getString(R.string.scene_mode_sports);
				}
				else if(s.equals(Parameters.SCENE_MODE_PARTY)){
					name = mContext.getString(R.string.scene_mode_party);
				}
				else if(s.equals(Parameters.SCENE_MODE_CANDLELIGHT)){
					name = mContext.getString(R.string.scene_mode_candle_light);
				}
				else{
					name = mContext.getString(R.string.scene_mode_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}

		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setSceneMode(value);
			mCamera.setParameters(param);
		}
	}

	
	//ホワイトバランス
	public class CameraSettingWhiteBalance extends CameraSettingString
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingWhiteBalance";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingWhiteBalance() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mWhiteBalance));
			mDefault = isSupported() ? getSupportedList().get(0):NOT_SUPPORTED_STRING;
			mDialogTitle = mContext.getString(R.string.white_balance_dialog_title);
			mTitle = mContext.getString(R.string.white_balance_title);
			mSummary = mContext.getString(R.string.white_balance_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<String> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedWhiteBalance();
		}
		@Override
		public List<String> getSupportedListName() {
			// TODO Auto-generated method stub
			ArrayList<String>ans = new ArrayList<String>();
			List<String> list =  getSupportedList();
			for(String s:list){
				String name = null;
				if(s.equals(Parameters.WHITE_BALANCE_AUTO)){
					name = mContext.getString(R.string.white_balance_auto);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_INCANDESCENT)){
					name = mContext.getString(R.string.white_balance_incandesent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_FLUORESCENT)){
					name = mContext.getString(R.string.white_balance_fluoresent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_WARM_FLUORESCENT)){
					name = mContext.getString(R.string.white_balance_warm_fluorecent);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_DAYLIGHT)){
					name = mContext.getString(R.string.white_balance_daylight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)){
					name = mContext.getString(R.string.white_balance_cloudy_daylight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_TWILIGHT)){
					name = mContext.getString(R.string.white_balance_twilight);
				}
				else if(s.equals(Parameters.WHITE_BALANCE_SHADE)){
					name = mContext.getString(R.string.white_balance_shade);
				}
				else{
					name = mContext.getString(R.string.white_balance_etc) + "(" + s + ")";
				}
				ans.add(name);
			}
			return ans;
		}
		@Override
		protected void setValue(String value) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			param.setWhiteBalance(value);
			mCamera.setParameters(param);
		}
	}
	
	
	private abstract class CameraSettingCameraSize extends CameraSettingBase{	
		protected String mDefault;
		protected int correctValue(int i){
			i = Math.max(i, 0);
			i = Math.min(i, getSupportedList().size()-1);
			return i;
		}
		protected boolean setPreferenceMain(PreferenceCategory pref, Context context){
			if(!isSupported())return false;
	        ListPreference listPref = new ListPreference(context);
	        listPref.setEntries(stringList2Array(getSupportedListName()));
	        listPref.setEntryValues(stringList2Array(getSupportedListString()));
	        listPref.setDialogTitle(mDialogTitle);
	        listPref.setKey(mKey);
	        listPref.setTitle(mTitle);
	        listPref.setSummary(mSummary);
	        listPref.setDefaultValue(mDefault);
	        pref.addPreference(listPref);
	        return true;
		}
		/**
		 * サポートされているかどうかを取得する
		 */
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			if(null == getSupportedList()) return false;
			return getSupportedList().size() > 0;
		}
		/**
		 * サポートされている解像度一覧を取得する。特にソートはされていない
		 * @return
		 */
		public abstract List<Camera.Size> getSupportedList();
		/**
		 * サポートされている解像度一覧を文字列で取得する。実際は一覧の番号を文字列にしているだけ
		 * @return
		 */
		public List<String> getSupportedListString(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(int i = 0;i < getSupportedList().size();i++){
				tmp.add(String.valueOf(i));
			}
			return tmp;
		}
		/**
		 * サポートされている解像度一覧を、設定メニューに表示する文字列として取得する
		 * @return
		 */
		public List<String> getSupportedListName(){
			ArrayList<String>tmp = new ArrayList<String>();
			for(Camera.Size s:getSupportedList()){
				String str = s.width + " x " + s.height;
				tmp.add(str);
			}
			return tmp;
		}
		/**
		 * ListPreferenceは選択番号を文字列で管理しているため、その文字列を数字に直す
		 * @return
		 */
		private int getPrefValue(){
			int ans;
			String s = mSharedPref.getString(mKey, mDefault);
			try{
				ans = Integer.parseInt(s);
			}
			catch(Exception e){
				e.printStackTrace();
				ans = 0;//Prefに設定されている値が変だったときは0を入れておく
			}
			ans = correctValue(ans);
			return ans;
		}
		public String getValueName(){
			return getSupportedListName().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を取得する
		 * @return
		 */
		@SuppressWarnings("unused")
		public Camera.Size getValue(){
			return getSupportedList().get(getPrefValue());
		}
		/**
		 * 現在Preferenceに設定されている解像度を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValue(){
			int prefVal = getPrefValue();
			if(MyDebug.DEBUG)Log.w(LOG_TAG, "prefVal = " + prefVal);
			setValue(prefVal);
		}
		/**
		 * 指定された順番にあるカメラの解像度を端末のカメラに反映させる
		 * @param pos
		 */
		protected abstract void setValue(int pos);
		/**
		 * 指定された解像度をPreferenceに書きこむ
		 * @param size
		 * 書きこむ解像度。指定された解像度をカメラがサポートしていない場合はデフォルトを書きこむ
		 * @param setNow
		 * true:すぐに端末のカメラに反映させる　false：させない
		 * @return
		 */
		@SuppressWarnings("unused")
		public boolean setValue(Camera.Size size, boolean setNow){
			if(false == isSupported()){
				return false;
			}
			int i = 0;
			boolean found = false;
			for(Camera.Size c:getSupportedList()){
				if(c.width == size.width && c.height == size.height){
					found = true;
					break;
				}
				i++;
			}
		    Editor ed = mSharedPref.edit();
		    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
		    String str = true != found ? mDefault:String.valueOf(i);
	    	ed.putString(mKey, str);
			ed.commit();
			if(true == setNow && true == found){
				setValue(i);
			}
			return found;
		}	
	}
	
	
	
	public class CameraSettingPictureSize extends CameraSettingCameraSize
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingPictureSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 1;
		}
		public CameraSettingPictureSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mPictureSize));
			mDefault = isSupported() ? "0":String.valueOf(NOT_SUPPORTED_INT);
			mDialogTitle = mContext.getString(R.string.picture_size_dialog_title);
			mTitle = mContext.getString(R.string.picture_size_title);
			mSummary = mContext.getString(R.string.picture_size_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			return param.getSupportedPictureSizes();
		}
		@Override
		protected void setValue(int pos) {
			// TODO Auto-generated method stu
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);
			int width = cm.get(pos).width;
			int height = cm.get(pos).height;
			Camera.Parameters param;
			param = mCamera.getParameters();
			param.setPictureSize(width, height);
			mCamera.setParameters(param);
		}

	}

	public class CameraSettingJpegThumbnailSize extends CameraSettingCameraSize
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingJpegThumbnailSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingJpegThumbnailSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mJpegThumbnailSize));
			mDefault = isSupported() ? "0":String.valueOf(NOT_SUPPORTED_INT);
			mDialogTitle = mContext.getString(R.string.jpeg_thumbnail_size_dialog_title);
			mTitle = mContext.getString(R.string.jpeg_thumbnail_size_title);
			mSummary = mContext.getString(R.string.jpeg_thumbnail_size_summary);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			Method method;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				method = Camera.Parameters.class.
						getMethod("getSupportedJpegThumbnailSizes", new Class[] {});
				return (List<Camera.Size>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}
		}
		@Override
		protected void setValue(int pos) {
			// TODO Auto-generated method stub
			if(isSupported() == false) return;
			
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);

			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method;
				method = Camera.Parameters.class.
							getMethod("setmJpegThumbnailSize", new Class[] {
																		int.class, 
																		int.class});
				method.invoke(param, cm.get(pos).width, cm.get(pos).height);
				mCamera.setParameters(param);
			} 
			catch (Exception e) {
			}
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
	}
	public class CameraSettingVideoSize extends CameraSettingCameraSize
										implements SetPreference{
		private static final String LOG_TAG = "CameraSettingVideoSize";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 11;
		}
		public CameraSettingVideoSize() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mVideoSize));
			mDefault = isSupported() ? "0":String.valueOf(NOT_SUPPORTED_INT);
			mDialogTitle = mContext.getString(R.string.video_size_dialog_title);
			mTitle = mContext.getString(R.string.video_size_title);
			mSummary = mContext.getString(R.string.video_size_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			return setPreferenceMain(pref, context);
		}
		@Override
		public List<Camera.Size> getSupportedList() {
			// TODO Auto-generated method stub
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method;
				method = Camera.Parameters.class.
									getMethod("getSupportedVideoSizes", new Class[] {});
				return (List<Camera.Size>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}	
		}
		@Override
		protected void setValue(int pos) {
			// TODO Auto-generated method stub
			List<Camera.Size> cm = getSupportedList();
			pos = correctValue(pos);

			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method;
				method = Camera.Parameters.class.getMethod("setVideoSizes", 
												new Class[] {
													int.class,
													int.class});
				method.invoke(param, cm.get(pos).width, cm.get(pos).height);
				mCamera.setParameters(param);
			} 
			catch (Exception e) {
			}	
		}

	}
	private abstract class CameraSettingInt extends CameraSettingBase{
		protected int mDefault;
		protected int	mRangeMin;
		protected int 	mRangeMax;
		
		protected int correctValue(int i){
			i = Math.max(i, getMinValue());
			i = Math.min(i, getMaxValue());
			return i;
		}
		protected int correctValue(float f){
			return correctValue((int)(f > 0f ? (f + 0.5f):(f - 0.5)));
		}
		/**
		 * 使用するレンジの最大値を取得する
		 * @return
		 */
		public abstract int getMaxValue();
		/**
		 * 使用するレンジの最小値を取得する
		 * @return
		 */
		public abstract int getMinValue();
		
		/**
		 * サポートされているかどうかを取得する
		 */
		public abstract boolean isSupported();
		@SuppressWarnings("unused")
		public int getValue(){
			float ans = mSharedPref.getFloat(mKey, mDefault);
			return correctValue(ans);
		}
		/**
		 * Preferenceに設定されている値を端末のカメラに反映させる
		 */
		@SuppressWarnings("unused")
		public void setValue(){
			setValue(getValue());
		}
		/**
		 * 指定された値を端末のカメラに反映させる
		 * @param zoom
		 */
		protected abstract  void setValue(int zoom);
		/**
		 * 指定された値をPreferenceに書きこむ
		 * @param i
		 * 書きこむ値
		 * @param setNow
		 * true:端末のカメラに反映させる　false:させない
		 * @return
		 * サポートしていない場合はfalse:
		 */
		@SuppressWarnings("unused")
		public boolean setValue(int i, boolean setNow){
			if(false == isSupported()){
				return false;
			}
			i = correctValue(i);
			
		    Editor ed = mSharedPref.edit();
	    	ed.putFloat(mKey, i);//SeekBarがfloatで扱うため
			ed.commit();
			if(true == setNow){
				setValue(i);
			}
			return true;
		}
	
	}
	public class CameraSettingJpegQuality extends CameraSettingInt
										implements SetPreference{
		private static final int MIN_VAL = 50;
		private static final int MAX_VAL = 85;
		private static final int DEF_VAL = 65;

		private static final String LOG_TAG = "CameraSettingJpegQuality";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 5;
		}
		public CameraSettingJpegQuality() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mJpegQuality));
			mDefault = isSupported() ? DEF_VAL:NOT_SUPPORTED_INT;
			mRangeMin = MIN_VAL;
			mRangeMax = MAX_VAL;
			mDialogTitle = mContext.getString(R.string.jpeg_quality_dialog_title);
			mTitle = mContext.getString(R.string.jpeg_quality_title);
			mSummary = mContext.getString(R.string.jpeg_quality_summary);
		}
		
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(!isSupported())return false;
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					mDefault);
	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, mDefault);
	        pref.addPreference(seekBarPref);
	        return true;
		}
		@Override
		public boolean isSupported() {
			// TODO Auto-generated method stub
			return getMinAPILevel() <= android.os.Build.VERSION.SDK_INT;
		}
		@Override
		public int getMaxValue() {
			// TODO Auto-generated method stub
			return MAX_VAL;
		}
		@Override
		public int getMinValue() {
			// TODO Auto-generated method stub
			return MIN_VAL;
		}
		@Override
		protected void setValue(int val) {
			// TODO Auto-generated method stub
			Camera.Parameters param;
			param = mCamera.getParameters();

			val = correctValue(val);
			param.setJpegQuality (val);
			mCamera.setParameters(param);
		}
	}

	public class CameraSettingZoom extends CameraSettingInt
											implements SetPreference{
		private static final String LOG_TAG = "CameraSettingZoom";
		private static final int MIN_VAL = 0;
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 8;
		}
		public CameraSettingZoom() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mZoom));
			mDefault = isSupported() ? 0:NOT_SUPPORTED_INT;
			mRangeMin = 0;
			mRangeMax = getMaxValue();
			mDialogTitle = mContext.getString(R.string.zoom_dialog_title);
			mTitle = mContext.getString(R.string.zoom_title);
			mSummary = mContext.getString(R.string.zoom_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					mDefault);
			if(!isSupported())return false;

	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, mDefault);
	        pref.addPreference(seekBarPref);
	        return true;
		}
		@Override
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"isZoomSupported", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return (boolean)Boolean.valueOf(o.toString());
			} 
			catch (Exception e) {
				return false;
			}
		}
		public boolean isSmoothZoomSupported(){
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"isSmoothZoomSupported", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return (boolean)Boolean.valueOf(o.toString());
			} 
			catch (Exception e) {
				return false;
			}
		}
		@Override
		public int getMaxValue(){
			if(false == isSupported()){
				return mDefault;
			}
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getMaxZoom", new Class[] {});
				Object o =  method.invoke(param, (Object [])null);
				return Integer.valueOf(o.toString());
			} 
			catch (Exception e) {
				return NOT_SUPPORTED_INT;
			}
		}

		@Override
		protected  void setValue(int zoom){
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"setZoom", new Class[] {int.class});
				zoom = correctValue(zoom);
				if(MyDebug.DEBUG)Log.i(LOG_TAG, "zoom setValue = " + zoom);
				method.invoke(param, zoom);
				mCamera.setParameters(param);
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public List<Integer>getZoomRatios(){
			if(false == isSupported()){
				return null;
			}
			try {
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"getZoomRatios", new Class[] {});
				return (List<Integer>) method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return null;
			}
			
		}
		@Override
		public int getMinValue() {
			// TODO Auto-generated method stub
			return MIN_VAL;
		}
	}

	public class CameraSettingExposurecompensation extends CameraSettingInt
													implements SetPreference{
		private static final String LOG_TAG = "CameraSettingExposurecompensation";
		@Override
		protected int getMinAPILevel() {
			// TODO Auto-generated method stub
			return 8;
		}
		public CameraSettingExposurecompensation() {
			// TODO Auto-generated method stub
			mKey = addCameraID(mContext.getString(R.string.mExposurecompensation));
			mDefault = isSupported() ? 0:NOT_SUPPORTED_INT;
			mRangeMin = getMinValue();
			mRangeMax = getMaxValue();
			mDialogTitle = mContext.getString(R.string.exposure_compensation_dialog_title);
			mTitle = mContext.getString(R.string.exposure_compensation_title);
			mSummary = mContext.getString(R.string.exposure_compensation_summary);
		}
		@Override
		public boolean setPreference(PreferenceCategory pref, Context context) {
			// TODO Auto-generated method stub
			if(!isSupported())return false;
			if(MyDebug.DEBUG) Log.i(LOG_TAG, mKey + " "+
					mRangeMin + " "+
					mRangeMax + " " +
					mDefault);
	        SeekBarPreference seekBarPref = new SeekBarPreference(context);
	        seekBarPref.setDialogTitle(mDialogTitle);
	        seekBarPref.setKey(mKey);
	        seekBarPref.setTitle(mTitle);
	        seekBarPref.setSummary(mSummary);
	        seekBarPref.setRange(mRangeMin, mRangeMax, mDefault);
	        pref.addPreference(seekBarPref);
	        return true;
		}
		
		@Override
		public boolean isSupported(){
			if(getMinAPILevel() > android.os.Build.VERSION.SDK_INT) return false;
			return (getMinValue() != 0 && getMaxValue() != 0);
		}
		@Override
		public int getMinValue(){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
											"getMinExposureCompensation", new Class[] {});
				return (Integer)method.invoke(param, (Object [])null);
			} 
			catch (Exception e) {
				return 0;
			}
		}
		@Override
		public int getMaxValue(){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
					"getMaxExposureCompensation", new Class[] {});
				return (Integer)method.invoke(param, (Object[])null);
			} 
			catch (Exception e) {
				return 0;
			}
		}	
		public Float getStep(){
			if(false ==  isSupported()){
				return 0f;
			}
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
											"getExposureCompensationStep", new Class[] {});
				return (Float)method.invoke(param, (Object[])null);
			} 
			catch (Exception e) {
				return 0f;
			}
		}
		
		@Override
		protected void setValue(int value){
			try{
				Camera.Parameters param;
				param = mCamera.getParameters();

				Method method = Camera.Parameters.class.getMethod(
						"setExposureCompensation", new Class[]{int.class});
				value = correctValue(value);
				method.invoke(param, value);

				mCamera.setParameters(param);
			} 
			catch(Exception e){
				e.printStackTrace();
			}
		}

	}	

	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
    public static int cameraPos2ID(int camPos){
    	if(CAMERA_FACING_UNKNOWN == camPos){
			return INVALID_CAMERA_ID;
    	}

    	if(hasMultipleCamera() == false){
			return INVALID_CAMERA_ID;
    	}
    	
    	int cNum = getNumberOfCameras();
    	Object cameraInfo;
		try {
			cameraInfo = clazzCameraInfo.newInstance();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return INVALID_CAMERA_ID;
		}
    	for(int i = 0;i < cNum;i++){
    		Field f;
    		try {
				mGetCameraInfo.invoke(null, i, cameraInfo);
	        	f = clazzCameraInfo.getDeclaredField("facing");
    		} 
    		catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return INVALID_CAMERA_ID;
			}

        	try{
        		int facing = (Integer)f.get(cameraInfo);
        		if(camPos == facing){
        			return i;
        		}
        	}
    		catch(Exception e){
    			e.printStackTrace();
    		}
    	}
    	return DEFAULT_CAMERA_ID;
    }	

	private static final String LOG_TAG = "CameraSetting";
	private static SharedPreferences mSharedPref = null;
	private Context mContext;
	private Camera mCamera = null;
	
	public static final String NOT_SUPPORTED_STRING = "";
	public static final int NOT_SUPPORTED_INT = Integer.MIN_VALUE;

	public CameraSettingAntibanding mAntibanding = null;
	public CameraSettingColorEffect mColorEffect = null;
	public CameraSettingFlashMode mFlashMode = null;
	public CameraSettingFocusMode mFocusMode = null;
	public CameraSettingSceneMode mSceneMode = null;
	public CameraSettingWhiteBalance mWhiteBalance = null;
	public CameraSettingPictureSize mPictureSize = null;
	public CameraSettingJpegThumbnailSize mJpegThumbnailSize = null;
	public CameraSettingJpegQuality mJpegQuality = null;
	public CameraSettingVideoSize mVideoSize = null;
	public CameraSettingZoom mZoom = null;
	public CameraSettingExposurecompensation mExposurecompensation = null;
	
	public ArrayList <SetPreference> mSettingPreferenceList = new ArrayList<SetPreference>();
	public ArrayList<CameraSettingBase> mCameraSettingList = new ArrayList<CameraSettingBase>();

	public CameraSetting(Context context){
		mContext = context;
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	private void setCameraPos(int camPos){
		if(false == hasMultipleCamera()) camPos = CAMERA_FACING_UNKNOWN;
		
	    Editor ed = mSharedPref.edit();
	    //ListPreferenceは選択番号を文字列で管理しているため、文字列にする
    	ed.putInt(mContext.getString(R.string.mCameraPos), camPos);
		ed.commit();
	}
	
	private int getCameraPos(){
		if(false == hasMultipleCamera())return CAMERA_FACING_UNKNOWN;
		return mSharedPref.getInt(mContext.getString(R.string.mCameraPos), CAMERA_FACING_BACK);
	}

	public SharedPreferences getPreference(){
		return mSharedPref;
	}

	/**
	 * キーにカメラIDを付加する
	 * @param key
	 * @return
	 */
	public String addCameraID(String key){
		return key + "_" + String.valueOf(getCameraPos());
	}
	
	
    public static final int INVALID_CAMERA_ID = -1;
    public static final int DEFAULT_CAMERA_ID = 0;
    private static Method mGetCameraInfo = null;
    private static Class<?> clazzCameraInfo = null;
    private static Method mGetNumberOfCameras = null;
    private static Method mOpen = null;

    public static final int CAMERA_FACING_UNKNOWN = -1;
    public static int CAMERA_FACING_BACK = CAMERA_FACING_UNKNOWN;
    public static int CAMERA_FACING_FRONT = CAMERA_FACING_UNKNOWN;
    
    static {  
        initCompatibility();  
    };  
  
    private static void initCompatibility() {
        try{
        	mGetNumberOfCameras = Camera.class
        			.getMethod("getNumberOfCameras", new Class[] {});
        }
        catch (NoSuchMethodException nsme) {
        	mGetNumberOfCameras = null;
        }

        try{
        	clazzCameraInfo = Class.forName("android.hardware.Camera$CameraInfo");
        	Field f = clazzCameraInfo.getDeclaredField("CAMERA_FACING_BACK");
        	CAMERA_FACING_BACK = (Integer)f.get(null);
        	f = clazzCameraInfo.getDeclaredField("CAMERA_FACING_FRONT");
        	CAMERA_FACING_FRONT = (Integer)f.get(null);
        	if(MyDebug.DEBUG){
        		Log.d(LOG_TAG, "FrontID = " + String.valueOf(CAMERA_FACING_FRONT) + " BackID = " + String.valueOf(CAMERA_FACING_BACK));
        	}
        	mGetCameraInfo = Camera.class
        			.getMethod("getCameraInfo", new Class[]{
        											int.class,
        											clazzCameraInfo});
        }
        catch (Exception e) {
        	e.printStackTrace();
        	mGetCameraInfo = null;
        }
        
        try{
        	mOpen = Camera.class.getMethod("open", new Class[]{int.class});
        }
        catch(NoSuchMethodException nsme) {
        	mOpen = null;
        }
    }
    
    /**
     * 内蔵カメラの個数を返す
     * @return
     * -1:複数カメラを未サポート(APIレベル　8以下)
     * 0以上：カメラの数
     */
    public static int getNumberOfCameras(){
    	
    	if(null == mGetNumberOfCameras){
    		return -1;
    	}
    	int ans = -1;
		try{
			Object o = mGetNumberOfCameras.invoke(null);
			ans = Integer.valueOf(o.toString());
		}
		catch(Exception e){
			e.printStackTrace();
			ans = -1;
		}
		return ans;
	}
    
    /**
     * 複数のカメラを持っているかどうか
     * @return
     * true:持っている　false:持っていない
     */
    public static boolean hasMultipleCamera(){
    	return getNumberOfCameras() >= 2;
    }
    /**
     * カメラを開く
     * @return
     */
    public boolean openCamera(){
    	boolean result = false;
    	
    	releaseCamera();
    	
    	int cameraPos = getCameraPos();
    	int cameraId = cameraPos2ID(cameraPos);
    	if(cameraId < 0 || !hasMultipleCamera()){
    		try{
    			mCamera = Camera.open();
        		cameraPos = CAMERA_FACING_UNKNOWN;
        		result = true;
    		}
    		catch(Exception e){
    			mCamera = null;
    			e.printStackTrace();
    			return false;
    		}
    	}
    	else{
    		if(null == mOpen){
    			mCamera = null;
    		}
    		try{
    			Object o = mOpen.invoke(null, cameraId);
    			mCamera = (Camera)o;
        		result = true;
    		}
    		catch(Exception e){
    			e.getCause().printStackTrace();
    			mCamera = null;
    		}
    	}
    	if(true == result){
    		
    		//カメラ設定情報を作成
    		mAntibanding = new CameraSettingAntibanding();
    		mColorEffect = new CameraSettingColorEffect();
    		mFlashMode = new CameraSettingFlashMode();
    		mFocusMode = new CameraSettingFocusMode();
    		mSceneMode = new CameraSettingSceneMode();
    		mWhiteBalance = new CameraSettingWhiteBalance();
    		mPictureSize = new CameraSettingPictureSize();
    		mJpegThumbnailSize = new CameraSettingJpegThumbnailSize();
    		mJpegQuality = new CameraSettingJpegQuality();
    		mVideoSize = new CameraSettingVideoSize();
    		mZoom = new CameraSettingZoom();
    		mExposurecompensation = new CameraSettingExposurecompensation();

    		//カメラ設定一欄をListに登録
    		mCameraSettingList.add(mColorEffect);
    		mCameraSettingList.add(mSceneMode);
    		mCameraSettingList.add(mPictureSize);
    		mCameraSettingList.add(mVideoSize);
    		mCameraSettingList.add(mJpegQuality);
    		mCameraSettingList.add(mJpegThumbnailSize);
    		mCameraSettingList.add(mZoom);
    		mCameraSettingList.add(mExposurecompensation);
    		mCameraSettingList.add(mFlashMode);
    		mCameraSettingList.add(mFocusMode);
    		mCameraSettingList.add(mWhiteBalance);
    		mCameraSettingList.add(mAntibanding);


    		//カメラPreference設定一欄をListに登録
    		mSettingPreferenceList.add(mColorEffect);
    		mSettingPreferenceList.add(mSceneMode);

    		mSettingPreferenceList.add(mPictureSize);
    		mSettingPreferenceList.add(mVideoSize);
    		mSettingPreferenceList.add(mJpegQuality);
    		mSettingPreferenceList.add(mJpegThumbnailSize);

    		mSettingPreferenceList.add(mZoom);
    		mSettingPreferenceList.add(mExposurecompensation);
    		mSettingPreferenceList.add(mFlashMode);
    		mSettingPreferenceList.add(mFocusMode);
    		mSettingPreferenceList.add(mWhiteBalance);
    		mSettingPreferenceList.add(mAntibanding);

    	}
    	if(false == result) releaseCamera();
    	return result;
    }
    /**
     * 開かれているカメラを取得する
     * @return
     */
    public Camera getCamera(){
    	return mCamera;
    }
    /**
     * カメラを閉じる
     */
    public void releaseCamera(){
    	if(null!= mSettingPreferenceList){
    		mSettingPreferenceList.clear();
    	}
    	if(null != mCameraSettingList){
    		mCameraSettingList.clear();
    	}
		mAntibanding = null;
		mColorEffect = null;
		mFlashMode = null;
		mFocusMode = null;
		mSceneMode = null;
		mWhiteBalance = null;
		mPictureSize = null;
		mJpegThumbnailSize = null;
		mJpegQuality = null;
		mVideoSize = null;
		mZoom = null;
		mExposurecompensation = null;
		
    	if(null != mCamera){
    		mCamera.release();
    		mCamera = null;
    	}
    }
    public boolean isCameraOpen(){
    	return null != mCamera;
    }
    
}
