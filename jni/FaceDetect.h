#ifndef FACE_DETECT_H_
#define FACE_DETECT_H_

#include <jni.h>
#include <cv.h>
#include <vector>

struct FaceDetectRect{
	int left;
	int top;
	int right;
	int bottom;
};
class FaceDetect
{
public:
	FaceDetect();
	virtual ~FaceDetect();
	bool init(int width, int height, char *cascade_name, char *errorcode);
	int detectFacePos(char *img, int *result, int size, int max_num, int orientation);
	enum{
		ROTATION_0 = 0,		//Portrait
		ROTATION_90 = 1,	//Landscape
		ROTATION_180 = 2,	//Portlrait‚ª‚Ð‚Á‚­‚è‚©‚¦‚Á‚½
		ROTATION_270 = 3,	//Landscape‚ª‚Ð‚Á‚­‚è•Ô‚Á‚½
		ROTATION_UNKNOWN = 4
	};
private:
	int m_L_Width;
	int	m_L_Height;
	int m_P_Width;
	int	m_P_Height;

	int m_Width;
	int	m_Height;
	
	int m_FilterW;
	int m_FilterH;

	static const int WIDTH;
	double m_ResizeNum;

	CvHaarClassifierCascade *m_Cascade;
	IplImage *m_SrcImg;
	IplImage *m_ProcessImg;
	IplImage *m_P_ProcessImg;
	IplImage *m_L_ProcessImg;


	CvMemStorage *m_Storage;
	void deleteImages();
	bool decodeYUV420SP(int wst, int hst,
									int imgWidth, int imgHeight,
									IplImage *rgbImg,
									int orgWidth, int orgHeight,
									char *orgImg);
};

#endif /* FACE_DETECT_H_ */
