package com.bng.scheduler;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import com.bng.bo.MsisdnBo;
import com.bng.bo.ServiceBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;
import com.bng.entity.Service;

public class JobStarter implements Runnable, Serializable {

	private static final long serialVersionUID = 1L;
	public static MsisdnBo msisdnBo;
	public static ServiceBo serviceBo;

//	public static int waitOnResources;
	
	public static SchedulerManager schedulerManager;
	
	public static String protocol;
	public static String service;
	public static String coreToTelephony;
	public static int retryWaitTimeInSec;

	

	@Override
	public void run() 
	{
		while (!BlackoutHoursMonitor.previousBlackoutHoursStatus) {
//			 System.out.println("entered jobstarter");
			try {
				
				List jobToStart = serviceBo.GetJobsToStart();
				// System.out.println("didnt get jobs");
				if (jobToStart.size()>0) {
					// System.out.println("got jobs");
					if(schedulerManager.getSum()==0){
						schedulerManager.evaluateInitialSum(jobToStart);
					}
					Iterator listIterator = jobToStart.iterator();
					while (listIterator.hasNext()) {
						schedulerManager.startJob((Service) listIterator.next());
					}
					schedulerManager.rescheduleRunningJobs();
				}
				// else
				// System.out.println("no jobs");
				Thread.sleep(60000);
			} catch (Exception e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
			}
		}
	}

	

	public MsisdnBo getMsisdnBo() {
		return msisdnBo;
	}

	public void setMsisdnBo(MsisdnBo msisdnBo) {
		this.msisdnBo = msisdnBo;
	}

	public ServiceBo getServiceBo() {
		return serviceBo;
	}

	public void setServiceBo(ServiceBo serviceBo) {
		this.serviceBo = serviceBo;
	}
	
	public SchedulerManager getSchedulerManager() {
		return schedulerManager;
	}

	public void setSchedulerManager(SchedulerManager schedulerManager) {
		this.schedulerManager = schedulerManager;
	}



	public String getProtocol() {
		return protocol;
	}



	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}



	public String getService() {
		return service;
	}



	public void setService(String service) {
		this.service = service;
	}



	public String getCoreToTelephony() {
		return coreToTelephony;
	}



	public void setCoreToTelephony(String coreToTelephony) {
		this.coreToTelephony = coreToTelephony;
	}



	public static int getRetryWaitTimeInSec() {
		return retryWaitTimeInSec;
	}



	public void setRetryWaitTimeInSec(int retryWaitTimeInSec) {
		this.retryWaitTimeInSec = retryWaitTimeInSec;
	}



//	public int getWaitOnResources() {
//		return waitOnResources;
//	}
//
//
//
//	public void setWaitOnResources(int waitOnResources) {
//		this.waitOnResources = waitOnResources;
//	}
	
}
