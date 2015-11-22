package com.bng.scheduler;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.core.MessageCreator;

import com.bng.bo.MsisdnBo;
import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.entity.Msisdn;
import com.bng.scheduler.JobState.JobRunningState;

public class MsisdnList {
	
	private MsisdnBo msisdnBo;
	
	private int jobRunningState=JobRunningState.initial.ordinal();
	private final int size = 1000;
	private ArrayList<Msisdn> elements;
	private String Jobname;
	private int currentPosition=0;
//	private int overallPosition=0;
	private boolean isLastChunk = false;
	private boolean jobOver = false;
	private boolean isStarcopy = false;
	private boolean retryEnabled=false;
	private Integer lastNumberState=null; 

	private ArrayList<Msisdn> elementsScheduled;
	private ArrayList<Msisdn> elementsRetry;
	private long starcopyLastRetryExtractedTime;
	private long starcopyLastScheduledExtractedTime;
	private int starcopyRetryInterval=2*60*1000;	// 60 min in millis
	private int starcopyScheduledInterval=1*60*1000			;	//5 min in millis
	
	public MsisdnList(String Jobname,boolean starcopy, boolean retryEnabled) {
		elements = new ArrayList<Msisdn>();
		this.Jobname = Jobname;
		this.isStarcopy = starcopy;
		msisdnBo = JobStarter.msisdnBo;
		this.retryEnabled = retryEnabled;
		extractMsisdns();
	}
	
	public MsisdnList(String Jobname,boolean starcopy,int retry, boolean retryEnabled) {
		elements = new ArrayList<Msisdn>();
		this.Jobname = Jobname;
		this.isStarcopy = starcopy;
		msisdnBo = JobStarter.msisdnBo;
		jobRunningState = retry;
		this.retryEnabled = retryEnabled;
		extractMsisdns();
	}

	public void extractMsisdns() {
		if(!isStarcopy){
			Logger.sysLog(LogValues.info, this.getClass().getName(),"jobRunningState : "+JobRunningState.values()[jobRunningState]);
			elements = (ArrayList<Msisdn>)msisdnBo.listMsisdn(Jobname,jobRunningState);
			if(elements==null ) {
				Logger.sysLog(LogValues.info, this.getClass().getName(), Jobname+" : extracted msisdns returned null");
				jobOver = true;
				jobRunningState = JobRunningState.failed.ordinal();
				return;
			}
			else if(elements.size()<1){
				Logger.sysLog(LogValues.info, this.getClass().getName(), Jobname+" : extracted msisdns list is < 1.");
				jobOver = true;
				jobRunningState = JobRunningState.jobover.ordinal();
				return;
			}
			if(elements.size() < size) isLastChunk = true;
			currentPosition=0;
//			overallPosition += elements.size();
			Logger.sysLog(LogValues.info, this.getClass().getName(), "extracted "+elements.size()+" more msisdns for "+ Jobname+" ["+JobRunningState.values()[jobRunningState]+"]");
		}
		else if(isStarcopy){
			starcopyExtractMsisdnsScheduled();
			if(retryEnabled)starcopyExtractMsisdnsRetry();
		}
	}
	
	private void starcopyExtractMsisdnsRetry() {
		elementsRetry = (ArrayList<Msisdn>)msisdnBo.listMsisdn(Jobname,JobRunningState.retryStarcopy.ordinal());
		starcopyLastRetryExtractedTime = System.currentTimeMillis();
		if(elementsRetry==null ) {
			Logger.sysLog(LogValues.info, this.getClass().getName(), Jobname+" : extracted msisdns returned null");
			jobOver = true;
			jobRunningState = JobRunningState.failed.ordinal();
			return;
		}
		Logger.sysLog(LogValues.info, this.getClass().getName(), "extracted "+elementsRetry.size()+" more msisdns for "+ Jobname +"(starcopy retry)");
	}

