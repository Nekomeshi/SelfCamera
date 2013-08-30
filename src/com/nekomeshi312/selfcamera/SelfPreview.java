package com.nekomeshi312.selfcamera;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.nekomeshi312.copyassets2local.CopyAssets2Local;
import com.nekomeshi312.selfcamera.jni.CpuCheck;
import com.nekomeshi312.selfcamera.jni.FaceDetect;

import android.media.FaceDetector;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

public class SelfPreview extends 	SurfaceView 
						implements	SurfaceHolder.Callback,
									Camera.PreviewCallback,
									Camera.PictureCallback{
	class FaceDetectInitException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3155173038223360887L;
		private String mMessage = null;
		/* (non-Javadoc)
		 * @see java.lang.Throwable#getMessage()
		 */
		@Override
		public String getMessage() {
			// TODO Auto-generated method stub
			return null == mMessage ? super.getMessage():mMessage;
		}
		public FaceDetectInitException() {
			super();
		}
	}
	private interface FaceDetection{
		/**
		 * 顔認識の初期化を行う
		 */
		public void init()throws IOException, FaceDetectInitException;
		/**
		 * 顔認識を実行する
		 * @param data
		 * カメラかキャプチャされたYUV420の画像データ
		 * @param camera
		 * カメラオブジェクト
		 * @param orientation
		 * 画面の向き
		 * @return
		 * 成功　顔の四隅のRectのArrayList
		 * 失敗　null or ArrayListのサイズが0
		 */
		public ArrayList <Rect> detect(byte[] data, Camera camera, int orientation);
		public void destroy();
	}
	private class AndroidFaceDetect implements FaceDetection{
		FaceDetector.Face[] mFaces = new FaceDetector.Face[FACE_DETECT_MAX_NUM]; // 結果受け取り用
		static final int FACE_WIDTH = 300;
		private int mFaceWidth = FACE_WIDTH;
		private int mFaceHeight;
		private Bitmap mBmpLandscape = null;
		private Bitmap mBmpPortrait = null;
		private Bitmap mBmp = null;
		private int [] mRGBs = null;
		private int [] mRGBtmp = null;
		private double mImageRatio;
		@Override
		public void init()throws IOException {
			// TODO Auto-generated method stub
			mImageRatio = (double)mPreviewWidth/(double)mFaceWidth;
			mFaceHeight = (int) (mPreviewHeight / mImageRatio) ;
			if(0 != (mFaceHeight % 2))mFaceHeight++;	//widthは偶数でなければいけない。portrait時は90°回転するため、heightも偶数になるようにしておく
			
			if(MyDebug.DEBUG) Log.i(LOG_TAG, "face size = " + mFaceWidth + ":" + mFaceHeight);
			mBmpLandscape = Bitmap.createBitmap(mFaceWidth, mFaceHeight, Bitmap.Config.RGB_565);  
			mBmpPortrait = Bitmap.createBitmap(mFaceHeight, mFaceWidth, Bitmap.Config.RGB_565);  
			mRGBs = new int[mFaceWidth*mFaceHeight];
			mRGBtmp = new int[mFaceWidth*mFaceHeight];
		}
		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			if(null != mBmpLandscape) mBmpLandscape.recycle();
			if(null != mBmpPortrait) mBmpPortrait.recycle();
			
		}
		@Override
		public ArrayList <Rect> detect(byte[] data, Camera camera, int orientation) {
			// TODO Auto-generated method stub
			//FaceDetector バージョン
			ArrayList <Rect> rct = null;
			long tm = 0;
			//YUV420をRGBに変換し画像を縮小する。中でorientaionにあわせて画像を回転させる
			if(MyDebug.DEBUG) tm = System.currentTimeMillis();
			if(MyDebug.DEBUG) Log.d(LOG_TAG, "prev size = " + String.valueOf(mPreviewWidth) + ":" + String.valueOf(mPreviewHeight));
			if(MyDebug.DEBUG) Log.d(LOG_TAG, "face size = " + String.valueOf(mFaceWidth) + ":" + String.valueOf(mFaceHeight));
			
			decodeYUV(data, 
					mPreviewWidth, mPreviewHeight, 
					mFaceWidth, mFaceHeight, 
					orientation);
			if(MyDebug.DEBUG) Log.i(LOG_TAG, "DecTime = " + (System.currentTimeMillis() - tm));

			//画面の向きで使用するビットマップの向きを切り替える
			if(orientation == FaceDetect.ROTATION_0 ||
					orientation == FaceDetect.ROTATION_180){
				mBmp = mBmpPortrait;
			}
			else{
				mBmp = mBmpLandscape;
			}
			final int w = mBmp.getWidth();
			final int h = mBmp.getHeight();
			if(MyDebug.DEBUG)Log.d(LOG_TAG, "w = " + String.valueOf(w) + " h = " + String.valueOf(h));
			// 変換した画素からビットマップにセット 
	        mBmp.setPixels(mRGBs, 0, w, 0, 0, w, h); 

	        //認識実行
			if(MyDebug.DEBUG) tm = System.currentTimeMillis();
	        FaceDetector detector = new FaceDetector(
	        		w, // ビットマップの幅
	        		h, // ビットマップの高さ
				    mFaces.length); // ここでは、最大4つの顔認識結果を受け取れるように指定
			int num = detector.findFaces(mBmp, mFaces); // 顔認識実行
			if(MyDebug.DEBUG) Log.i(LOG_TAG, "AfterDetect num = " + num + "time = "+ (System.currentTimeMillis() - tm));

			if(null !=  mFaceMarkView){
				rct = new ArrayList<Rect>();
				for(int i = 0;i < num;i++){
					Rect r = new Rect();
					final float eyesDistance = mFaces[i].eyesDistance();
					PointF midPoint = new PointF(0, 0);
			        mFaces[i].getMidPoint(midPoint); // 顔認識結果を取得
			        r.left = (int) ((midPoint.x - eyesDistance) *mImageRatio/mWidthRatio + 0.5);
			        r.top = (int) ((midPoint.y - eyesDistance) * mImageRatio/mHeightRatio + 0.5);
			        r.right = (int) ((midPoint.x + eyesDistance) *mImageRatio/mWidthRatio + 0.5);
			        r.bottom = (int) ((midPoint.y + eyesDistance) *mImageRatio/mHeightRatio + 0.5);

			        //回転した画像に対する認識結果を戻す
		    		final int width = r.right - r.left;
		    		final int height = r.bottom - r.top;
			    	if(FaceDetect.ROTATION_90 == orientation){
			    		r.bottom = (int) (r.top + height*1.3 + 0.5);
			    	}
			    	else if(FaceDetect.ROTATION_270 == orientation){
			    		r.left = (int) (mPreviewWidth/mWidthRatio - r.right + 0.5);
			    		r.right = r.left + width;
			    		r.bottom = (int) (mPreviewHeight/mHeightRatio - r.top + 0.5);
			    		r.top = (int) (r.bottom - height*1.3 + 0.5);
			    	}
			    	else if(FaceDetect.ROTATION_0 == orientation){
			    		int tmp = r.left;
			    		r.left = (int) (r.top*mHeightRatio/mWidthRatio + 0.5);
			    		r.top = (int)((mPreviewHeight - (tmp + height)*mWidthRatio)/mHeightRatio + 0.5);
			    		r.right = (int) (r.left + width*1.3 + 0.5);
			    		r.bottom = r.top + height;
			    	}
			    	else if(FaceDetect.ROTATION_180 == orientation){
			    		int tmp = r.left;
						r.left = (int)((mPreviewHeight - r.top*mHeightRatio)/mWidthRatio + 0.5);
						r.top = (int)(tmp*mWidthRatio/mHeightRatio + 0.5);
			    		r.right = (int) (r.left + width*1.3 + 0.5);
			    		r.bottom = r.top + height;
			    	}
			    	else{
			    		Log.w(LOG_TAG, "Illegal argment orientation =  " + orientation);
			    		continue;
			    	}

					rct.add(r);
					if(MyDebug.DEBUG)Log.i(LOG_TAG, 	"left = " + r.left + 
													" top = " + r.top + 
													" right = " + r.right + 
													" bottom = " + r.bottom);
				}
			}
			return rct;
		}
		
		/**
		 * YUV420をRGBに変換し、縮小する。orientaionにあわせて画像を回転させる
		 * @param yuvDataArray
		 * カメラでキャプチャされたYUV420画像
		 * @param width
		 * オリジナルの幅
		 * @param height
		 * オリジナルの高さ
		 * @param faceWidth
		 * 縮小後の幅
		 * @param faceHeight
		 * 縮小後の高さ
		 * @param orientation
		 * 画面の向き
		 * @throws NullPointerException
		 * @throws IllegalArgumentException
		 */
	    private void decodeYUV(byte[] yuvDataArray, 
	    					int width, int height,
	    					int faceWidth, int faceHeight, 
	    					int orientation)
	    					throws NullPointerException{
	    	final int size = width * height;
	    	boolean isOutOfBounds = false;
	    	if (yuvDataArray == null)
	    			throw new NullPointerException("buffer yuvDataArray is null");

	    	final double imageRatio = (double)width/(double)faceWidth;

	    	if(MyDebug.DEBUG) Log.i(LOG_TAG, "w = " + faceWidth + " h = " + faceHeight + " ratio = " + imageRatio);
	    	int Y, Cr = 0, Cb = 0;
	    	int idx = 0;
	    	for (int jj = 0; jj < faceHeight; jj++) {
	    		final int j = (int)(jj*imageRatio);
	    		final int pdx = j * width;
	    		final int jDiv2 = j >> 1;

	    		for (int ii = 0; ii < faceWidth; ii++) {
	    			int R = 0;
	    			int G = 0;
	    			int B = 0;
	    			if(!isOutOfBounds){
	    				try{
	    	    			final int i = (int)(ii*imageRatio);
	    	    			Y = yuvDataArray[pdx + i];
	    	    			if (Y < 0)
	    	    				Y += 255;
	    	    			if ((i & 0x1) != 1) {
	    	    				int cOff = size + jDiv2 * width + (i >> 1) * 2;
	    	    				Cb = yuvDataArray[cOff];
	    	    				if (Cb < 0) {
	    	    					Cb += 127;
	    	    				}
	    	    				else {
	    	    					Cb -= 128;
	    	    				}
	    	    				Cr = yuvDataArray[cOff + 1];
	    	    				if (Cr < 0) {
	    	    					Cr += 127;
	    	    				}
	    	    				else {
	    	    					Cr -= 128;
	    	    				}
	    	    			}
	    	    			R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
	    	    			if (R < 0) {
	    	    				R = 0;
	    	    			}
	    	    			else if (R > 255) {
	    	    				R = 255;
	    	    			}

	    	    			G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >> 3) + (Cr >> 4) + (Cr >> 5);
	    	    			if (G < 0) {
	    	    				G = 0;
	    	    			}
	    	    			else if (G > 255) {
	    	    				G = 255;
	    	    			}
	    	    			B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
	    	    			if (B < 0) {
	    	    				B = 0;
	    	    			}
	    	    			else if (B > 255) {
	    	    				B = 255;
	    	    			}
	    				}
	    				catch(ArrayIndexOutOfBoundsException e){ //height を偶数にするためfaceHeight を+1することがある。その場合outofboundsが発生するため
	    														//残りの画素は0x00(黒)で埋めておく
	    					if(MyDebug.DEBUG) e.printStackTrace();
	    					isOutOfBounds = true;
	    				}
	    			}
	    			
    				mRGBs[idx++] = 0xff000000 + (B << 16) + (G << 8) + R;
	    		}
	    	}
	    	//認識出来るように画像を回転させる
	    	rot(orientation);
	    }
	    /**
	     * 画像を回転させる
	     * @param orientation
	     */
	    private void rot(int orientation){
	    	//ここでディスプレィの向きに合わせて回転する
	    	if(FaceDetect.ROTATION_90 == orientation){
	    	}
	    	else if(FaceDetect.ROTATION_270 == orientation){
		    	int i, j;
   				final int pix = mFaceWidth*mFaceHeight;
   				for(i = pix - 1, j = 0;i > j ;i--, j++){
   					int a = mRGBs[i];
   					mRGBs[i] = mRGBs[j];
    				mRGBs[j] = a;
    			}
	    	}
	    	else if(FaceDetect.ROTATION_0 == orientation){
	    		final int w = mFaceHeight;
	    		final int h = mFaceWidth;
    			int i = 0;
    			for(int x = w - 1;x >= 0 ;x--){
    				for(int y = 0;y < h;y++){
    					mRGBtmp[y*w + x] = mRGBs[i];
    					i++;
    				}
    			}
    			int a [];
    			a = mRGBs;
    			mRGBs = mRGBtmp;
    			mRGBtmp = a;
	    	}
	    	else{
	    		final int w = mFaceHeight;
	    		final int h = mFaceWidth;
	    		int i = 0;
    			for(int x = 0;x < w;x++){
    				for(int y = h-1;y >= 0;y--){
    					mRGBtmp[y*w + x] = mRGBs[i];
    					i++;
    				}
    			}
    			int a [];
    			a = mRGBs;
    			mRGBs = mRGBtmp;
    			mRGBtmp = a;
	    	}
	    }


	}
	private class OpenCVFaceDetect implements FaceDetection{
		private static final String CASCADE_ZIP_NAME = "haarcascade_frontalface_default.zip";
		private static final String CASCADE_NAME = "haarcascade_frontalface_default.xml";
//		private static final String CASCADE_NAME = "haarcascade_frontalface_alt.xml";
//		private static final String CASCADE_NAME = "haarcascade_frontalface_alt_tree.xml";
//		private static final String CASCADE_NAME = "haarcascade_frontalface_alt2.xml";
		
		private FaceDetect m_FaceDetecter = new FaceDetect();
		private final int[] mFaceDetectResult = new int[FACE_DETECT_MAX_NUM*4];

		private boolean mInitOK = false;
		@Override
		public void init()throws IOException, FaceDetectInitException {
			// TODO Auto-generated method stub
			mInitOK = false;
			if(MyDebug.DEBUG)Log.i(LOG_TAG, "before faceDetect init");
			try {
				//1MBを超えるファイルはassetsでは扱えない
//				CopyAssets2Local.copy(mContext, CASCADE_NAME);//認識テンプレートをローカルにコピー
				CopyAssets2Local.extractZipFiles(mContext, CASCADE_ZIP_NAME);//認識テンプレートをローカルに解凍
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw e;
			}
			final String cascadeNamePath = "/data/data/" + mContext.getPackageName() + "/files/" + CASCADE_NAME;
			byte [] errorcode = new byte[]{0};
			if(false == m_FaceDetecter.init(mPreviewWidth, mPreviewHeight, cascadeNamePath, errorcode)){
				FaceDetectInitException e = new FaceDetectInitException();
				e.mMessage = mMainActivity.getString(R.string.ErrorInitFaceDetection) + " (" + errorcode[0] + ")";
				throw e;
			}
			if(MyDebug.DEBUG)Log.i(LOG_TAG, "CascadeName = " + cascadeNamePath);
			mInitOK = true;
		}
		@Override
		public ArrayList <Rect> detect(byte[] data, Camera camera, int orientation) {
			// TODO Auto-generated method stub
			//OpenCVバージョン
			ArrayList <Rect> rct = null;
			if(false == mInitOK) return rct;
			long tm;
			if(MyDebug.DEBUG)	tm = System.currentTimeMillis();
			int num = m_FaceDetecter.detectFacePos(data, 
													mFaceDetectResult, 
													FACE_DETECT_MAX_NUM, 
													orientation);
			if(MyDebug.DEBUG){
				Log.i(LOG_TAG, "DetectTime = " + (System.currentTimeMillis() - tm) + " last time = " + tm);		
			}
			
			if(null !=  mFaceMarkView){
				rct = new ArrayList<Rect>();
				for(int i = 0;i < num;i++){
					int j = i << 2;
					Rect r = new Rect();
					//previewサイズとスクリーンサイズのズレを修正
					r.left = (int)(mFaceDetectResult[j]/mWidthRatio + 0.5f);
					r.top = (int)(mFaceDetectResult[j+1]/mHeightRatio + 0.5f);
					r.right = (int)(mFaceDetectResult[j+2]/mWidthRatio + 0.5f);
					r.bottom = (int)(mFaceDetectResult[j+3]/mHeightRatio + 0.5f);
					rct.add(r);
					if(MyDebug.DEBUG) Log.i(LOG_TAG, i + ":" + 
									"(" + mFaceDetectResult[j + 0] + ", " + mFaceDetectResult[j + 1] + ")/" +
									"(" + mFaceDetectResult[j + 2] + ", " + mFaceDetectResult[j + 3] + ")");
				}
			}
			return rct;
		}
		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			//特に何もすることなし
		}
	}
	private static final String LOG_TAG = "SelfPreview";
	private Context mContext = null;
	private Handler mHandler = null;
	private SurfaceHolder mHolder = null;
	
	private FaceDetection mFaceDetectMain = null;
	private byte[] mBuffer = null;

	private final int FACE_DETECT_MAX_NUM = 4;
	private FaceMarkView mFaceMarkView = null;
	private boolean mAutoFocusRunning = false;
	
	private boolean mShootOnFramed = false;

	private SelfCameraActivity mMainActivity = null;
	
	/**
	 * エンジン名の一欄を取得する
	 * @return
	 * エンジン一覧
	 */
	public ArrayList<String>getEngineName(){
		ArrayList<String>list = new ArrayList<String>();
		CpuCheck cpu = new CpuCheck();
		if(cpu.NeonCheck()){
			list.add(mContext.getString(R.string.OpenCVEngineName));
		}
		list.add(mContext.getString(R.string.AndroidEngineName));
		return list;
	}
	/**
	 * エンジンのentryvalue一覧を取得する
	 * @return
	 * entryvalue一覧
	 */
	public ArrayList<String>getEngineEntryValue(){
		ArrayList<String>list = new ArrayList<String>();
		CpuCheck cpu = new CpuCheck();
		if(cpu.NeonCheck()){
			list.add(mContext.getString(R.string.OpenCVEngineEntryValue));
		}
		list.add(mContext.getString(R.string.AndroidEngineEntryValue));
		return list;
	}
	
	
	/**
	 * UI以外のthreadからToastを投げる
	 * @param id
	 * 表示する文字列リソースID
	 */
	private void threadToast(int id){
		threadToast(mContext.getString(id));
	}
	/**
	 * UI以外のthreadからToastを投げる
	 * @param msg
	 * 表示する文字列
	 */
	private void threadToast(String msg){
		class ToastRunnable implements Runnable{
			String mMsg = null;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				ToastMaster.makeText(mContext, mMsg, Toast.LENGTH_SHORT).show();
			}
		}
		ToastRunnable r = new ToastRunnable();
		r.mMsg = msg;
		mHandler.post(r);
	}
	
	public void setMainActivity(SelfCameraActivity activity){
		mMainActivity = activity;
	}
	/**
	 * @param mFaceMarkView the mFaceMarkView to set
	 */
	public void setFaceMarkView(FaceMarkView mFaceMarkView) {
		this.mFaceMarkView = mFaceMarkView;
	}
	private Method mAddCallbackBuffer = null;
	private Method mSetPreviewCallbackWithBuffer = null;
	private int mPreviewWidth;
	private float mWidthRatio = 0f;
	private int mPreviewHeight;
	private float mHeightRatio = 0f;
	private ReentrantReadWriteLock mCamLock = new ReentrantReadWriteLock();

	ProgressDialog mCameraOpenProgress = null;
	Thread		 	mCameraOpenThread = null;
	
	/**
	 * 使用する認識エンジンを選択する
	 */
	private void setEngine(){
		CpuCheck cpu = new CpuCheck();

		boolean neonAvailable = false;
		
		//認識エンジンを切り替えられるように
		String tmp = SelfCameraSettingActivity.getEngine(mContext, getEngineEntryValue());
		if(null == tmp){
			neonAvailable = true;
		}
		else{
			neonAvailable = tmp.equals(mContext.getString(R.string.OpenCVEngineEntryValue));
		}
		//OpenCVが選択されていてもNEONがない場合はAndroidに切り替え
		if(neonAvailable) neonAvailable = cpu.NeonCheck();
		if(false == neonAvailable){//NEONが使えない場合はAndroidオリジナルの顔認識を使う
			Log.w(LOG_TAG, "NEON not available");
			mFaceDetectMain = new AndroidFaceDetect();
		}
		else{
			Log.i(LOG_TAG, "NEON OK");
			mFaceDetectMain = new OpenCVFaceDetect();
		}
	}
	private void init(Context context){
		mContext = context;
		mHandler = new Handler();

		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		

	}
	public SelfPreview(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}
	public SelfPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}
	public SelfPreview(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h, double aspect_tolerance) {
		if (sizes == null) return null;
//	    final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
        	if(size.width < size.height){
        		int a = size.width;
        		size.width = size.height;
        		size.height = a;
        	}
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > aspect_tolerance) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
            	if(size.width < size.height){
            		int a = size.width;
            		size.width = size.height;
            		size.height = a;
            	}
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	boolean mCameraOpenQuitFlug = false;
	
	private boolean cameraOpen(SurfaceHolder holder, int width, int height){
		setEngine();
		
		mCameraOpenQuitFlug = false;
		if(!SelfCameraActivity.mSettingInfo.isCameraOpen()) return false;
		cameraRelease();

		Camera camera = SelfCameraActivity.mSettingInfo.getCamera();
		try{
			if(null != holder){
				camera.setPreviewDisplay(holder);
			}
		}
		catch(Exception e){
			threadToast(R.string.ErrorCantSetPreviewDisplay);
			cameraRelease();
			e.printStackTrace();
			return false;
		}

		WriteLock wr = mCamLock.writeLock();//カメラ操作中にリリースされないように
		Camera.Parameters parameters = null;
		int count =0;
		try{
			wr.lock();
			camera.stopPreview();		

			parameters = camera.getParameters();
			if(MyDebug.DEBUG) Log.i(LOG_TAG, parameters.flatten());
			List<Camera.Size> pvsizes = camera.getParameters().getSupportedPreviewSizes();
			
			//toleranceが0.1では不足する端末がある？
			final double [] tolerance = {0.05, 0.1, 0.15, 0.2, 0.25};
			
			int tolNo = 0;
			do{
				if(mCameraOpenQuitFlug){
					Log.w(LOG_TAG, "Camera opening aborted:0-" + tolNo);
					cameraRelease();
					return false;
				}

				try{
					Camera.Size bestSize = getOptimalPreviewSize(pvsizes, 
							width, 
							height, 
							tolerance[tolNo]);
					Log.d(LOG_TAG, "Screen size = " + width + ":" + height +
									"Determined compatible preview size is: ("
									+ bestSize.width + 
									"," +
									bestSize.height + 
									")");

					parameters.setPreviewSize(bestSize.width, bestSize.height);
					camera.setParameters(parameters);
				}
				catch(Exception e){
					e.printStackTrace();
					tolNo++;
					if(tolerance.length == tolNo){
						cameraRelease();
						String errMsg = mMainActivity.getString(R.string.ErrorCantStartPreview);
						if(null != e.getLocalizedMessage()){
							errMsg += (" (" + e.getLocalizedMessage() + ":0)");
						}
						threadToast(errMsg);
						break;
					}
					else{
						continue;
					}
				}
				break;
			}while(true);

			if(tolerance.length != tolNo){
				//Preferenceに設定されている内容を反映させる
				for(CameraSetting.CameraSettingBase s:SelfCameraActivity.mSettingInfo.mCameraSettingList){
					if(mCameraOpenQuitFlug){
						Log.w(LOG_TAG, "Camera opening aborted:1-" + count);
						cameraRelease();
						return false;
					}
					count++;
					if(s.isSupported()){
						s.setValue();
					}
				}
				//setPictureSize()でPreviewSizeが変わることがある（端末による？）ため撮り直しておく
				count++;
				Camera.Size s = camera.getParameters().getPreviewSize();

				mPreviewWidth = s.width;
				mPreviewHeight = s.height;
				mWidthRatio = (float)mPreviewWidth/(float)width;
				mHeightRatio = (float)mPreviewHeight/(float)height;
				
				//顔認識の準備
				count++;
				if(null != mFaceDetectMain){
					try{
						mFaceDetectMain.init();
					}
					catch(IOException e){
						String errMsg = mContext.getString(R.string.ErrorLoadCascadeFile1);
						if(null != e.getMessage()){
							errMsg += ("(" + e.getMessage() + ")");
						}
						threadToast(errMsg);
					}
					catch(FaceDetectInitException e){
						threadToast(e.getMessage());
					}
				}
				if(mCameraOpenQuitFlug){
					Log.w(LOG_TAG, "Camera opening aborted:2");
					cameraRelease();
					return false;
				}
				count++;
				startPreview();
				count++;
				parameters = camera.getParameters();
				Log.i(LOG_TAG, "Camera preview started " + parameters.getPreviewSize().width + ":"+parameters.getPreviewSize().height);
				Log.i(LOG_TAG, "Camera screen size " + width + ":" + height);
				Log.i(LOG_TAG, "Camera picture size " + parameters.getPictureSize().width + ":"+parameters.getPictureSize().height);
			}
		}
		catch(Exception e){
			e.printStackTrace();
			String errMsg = mMainActivity.getString(R.string.ErrorCantStartPreview);
			if(null != e.getLocalizedMessage()){
				errMsg += (" (" + e.getLocalizedMessage() + ":" + count + ")");
			}
			threadToast(errMsg);
			//cameraは途中でreleaseされている可能性があるため取りなおし
//			camera = SelfCameraActivity.mSettingInfo.getCamera();
//			if(null != camera){
//				Camera.Size s = null;
//				s = camera.getParameters().getPreviewSize();
//				threadToast("PrevSize = " + s.width + ":" + s.height + ")");
//				s = camera.getParameters().getPictureSize();
//				threadToast("PicSize = " + s.width + ":" + s.height + ")");
//			}
			cameraRelease();
		}
		finally{
			wr.unlock();
		}
		return true;
	}
	
	/**
	 * カメラを開放する
	 */
	private void cameraRelease(){
		Log.i(LOG_TAG, "Camera will release");  
		if(!SelfCameraActivity.mSettingInfo.isCameraOpen()){
			Log.i(LOG_TAG, "camera is already closed");
			return;
		}
		WriteLock w = mCamLock.writeLock();//カメラ操作中にリリースされないように
		try{
			w.lock();
			stopPreview();
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			w.unlock();
		}
	    Log.i(LOG_TAG, "Camera released");  
	    mFaceDetectMain.destroy();
	}
	
	
	/**
	 * 画像のバッファを確保する
	 * @return
	 * true:成功　false:失敗
	 */
	private boolean addCallbackBuffer() {
		try {
			mAddCallbackBuffer.invoke(SelfCameraActivity.mSettingInfo.getCamera(),
									mBuffer);
		}
		catch (Exception e) {
			if(MyDebug.DEBUG)e.printStackTrace();
			return false;
		}
		return true;
	}
	/**
	 * プレビューを開始する。2.2以降の場合はsetPreviewCallbackWithBufferを使用する
	 * @param width
	 * 画面の幅
	 * @param height
	 * 画面の高さ
	 * @throws Exception
	 * preview開始に失敗した場合はExceptionをthrowする
	 */
	private void startPreview() throws Exception{
		Log.i(LOG_TAG, "startPreview");
		boolean hasCallBackWithBuffer = true;

		Camera camera = SelfCameraActivity.mSettingInfo.getCamera();
 		mBuffer = null;
 	    if(android.os.Build.VERSION.SDK_INT < 8){//2.1　or それ以前の場合
 	    	hasCallBackWithBuffer = false;
 	    }
 	    else{
 			try {
 				//addCallbackBufferがあるか確認
 				mAddCallbackBuffer = Class.forName("android.hardware.Camera").getMethod(
 						"addCallbackBuffer", byte[].class);

 			} 
 			catch (Exception e) {
 				hasCallBackWithBuffer = false;
 			}
 			try {
 				//setPreviewCallbackWithBufferがあるか確認
 				mSetPreviewCallbackWithBuffer = Class.forName("android.hardware.Camera").getMethod(
 						"setPreviewCallbackWithBuffer", PreviewCallback.class);
 			}
 			catch (Exception e) {
 				hasCallBackWithBuffer = false;

 			}
 			//setPreviewCallbackWithBufferがある場合
 			if(false != hasCallBackWithBuffer){
 				//画面の幅、高さ、色深度からバッファを用意
 				PixelFormat pixelinfo = new PixelFormat();
 				int pixelformat = camera.getParameters().getPreviewFormat();
 				PixelFormat.getPixelFormatInfo(pixelformat, pixelinfo);
 				int bufSize = mPreviewWidth * mPreviewHeight * pixelinfo.bitsPerPixel / 8;
 				mBuffer = new byte[bufSize];
 				hasCallBackWithBuffer = addCallbackBuffer();
 				if(false != hasCallBackWithBuffer){
 					try {
 						mSetPreviewCallbackWithBuffer.invoke(camera, this);
 					} 
 					catch (Exception e) {
 						e.printStackTrace();
 						hasCallBackWithBuffer = false;
 					}
 				}
 			}
 	    }

		//setPreviewCallbackWithBufferに失敗した場合は通常のsetPreviewCallbackを使用する
		if(false == hasCallBackWithBuffer){
			startPreviewLegacy();
		}
		else{
			try{//プレビュー開始
				camera.startPreview();
			}
			catch(Exception e){//プレビュー開始できなかった場合はexceptionをthrowする
				stopPreview();
				throw e;
			}
		}
		mAutoFocusRunning = false;
	}
	/**
	 * setPreviewCallbackWithBufferがない端末の場合のプレビュースタート
	 * @throws Exception
	 */
	private void startPreviewLegacy() throws Exception{
		Log.i(LOG_TAG, "startPreviewLegacy");
		mAddCallbackBuffer = null;
		mSetPreviewCallbackWithBuffer = null;
		mBuffer = null;
		Camera camera = SelfCameraActivity.mSettingInfo.getCamera();

		try{
			camera.setPreviewCallback(this);
		}
		catch(Exception e){//callbackの設定に失敗した場合はエラーをthrowする
			throw e;
		}
		try{//プレビュー開始
			camera.startPreview();
		}
		catch(Exception e){//プレビュー開始できなかった場合はexceptionをthrowする
			stopPreview();
			throw e;
		}
	}
	/**
	 * プレビューを停止する
	 * @throws Exception 
	 */
	public void stopPreview() throws Exception{
		Log.i(LOG_TAG, "stopPreview");
		Camera camera = SelfCameraActivity.mSettingInfo.getCamera();
		if(null == camera)return;
		if(null != mSetPreviewCallbackWithBuffer){//setPreviewCallbackWithBufferを使用していた場合
			mSetPreviewCallbackWithBuffer.invoke(camera, (PreviewCallback) null);
			mAddCallbackBuffer = null;
			mSetPreviewCallbackWithBuffer = null;
			mBuffer = null;
		}
		else{
			camera.setPreviewCallback(null);
		}
		try{
			camera.stopPreview();
		}
		catch(Exception e){
			throw e;
		}
	}	
	public void dismissCameraOpenDialog(){
		if(null != mCameraOpenProgress && mCameraOpenProgress.isShowing()){
			mCameraOpenProgress.dismiss();
		}
		mCameraOpenProgress = null;
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, 
								int format, 
								int width,
								int height) {
		// TODO Auto-generated method stub
		class CameraOpenRunnable implements Runnable{
			private SurfaceHolder mHolder; 
			private int mWidth;
			private int mHeight;
			@Override
			public void run() {
				// TODO Auto-generated method stub
				cameraOpen(mHolder, mWidth, mHeight);
				dismissCameraOpenDialog();
			}
		}
		Log.i(LOG_TAG, "surfaceChanged");

//		cameraOpen(holder, width, height);
		
		dismissCameraOpenDialog();
		
		mCameraOpenProgress = new ProgressDialog(mContext);
		mCameraOpenProgress.setTitle(mContext.getString(R.string.progress_title_opening_camera));
		mCameraOpenProgress.setMessage(mContext.getString(R.string.progress_message_wait));
		mCameraOpenProgress.setIndeterminate(false);
		mCameraOpenProgress.setCancelable(false);
		mCameraOpenProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mCameraOpenProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				// TODO Auto-generated method stub
				 mCameraOpenQuitFlug = true;
				Toast.makeText(mContext, R.string.camera_open_aborted, Toast.LENGTH_SHORT).show();
			}
		});
		mCameraOpenProgress.show();
		CameraOpenRunnable r = new CameraOpenRunnable();
		r.mHeight = height;
		r.mWidth = width;
		r.mHolder = holder;
		mCameraOpenThread = new Thread(r);
		mCameraOpenThread.start();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "surfaceCreated");
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "surfaceDestroyed");
		dismissCameraOpenDialog();
		cameraRelease();
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		// TODO Auto-generated method stub

		if(false == mAutoFocusRunning && null != mMainActivity){
			if(null != mFaceDetectMain){

				if(data.length == mPreviewHeight*mPreviewWidth*3/2){//解像度を切り替えたときに、古い解像度のキャプチャデータが来ることがあるため
																	//解像度が合わない場合は無視する
					int orientation = mMainActivity.getOrientation();
					ArrayList<Rect>rct = mFaceDetectMain.detect(data, camera, orientation);
					if(null != rct){
						if(true == mFaceMarkView.setFacePos(rct, 
															orientation, 
															isShootOnFramed())){//顔がいい位置に来た
							mMainActivity.takePicture();//撮影実行
						}
						mFaceMarkView.invalidate();
					}
				}
				else{
					Log.w(LOG_TAG, "Invalid cam data size data size = " + data.length + 
								" Requied data size = " + mPreviewHeight*mPreviewWidth*3/2);
				}
			}
		}
		addCallbackBuffer();
	}

	/**
	 * 顔位置OKで自動シャッタを切るかどうかのフラグを設定する
	 * @param mShootOnFramed
	 */
	public void setShootOnFramed(boolean mShootOnFramed) {
		this.mShootOnFramed = mShootOnFramed;
	}
	public boolean isShootOnFramed() {
		return mShootOnFramed;
	}
	
	private MediaPlayer mDirectionVoice = null;
	private Thread mTakePicThread = null;
	/**
	 * 撮影を実行する
	 * @return
	 * false:正しく撮影を実行できなかった　true:できた
	 */
	public boolean capture(){
		
		class TakePictureRunnable implements Runnable{
			private Camera 	cam;
			private boolean suc;
			private void init(boolean success, Camera c){
				cam = c;
				suc = success;
			}
			
			@Override
			public void run() {
				// TODO Auto-generated method stub

				if(null != mDirectionVoice){
					while(mDirectionVoice.isPlaying()){//チーズ　の再生が終わるまで待つ。美しくないけどどうすればいいだろう？？？？
						try {
							Thread.sleep(300);
						}
						catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					mDirectionVoice.release();
					mDirectionVoice = null;
				}
				
				if(true == suc){
					try {
						if(null != mAddCallbackBuffer){
							cam.setPreviewCallback(null);
						}
						if(MyDebug.DEBUG)Log.i(LOG_TAG, "Before Capture");
						cam.takePicture(null, null, null, SelfPreview.this);
						if(MyDebug.DEBUG)Log.i(LOG_TAG, "After Capture");

					} 
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					threadToast(R.string.AutoFocusNG);
					try {
//						cam.autoFocus(null); //autoFocus(null)はいれてはいけない。これを入れると再度AFしに行く。
											//その途中でtakePicture()するとmedia serverが壊れてonPictureTaken()が呼ばれない。
						stopPreview();
						startPreview();

					}
					catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					mAutoFocusRunning = false;
				}
			}
		}

		Log.i(LOG_TAG, "Capture");
		if(!SelfCameraActivity.mSettingInfo.isCameraOpen()) return false;
		
		Camera camera = SelfCameraActivity.mSettingInfo.getCamera();

		//オートフォーカス中はなにもしない。
		if(true == mAutoFocusRunning){
			Log.w(LOG_TAG, "Autofocus is Running");
			return false;
		}

		mAutoFocusRunning = true;
		
		//チーズを再生する。
		//現在再生している音声があったら止める
		mFaceMarkView.stopVoice();
		//チーズのリソースIDを取得
		int voice = mFaceMarkView.getVoice(FaceMarkView.FACE_POS_GOOD, true);
		//再生開始
		mDirectionVoice = MediaPlayer.create(mContext, voice);
		mDirectionVoice.start();

		try{
			camera.autoFocus(new Camera.AutoFocusCallback() {//カメラにAFがある場合
				@Override
				public void onAutoFocus(boolean success, Camera c) {

					// TODO Auto-generated method stub
					Log.i(LOG_TAG, "Autofocus = " + success);
					if(null == mTakePicThread || !mTakePicThread.isAlive()){
						TakePictureRunnable r = new TakePictureRunnable();
						r.init(success, c);
						mTakePicThread = new Thread(r);
						mTakePicThread.start();
					}
				}
			});
		}
		catch(Exception e){//ない場合はいきなりシャッタを切る
			e.printStackTrace();
			if(null == mTakePicThread || !mTakePicThread.isAlive()){
				TakePictureRunnable r = new TakePictureRunnable();
				r.init(true, camera);
				mTakePicThread = new Thread(r);
				mTakePicThread.start();
			}
		}

		return true;
	}
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.i(LOG_TAG, "onPictureTaken");

		String fn = null;
		if(null != data){
			if(MyDebug.DEBUG) Log.i(LOG_TAG, "Captured:size = " + data.length);
			int sdErrorID = SelfCameraActivity.SDCardAccess.checkSDCard(mContext);
			if(0 != sdErrorID){//SDカードが刺さっていないとき
				Toast.makeText(mContext, 
			    		sdErrorID, 
			            Toast.LENGTH_LONG).show();
			}
			else{
				fn = SelfCameraActivity.SDCardAccess.createPicturePath(mContext);
				FileOutputStream fileOutputStream = null;
				try {
					fileOutputStream = new FileOutputStream(fn);
					fileOutputStream.write(data);
				}
				catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					fn = null;
				}
				finally{
					if(null != fileOutputStream){
						try {
							fileOutputStream.flush();
							fileOutputStream.close();
						} 
						catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							fn = null;
						}
					}
				}
			}
		}
		
		try {
			stopPreview();//バッファメモリを解放するためとりあえず実行しておく。なくてもいいかも。
			if(null == fn){
				startPreview();//下のpictureTakenで、撮影結果を表示するactivityを表示するため、その時にカメラが造り直されるのでここでstartPreviewする必要なし
			}

		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(null != fn){//写真を保存できたときは撮影結果を表示する
			mMainActivity.pictureTaken(fn, mPreviewWidth, mPreviewHeight);
		}
		else{
			Toast.makeText(mContext, R.string.PictureSaveNG, Toast.LENGTH_SHORT).show();			
		}
		//カメラのpreview開始で設定する　
		//mAutoFocusRunning = false;
	}

}
