#ifndef CPU_CHECK_H_
#define CPU_CHECK_H_

class CpuCheck{
public:
	CpuCheck(){};
	virtual ~CpuCheck(){};


	bool NeonCheck(void);
};
#endif
