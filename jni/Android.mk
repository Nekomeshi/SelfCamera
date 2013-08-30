LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=off
#OPENCV_LIB_TYPE:=STATIC

OPENCV_MK_PATH:=~/OpenCV-2.3.1/share/OpenCV/OpenCV.mk
#OPENCV_MK_PATH:=~/OpenCV-2.4.3-android-sdk/sdk/native/jni/OpenCV.mk

include $(OPENCV_MK_PATH)


#ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#	#try to load OpenCV.mk from default install location
#	include $(TOOLCHAIN_PREBUILT_ROOT)/user/share/OpenCV/OpenCV.mk
#else
#	include $(OPENCV_MK_PATH)
#endif

LOCAL_MODULE    := selfcamera
LOCAL_SRC_FILES := FaceDetect.cpp CpuCheck.cpp gen/selfcamera_swig.cpp
LOCAL_LDLIBS += $(OPENCV_LIBS) $(ANDROID_OPENCV_LIBS) -llog
LOCAL_C_INCLUDES +=  $(OPENCV_INCLUDES) $(ANDROID_OPENCV_INCLUDES)  $(NDK_ROOT)/sources/cpufeatures
LOCAL_STATIC_LIBRARIES += cpufeatures
LOCAL_ARM_NEON := true
include $(BUILD_SHARED_LIBRARY)
include $(NDK_ROOT)/sources/cpufeatures/Android.mk
