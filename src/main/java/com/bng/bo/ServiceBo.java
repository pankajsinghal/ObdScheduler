package com.bng.bo;

import java.util.List;

import com.bng.entity.Service;

public interface ServiceBo {
	void save(Service service);
	void update(Service service);
	void delete(Service service);
	List GetJobsToStart();
	List GetJobsToEnd();
	boolean getBlackoutHoursStatus();
	Service update(String jobname, String status);
	String stopJob(String jobName);
	Service getJob(String jobName);
}
