#include "FaceDetect.h"
#include <android/log.h>
using namespace std;
#define DEBUG
#undef DEBUG
#define LOG_TAG "FaceDetect"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

const int FaceDetect::WIDTH = 200;//pixel ���̒l��4�̔{���łȂ���΂Ȃ�Ȃ�


FaceDetect::FaceDetect()
{
	m_Cascade = NULL;
	m_SrcImg = NULL;
	m_ProcessImg = NULL;
	m_P_ProcessImg = NULL;
	m_L_ProcessImg = NULL;
	m_Storage = NULL;
}

FaceDetect::~FaceDetect()
{
	deleteImages();
}

void FaceDetect::deleteImages()
{
	if(NULL != m_Cascade) cvReleaseHaarClassifierCascade(&m_Cascade);
	if(NULL != m_SrcImg) cvReleaseImage(&m_SrcImg);
	if(NULL != m_P_ProcessImg) cvReleaseImage(&m_P_ProcessImg);
	if(NULL != m_L_ProcessImg) cvReleaseImage(&m_L_ProcessImg);
	if(NULL != m_Storage) cvReleaseMemStorage (&m_Storage);
	m_Cascade = NULL;
	m_SrcImg = NULL;
	m_ProcessImg = NULL;
	m_P_ProcessImg = NULL;
	m_L_ProcessImg = NULL;
	m_Storage = NULL;
}

bool FaceDetect::init(int width, int height, char *cascade_name, char *errorcode)
{
	deleteImages();
	LOGI("init width =  %d, WIDTH = %d", width, WIDTH);
	m_ResizeNum = (double)width/(double)WIDTH;
	LOGI("resize num = %lf", m_ResizeNum);
	m_SrcImg = cvCreateImage(cvSize(width, height), IPL_DEPTH_8U, 1);

	m_L_Width = WIDTH;
	m_L_Height = (int)(height/m_ResizeNum + 0.5);
	m_L_ProcessImg = cvCreateImage(cvSize(m_L_Width, m_L_Height), IPL_DEPTH_8U, 1);

	m_P_Height = m_L_Width;
	m_P_Width = m_L_Height;
	m_P_ProcessImg = cvCreateImage(cvSize(m_P_Width, m_P_Height), IPL_DEPTH_8U, 1);

	m_FilterW = 0;
	m_FilterH = 0;

	//cvLoad()�ŏo��G���[�ɑ΂��邨�܂��Ȃ�
	CvHaarClassifierCascade * cascade = 0;
	cvReleaseHaarClassifierCascade(&cascade);
	IplImage* img = cvCreateImage(cvSize(1,1), IPL_DEPTH_8U, 1);
	cvSmooth(img, img);
	cvReleaseImage(&img);
	//�����܂�

	LOGI("CascadeName = %s", cascade_name);
	m_Cascade = (CvHaarClassifierCascade *) cvLoad (cascade_name, 0, 0, 0);
	LOGI("m_Cascade = 0x%x", m_Cascade);
	m_Storage = cvCreateMemStorage (0);

	if(NULL == m_Cascade){
		(*errorcode) = 1;
	}
	else if(NULL == m_SrcImg){
		(*errorcode) = 2;
	}
	else if(NULL == m_P_ProcessImg){
		(*errorcode) = 3;
	}
	else if(NULL == m_L_ProcessImg){
		(*errorcode) = 4;
	}
	else if(NULL == m_Storage){
		(*errorcode) = 5;
	}
	else{
		(*errorcode) = 0;
	}
	return (*errorcode) == 0 ? true:false;
}
bool compare(const CvRect * r1, const CvRect * r2)
{
	return (r1->width * r1->height) < (r2->width * r2->height);
}

