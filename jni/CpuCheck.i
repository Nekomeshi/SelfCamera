/*
 * include the headers required by the generated cpp code
 */
%{
#include "CpuCheck.h"
%}


class CpuCheck{
public:
	CpuCheck(){};
	virtual ~CpuCheck(){};


	bool NeonCheck(void);
};