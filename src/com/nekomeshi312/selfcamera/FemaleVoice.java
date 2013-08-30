package com.nekomeshi312.selfcamera;

public class FemaleVoice implements VoiceInfo {
	private static final String LOG_TAG = "FemaleVoice";

	@Override
	public int getVoiceEntryValue() {
		// TODO Auto-generated method stub
		return R.string.female_voice_entry_value;
	}
	@Override
	public int getVoiceName() {
		// TODO Auto-generated method stub
		return R.string.female_voice_name;
	}

	@Override
	public int getVoiceResource(int dir, boolean isShootOnFramed) {
		// TODO Auto-generated method stub
		switch(dir){
			case FaceMarkView.FACE_POS_GOOD:
				if(isShootOnFramed){
					return R.raw.female_cheez;
				}
				else{
					return R.raw.female_goodangle;					
				}
			case FaceMarkView.FACE_POS_LEFT:
				return R.raw.female_left;
			case FaceMarkView.FACE_POS_LEFT_UP:
				return R.raw.female_leftup;
			case FaceMarkView.FACE_POS_UP:
				return R.raw.female_up;
			case FaceMarkView.FACE_POS_RIGHT_UP:
				return R.raw.female_rightup;
			case FaceMarkView.FACE_POS_RIGHT:
				return R.raw.female_right;
			case FaceMarkView.FACE_POS_RIGHT_DOWN:
				return R.raw.female_rightdown;
			case  FaceMarkView.FACE_POS_DOWN:
				return R.raw.female_down;
			case FaceMarkView.FACE_POS_LEFT_DOWN:
				return R.raw.female_leftdown;
			case FaceMarkView.FACE_POS_NOT_DETECTED:
				return R.raw.no_face_detect_female;
			case  FaceMarkView.FACE_POS_NOT_ENOUGH:
				return R.raw.no_enough_face_detect_female;
			default:
				return 0;
				
		}
	}

	@Override
	public int getvoiceComment() {
		// TODO Auto-generated method stub
		return R.string.female_voice_comment;
	}



}
