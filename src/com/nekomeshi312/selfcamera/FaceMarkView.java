package com.nekomeshi312.selfcamera;

import java.util.ArrayList;
import java.util.Calendar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;

public class FaceMarkView extends View {
	public interface FaceAreaDraw{
		public static final int FRAME_COLOR = 0xff00b000;
		public void init(Context context);
		public void setScreenSize(int width, int height, int rotation);
		public boolean setOrientation(int orientation);
		public void drawFaceArea(Canvas canvas);
		public int isFacePosGood(ArrayList<Rect> facePos, ArrayList<Float> delta);
		public int getFaceNum();
		public String getFacePosEntryValue();
		public String getFacePosScreenName();
	}
	//45°おきに向きを判断する
	public final static int FACE_POS_GOOD = 0;
	public final static int FACE_POS_DETECTED = 0;
	public final static int FACE_POS_RIGHT = 1;
	public final static int FACE_POS_RIGHT_UP = 2;
	public final static int FACE_POS_UP = 3;
	public final static int FACE_POS_LEFT_UP =4;
	public final static int FACE_POS_LEFT =5;
	public final static int FACE_POS_LEFT_DOWN = 6;
	public final static int FACE_POS_DOWN = 7;
	public final static int FACE_POS_RIGHT_DOWN = 8;
	
	public final static int FACE_POS_NOT_DETECTED = 253;
	public final static int FACE_POS_NOT_ENOUGH = 254;
	public final static int FACE_POS_NOT_PREPARED = 255;
	private int mWidth = -1;
	private int mHeight = -1;
	private Context mContext = null;
	private static final String LOG_TAG = "FaceMarkView";
	private ArrayList<Path> mLinePathList = new ArrayList<Path>();
	private ArrayList<Paint> mLinePaintList = new ArrayList<Paint>();
	private ArrayList<FaceAreaDraw> mFaceAreaDrawList = null;
	private FaceAreaDraw mCurrentFaceAreaDraw = null;
	
	private ArrayList<VoiceInfo>mVoiceInfoArray = null;
	private VoiceInfo mCurrentVoiceInfo = null;
	
	private MediaPlayer mDirectionVoice = null;
	private int mLastDirection = FACE_POS_NOT_PREPARED;
	private long mLastDirectionTime = -1;
	private static final int DIRECTION_VOICE_INTERVAL =3000;//(mS)ズレが同じ方向にいた場合に声を再度再生する時間間隔
	private static final double CHATTER_ANGLE_SIZE = 5.0*Math.PI/180.0;//チャタリング防止用の余裕分５度
	private static final int MAX_FACE_NUM = 4;
	private int faceColorList[] = new int[MAX_FACE_NUM];


