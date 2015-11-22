package com.bng.scheduler;

import java.util.Iterator;
import java.util.List;

import com.bng.bo.MsisdnBo;
import com.bng.bo.ServiceBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;
import com.bng.entity.Service;

public class JobEnder implements Runnable {

	private ServiceBo serviceBo;
	private MsisdnBo msisdnBo;

	private SchedulerManager schedulerManager;

	@Override
	public void run() {
//		System.out.println("entered jobender");
		try {
			//starting 5 sec. late so that the jobs which need to start gets started before deleting.
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e1));
		}
//		while (!BlackoutHoursMonitor.previousBlackoutHoursStatus) {
		while (true) {
			try {
				List jobToEnd = serviceBo.GetJobsToEnd();
				Service s;
				if (jobToEnd.size()>0) {
					Iterator listIterator = jobToEnd.iterator();
					while (listIterator.hasNext()) {
						schedulerManager.endJob((Service) listIterator.next());
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
		//now the thread is exiting which means blackout hours has started. we have to shut dow every job
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

}
