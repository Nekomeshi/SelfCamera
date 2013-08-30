
%module selfcamera


%pragma(java) jniclasscode=%{
	static {
		try {
			//load the selfcamera library, make sure that libselfcamera.so is in your <project>/libs/armeabi directory
			//so that android sdk automatically installs it along with the app.
			
			//the android-opencv lib must be loaded first inorder for the selfcamera
			//lib to be found
			//check the apk generated, by opening it in an archive manager, to verify that
			//both these libraries are present
			System.loadLibrary("selfcamera");
		} catch (UnsatisfiedLinkError e) {
			//badness
			throw e;
		}
	}

%}

//include the FaceDetect class swig interface file
%include "FaceDetect.i"
%include "CpuCheck.i"