	/**
	 * 顔検出された位置を描画し、現在選択されている適正化おエリアに対しどちらにズレているか計算する
	 * @param mFacePos the mFacePos to set
	 * @return
	 * true:
	 */
	public boolean setFacePos(ArrayList<Rect> mFacePos, 
							int orientation,
							boolean isShootOnFramed) {
		final float div0 = 3.0f/5.0f;
		final float div1 = 1.0f - div0;
		if(null == mCurrentFaceAreaDraw) return false;
		mCurrentFaceAreaDraw.setOrientation(orientation);

		for(Path pth :mLinePathList){
			pth.reset();
		}
		int fNum = Math.min(mFacePos.size(), mCurrentFaceAreaDraw.getFaceNum());
		
		if(fNum > mLinePathList.size())return false;
		
		if(MyDebug.DEBUG)Log.i(LOG_TAG, "fNum = " + fNum);
		for(int i = 0;i < fNum;i++){
			Rect facePos = mFacePos.get(i);
			Path pth = mLinePathList.get(i);
			//線のパスを設定
			pth.reset();
			pth.moveTo(facePos.left, facePos.top*div0+ facePos.bottom*div1);
			pth.lineTo(facePos.left, facePos.top);
			pth.lineTo(facePos.left*div0 + facePos.right*div1, facePos.top);
			pth.moveTo(facePos.left*div1 + facePos.right*div0, facePos.top);
			pth.lineTo(facePos.right, facePos.top);
			pth.lineTo(facePos.right, facePos.top*div0 + facePos.bottom*div1);
			pth.moveTo(facePos.right, facePos.top*div1 + facePos.bottom*div0);
			pth.lineTo(facePos.right, facePos.bottom);
			pth.lineTo(facePos.left*div1 + facePos.right*div0, facePos.bottom);
			pth.moveTo(facePos.left*div0 + facePos.right*div1, facePos.bottom);
			pth.lineTo(facePos.left, facePos.bottom);
			pth.lineTo(facePos.left, facePos.top*div1 + facePos.bottom*div0);

		}
		//どちらにズレているか
		int pos = FACE_POS_NOT_PREPARED;

		ArrayList<Float>delta = new ArrayList<Float>();
		//顔のズレ方向の指示　画面の回転で向きが変わるのは、このisFacePosGoodの中で吸収する
		pos = mCurrentFaceAreaDraw.isFacePosGood(mFacePos, delta);
		float dX = 0f;
		float dY = 0f;
		//向きの良し悪しを設定
		if(FACE_POS_DETECTED == pos){
			dX = delta.get(0);
			dY = delta.get(1);
			pos = FACE_POS_NOT_PREPARED;
			if(FACE_POS_GOOD == mLastDirection){
				if(Math.abs(dX) < mWidth/25 && Math.abs(dY) < mHeight/25){//前回がOKだった場合は、チャタ防止で少し大きめにしておく
					pos = FACE_POS_GOOD;
				}
			}
			else{
				if(Math.abs(dX) < mWidth/30 && Math.abs(dY) < mHeight/30){
					pos = FACE_POS_GOOD;
				}
			}
			if(FACE_POS_GOOD != pos){
				double angle = Math.atan2((double)dY, (double)dX) + Math.PI;//0～2piにしておく
				if((mLastDirection == FACE_POS_RIGHT) &&//前回の指定範囲が±22.5度の範囲の場合
						((angle < (Math.PI/8.0 + CHATTER_ANGLE_SIZE)) || 
								(angle > (7.0*Math.PI/4.0 + Math.PI/8.0 - CHATTER_ANGLE_SIZE)))){//前回と同じ位置にいる場合
					pos = mLastDirection;							
				}
				else if((mLastDirection >= FACE_POS_RIGHT_UP && mLastDirection <= FACE_POS_RIGHT_DOWN) &&//前回の指定範囲が±22.5度以外の場合
						(angle < ((double)mLastDirection-1.0)*Math.PI/4.0 + Math.PI/8.0 + CHATTER_ANGLE_SIZE && 
								angle > ((double)mLastDirection-1.0)*Math.PI/4.0 - Math.PI/8.0 - CHATTER_ANGLE_SIZE)){//前回と同じ位置にいる場合
					pos = mLastDirection;
				}
				else {
					if((angle < Math.PI/8.0) || (angle > (7.0*Math.PI/4.0 + Math.PI/8.0))){
						pos = FACE_POS_RIGHT;
					}
					else{
						for(int i = FACE_POS_RIGHT+1;i <= FACE_POS_RIGHT_DOWN;i++){
							if((angle > ((double)i-1.0)*Math.PI/4.0 - Math.PI/8.0) && 
									(angle < ((double)i-1.0)*Math.PI/4.0 + Math.PI/8.0)){
								pos = i;
								break;
							}
						}
					}
				}
			}
		}
		//方向を知らせる音声リソースを選択
		long tm = Calendar.getInstance().getTimeInMillis();
		if(pos != mLastDirection ||//指示内容が変わったら
				tm >= mLastDirectionTime + DIRECTION_VOICE_INTERVAL){//前回の再生から時間が立っていたら
			if(null != mDirectionVoice &&
					mDirectionVoice.isPlaying() == true &&
					pos != FACE_POS_GOOD){
				//前の音声を再生中の場合はなにもしない
			}
			else{
				int voice = 0;
				if(null != mCurrentVoiceInfo){
					voice = mCurrentVoiceInfo.getVoiceResource(pos, isShootOnFramed);//新しい音声リソースを取得
				}
				
				if(voice != 0){
					mLastDirectionTime = tm;

					stopVoice();
					
					if((FACE_POS_NOT_DETECTED == pos ||	FACE_POS_NOT_ENOUGH == pos) && //顔が見つかりません　のアナウンスは２回連続で見つからない場合のみ発音
							pos != mLastDirection){
					}
					else{
						if(FACE_POS_GOOD == pos && isShootOnFramed){//チーズ　はここでは再生しない
							
						}
						else{
							mDirectionVoice = MediaPlayer.create(mContext, voice);

							mDirectionVoice.start();//再生開始
						}						
					}
					mLastDirection = pos;						
				}
			}
		}
		if(MyDebug.DEBUG){
			String str = dX + ":" + dY + ":";
			switch(pos){
				case FACE_POS_GOOD:
					str += "OK";
					break;
				case FACE_POS_NOT_DETECTED:
					str += "not detected";
					break;
				case FACE_POS_NOT_ENOUGH:
					str += "not enough";
					break;
				default:
					if((pos & FACE_POS_RIGHT) == FACE_POS_RIGHT){
						str += "Right:";
					}
					if((pos & FACE_POS_LEFT) == FACE_POS_LEFT){
						str += "Left:";
					}
					if((pos & FACE_POS_UP) == FACE_POS_UP){
						str += "Up:";
					}
					if((pos & FACE_POS_DOWN) == FACE_POS_DOWN){
						str += "Down:";
					}
					break;
			}
			Log.i(LOG_TAG, "Pos = " + str);
		}
		return pos == FACE_POS_GOOD && isShootOnFramed;
	}

