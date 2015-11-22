package com.bng.scheduler;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import com.bng.bo.MsisdnBo;
import com.bng.bo.ServiceBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;
import com.bng.entity.Service;
import com.bng.jobs.JobA;

public class JobResumer implements Runnable, Serializable {


	public static SchedulerManager schedulerManager;
	
	
	@Override
	public void run() {
		while (!BlackoutHoursMonitor.previousBlackoutHoursStatus) {
			JobTimerObject jobTimerObject = schedulerManager.expiredTimer();
			try {
				JobDataMap data = schedulerManager.getScheduler().getJobDetail(jobTimerObject.getJobKey()).getJobDataMap();
				Service service = (Service) data.get(JobA.service);
				if(service.getStatus().equalsIgnoreCase(JobState.RETRY)){
					MsisdnList msisdnList =(MsisdnList) data.get(JobA.msisdns);
					msisdnList.extractMsisdns();
					service.setStatus(JobState.RUNNING);
					if((jobTimerObject.getTimeOut()/1000)>300)		//if interval is > 5 min, we should reschedule
						schedulerManager.rescheduleRunningJobs();
					schedulerManager.getScheduler().resumeJob(jobTimerObject.getJobKey());
					Logger.sysLog(LogValues.info, this.getClass().getName(), "resuming job :"+jobTimerObject.getJobKey().getName()+":"+jobTimerObject.getJobKey().getGroup());
				}
				else if(service.getStatus().equalsIgnoreCase(JobState.PAUSED)){
					MsisdnList msisdnList =(MsisdnList) data.get(JobA.msisdns);
					msisdnList.extractMsisdns();
					msisdnList.updateMsisdnsForStopJob();
					Logger.sysLog(LogValues.info, this.getClass().getName(), "could not resume job :"+jobTimerObject.getJobKey().getName()+":"+jobTimerObject.getJobKey().getGroup()+" because of following reason - Job_Status : " + service.getStatus());
				}
				else 
					Logger.sysLog(LogValues.info, this.getClass().getName(), "could not resume job :"+jobTimerObject.getJobKey().getName()+":"+jobTimerObject.getJobKey().getGroup()+" because of following reason - Job_Status : " + service.getStatus());
			} catch (SchedulerException e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
			}
		}
	}


	
	public SchedulerManager getSchedulerManager() {
		return schedulerManager;
	}

	public void setSchedulerManager(SchedulerManager schedulerManager) {
		this.schedulerManager = schedulerManager;
	}

	
}
