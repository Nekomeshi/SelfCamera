#include <cpu-features.h>
#include <android/log.h>
#include "CpuCheck.h"

bool CpuCheck::NeonCheck(void)
{
	return (android_getCpuFamily() == ANDROID_CPU_FAMILY_ARM &&
	        (android_getCpuFeatures() & ANDROID_CPU_ARM_FEATURE_NEON) != 0);
}
