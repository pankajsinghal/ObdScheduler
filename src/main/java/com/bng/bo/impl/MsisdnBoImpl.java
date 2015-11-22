package com.bng.bo.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.bng.bo.MsisdnBo;
import com.bng.dao.MsisdnDao;
import com.bng.entity.Msisdn;

public class MsisdnBoImpl implements MsisdnBo, Serializable {
	
	private static final long serialVersionUID = 1L;
	private MsisdnDao msisdnDao;

	public void setMsisdnDao(MsisdnDao msisdnDao) {
		this.msisdnDao = msisdnDao;
	}

	@Override
	public List<Msisdn> listMsisdn(String jobname, int jobRunningState) {
		return msisdnDao.listMsisdn(jobname,jobRunningState);
	}

	@Override
	public void update(Integer id, String jobname, String status) {
		msisdnDao.update(id,jobname,status);
		
	}

	@Override
	public void update(ArrayList<Msisdn> elements, String jobname) {
		msisdnDao.update(elements,jobname);
	}

	@Override
	public void resetRetryStatus(String jobname) {
		msisdnDao.resetRetryStatus(jobname);
	}

	@Override
	public List<String> getReasonColumns(String jobname) {
		return msisdnDao.getReasonColumns(jobname);
	}



}