bool FaceDetect::decodeYUV420SP(int wst, int hst,
								int imgWidth, int imgHeight,
								IplImage *rgbImg,
								int orgWidth, int orgHeight,
								char *orgImg)
{
	int cnt = 0;
	int frameSize = orgWidth*orgHeight;
	int j, k;
	char  *ib = rgbImg->imageData;
	if((wst & 0x01) == 0x01){//�摜�̍��[����̏ꍇ�͉E�ɃV�t�g
		wst++;
		imgWidth--;
		if(2 > imgWidth){
			return false;
		}
	}
	for (j = hst,k = 0 ; k < imgHeight; j++, k++) {
		int	yp = j*orgWidth + wst;
		int uvp = frameSize + (j >> 1) * orgWidth + wst;
		int u = 0;
		int v = 0;
		int i;
		for (i = 0; i < imgWidth; i++, yp++) {
			int y = (0xff & ((int) orgImg[yp])) - 16;
			if (y < 0) y = 0;
			if (((i + wst) & 0x01) == 0x00) {
				v = (0xff & orgImg[uvp++]) - 128;
				u = (0xff & orgImg[uvp++]) - 128;
			}
			int y1192 = 1192 * y;
			int r = (y1192 + 1634 * v);
			int g = (y1192 - 833 * v - 400 * u);
			int b = (y1192 + 2066 * u);
			if (r < 0) r = 0; else if (r > 262143) r = 262143;
			if (g < 0) g = 0; else if (g > 262143) g = 262143;
			if (b < 0) b = 0; else if (b > 262143) b = 262143;

			ib[cnt] = (r >> 10) & 0xff;
			cnt++;
			ib[cnt] = (g >> 10) & 0xff;
			cnt++;
			ib[cnt] = (b >> 10) & 0xff;
			cnt++;
		}
	}
	return true;
}


