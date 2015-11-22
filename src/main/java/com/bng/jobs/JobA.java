package com.bng.jobs;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.jms.core.MessageCreator;

import com.bng.core.jsonBean.Event;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.Utility;
import com.bng.core.utils.coreException;
import com.bng.entity.Msisdn;
import com.bng.entity.Service;
import com.bng.scheduler.JobStarter;
import com.bng.scheduler.JobState.JobRunningState;
import com.bng.scheduler.MsisdnList;
import com.bng.scheduler.SchedulerManager;
import com.bng.scheduler.jsonResponse.Resources;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class JobA implements Job {

	public static final String service = "service";
	public static final String msisdns = "msisdns";
	
//	@Autowired
	private SchedulerManager schedulerManager;
	
	private Integer hardware=null;
	private Integer vid=null;
	
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException 
	{
		schedulerManager = JobStarter.schedulerManager;
		JobDataMap data = context.getJobDetail().getJobDataMap();
		Service service = (Service) data.get(JobA.service);
		MsisdnList msisdnList =(MsisdnList) data.get(JobA.msisdns);
		if(!msisdnList.isJobOver()){

			Msisdn msisdn = msisdnList.get();
			if(!msisdn.getMsisdn().equalsIgnoreCase("no_msisdn"))
			{
				while(!getresources(service.getJobname(),msisdn.getMsisdn()))
				{
					try {
						
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Logger.sysLog(LogValues.error, this.getClass().getName(), coreException.GetStack(e));
					}
				}
//				vid=1;
//				hardware=1;
				Event event = generateEvent(service,msisdn);
				Logger.sysLog(LogValues.debug, this.getClass().getName(), "("+service.getJobname()+")adding event to queue(original) : "+event.toString());
				schedulerManager.getJmsTemplate().send(schedulerManager.getQueue(),getMessageCreator(Utility.convertObjectToJsonStr(event)));
				Logger.sysLog(LogValues.info, this.getClass().getName(), "("+service.getJobname()+")adding event to queue(original) : "+event.toString());
				msisdnList.updateSentStatusInDb(msisdn);
				SchedulerManager.counter++;
				if((System.currentTimeMillis()-SchedulerManager.time)>1000){
					Logger.sysLog(LogValues.info, this.getClass().getName(), SchedulerManager.counter+" msisdns sent to core engine in last sec. tps : "+schedulerManager.getTps());
					SchedulerManager.time = System.currentTimeMillis();
					SchedulerManager.counter=0;
				}
			}
		}
	}

	private Event generateEvent(Service service,Msisdn msisdn) {
		Event event = new Event();
		
		if(service.isRecorddedication()){
			event.setaPartyMsisdn(msisdn.getCli());
			event.setJobType("recorddedication");
		}
		else event.setaPartyMsisdn(service.getObdClis().get((int) Math.random()* service.getObdClis().size()).getCli());
		event.setEvent(11);
		event.setSubEvent(1);
		event.setvId(this.vid);
		event.setHardware(this.hardware);
		event.setIvrCode(service.getMxgraph().getShortcode());
		event.setCallType(1);
		event.setIp(schedulerManager.getTelephonyIps().get((int) Math.random()* schedulerManager.getTelephonyIps().size()));
		event.setSeekBytes("");
		event.setFilePath("");
		event.setSubEventCause(-1);
		event.setbPartyMsisdn(msisdn.getMsisdn());
		event.setServiceName(service.getMxgraph().getServiceName());
		event.setService(JobStarter.service);
		event.setCoreToTelephony(JobStarter.coreToTelephony);
		event.setDtmfBuffer("");
		event.setObdlist(service.getJobname());
		event.setProtocol(JobStarter.protocol);
		return event;
	}

	private boolean getresources(String jobname,String msisdn) {
		try {
			String response = schedulerManager.getFromServer(SchedulerManager.serverBaseUrl+SchedulerManager.hardwareUrl);
			while(response==null){
				response = schedulerManager.getFromServer(SchedulerManager.serverBaseUrl+SchedulerManager.hardwareUrl);
			}
			Resources resourcesReceived = Utility.convertJsonStrToObject(response, Resources.class);
			this.hardware = resourcesReceived.getHardware();
			this.vid = resourcesReceived.getvId();
			
			Logger.sysLog(LogValues.info, this.getClass().getName(), "("+jobname+":"+msisdn+")hardware: "+hardware +",vid : "+vid);
			if(vid==-1){
				SchedulerManager.needTps = true;
//				SchedulerManager.timeCount=0;
//				SchedulerManager.time10sec = System.currentTimeMillis();
				return false;
			}
			
			
		} catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(), "("+jobname+")getting resource error"+coreException.GetStack(e));
			return false;
		}
		return true;
	}

	private MessageCreator getMessageCreator(final String message) {
		return new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(message);
			}
		};
	}
	


}
