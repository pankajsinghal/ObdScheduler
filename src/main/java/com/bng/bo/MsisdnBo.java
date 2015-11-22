package com.bng.bo;

import java.util.ArrayList;
import java.util.List;

import com.bng.entity.Msisdn;


public interface MsisdnBo {
	public List<Msisdn> listMsisdn (String jobname, int jobRunningState);
	public void update(Integer id, String jobname, String status);
	public void update(ArrayList<Msisdn> elements,String jobname);
	public void resetRetryStatus(String jobname);
	public List<String> getReasonColumns(String jobname);
}