	private void starcopyExtractMsisdnsScheduled() {
		elementsScheduled = (ArrayList<Msisdn>)msisdnBo.listMsisdn(Jobname,JobRunningState.initial.ordinal());
		starcopyLastScheduledExtractedTime = System.currentTimeMillis();
		if(elementsScheduled==null ) {
			Logger.sysLog(LogValues.info, this.getClass().getName(), Jobname+" : extracted msisdns returned null");
			jobOver = true;
			jobRunningState = JobRunningState.failed.ordinal();
			return;
		}
		Logger.sysLog(LogValues.info, this.getClass().getName(), "extracted "+elementsScheduled.size()+" more msisdns for "+ Jobname +"(starcopy scheduled)");
	}

	public Msisdn get(){
		if(!isStarcopy){
			return getNormal();
		}
		else{
			return getStarcopy();
		}
		
	}

	private Msisdn getStarcopy() {
		if((System.currentTimeMillis()>starcopyLastScheduledExtractedTime+starcopyScheduledInterval) && elementsScheduled.size()<1){
			starcopyExtractMsisdnsScheduled();
		}
		if(retryEnabled && (System.currentTimeMillis()>starcopyLastRetryExtractedTime+starcopyRetryInterval) && elementsRetry.size()<1){
			starcopyExtractMsisdnsRetry();
		}
		if(elementsScheduled==null && elementsRetry==null) extractMsisdns();
		if(elementsScheduled==null && elementsRetry==null) {
			//this failed is due to msisdn not found while the job was running(something might have happened while pausing or any other thing)
			Logger.sysLog(LogValues.info, this.getClass().getName(), "inside getStarcopy(). No msisdns found");
			String query = "update service set status = '"+JobState.FAILED+"' where jobname = '"+Jobname+"'";
			JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query));
//			jobOver = true;
			return null;
		}
		Msisdn msisdn=null;
		if(elementsScheduled.size()>0){
			msisdn = elementsScheduled.get(0);
			elementsScheduled.remove(msisdn);
			jobRunningState = JobRunningState.initial.ordinal();
		}
		else if(retryEnabled && elementsRetry.size()>0){
			msisdn = elementsRetry.get(0);
			elementsRetry.remove(msisdn);
			jobRunningState = JobRunningState.retry.ordinal();
		}
		else{
			msisdn = new Msisdn(1, "no_msisdn", "", "", "");
			return msisdn;
		}
		msisdn.setStatus(JobState.MSISDN_STATE_TO_CORE_ENGINE);
		Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+")processing : "+msisdn.getMsisdn());
		Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+")isLastChunk ("+isLastChunk+") & currentPosition ("+currentPosition+") & elements.size() ("+elements.size()+")");
		return msisdn;
	}

	private Msisdn getNormal() {
		if(elements==null)	extractMsisdns();
		if(elements==null){
			//this failed is due to msisdn not found while the job was running(something might have happened while pausing or any other thing)
			Logger.sysLog(LogValues.info, this.getClass().getName(), "inside getNormal(). No msisdns found");
			String query = "update service set status = '"+JobState.FAILED+"' where jobname = '"+Jobname+"'";
			JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query));
//			jobOver = true;
			return null;
		}
		Msisdn msisdn = elements.get(currentPosition);
		msisdn.setStatus(JobState.MSISDN_STATE_TO_CORE_ENGINE);
		elements.set(currentPosition, msisdn);
		currentPosition++;
		Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+")processing : "+msisdn.getMsisdn());
		Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+")isLastChunk ("+isLastChunk+") & currentPosition ("+currentPosition+") & elements.size() ("+elements.size()+")");
		if(!isLastChunk && currentPosition >=size)
		{
//			updateMsisdns();
			extractMsisdns();
		}
		else if(isLastChunk && currentPosition >= elements.size()){
			Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+") last chunck is over.");
//			updateMsisdns();
			jobOver = true;
			lastNumberState = jobRunningState;
			jobRunningState = JobRunningState.jobover.ordinal();
			Logger.sysLog(LogValues.info, this.getClass().getName(), "("+Jobname+")job over status : "+jobRunningState);
		}
		return msisdn;
	}

