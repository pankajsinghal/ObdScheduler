package com.bng.jobs;

import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.impl.matchers.KeyMatcher;

import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;
import com.bng.entity.Service;
import com.bng.scheduler.JobStarter;
import com.bng.scheduler.JobState;
import com.bng.scheduler.JobState.JobRunningState;
import com.bng.scheduler.MsisdnList;

public class JobListener implements org.quartz.JobListener {

	public static final String LISTENER_NAME = "JobListener";
	
	@Override
	public String getName() {
		return LISTENER_NAME;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
//		System.out.println("started :"+System.currentTimeMillis());
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context,
			JobExecutionException jobException) {
		JobDataMap data = context.getJobDetail().getJobDataMap();
		Service service = (Service) data.get(JobA.service);
		MsisdnList msisdnList =(MsisdnList) data.get(JobA.msisdns);
		if(!msisdnList.isStarcopy())Logger.sysLog(LogValues.info, this.getClass().getName(), "("+service.getJobname()+":"+msisdnList.getJobname()+") msisdnList.isJobOver(): "+msisdnList.isJobOver()+") msisdnList.getJobRunningState(): "+JobRunningState.values[msisdnList.getJobRunningState()].toString());
		if(msisdnList.isJobOver())
		{
			service.setRemainingRetry(service.getRemainingRetry()-1);
			if(msisdnList.getJobRunningState()==JobRunningState.jobover.ordinal())
			{
				if(service.getRemainingRetry()<0){
					service.setStatus(JobState.PROCESSED);
				}
				else{
					Logger.sysLog(LogValues.info, this.getClass().getName(), "inside retry");
					JobStarter.serviceBo.update(service);// update "remaining entry" change in db
					msisdnList.resetSettingsToRetry();
//					System.out.println("sending obd_msisdn_" + service.getJobname()+" to scheduled");
					JobStarter.msisdnBo.resetRetryStatus(service.getJobname());
					service.setStatus(JobState.RETRY);	//because running-status jobs can be rescheduled. we don't want to block the channels..:P
					JobStarter.schedulerManager.startTimer(context.getJobDetail().getKey(), JobStarter.retryWaitTimeInSec);
					Logger.sysLog(LogValues.info, this.getClass().getName(), "("+service.getJobname()+":"+msisdnList.getJobname()+") new jobover status : msisdnList.isJobOver(): "+msisdnList.isJobOver()+") msisdnList.getJobRunningState(): "+JobRunningState.values[msisdnList.getJobRunningState()].toString());
					return;
				}
				
			}
			else if(msisdnList.getJobRunningState()==JobRunningState.failed.ordinal())	//this failed is due to msisdn table not found at the very initial level
				service.setStatus(JobState.FAILED);
			Logger.sysLog(LogValues.info, this.getClass().getName(), "job processed: "+service.getJobname());
			JobStarter.serviceBo.update(service);
			try {
				List<Matcher<JobKey>> matcher = JobStarter.schedulerManager.getMatchers();
				matcher.remove(KeyMatcher.keyEquals(new JobKey(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup())));
				JobStarter.schedulerManager.setMatchers(matcher);
				JobStarter.schedulerManager.getScheduler().getListenerManager().addJobListener(new JobListener(),matcher);
				boolean status = JobStarter.schedulerManager.getScheduler().deleteJob(new JobKey(context.getJobDetail().getKey().getName(), context.getJobDetail().getKey().getGroup()));
				Logger.sysLog(LogValues.info, this.getClass().getName(), "job delete status: "+status);
				JobStarter.schedulerManager.rescheduleRunningJobs();
			} catch (Exception e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
			}
		}
	}

}
