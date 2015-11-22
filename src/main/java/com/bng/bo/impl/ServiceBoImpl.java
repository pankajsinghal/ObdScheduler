package com.bng.bo.impl;

import java.io.Serializable;
import java.util.List;

import com.bng.bo.ServiceBo;
import com.bng.dao.ServiceDao;
import com.bng.entity.Service;

public class ServiceBoImpl implements ServiceBo, Serializable {

	private static final long serialVersionUID = 1L;
	private ServiceDao serviceDao;

	public void setServiceDao(ServiceDao serviceDao) {
		this.serviceDao = serviceDao;
	}

	@Override
	public void save(Service service) {
		serviceDao.save(service);
	}

	@Override
	public void update(Service service) {
		serviceDao.update(service);
	}

	@Override
	public void delete(Service service) {
		serviceDao.delete(service);
	}

	@Override
	public List GetJobsToStart() {
		return serviceDao.GetJobsToStart();
	}

	@Override
	public List GetJobsToEnd() {
		return serviceDao.GetJobsToEnd();
	}

	@Override
	public boolean getBlackoutHoursStatus() {
		// TODO Auto-generated method stub
		return serviceDao.getBlackoutHoursStatus();
	}

	@Override
	public Service update(String jobname, String status) {
		return serviceDao.update(jobname,status);
	}

	@Override
	public String stopJob(String jobName) {
		return serviceDao.stopJob(jobName);
	}

	@Override
	public Service getJob(String jobName) {
		return serviceDao.getJob(jobName);
	}

}
