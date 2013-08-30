package com.nekomeshi312.selfcamera;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.google.ads.AdRequest.ErrorCode;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PictureViewerActivity extends Activity {
	private static final String LOG_TAG = "PictureViewerActivity";
	private String mTmpPicFilename = null;
	private Bitmap mPicBitmap = null;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate");
        setContentView(R.layout.pic_viewer_layout);
        
        AdView adView = (AdView)this.findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest();
        adView.loadAd(adRequest);
		adView.setAdListener(new AdListener(){

			@Override
			public void onDismissScreen(Ad arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, "Failed to receive an Ad.  Requesting a new one..." + arg1);
                arg0.stopLoading();
                arg0.loadAd(new AdRequest());
			}

			@Override
			public void onLeaveApplication(Ad arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onPresentScreen(Ad arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onReceiveAd(Ad arg0) {
				// TODO Auto-generated method stub
			}
			
		});        
       
        Bundle extras = getIntent().getExtras();
        if(null != extras){
        	mTmpPicFilename = extras.getString(getString(R.string.pic_tmp_file));
        	if(MyDebug.DEBUG){
        		int picWidth = extras.getInt(getString(R.string.pic_width));
        		int picHeight = extras.getInt(getString(R.string.pic_height));
            	Toast.makeText(this, "width" + picWidth + "height" + picHeight + " size " + mTmpPicFilename, Toast.LENGTH_SHORT).show();
        	}
        	//Activityが戻ったときにファイル名がわかるようsharedprefにいれておく
        	SharedPreferences pref = getSharedPreferences(getString(R.string.pic_pref), MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(getString(R.string.pic_pref_filename), mTmpPicFilename);
            editor.commit();
        }
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		ImageView iv = (ImageView)findViewById(R.id.PictureViewMain);
		iv.setImageBitmap(null);
		if(null != mPicBitmap) mPicBitmap.recycle();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Button okButton = (Button)findViewById(R.id.PicViewButtonOK);
		okButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setResult(RESULT_OK);
				finish();
				
			}
		});
		Button cancelButton = (Button)findViewById(R.id.PicViewButtonCancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setResult(RESULT_CANCELED);
				finish();
		}
		});
		boolean loaded = false;
		if(null == mTmpPicFilename){//何故か撮影画像が読めなかったとき
		}
		else{
			//画面サイズを取得
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			float screenWidth = (float) metrics.widthPixels;
			float screenHeight = (float) metrics.heightPixels;
			//画像のサイズを先読み
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(mTmpPicFilename, opt);
			int ww = Math.max((int) (opt.outWidth/screenWidth), 1);
			int hh = Math.max((int) (opt.outHeight/screenHeight), 1);
			
			//縮小サイズを計算
			opt.inSampleSize = Math.max(ww, hh);
			//読み直し
			opt.inJustDecodeBounds = false;
			mPicBitmap = BitmapFactory.decodeFile(mTmpPicFilename, opt);
			if(null != mPicBitmap){
				//ImageViewに設定
				ImageView iv = (ImageView)findViewById(R.id.PictureViewMain);
				iv.setImageBitmap(mPicBitmap);
				loaded = true;
			}
		}
		if(false == loaded){//画像が読めなかったときはOKを押せなくする
			Toast.makeText(this, R.string.pic_load_error, Toast.LENGTH_SHORT).show();
			okButton.setEnabled(false);
		}

		super.onResume();
	}
}
