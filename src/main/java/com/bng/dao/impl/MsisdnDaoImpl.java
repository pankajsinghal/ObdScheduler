package com.bng.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.core.utils.coreException;
import com.bng.dao.MsisdnDao;
import com.bng.entity.Msisdn;
import com.bng.entity.MsisdnMapper;
import com.bng.scheduler.JobState;
import com.bng.scheduler.JobState.JobRunningState;
import com.jolbox.bonecp.BoneCPDataSource;
import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

public class MsisdnDaoImpl implements MsisdnDao {// , Serializable {
	private BoneCPDataSource dataSource;
	private JdbcTemplate jdbcTemplateObject;

	@Override
	public List<Msisdn> listMsisdn(String jobname,int jobRunningState) {
		
		if(jobRunningState==JobRunningState.initial.ordinal())
			return getScheduledList(jobname);
		else if(jobRunningState==JobRunningState.retry.ordinal())
			return getFailedList(jobname);
		else if(jobRunningState==JobRunningState.retryStarcopy.ordinal())
			return getFailedListStarcopy(jobname);
		else return null;
	}

	private List<Msisdn> getFailedListStarcopy(String jobname) {

		String tablename = "obd_msisdn_" + jobname;
		
		List<String> reasons = getReasonColumns(jobname);
		
		String SQL = "SELECT *,"
			       +" CASE reason";
       	for(String reason:reasons){
       		if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
       			SQL+=" WHEN '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"' THEN (case when "+reason+">0 then "+reason+" end)";
       	}
			       SQL+=" END"
			       +" as value"
			       +" FROM "+tablename +" where status = '"+JobState.MSISDN_STATE_FAILED + "' and "+JobState.FAILED_REASON_PREFIX+"status = '"+JobState.SCHEDULED + "' and `"+JobState.FAILED_REASON_PREFIX+"max-retry` > 0 "
			       +" HAVING value IS NOT NULL limit 1000";
		List<Msisdn> msisdns = null;
		try{
		msisdns = jdbcTemplateObject.query(SQL,
				new Object[] {  }, new MsisdnMapper());
		Logger.sysLog(LogValues.info, this.getClass().getName(),SQL);
		Logger.sysLog(LogValues.info, this.getClass().getName(),"msisdn extracted : "+msisdns.size());
		for(Msisdn msisdn : msisdns){
			Logger.sysLog(LogValues.info, this.getClass().getName(),"msisdn : "+msisdn.getMsisdn() +" & status : " + msisdn.getStatus() + " & Failedreason_status : "+msisdn.getFailedreason_status() +" & reason : "+msisdn.getReason());
		}
		}
		catch(BadSqlGrammarException e){
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			if(e.getCause() != null && e.getCause() instanceof MySQLSyntaxErrorException) {
				MySQLSyntaxErrorException ex = (MySQLSyntaxErrorException)e.getCause();
		        if(ex.getMessage().contains("doesn't exist")){
		        	return msisdns;
		        }
		    }
		}
		catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return msisdns;
		}
		if(msisdns.size()<=0)
			return msisdns;
		SQL = "UPDATE "+tablename
				+" SET ";
		for(String reason:reasons){
			if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
				SQL+=reason +" = IF(reason = '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',"+reason+"- 1, "+reason+"),";
       	}
			  SQL+= JobState.FAILED_REASON_PREFIX+"status = ?,`"+JobState.FAILED_REASON_PREFIX+"max-retry` = `"+JobState.FAILED_REASON_PREFIX+"max-retry` - 1 "
			    +" WHERE reason IN(";
	    for(String reason:reasons){
	    	if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
	    		SQL+="'"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',";
       	}
	    SQL = SQL.substring(0, SQL.length()-1);
		SQL+=")  and "+JobState.FAILED_REASON_PREFIX+"status=? ";
		//UPDATE obd_msisdn_pankaj 
		//SET failedreason_CallRejection = IF(reason = 'CallRejection',failedreason_CallRejection- 1, failedreason_CallRejection),
		//failedreason_Unavailable = IF(reason = 'Unavailable',failedreason_Unavailable- 1, failedreason_Unavailable),
		//failedreason_NoAnswer = IF(reason = 'NoAnswer',failedreason_NoAnswer- 1, failedreason_NoAnswer),
		//failedreason_NetworkBusy = IF(reason = 'NetworkBusy',failedreason_NetworkBusy- 1, failedreason_NetworkBusy),
		//failedreason_status = ? 
		//WHERE reason IN('CallRejection','Unavailable','NoAnswer','NetworkBusy')  
		//and failedreason_status=? and (msisdn=9999999901 or msisdn=9999999902 )
		SQL+="and (";
		for(Msisdn msisdn : msisdns){
			SQL+="msisdn="+msisdn.getMsisdn()+" or ";
		}
		StringBuilder stringBuilder = new StringBuilder(SQL);
		stringBuilder.delete(stringBuilder.length()-3, stringBuilder.length());
		SQL = stringBuilder.toString()+")";
		int i = jdbcTemplateObject.update(SQL, JobState.PICKED, JobState.SCHEDULED);
		Logger.sysLog(LogValues.info, this.getClass().getName(),SQL);
		Logger.sysLog(LogValues.info, this.getClass().getName(),"update status = "+i);
		return msisdns;
	}

	private List<Msisdn> getScheduledList(String jobname) {
		String tablename = "obd_msisdn_" + jobname;
		String SQL = "select * from " + tablename
				+ " where status = ? limit 1000";
		List<Msisdn> msisdns = null;
		try{
		msisdns = jdbcTemplateObject.query(SQL,
				new Object[] { JobState.SCHEDULED }, new MsisdnMapper());
		}
		catch(BadSqlGrammarException e){
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			if(e.getCause() != null && e.getCause() instanceof MySQLSyntaxErrorException) {
				MySQLSyntaxErrorException ex = (MySQLSyntaxErrorException)e.getCause();
		        if(ex.getMessage().contains("doesn't exist")){
		        	return msisdns;
		        }
		    }
		}
		catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return msisdns;
		}
		SQL = "update " + tablename
				+ " set status = ? where status = ? limit 1000";
		jdbcTemplateObject.update(SQL, JobState.PICKED, JobState.SCHEDULED);
		return msisdns;
	}

	private List<Msisdn> getFailedList(String jobname) {
		
		String tablename = "obd_msisdn_" + jobname;
		
		List<String> reasons = getReasonColumns(jobname);
		
		String SQL = "SELECT *,"
			       +" CASE reason";
       	for(String reason:reasons){
       		if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
       			SQL+=" WHEN '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"' THEN (case when "+reason+">0 then "+reason+" end)";
       	}
			       SQL+=" END"
			       +" as value"
			       +" FROM "+tablename +" where status = '"+JobState.MSISDN_STATE_FAILED + "' and "+JobState.FAILED_REASON_PREFIX+"status = '"+JobState.SCHEDULED
			       +"' HAVING value IS NOT NULL limit 1000";
		List<Msisdn> msisdns = null;
		try{
		msisdns = jdbcTemplateObject.query(SQL,
				new Object[] {  }, new MsisdnMapper());
		Logger.sysLog(LogValues.info, this.getClass().getName(),SQL);
		Logger.sysLog(LogValues.info, this.getClass().getName(),"msisdn extracted : "+msisdns.size());
		for(Msisdn msisdn : msisdns){
			Logger.sysLog(LogValues.info, this.getClass().getName(),"msisdn : "+msisdn.getMsisdn() +" & status : " + msisdn.getStatus() + " & Failedreason_status : "+msisdn.getFailedreason_status() +" & reason : "+msisdn.getReason());
		}
		}
		catch(BadSqlGrammarException e){
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			if(e.getCause() != null && e.getCause() instanceof MySQLSyntaxErrorException) {
				MySQLSyntaxErrorException ex = (MySQLSyntaxErrorException)e.getCause();
		        if(ex.getMessage().contains("doesn't exist")){
		        	return msisdns;
		        }
		    }
		}
		catch (Exception e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
			return msisdns;
		}
		if(msisdns.size()<=0)
			return msisdns;
		SQL = "UPDATE "+tablename
				+" SET ";
		for(String reason:reasons){
			if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
				SQL+=reason +" = IF(reason = '"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',"+reason+"- 1, "+reason+"),";
       	}
			  SQL+= JobState.FAILED_REASON_PREFIX+"status = ?"
			    +" WHERE reason IN(";
	    for(String reason:reasons){
	    	if(!reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"status") && !reason.equalsIgnoreCase(JobState.FAILED_REASON_PREFIX+"max-retry"))
	    		SQL+="'"+reason.replace(JobState.FAILED_REASON_PREFIX, "")+"',";
       	}
	    SQL = SQL.substring(0, SQL.length()-1);
		SQL+=")  and "+JobState.FAILED_REASON_PREFIX+"status=? ";
		//UPDATE obd_msisdn_pankaj SET failedreason_CallRejection = IF(reason = 'CallRejection',failedreason_CallRejection- 1, failedreason_CallRejection),failedreason_Unavailable = IF(reason = 'Unavailable',failedreason_Unavailable- 1, failedreason_Unavailable),failedreason_NoAnswer = IF(reason = 'NoAnswer',failedreason_NoAnswer- 1, failedreason_NoAnswer),failedreason_NetworkBusy = IF(reason = 'NetworkBusy',failedreason_NetworkBusy- 1, failedreason_NetworkBusy),failedreason_status = ? WHERE reason IN('CallRejection','Unavailable','NoAnswer','NetworkBusy')  and failedreason_status=? and (msisdn=9999999901 or msisdn=9999999902 ) limit 1000
		SQL+="and (";
		for(Msisdn msisdn : msisdns){
			SQL+="msisdn="+msisdn.getMsisdn()+" or ";
		}
		StringBuilder stringBuilder = new StringBuilder(SQL);
		stringBuilder.delete(stringBuilder.length()-3, stringBuilder.length());
		SQL = stringBuilder.toString()+")";
		int i = jdbcTemplateObject.update(SQL, JobState.PICKED, JobState.SCHEDULED);
		Logger.sysLog(LogValues.info, this.getClass().getName(),SQL);
		Logger.sysLog(LogValues.info, this.getClass().getName(),"update status = "+i);
		return msisdns;
	}

	@Override
	public void setDataSource(BoneCPDataSource datasource) {
		this.dataSource = datasource;
		this.jdbcTemplateObject = new JdbcTemplate(datasource);
	}

	@Override
	public void update(Integer id, String jobname, String status) {
		String SQL = "update obd_msisdn_" + jobname
				+ " set status = ? where id = ?";
		jdbcTemplateObject.update(SQL, status, id);
		// System.out.println("Updated Record with ID = " + id );
		return;
	}

	@Override
	public void update(ArrayList<Msisdn> elements, String jobname) {
		String sql = "update obd_msisdn_" + jobname
				+ " set status = ? where msisdn = ?";

		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = dataSource.getConnection();
			ps = connection.prepareStatement(sql);

			for (Msisdn msisdn : elements) {

				ps.setString(1, msisdn.getStatus());
				ps.setString(2, msisdn.getMsisdn());
				ps.addBatch();

			}
			ps.executeBatch(); // insert remaining records
		} catch (SQLException e) {
			Logger.sysLog(LogValues.error, this.getClass().getName(),
					coreException.GetStack(e));
		} finally {
			try {
				ps.close();
				connection.close();
			} catch (SQLException e) {
				Logger.sysLog(LogValues.error, this.getClass().getName(),
						coreException.GetStack(e));
			}

		}

	}

	@Override
	public void resetRetryStatus(String jobname) {
//		System.out.println("updating obd_msisdn_" + jobname +" to scheduled");
		String SQL = "update obd_msisdn_" + jobname
				+ " set "+JobState.FAILED_REASON_PREFIX+"status = ?";
		jdbcTemplateObject.update(SQL, JobState.SCHEDULED);
	}

	@Override
	public List<String> getReasonColumns(String jobname) {
		String tablename = "obd_msisdn_" + jobname;
		
		String sqlstmt = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME=? and column_name like '"+JobState.FAILED_REASON_PREFIX+"%'";
		return jdbcTemplateObject.queryForList(sqlstmt, String.class, JobState.DBName,tablename);
		
	}
}
