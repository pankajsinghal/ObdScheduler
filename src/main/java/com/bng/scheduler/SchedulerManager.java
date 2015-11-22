package com.bng.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.DelayQueue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.KeyMatcher;
import org.springframework.jms.core.JmsTemplate;

import com.bng.bo.MsisdnBo;
import com.bng.bo.ServiceBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.Utility;
import com.bng.core.utils.coreException;
import com.bng.entity.Service;
import com.bng.jobs.JobA;
import com.bng.jobs.JobListener;
import com.bng.scheduler.JobState.JobRunningState;
import com.bng.scheduler.jsonResponse.Tps;

/*@ManagedBean(name = "scheduler")
 @SessionScoped*/
public class SchedulerManager implements Serializable {

	private MsisdnBo msisdnBo;
	private ServiceBo serviceBo;
	private static final long serialVersionUID = 1L;

	private Scheduler scheduler;
	private List<Matcher<JobKey>> matchers = new ArrayList<Matcher<JobKey>>();

	private List<QuartzJob> quartzJobList = new ArrayList<QuartzJob>();
	
	private DelayQueue<JobTimerObject> pauseJobQueue =  new DelayQueue<JobTimerObject>();	//for pausing for a certain time before every retry attempt

	public static String serverBaseUrl;
	public static String tpsUrl;
	public static String hardwareUrl;

	private boolean allJobsPaused = false;
	private int tps;
	public static boolean needTps = false;
	private int sum = 0;

	volatile public static int counter = 0;
	public static long time = System.currentTimeMillis();
	public static long time10sec = System.currentTimeMillis();
//	public static int timeCount = -1;

	private JmsTemplate jmsTemplate;
	private String queue;
	private String queuedb = "dbdirectQuery";
	private ArrayList<String> telephonyIps;

	private BlackoutHoursMonitor blackoutHoursMonitor;

	// public SchedulerBean() throws SchedulerException {

	// System.out.println("started");
	// ServletContext servletContext = (ServletContext) FacesContext
	// .getCurrentInstance().getExternalContext().getContext();
	// StdSchedulerFactory stdSchedulerFactory = (StdSchedulerFactory)
	// servletContext
	// .getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
	// scheduler = stdSchedulerFactory.getScheduler();

	// }

	// @PostConstruct
	public void init() {
		Thread t = new Thread(blackoutHoursMonitor);
		t.start();
		refresh();
	}

