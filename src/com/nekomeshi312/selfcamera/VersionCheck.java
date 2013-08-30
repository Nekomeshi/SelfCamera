package com.nekomeshi312.selfcamera;

import java.lang.reflect.Field;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class VersionCheck {
	public static boolean isTablet(Context context){
		try{
			Class<?> c = Class.forName("android.content.res.Configuration");
	    	Field f = c.getDeclaredField("SCREENLAYOUT_SIZE_XLARGE");
	    	int SCREENLAYOUT_SIZE_XLARGE = (Integer)f.get(null);
		    return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
		    											== SCREENLAYOUT_SIZE_XLARGE;
		}
		catch(Exception e){
			return false;
		}
	}
	public static boolean isICS(){
	    return Build.VERSION.SDK_INT >= 14;
	}
	public static boolean isICSTablet(Context context){
		return isICS() && isTablet(context);
	}
	
	public static boolean isHoneycomb() {
	    return Build.VERSION.SDK_INT >= 11;
	}	 
	public static boolean isHoneycombTablet(Context context) {
	    return isHoneycomb() && isTablet(context);
	}
}
