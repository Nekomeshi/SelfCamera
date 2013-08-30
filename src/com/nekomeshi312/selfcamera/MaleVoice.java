package com.nekomeshi312.selfcamera;

public class MaleVoice implements VoiceInfo {

	@Override
	public int getVoiceEntryValue() {
		// TODO Auto-generated method stub
		return R.string.male_voice_entry_value;
	}

	@Override
	public int getVoiceName() {
		// TODO Auto-generated method stub
		return R.string.male_voice_name;
	}

	@Override
	public int getVoiceResource(int dir, boolean isShootOnFramed) {
		// TODO Auto-generated method stub
		switch(dir){
			case FaceMarkView.FACE_POS_GOOD:
				if(isShootOnFramed){
					return R.raw.male_cheez;
				}
				else{
					return R.raw.male_goodangle;					
				}
			case FaceMarkView.FACE_POS_LEFT:
				return R.raw.male_left;
			case FaceMarkView.FACE_POS_LEFT_UP:
				return R.raw.male_leftup;
			case FaceMarkView.FACE_POS_UP:
				return R.raw.male_up;
			case FaceMarkView.FACE_POS_RIGHT_UP:
				return R.raw.male_rightup;
			case FaceMarkView.FACE_POS_RIGHT:
				return R.raw.male_right;
			case FaceMarkView.FACE_POS_RIGHT_DOWN:
				return R.raw.male_rightdown;
			case  FaceMarkView.FACE_POS_DOWN:
				return R.raw.male_down;
			case FaceMarkView.FACE_POS_LEFT_DOWN:
				return R.raw.male_leftdown;
			case FaceMarkView.FACE_POS_NOT_DETECTED:
				return R.raw.no_face_detect_male;
			case  FaceMarkView.FACE_POS_NOT_ENOUGH:
				return R.raw.no_enough_face_detect_male;
			default:
				return 0;
		}
	}

	@Override
	public int getvoiceComment() {
		// TODO Auto-generated method stub
		return R.string.male_voice_comment;
	}

}