int FaceDetect::detectFacePos(char *img, int *result, int size, int max_num, int orientation)
{
#ifdef DEBUG
	LOGI("orientation = %d", orientation);
#endif
	int i;
	memcpy(m_SrcImg->imageData, img, m_SrcImg->width*m_SrcImg->height);
	//�������̂��߉摜���k��
	cvResize(m_SrcImg, m_L_ProcessImg, CV_INTER_LINEAR);
	//�����Ńf�B�X�v���B�̌����ɍ��킹�ĉ�]����
	switch(orientation){
		case ROTATION_90:
			m_Width = m_L_Width;
			m_Height = m_L_Height;
			m_ProcessImg = m_L_ProcessImg;
			break;
		case ROTATION_270:
			m_Width = m_L_Width;
			m_Height = m_L_Height;
			m_ProcessImg = m_L_ProcessImg;
			{
				int j;
				int pix = m_Width*m_Height;
				for(i = pix - 1, j = 0;i > j ;i--, j++){
					char a = m_ProcessImg->imageData[i];
					m_ProcessImg->imageData[i] = m_ProcessImg->imageData[j];
					m_ProcessImg->imageData[j] = a;
				}
			}
			break;
		case ROTATION_0:
			m_Width = m_P_Width;
			m_Height = m_P_Height;
			m_ProcessImg = m_P_ProcessImg;
			i = 0;
			for(int x = m_Width - 1;x >= 0 ;x--){
				for(int y = 0;y < m_Height;y++){
					//���ۂ̉摜�T�C�Y�́A����pixel����4�Ŋ���؂��悤����������Ă���̂Œ���
					m_ProcessImg->imageData[y*m_ProcessImg->widthStep + x] = m_L_ProcessImg->imageData[i];
					i++;
				}
			}
			break;
		case ROTATION_180:
			m_Width = m_P_Width;
			m_Height = m_P_Height;
			m_ProcessImg = m_P_ProcessImg;
			i = 0;
			for(int x = 0;x < m_Width;x++){
				for(int y = m_Height-1;y >= 0;y--){
					m_ProcessImg->imageData[y*m_ProcessImg->widthStep + x] = m_L_ProcessImg->imageData[i];
					i++;
				}
			}
			break;
	}

	//�P�x�𕽋ω�
	cvEqualizeHist (m_ProcessImg, m_ProcessImg);
	cvClearMemStorage(m_Storage);
	//�猟�o
	CvSeq *faces = cvHaarDetectObjects (m_ProcessImg,
									m_Cascade, 
									m_Storage, 
									1.2,
									2,
									CV_HAAR_SCALE_IMAGE,
									cvSize(m_FilterW, m_FilterH));

	if(NULL == faces){
		return 0;
	}
	//���o�̈�̉�]�����Ƃɖ߂�
	switch(orientation){
		case ROTATION_90:
			break;
		case ROTATION_270:
			for(i = 0;i < faces->total;i++){
				int tmp;
				CvRect *r = (CvRect *) cvGetSeqElem (faces, i);
				r->x = m_Width - r->x  - r->width;
				r->y = m_Height - r->y - r->height;
				cvSeqRemove(faces, i);
				cvSeqInsert(faces, i, r);
			}
			break;
		case ROTATION_0:
			for(i = 0;i < faces->total;i++){
				int tmp;
				CvRect *r = (CvRect *) cvGetSeqElem (faces, i);
				tmp = r->x;
				r->x = r->y;
				r->y = m_Width - tmp - r->width;
				tmp = r->width;
				r->width = r->height;
				r->height = tmp;
				cvSeqRemove(faces, i);
				cvSeqInsert(faces, i, r);
			}
			break;
		case ROTATION_180:
			for(i = 0;i < faces->total;i++){
				int tmp;
				CvRect *r = (CvRect *) cvGetSeqElem (faces, i);
				tmp = r->x;
				r->x = m_Height - r->y - r->height;
				r->y = tmp;
				tmp = r->width;
				r->width = r->height;
				r->height = tmp;
				cvSeqRemove(faces, i);
				cvSeqInsert(faces, i, r);
			}
			break;
	}

	//�����Ŕ��F�`�F�b�N�BHSV��H��330�`60(@360��)�Ɏ��܂�炵��
	int rsz = (int)(m_ResizeNum + 0.5);
	if(0 == rsz)rsz++;
	for(i = 0;i < faces->total;i++){
		//���o���ꂽ��ʒu�B�摜���k������Ă���̂Ŋg��B
		CvRect *r = (CvRect *) cvGetSeqElem (faces, i);
		int wst = (int)(r->x*m_ResizeNum + 0.5);
		int hst = (int)(r->y*m_ResizeNum + 0.5);
		int imgWidth = (int)(r->width*m_ResizeNum + 0.5);
		int imgHeight = (int)(r->height*m_ResizeNum + 0.5);
		int imgTotal = imgHeight*imgWidth;

		//���F���o�p�摜��p��
		IplImage *faceImg = cvCreateImage(cvSize(imgWidth, imgHeight), IPL_DEPTH_8U, 3);
		IplImage *hueImg = cvCreateImage(cvSize(imgWidth, imgHeight), IPL_DEPTH_8U, 1);

		//YUV ���@RGB�ɕϊ�
		if(true == decodeYUV420SP(wst, hst, imgWidth, imgHeight, faceImg,
								m_SrcImg->width, m_SrcImg->height, img)){
			//RGB��HSV�ɕϊ�
			cvCvtColor(faceImg, faceImg, CV_RGB2HSV);
			//Hue�����̂ݒ��o�B
			cvSplit(faceImg,hueImg,NULL,NULL,NULL);

			//H�̒l����Ƃ����Ă����f�����J�E���g
			int count = 0;
			int total = 0;
			for(int h = 0;h < imgHeight;h += rsz){
				int hh = h*imgWidth;
				for(int w = 0;w < imgWidth;w += rsz){
					char hue = hueImg->imageData[hh+w];//Hue�����擾
					if(hue >= 165 || hue <= 30) count++;
					total++;
				}
			}
#ifdef DEBUG
			LOGI("hue %d / %d", count, total);
#endif

			//�G���A��10%�ȏオ���F���߂Ă��Ȃ��ꍇ�͖�������
			if(total == 0 || 0.1 > ((double)count / (double)total)){
				cvSeqRemove(faces, i);
			}
		}
		cvReleaseImage(&faceImg);
		cvReleaseImage(&hueImg);
	}

	if(0 == faces->total){
		return 0;
	}
	//���o���ʂ̖ʐς̑傫�����Ƀ\�[�g
	vector<CvRect *> faceRect;
	int totalNum = faces->total;
	for (i = 0; i < totalNum; i++) {
		CvRect *r = (CvRect *) cvGetSeqElem (faces, i);
		faceRect.push_back(r);
	}
	sort(faceRect.begin(), faceRect.end(), compare);
	//�傫��������w�萔�l���i�[

	totalNum = min(totalNum, max_num);
	FaceDetectRect *ans = (FaceDetectRect *)result;
	for(i = 0;i < totalNum;i++){
		//�摜���k�����Ă��邽�߁ARESIZE_NUM�{���č��W��߂��Ă��邱�Ƃɒ���
		ans[i].left = (int)(faceRect.at(i)->x*m_ResizeNum + 0.5);
		ans[i].top = (int)(faceRect.at(i)->y*m_ResizeNum + 0.5);
		ans[i].right = (int)((faceRect.at(i)->x + faceRect.at(i)->width)*m_ResizeNum + 0.5);
		ans[i].bottom = (int)((faceRect.at(i)->y + faceRect.at(i)->height)*m_ResizeNum + 0.5);
	}
	faceRect.clear();
	return totalNum;
}
