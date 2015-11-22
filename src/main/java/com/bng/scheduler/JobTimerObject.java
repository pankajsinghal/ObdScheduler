package com.bng.scheduler;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.quartz.JobKey;

import com.bng.core.jsonBean.Event;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;

public class JobTimerObject implements Delayed {
	private long id;
	private JobKey jobKey;
	private long timeOut;
	private long endTime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public JobKey getJobKey() {
		return jobKey;
	}

	public void setJobKey(JobKey jobKey) {
		this.jobKey = jobKey;
	}

	public long getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	@Override
	public boolean equals(Object obj) {
		JobTimerObject jobTimerObject = (JobTimerObject) obj;
		if (jobTimerObject.getId() == id) {
			Logger.sysLog(LogValues.info, this.getClass().getName(),"removed jobtimer id: " + id + "\nreturning true");
//			Timer.removedTimerObject = jobTimerObject;
			return true;
		} else
			return false;
	}

	public long getDelay(TimeUnit unit) {
		return unit.convert((endTime - System.currentTimeMillis()),
				TimeUnit.MILLISECONDS);

	}

	public int compareTo(Delayed o) {
		return Long.valueOf(endTime).compareTo(((JobTimerObject) o).getEndTime());
	}

	/*@Override
	public String toString() {
		String s ="id : "+ id + ",event: "+event.toString() +",startTime: "+startTime + ",timeOut: "+timeOut+",endTime: "+endTime +",queueName: "+queueName;

		return s;
	}*/
	
}