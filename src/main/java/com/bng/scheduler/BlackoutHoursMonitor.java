package com.bng.scheduler;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bng.bo.MsisdnBo;
import com.bng.bo.ServiceBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;

public class BlackoutHoursMonitor implements Runnable {

	private ServiceBo serviceBo;
	private MsisdnBo msisdnBo;
	private JobStarter jobStarter;
	private JobEnder jobEnder;
	private JobResumer jobResumer;
	private ThreadPoolTaskExecutor taskExecutor;
	private SchedulerManager schedulerManager;
	volatile static boolean previousBlackoutHoursStatus = true;
	

	@Override
	public void run() {
		taskExecutor.execute(jobEnder);
		taskExecutor.execute(jobResumer);
		while (true) {
			try {
				boolean currentBlackoutHoursStatus = serviceBo.getBlackoutHoursStatus();
				if(!previousBlackoutHoursStatus && currentBlackoutHoursStatus){
					//entering blackout hours
					Logger.sysLog(LogValues.info, this.getClass().getName(), "entering blackout hours");
					previousBlackoutHoursStatus = true;
					if(schedulerManager.getScheduler().isStarted())
						schedulerManager.getScheduler().standby();
				}
				else if(previousBlackoutHoursStatus && !currentBlackoutHoursStatus){
					//exiting blackout hours
					Logger.sysLog(LogValues.info, this.getClass().getName(), "exiting blackout hours");
					if(schedulerManager.getScheduler().isInStandbyMode())
						schedulerManager.getScheduler().start();
					previousBlackoutHoursStatus = false;
					taskExecutor.execute(jobStarter);
					taskExecutor.execute(jobResumer);
//					taskExecutor.execute(jobEnder);
					
//					Thread t = new Thread(jobStarter);
//					t.start();
//					t = new Thread(jobEnder);
//					t.start();
					
				}
				Thread.sleep(60000);
			} catch (Exception e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
			}
		}
	}



	public ServiceBo getServiceBo() {
		return serviceBo;
	}

	public void setServiceBo(ServiceBo serviceBo) {
		this.serviceBo = serviceBo;
	}

	public MsisdnBo getMsisdnBo() {
		return msisdnBo;
	}

	public void setMsisdnBo(MsisdnBo msisdnBo) {
		this.msisdnBo = msisdnBo;
	}
	
	public SchedulerManager getSchedulerManager() {
		return schedulerManager;
	}

	public void setSchedulerManager(SchedulerManager schedulerManager) {
		this.schedulerManager = schedulerManager;
	}



	public JobStarter getJobStarter() {
		return jobStarter;
	}



	public void setJobStarter(JobStarter jobStarter) {
		this.jobStarter = jobStarter;
	}



	public JobEnder getJobEnder() {
		return jobEnder;
	}



	public JobResumer getJobResumer() {
		return jobResumer;
	}



	public void setJobResumer(JobResumer jobResumer) {
		this.jobResumer = jobResumer;
	}



	public void setJobEnder(JobEnder jobEnder) {
		this.jobEnder = jobEnder;
	}

	public ThreadPoolTaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