	// method to get tps from telephony.
	public void getTpsFromTelephonyServer() {
		String response = null;
		try {
			response = getFromServer(serverBaseUrl + tpsUrl);
			// response = hitURL(serverBaseUrl + tpsUrl);
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					com.bng.core.utils.coreException.GetStack(e));
		}
		if (response != null) {
			Tps tpsReceived = Utility.convertJsonStrToObject(response,
					Tps.class);
			this.tps = tpsReceived.getTps();
		}
	}

	public synchronized String getFromServer(String url) throws IOException {
		Logger.sysLog(LogValues.info, this.getClass().getName(),
				"sending request for vid & hardware");
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		// print result
		// System.out.println(response.toString());
		return response.toString();

	}

	// public synchronized String hitURL(String surl){
	//
	// Logger.sysLog(LogValues.info,this.getClass().getName(),"url to hit "+surl);
	// String szCnt = "";
	// HttpMethod method = null;
	// try {
	// HttpClient client = new HttpClient();
	// client.setConnectionTimeout(10000);
	// method = new GetMethod(surl);
	// int status = client.executeMethod(method);
	// if (status > 0) {
	// szCnt = method.getResponseBodyAsString();
	// } else {
	// szCnt = "Err";
	// }
	// } catch (Exception e) {
	// Logger.sysLog(LogValues.error,this.getClass().getName(),com.bng.core.utils.coreException.GetStack(e));
	//
	// } finally {
	// method.releaseConnection();
	// }
	// Logger.sysLog(LogValues.debug,this.getClass().getName(),"url response "+szCnt);
	// return szCnt;
	// }

	public synchronized List<QuartzJob> refresh() {
		quartzJobList.clear();
		// loop jobs by group
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				// System.out.println("scheduler.getJobGroupNames() : "+Arrays.toString(scheduler.getJobGroupNames().toArray()));
				// get jobkey
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					// System.out.println("scheduler.getJobKeys : "+Arrays.toString(scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName)).toArray()));
					String jobName = jobKey.getName();
					String jobGroup = jobKey.getGroup();
					// System.out.println(jobName +" : "+jobGroup);
					// get job's trigger
					@SuppressWarnings("unchecked")
					List<Trigger> triggers = ((List<Trigger>) scheduler
							.getTriggersOfJob(jobKey));
					Date nextFireTime = null;
					if (triggers.size() > 0)
						nextFireTime = triggers.get(0).getNextFireTime();

					if (!quartzJobList.contains(new QuartzJob(jobName,
							jobGroup, nextFireTime))) {
						quartzJobList.add(new QuartzJob(jobName, jobGroup,
								nextFireTime));
						// System.out.println("new element added : "+jobName
						// +" : "+jobGroup);
						// System.out.println("net queue size : "+quartzJobList.size());
					}

				}

			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
		return quartzJobList;
	}

	public synchronized String changeTps(int tps) {
		this.tps = tps;
		needTps = false;
//		timeCount=-1;
		Logger.sysLog(LogValues.info, this.getClass().getName(),
				"(TPS received from Telephony)current tps : " + this.tps);
		if (!allJobsPaused && tps == 0) {
			pauseRunningJobs();
			allJobsPaused = true;
		} else
			try {
				if (scheduler.getJobGroupNames() != null && tps != 0) {
					if (allJobsPaused) {
						resumeRunningJobs();
						allJobsPaused = false;
					}
					rescheduleRunningJobs();
				}
			} catch (Exception e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(),
						coreException.GetStack(e));
				return ExceptionUtils.getRootCauseMessage(e).replace("'", "");
			}
		return "ok";
	}

	public synchronized void rescheduleRunningJobs() {
		try {
			Logger.sysLog(LogValues.info, this.getClass().getName(),
					"rescheduleRunningJobs(): ");
			reevaluateSum();
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					// System.out.println(jobKey.toString());
					@SuppressWarnings("unchecked")
					List<Trigger> triggers = ((List<Trigger>) scheduler
							.getTriggersOfJob(jobKey));

					// System.out.println("triggers.size(): " +triggers.size());
					if (triggers.size() < 1)
						continue;
					TriggerKey triggerKey = new TriggerKey(triggers.get(0)
							.getKey().getName(), triggers.get(0).getKey()
							.getGroup());

					JobDetail jobDetail = scheduler.getJobDetail(jobKey);
					Service service = (Service) jobDetail.getJobDataMap().get(
							JobA.service);
					if (!service.getStatus().equalsIgnoreCase(JobState.RUNNING))
						continue;

					long interval = (sum * 1000)
							/ (service.getPriority() * tps);// in
					// System.out.println("rescheduleRunningJobs interval = "+
					// interval);
					SimpleTrigger trigger1 = TriggerBuilder
							.newTrigger()
							.withIdentity(triggerKey)
							.withSchedule(
									SimpleScheduleBuilder
											.simpleSchedule()
											.withIntervalInMilliseconds(
													interval).repeatForever())
							.build();

					try {
						scheduler.rescheduleJob(triggerKey, trigger1);
					} catch (Exception e) {
						Logger.sysLog(LogValues.error,
								this.getClass().getName(),
								coreException.GetStack(e));
					}

				}
			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	synchronized void reevaluateSum() {
		sum = 0;
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					//
					JobDetail jobDetail;
					try {
						jobDetail = scheduler.getJobDetail(jobKey);
						Service service = (Service) jobDetail.getJobDataMap()
								.get(JobA.service);

						if (service.getStatus().equalsIgnoreCase(
								JobState.RUNNING))
							sum += service.getPriority();
					} catch (Exception e) {
						Logger.sysLog(LogValues.error,
								this.getClass().getName(),
								coreException.GetStack(e));
					}

				}
			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	synchronized void evaluateInitialSum(List list) {
		sum = 0;
		Iterator listIterator = list.iterator();
		while (listIterator.hasNext()) {
			Service service = (Service) listIterator.next();
			sum += service.getPriority();
			// System.out.println("service.getJobname(): "+service.getJobname());
		}

		// System.out.println("(evaluateInitialSum) sum = "+sum);
	}

	// resume all jobs with running status.(coz tps went from 0 to non-0)
	private synchronized void resumeRunningJobs() {
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					//
					JobDetail jobDetail;
					try {
						jobDetail = scheduler.getJobDetail(jobKey);
						Service service = (Service) jobDetail.getJobDataMap()
								.get(JobA.service);

						if (service.getStatus().equalsIgnoreCase(
								JobState.RUNNING))
							scheduler.resumeJob(jobKey);
					} catch (Exception e) {
						Logger.sysLog(LogValues.error,
								this.getClass().getName(),
								coreException.GetStack(e));
					}

				}
			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	// pause all jobs with running status.(coz tps went 0)
	private synchronized void pauseRunningJobs() {
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					//
					JobDetail jobDetail;
					try {
						jobDetail = scheduler.getJobDetail(jobKey);
						Service service = (Service) jobDetail.getJobDataMap()
								.get(JobA.service);

						if (service.getStatus().equalsIgnoreCase(
								JobState.RUNNING))
							scheduler.pauseJob(jobKey);
					} catch (Exception e) {
						Logger.sysLog(LogValues.error,
								this.getClass().getName(),
								coreException.GetStack(e));
					}

				}
			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	synchronized void startJob(Service service,int... retry) {
		try {
			String jobName = service.getJobname();

			JobDetail job1 = JobBuilder.newJob(JobA.class)
					.withIdentity(jobName, JobState.JOB_GROUP).storeDurably()
					.build();

			// Logger.sysLog(LogValues.info, this.getClass().getName(),
			// "starting job: " + jobName);
			long interval = (sum * 1000) / (service.getPriority() * tps);// in
																			// millis
																			// System.out.println("startJob interval = "+
																			// interval);
			Logger.sysLog(LogValues.info, this.getClass().getName(),
					"starting job: " + jobName + " & tps : " + tps
							+ " & interval : " + interval);

			MsisdnList msisdnList;
			if(retry.length>0)
				msisdnList= new MsisdnList(service.getJobname(),service.isStarcopy(),retry[0],(service.getMaxRetry()>0?true:false));
			else
				msisdnList= new MsisdnList(service.getJobname(),service.isStarcopy(),(service.getMaxRetry()>0?true:false));
			Logger.sysLog(LogValues.info, this.getClass().getName(),
					"getting initial msisdn for job: " + jobName);
			SimpleTrigger trigger1 = TriggerBuilder
					.newTrigger()
					.withIdentity("trigger " + service.getJobname(),
							JobState.JOB_GROUP)
					.withSchedule(
							SimpleScheduleBuilder.simpleSchedule()
									.withIntervalInMilliseconds(interval)
									.repeatForever()).build();

			job1.getJobDataMap().put(JobA.service, service);
			job1.getJobDataMap().put(JobA.msisdns, msisdnList);

			Logger.sysLog(LogValues.info, this.getClass().getName(),
					"scheduling job: " + jobName);
			matchers.add(KeyMatcher.keyEquals(new JobKey(jobName,
					JobState.JOB_GROUP)));
			scheduler.getListenerManager().addJobListener(
					new JobListener(), matchers);
			scheduler.scheduleJob(job1, trigger1);
			Logger.sysLog(LogValues.info, this.getClass().getName(),
					"job scheduled: " + jobName);
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	synchronized void endJob(Service service) {
		try {
			for (String groupName : scheduler.getJobGroupNames()) {
				// get jobkey
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher
						.jobGroupEquals(groupName))) {
					if (jobKey.getName().equalsIgnoreCase(service.getJobname())
							&& jobKey.getGroup().equalsIgnoreCase(
									JobState.JOB_GROUP)) {

						processEndingJob(jobKey.getName(), jobKey.getGroup(),
								JobState.STOPPED);
						Logger.sysLog(LogValues.info,
								this.getClass().getName(),
								"deleted job: " + jobKey.getName() + " --> "
										+ jobKey.getGroup());
						// try {
						// matchers.remove(KeyMatcher.keyEquals(jobKey));
						// scheduler.getListenerManager().addJobListener(new
						// JobListener(),matchers);
						// boolean status = scheduler.deleteJob(jobKey);
						// } catch (SchedulerException e) {
						// Logger.sysLog(LogValues.error,
						// this.getClass().getName(),
						// coreException.GetStack(e));
						// }
						// MsisdnList msisdnList;
						// try {
						// msisdnList = (MsisdnList) scheduler
						// .getJobDetail(jobKey).getJobDataMap()
						// .get(JobA.msisdns);
						//
						// msisdnList.updateMsisdns();
						//
						// } catch (SchedulerException e) {
						// Logger.sysLog(LogValues.error,
						// this.getClass().getName(),
						// coreException.GetStack(e));
						// }
						// System.out.println("status: " + status);
					}
				}
			}
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		}
	}

	// trigger a job
	// public synchronized void fireNow(String jobName, String jobGroup){
	// JobKey jobKey = new JobKey(jobName, jobGroup);
	// try {
	// scheduler.triggerJob(jobKey);
	// } catch (SchedulerException e) {
	// Logger.sysLog(LogValues.error, this.getClass().getName(),
	// coreException.GetStack(e));
	// }
	// }

	public synchronized String pause(String jobName, String jobGroup) {

		JobKey jobKey = new JobKey(jobName, jobGroup);
		JobDetail jobDetail;
		try {
			if(!scheduler.checkExists(jobKey)) 
				return "Job ["+jobName+"] does not exist.";
			jobDetail = scheduler.getJobDetail(jobKey);
			Service service = (Service) jobDetail.getJobDataMap().get(
					JobA.service);
			MsisdnList msisdnList = (MsisdnList) jobDetail.getJobDataMap().get(
					JobA.msisdns);
			jobDetail.getJobDataMap().put(JobA.service, service);
			jobDetail.getJobDataMap().put(JobA.msisdns, msisdnList);
			scheduler.addJob(jobDetail, true);
			scheduler.pauseJob(jobKey);
			msisdnList.updateMsisdnsForStopJob();
			service.setStatus(JobState.PAUSED);
			serviceBo.update(service);
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return ExceptionUtils.getRootCauseMessage(e).replace("'", "");
		}
		this.rescheduleRunningJobs();
		return "ok";
	}

	public synchronized String resume(String jobName, String jobGroup) {

		JobKey jobKey = new JobKey(jobName, jobGroup);
		JobDetail jobDetail;
		try {
			if(scheduler.checkExists(jobKey)){
				jobDetail = scheduler.getJobDetail(jobKey);
				Service service = (Service) jobDetail.getJobDataMap().get(
						JobA.service);
				MsisdnList msisdnList = (MsisdnList) jobDetail.getJobDataMap().get(
						JobA.msisdns);
				service = serviceBo.update(service.getJobname(), JobState.RUNNING);
				msisdnList.updateMsisdnsForResumeJob();
				jobDetail.getJobDataMap().put(JobA.service, service);
				jobDetail.getJobDataMap().put(JobA.msisdns, msisdnList);
				scheduler.addJob(jobDetail, true);
				scheduler.triggerJob(jobKey);
				scheduler.resumeJob(jobKey);
			}
			else
			{
				Service s = serviceBo.getJob(jobName);
				if(s!=null && s.getStatus().equalsIgnoreCase("paused")){
					Service service = serviceBo.update(jobName, JobState.RUNNING);
					if(getSum()==0){
						ArrayList<Service> list = new ArrayList<Service>();
						list.add(service);
						evaluateInitialSum(list);
					}
					if(s.getMaxRetry()==s.getRemainingRetry())
						startJob(service,JobRunningState.initial.ordinal());
					if(s.getMaxRetry()>0 && s.getRemainingRetry() >=0 && (s.getMaxRetry()-s.getRemainingRetry()>0))
						startJob(service,JobRunningState.retry.ordinal());
				}
				
			}
			
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return ExceptionUtils.getRootCauseMessage(e).replace("'", "");
		}
		rescheduleRunningJobs();
		return "ok";
	}

	public synchronized String stop(String jobName, String jobGroup) {

		try {
			if(!scheduler.checkExists(new JobKey(jobName, JobState.JOB_GROUP))) 
				return "Job ["+jobName+"] does not exist.";
		} catch (SchedulerException e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return "Error occured. Please try again.";
		}
		String oldStatus = serviceBo.stopJob(jobName);
		if (oldStatus.equalsIgnoreCase(JobState.RUNNING)) {
			String status = processEndingJob(jobName, jobGroup, JobState.STOPPED);
			if(status.equalsIgnoreCase("ok"))
				rescheduleRunningJobs();
			else 
				return status;
		}
		return "ok";
	}

	public String processEndingJob(String jobName, String jobGroup, String status) {
		JobKey jobKey = new JobKey(jobName, jobGroup);
		JobDetail jobDetail;
		try {
			scheduler.pauseJob(jobKey);
			jobDetail = scheduler.getJobDetail(jobKey);
			Service service = (Service) jobDetail.getJobDataMap().get(
					JobA.service);
			service.setStatus(status);
			MsisdnList msisdnList = (MsisdnList) jobDetail.getJobDataMap().get(
					JobA.msisdns);
			jobDetail.getJobDataMap().put(JobA.service, service);
			jobDetail.getJobDataMap().put(JobA.msisdns, msisdnList);
			scheduler.addJob(jobDetail, true);
			msisdnList.updateMsisdnsForStopJob();
			// service.setStatus(JobState.STOPPED);
			// serviceBo.update(service);
			matchers.remove(KeyMatcher.keyEquals(jobKey));
			scheduler.getListenerManager().addJobListener(new JobListener(),
					matchers);
			scheduler.deleteJob(jobKey);
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return ExceptionUtils.getRootCauseMessage(e).replace("'", "");
		}
		return "ok";
	}

	public String startTimer(JobKey jobKey, int timeout) {
		Logger.sysLog(LogValues.info, this.getClass().getName(), "new jobkey received = "+jobKey.getName()+":"+jobKey.getGroup()+", timeout = "+timeout);
		JobTimerObject timerObject = generateTimerObject(jobKey, timeout);
		Logger.sysLog(LogValues.info, this.getClass().getName(),"registered timer object : "+timerObject.toString());
		String id = startTimer(timerObject);
		Logger.sysLog(LogValues.info, this.getClass().getName(), "new element added in queue: "+queue.toString() +"with id: "+id);
		return id;
	}
	
	private String startTimer(JobTimerObject jobtimerObject) {
		try {
			scheduler.pauseJob(jobtimerObject.getJobKey());
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
		}
		pauseJobQueue.put(jobtimerObject);
		if((jobtimerObject.getTimeOut()/1000)>300)		//if interval is > 5 min, we should reschedule
			rescheduleRunningJobs();
		//Logger.sysLog(LogValues.info, Timer.class.getName(), "seccussfully received. returning id  = "+ timerObject.getId());
		return Long.toString(jobtimerObject.getId());
	}
	
	/*
	 * timeout - in sec
	 */
	private synchronized JobTimerObject generateTimerObject(JobKey jobKey, int timeout) {
		JobTimerObject timerObject = new JobTimerObject();
		timerObject.setId(System.currentTimeMillis());
		timerObject.setJobKey(jobKey);
		timerObject.setTimeOut(timeout*1000);
		timerObject.setEndTime(timerObject.getId() + timerObject.getTimeOut());
		Logger.sysLog(LogValues.info, this.getClass().getName(), "successfully generated id  = "+ timerObject.getId());
		return timerObject;
	}
	
	protected JobTimerObject expiredTimer() {
		try {
			return pauseJobQueue.take();
		} catch (InterruptedException e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
		}
		return null;
	}
	
	public static class QuartzJob {

		private static final long serialVersionUID = 1L;

		String jobName;
		String jobGroup;
		Date nextFireTime;

		public QuartzJob(String jobName, String jobGroup, Date nextFireTime) {

			this.jobName = jobName;
			this.jobGroup = jobGroup;
			this.nextFireTime = nextFireTime;
		}

		public String getJobName() {
			return jobName;
		}

		public void setJobName(String jobName) {
			this.jobName = jobName;
		}

		public String getJobGroup() {
			return jobGroup;
		}

		public void setJobGroup(String jobGroup) {
			this.jobGroup = jobGroup;
		}

		public Date getNextFireTime() {
			return nextFireTime;
		}

		public void setNextFireTime(Date nextFireTime) {
			this.nextFireTime = nextFireTime;
		}

		@Override
		public boolean equals(Object obj) {
			QuartzJob quartzJob = (QuartzJob) obj;
			if (this.jobGroup == quartzJob.getJobGroup()
					&& this.jobName == quartzJob.getJobName())// &&
																// this.getNextFireTime()
																// ==
																// quartzJob.getNextFireTime())
				return true;
			else
				return false;
		}

	}

	public List<QuartzJob> getQuartzJobList() {

		return quartzJobList;
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

	public Scheduler getScheduler() {
		return scheduler;
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public int getTps() {
		return tps;
	}

	public void setTps(int tps) {
		this.tps = tps;
		// try {
		// changeTps(tps);
		// } catch (SchedulerException e) {
		// Logger.sysLog(LogValues.error, this.getClass().getName(),
		// coreException.GetStack(e));
		// }
	}

	public String getServerBaseUrl() {
		return serverBaseUrl;
	}

	public void setServerBaseUrl(String serverBaseUrl) {
		this.serverBaseUrl = serverBaseUrl;
	}

	public BlackoutHoursMonitor getBlackoutHoursMonitor() {
		return blackoutHoursMonitor;
	}

	public void setBlackoutHoursMonitor(
			BlackoutHoursMonitor blackoutHoursMonitor) {
		this.blackoutHoursMonitor = blackoutHoursMonitor;
	}

	public ArrayList<String> getTelephonyIps() {
		return telephonyIps;
	}

	public void setTelephonyIps(ArrayList<String> telephonyIps) {
		this.telephonyIps = telephonyIps;
	}

	public String getQueue() {
		return queue;
	}

	public void setQueue(String queue) {
		this.queue = queue;
	}

	public JmsTemplate getJmsTemplate() {
		return jmsTemplate;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public int getSum() {
		return sum;
	}

	public void setSum(int sum) {
		this.sum = sum;
	}

	public String getQueuedb() {
		return queuedb;
	}

	public void setQueuedb(String queuedb) {
		this.queuedb = queuedb;
	}

	public List<Matcher<JobKey>> getMatchers() {
		return matchers;
	}

	public void setMatchers(List<Matcher<JobKey>> matchers) {
		this.matchers = matchers;
	}

	public String getHardwareUrl() {
		return hardwareUrl;
	}

	public void setHardwareUrl(String hardwareUrl) {
		this.hardwareUrl = hardwareUrl;
	}

	public String getTpsUrl() {
		return tpsUrl;
	}

	public void setTpsUrl(String tpsUrl) {
		this.tpsUrl = tpsUrl;
	}

}