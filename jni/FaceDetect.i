/*
 * include the headers required by the generated cpp code
 */
%{
#include "FaceDetect.h"
%}

#ifndef SWIGIMPORTED
%include "various.i"
%include "typemaps.i"
%include "arrays_java.i"
#endif

%typemap(in) (int *INTARRAY, int INTARRAYSIZE) {
    $1 = (int *) JCALL2(GetIntArrayElements, jenv, $input, 0); 
    jsize sz = JCALL1(GetArrayLength, jenv, $input);
    $2 = (int)sz;
}

%typemap(argout) (int *INTARRAY, int INTARRAYSIZE) {
    JCALL3(ReleaseIntArrayElements, jenv, $input, (jint *) $1, 0); 
}


/* Prevent default freearg typemap from being used */
%typemap(freearg) (int *INTARRAY, int INTARRAYSIZE) ""

%typemap(jni) (int *INTARRAY, int INTARRAYSIZE) "jintArray"
%typemap(jtype) (int *INTARRAY, int INTARRAYSIZE) "int[]"
%typemap(jstype) (int *INTARRAY, int INTARRAYSIZE) "int[]"
%typemap(javain) (int *INTARRAY, int INTARRAYSIZE) "$javainput"



%typemap(javaimports) FaceDetect "


/** FaceDetect - for processing images that are stored in an image pool
*/"
%apply (char* BYTE) { (char *img)}; //byte[] to char*
%apply (char* BYTE) { (char *errorcode)}; //byte[] to char*
%apply (int *INTARRAY, int INTARRAYSIZE) { (int *result, int size)};

struct FaceDetectRect{
	int left;
	int top;
	int right;
	int bottom;
};
class FaceDetect {
public:
	FaceDetect();
	virtual ~FaceDetect();
	bool init(int width, int height, char *cascade_name, char *errorcode);
	int detectFacePos(char *img, int *result, int size, int max_num, int orientation);
	enum{
		ROTATION_0 = 0,
		ROTATION_90 = 1,
		ROTATION_180 = 2,
		ROTATION_270 = 3,
		ROTATION_UNKNOWN = 4
	};
};