//	public void updateMsisdns() {
////		if(elements!=null)
////		msisdnBo.update(elements,Jobname);
////		elements = null;
//	}

	
	public void updateMsisdnsForStopJob(){
		if(isStarcopy){
			updateMsisdnsForStopJobScheduled(elementsScheduled);
			if(retryEnabled)updateMsisdnsForStopJobRetry(elementsRetry);
		}
		else if(!isStarcopy){
			if(jobRunningState==JobRunningState.initial.ordinal())
			{
				updateMsisdnsForStopJobScheduled(elements);
			}
			else if(jobRunningState==JobRunningState.retry.ordinal())
			{
				updateMsisdnsForStopJobRetry(elements);
			}
		}
		
	}

	private void updateMsisdnsForStopJobScheduled(ArrayList<Msisdn> elements) {
		if(elements.size()-currentPosition<1){
			Logger.sysLog(LogValues.info, this.getClass().getName(), "paused/stopped job(initial) : No msisdns for updating ");
			return;
		}
		StringBuilder query = new StringBuilder("update obd_msisdn_"+Jobname +" set status = '"+JobState.SCHEDULED+"' where status = '"+JobState.PICKED+"' and (");
		for(int i=currentPosition;i<elements.size();i++){
			query.append(" msisdn = '"+elements.get(i).getMsisdn()+"' or");
		}
		query.delete(query.length()-2, query.length());
		query.append(")");
		JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query.toString()));
		Logger.sysLog(LogValues.info, this.getClass().getName(), "paused/stopped job(initial) : query : "+query);
	}
	
	private void updateMsisdnsForStopJobRetry(ArrayList<Msisdn> elements) {
		if(elements.size()-currentPosition<1){
			Logger.sysLog(LogValues.info, this.getClass().getName(), "paused/stopped job(retry) : No msisdns for updating ");
			return;
		}
		String tablename = "obd_msisdn_" + Jobname;
		List<String> reasons = msisdnBo.getReasonColumns(Jobname);
		StringBuilder SQL = new StringBuilder("UPDATE "+tablename
				+" SET ");
		for(String reason:reasons){
			if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
				SQL.append(reason +" = IF(reason = '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',"+reason+"+ 1, "+reason+"),");
       	}
		SQL.append( JobState.FAILED_REASON_PREFIX+"status = '"+JobState.SCHEDULED + (isStarcopy?("' ,`"+JobState.FAILED_REASON_PREFIX+"max-retry` = `"+JobState.FAILED_REASON_PREFIX+"max-retry` + 1 "):"'")
			    +" WHERE reason IN(");
	    for(String reason:reasons){
	    	if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
	    		SQL.append("'"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',");
       	}
	    SQL.deleteCharAt(SQL.length()-1);
		SQL.append(")  and "+JobState.FAILED_REASON_PREFIX+"status='"+JobState.PICKED+"' and (");
		for(int i=currentPosition;i<elements.size();i++){
			SQL.append(" msisdn = '"+elements.get(i).getMsisdn()+"' or");
		}			
		SQL.delete(SQL.length()-2, SQL.length());
		SQL.append(" )");
		JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(SQL.toString()));
		Logger.sysLog(LogValues.info, this.getClass().getName(), "paused/stopped job(retry) : query : "+SQL);
	}

	public void updateMsisdnsForResumeJob(){
		if(isStarcopy){
			updateMsisdnsForResumeJobScheduled(elementsScheduled);
			if(retryEnabled)updateMsisdnsForResumeJobRetry(elementsRetry);
		}
		else if(!isStarcopy){
			if(jobRunningState==JobRunningState.initial.ordinal())
			{
				updateMsisdnsForResumeJobScheduled(elements);
			}
			else if(jobRunningState==JobRunningState.retry.ordinal())
			{
				updateMsisdnsForResumeJobRetry(elements);
			}
		}
	}
	
	private void updateMsisdnsForResumeJobScheduled(ArrayList<Msisdn> elements) {
		if(elements.size()-currentPosition<1){
			Logger.sysLog(LogValues.info, this.getClass().getName(), "resumed job(initial) : No msisdns for updating ");
			return;
		}
		StringBuilder query = new StringBuilder("update obd_msisdn_"+Jobname +" set status = '"+JobState.PICKED+"' where status = '"+JobState.SCHEDULED+"' and (");
		for(int i=currentPosition;i<elements.size();i++){
			query.append(" msisdn = '"+elements.get(i).getMsisdn()+"' or"); 
		}
		query.delete(query.length()-2, query.length());
		query.append(")");
		JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query.toString()));
		Logger.sysLog(LogValues.info, this.getClass().getName(), "resumed job(initial) : query : "+query);
	}

	private void updateMsisdnsForResumeJobRetry(ArrayList<Msisdn> elements) {
		if(elements.size()-currentPosition<1){
			Logger.sysLog(LogValues.info, this.getClass().getName(), "resumed job(retry) : No msisdns for updating ");
			return;
		}
		String tablename = "obd_msisdn_" + Jobname;
		List<String> reasons = msisdnBo.getReasonColumns(Jobname);
		StringBuilder SQL = new StringBuilder("UPDATE "+tablename
				+" SET ");
		for(String reason:reasons){
			if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
				SQL.append(reason +" = IF(reason = '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',"+reason+"- 1, "+reason+"),");
       	}
		SQL.append( JobState.FAILED_REASON_PREFIX+"status = '"+JobState.PICKED + (isStarcopy?("' ,`"+JobState.FAILED_REASON_PREFIX+"max-retry` = `"+JobState.FAILED_REASON_PREFIX+"max-retry` - 1 "):"'")
			    +" WHERE reason IN(");
	    for(String reason:reasons){
	    	if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
	    		SQL.append("'"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',");
       	}
	    SQL.deleteCharAt(SQL.length()-1);
		SQL.append(")  and "+JobState.FAILED_REASON_PREFIX+"status='"+JobState.SCHEDULED+"' and (");
		for(int i=currentPosition;i<elements.size();i++){
			SQL.append(" msisdn = '"+elements.get(i).getMsisdn()+"' or");
		}			
		SQL.delete(SQL.length()-2, SQL.length());
		SQL.append(" )");
		JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(SQL.toString()));
		Logger.sysLog(LogValues.info, this.getClass().getName(), "resumed job(retry) : query : "+SQL);
	}

	public void resetSettingsToRetry(){
		jobOver=false;
		jobRunningState = JobRunningState.retry.ordinal();
		isLastChunk=false;
	}
	
	public boolean isJobOver() {
		return jobOver;
	}

	public String getJobname() {
		return Jobname;
	}
	
	private MessageCreator getMessageCreator(final String message) {
		return new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(message);
			}
		};
	}

	public int getJobRunningState() {
		return jobRunningState;
	}
	
	public void updateSentStatusInDb(Msisdn msisdn){

		if(jobRunningState==JobRunningState.initial.ordinal()){
			String query = "update obd_msisdn_"+Jobname +" set status = '"+JobState.MSISDN_STATE_TO_CORE_ENGINE+"' where msisdn = '"+msisdn.getMsisdn()+"' and status = '"+JobState.PICKED+"'";
			JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query));
		}
		else if(jobRunningState==JobRunningState.retry.ordinal()){
			String query = "update obd_msisdn_"+Jobname +" set status = '"+JobState.MSISDN_STATE_TO_CORE_ENGINE+"', "+JobState.FAILED_REASON_PREFIX+"status='"+JobState.SCHEDULED+"' where msisdn = '"+msisdn.getMsisdn()+"' and status = '"+JobState.FAILED+"'";
			JobStarter.schedulerManager.getJmsTemplate().send(JobStarter.schedulerManager.getQueuedb(), getMessageCreator(query));
		}
		else if(isLastChunk && jobOver && (jobRunningState == JobRunningState.jobover.ordinal())){
			jobRunningState = lastNumberState;
			updateSentStatusInDb(msisdn);
			jobRunningState = JobRunningState.jobover.ordinal();
			lastNumberState=null;
		}
	}

	public boolean isStarcopy() {
		return isStarcopy;
	}

//	public void setJobRunningState(int jobRunningState) {
//		this.jobRunningState = jobRunningState;
//	}
	
}

