package com.bng.dao.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Transactional;

import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.dao.ServiceDao;
import com.bng.entity.ObdBlackoutHours;
import com.bng.entity.Service;
import com.bng.scheduler.JobState;
import com.jolbox.bonecp.BoneCPDataSource;

public class ServiceDaoImpl implements ServiceDao {// , Serializable {

	// private static final long serialVersionUID = 1L;
	private SessionFactory sessionFactory;
	private BoneCPDataSource dataSource;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Transactional
	@Override
	public void save(Service service) {
		sessionFactory.getCurrentSession().save(service);
	}

	@Transactional
	public void update(Service service) {
		sessionFactory.getCurrentSession().update(service);
	}

	@Override
	public void delete(Service service) {
		sessionFactory.getCurrentSession().delete(service);
	}

	@Transactional
	public List GetJobsToStart() {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(Service.class);

		List list = crit.add(Restrictions.le("startDate", new Date()))
				.add(Restrictions.ge("endDate", new Date()))
				.add(Restrictions.le("startTime", new Date()))
				.add(Restrictions.ge("endTime", new Date()))
				.add(Restrictions.eq("status", JobState.SCHEDULED))
				.add(Restrictions.ge("remainingRetry", 0))
				.list();
		Service s;
		Iterator listIterator = list.iterator();
		Logger.sysLog(LogValues.info, this.getClass().getName(),"starting "+list.size()+" jobs." );
		while (listIterator.hasNext())
		{
			s = (Service) listIterator.next();
			s.getMxgraph().getServiceName();
			s.getObdBlackoutHours().getBlackoutEnd();
			s.getObdClis().size();
			Logger.sysLog(LogValues.info, this.getClass().getName(), "processing " + s.toString());
			s.setStatus(JobState.RUNNING);
			update(s);
			s.getMxgraph();
		}
		return list;
	}

	
	@Transactional
	public List GetJobsToEnd() {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(Service.class);
		List list = crit.add(Restrictions.eq("status", JobState.RUNNING)).list();
		Service s;
		Iterator listIterator = list.iterator();
		while (listIterator.hasNext()) {
			s = (Service) listIterator.next();
			if (dateTime(s.getEndDate(), s.getEndTime()).before(Calendar.getInstance()))		//this ensures that the scheduler is implemented on daily basis
				s.setStatus(JobState.EXPIRED);
			else if(!(dateTime(new Date(), s.getStartTime()).before(Calendar.getInstance()) && dateTime(new Date(), s.getEndTime()).after(Calendar.getInstance())))
				s.setStatus(JobState.SCHEDULED);
			else
				listIterator.remove();
			update(s);
			// for initialization
			s.getMxgraph();
		}
		
		Logger.sysLog(LogValues.info, this.getClass().getName(),"ending "+list.size()+" jobs." );
		
		return list;
	}
	
	public Calendar dateTime(Date date, Date time) {
		
		Calendar receivedDate = Calendar.getInstance();
		receivedDate.setTime(date);
		Calendar receivedTime = Calendar.getInstance();
		receivedTime.setTime(time);
		
		Calendar finalDatetime = Calendar.getInstance();
		finalDatetime.set(receivedDate.get(Calendar.YEAR), receivedDate.get(Calendar.MONTH), receivedDate.get(Calendar.DAY_OF_MONTH), receivedTime.get(Calendar.HOUR_OF_DAY), receivedTime.get(Calendar.MINUTE), receivedTime.get(Calendar.SECOND));
		return finalDatetime;
	}
	
	@Transactional
	public boolean getBlackoutHoursStatus() {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(ObdBlackoutHours.class);
		
		List daycheck = crit.add(Restrictions.leProperty("blackoutStart", "blackoutEnd")).list();
		if(!daycheck.isEmpty()){
			//intraday
			crit = session.createCriteria(ObdBlackoutHours.class);
			
			Criterion start = Restrictions.le("blackoutStart", new ObdBlackoutHours(new Date(),new Date()).getBlackoutStart());
			Criterion end = Restrictions.gt("blackoutEnd",new ObdBlackoutHours(new Date(),new Date()).getBlackoutStart());
			List list = crit.add(Restrictions.conjunction().add(start).add(end))
					.list();
			if(list.isEmpty())
				return false;//can call
			else 
				return true;//can not call
		}
		else{
			//interday
			crit = session.createCriteria(ObdBlackoutHours.class);
			Date d = new ObdBlackoutHours(new Date(),new Date()).getBlackoutStart();
			Criterion start = Restrictions.le("blackoutStart", d);
			Criterion end = Restrictions.gt("blackoutEnd", new ObdBlackoutHours(new Date(),new Date()).getBlackoutStart());
			List list = crit.add(Restrictions.disjunction().add(start).add(end))
					.list();
			if(list.isEmpty())
				return false;//can call
			else 
				return true;//can not call
		}
			
	}

	@Transactional
	public Service update(String jobname, String status) {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(Service.class);
		List list = crit.add(Restrictions.eq("jobname", jobname))
				.list();
		Service s = (Service) list.get(0);
		s.setStatus(status);
		s.getObdBlackoutHours().getBlackoutEnd();
		s.getObdClis().get(0).getCli();
		s.getMxgraph().getServiceName();
		update(s);
		return s;
	}
	
	@Transactional
	public String stopJob(String jobName) {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(Service.class);
		List list = crit.add(Restrictions.eq("jobname", jobName))
				.list();
		Service s = (Service) list.get(0);
		String oldStatus = s.getStatus();
		s.getObdBlackoutHours();
		s.getObdClis();
		s.setStatus(JobState.STOPPED);
		update(s);
		return oldStatus;
	}
	
	public BoneCPDataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(BoneCPDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Transactional
	public Service getJob(String jobName) {
		Session session = sessionFactory.getCurrentSession();
		Criteria crit = session.createCriteria(Service.class);
		List list = crit.add(Restrictions.eq("jobname", jobName))
				.list();
		Service s=null;
		if(list.size()>0)
			s = (Service) list.get(0);
		return s;
	}
}