	private boolean setFacePaint(){

		if(mWidth < 0)return false;
		if(null != mCurrentFaceAreaDraw){
			mLinePaintList.clear();
			mLinePathList.clear();
			for(int i = 0;i < mCurrentFaceAreaDraw.getFaceNum();i++){
				//線の色・幅を設定
				Paint p = new Paint();
				p.setColor(faceColorList[i % MAX_FACE_NUM]);
				p.setStyle(Paint.Style.STROKE);
				p.setStrokeWidth(mWidth/120.0f);
				mLinePaintList.add(p);

				Path pp = new Path();
				mLinePathList.add(pp);
			}
			return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see android.view.View#onSizeChanged(int, int, int, int)
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		for(FaceAreaDraw faceArea:mFaceAreaDrawList){//全顔位置設定エリアを初期化する
			faceArea.setScreenSize(w, h, Surface.ROTATION_0);
		}
		mWidth = w;
		mHeight = h;
		setFacePaint();

		super.onSizeChanged(w, h, oldw, oldh);
	}
	public void stopVoice(){
		if(null == mDirectionVoice){
			return;
		}
		if(mDirectionVoice.isPlaying() == true){
			mDirectionVoice.stop();
		}
		mDirectionVoice.release();
		mDirectionVoice = null;
	}
	public int getVoice(int pos, boolean  isShootOnFramed){
		if(null == mCurrentVoiceInfo){
			return 0;
		}
		return	 mCurrentVoiceInfo.getVoiceResource(pos, isShootOnFramed);//新しい音声リソースを取得
	}
	
	
	public ArrayList<String>getVoiceEntryValues(){
		ArrayList<String>list = new ArrayList<String>();
		for(VoiceInfo vi:mVoiceInfoArray){
			list.add(mContext.getString(vi.getVoiceEntryValue()));
		}
		return list;
		
	}
	/**
	 * 音声名の一欄を取得する
	 * @return
	 * 音声名一覧
	 */
	public ArrayList<String>getVoiceName(){
		ArrayList<String>list = new ArrayList<String>();
		for(VoiceInfo vi:mVoiceInfoArray){
			list.add(mContext.getString(vi.getVoiceName()));
		}
		return list;
	}
	/**
	 * 各音声に関するコメントの一覧を取得する
	 * @return
	 * 音声に関するコメント一覧
	 */
	public ArrayList<String>getVoiceComment(){
		ArrayList<String> list = new ArrayList<String>();
		for(VoiceInfo vi:mVoiceInfoArray){
			list.add(mContext.getString(vi.getvoiceComment()));
		}
		return list;
	}
	/**
	 * 指定された名前の音声をカレントに設定する。名前が不正な場合はnullがセットされる
	 * @param name
	 * 音声名
	 */
	public void setVoiceEntryValue(String name){
		mCurrentVoiceInfo = null;
		for(VoiceInfo vi:mVoiceInfoArray){
			String nm = mContext.getString(vi.getVoiceEntryValue());
			if(name.equals(nm)){
				mCurrentVoiceInfo=vi;
				return;
			}
		}
		mCurrentVoiceInfo=mVoiceInfoArray.get(0);
	}
	
	/**
	 * 適正位置顔エリア名一覧を取得する.
	 * @return
	 * エリア名のArrayList
	 */
	public ArrayList<String>getAngleName(){
		ArrayList<String>list = new ArrayList<String>();
		for(FaceAreaDraw faceArea:mFaceAreaDrawList){
			list.add(faceArea.getFacePosScreenName());
		}
		return list;
	}
	/**
	 * 適正位置顔エリアentryvalue一覧を取得する
	 * @return
	 * エリアのentryvalue
	 */
	public ArrayList<String>getAngleEntryValue(){
		ArrayList<String>list = new ArrayList<String>();
		for(FaceAreaDraw faceArea:mFaceAreaDrawList){
			list.add(faceArea.getFacePosEntryValue());
		}
		return list;
	}
	/**
	 * 指定された名前のエリア名を現在の適性位置顔エリアに設定する
	 * @param name
	 * エリア名
	 * @return
	 * true:成功　false:エリア名が不適切
	 */
	public boolean setAngleEntryValue(String name){
		for(FaceAreaDraw faceArea:mFaceAreaDrawList){
			String s = faceArea.getFacePosEntryValue();
			if(s.equals(name)){
				mCurrentFaceAreaDraw = faceArea;
				setFacePaint();
				invalidate();
				return true;
			}
		}
		return false;
	}

	/**
	 * 初期化
	 * @param context
	 */
	private void init(Context context){
		mContext = context;
		
		//顔位置boxの色をリソースから読み出し
		faceColorList[0] = context.getResources().getColor(R.color.firstFaceBox);
		faceColorList[1] = context.getResources().getColor(R.color.secondFaceBox);
		faceColorList[2] = context.getResources().getColor(R.color.thirdFaceBox);
		faceColorList[3] = context.getResources().getColor(R.color.forthFaceBox);

		mFaceAreaDrawList = new ArrayList<FaceAreaDraw>();
		FaceAreaDraw faceArea = new FaceMarkDouble();
		faceArea.init(context);
		mFaceAreaDrawList.add(faceArea);
		faceArea = new FaceMarkSingle();
		faceArea.init(context);
		mFaceAreaDrawList.add(faceArea);
		mCurrentFaceAreaDraw = null;;
		
        //音声情報を作成
    	mVoiceInfoArray = new ArrayList<VoiceInfo>();
    	mVoiceInfoArray.add(new MaleVoice());
    	mVoiceInfoArray.add(new FemaleVoice());
    	mCurrentVoiceInfo = null;
		
		mLinePathList.clear();
		mLinePaintList.clear();

		new Handler();		
	}

	public FaceMarkView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}
	public FaceMarkView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}
	public FaceMarkView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}	

	/* (non-Javadoc)
	 * @see android.view.View#draw(android.graphics.Canvas)
	 */
	@Override
	public void draw(Canvas canvas) {
		// TODO Auto-generated method stub
		if(null != mCurrentFaceAreaDraw) mCurrentFaceAreaDraw.drawFaceArea(canvas);
		
		for(int i = 0;i < mLinePathList.size();i++){
			canvas.drawPath(mLinePathList.get(i), mLinePaintList.get(i));
		}
		super.draw(canvas);
	}
}
