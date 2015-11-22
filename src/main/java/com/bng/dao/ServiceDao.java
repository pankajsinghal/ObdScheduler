package com.bng.dao;

import java.util.List;

import com.bng.entity.Service;

public interface ServiceDao {
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
