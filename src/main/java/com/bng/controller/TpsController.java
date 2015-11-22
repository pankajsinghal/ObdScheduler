package com.bng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bng.core.utils.LogValues;
import com.bng.core.utils.Logger;
import com.bng.scheduler.JobState;
import com.bng.scheduler.SchedulerManager;

@Controller
public class TpsController {

	@Autowired
	private SchedulerManager schedulerManager;

	@RequestMapping(value = "/tps/{tps}")
	public @ResponseBody String changeTps(@PathVariable("tps") int tps)  {
		String status  = this.schedulerManager.changeTps(tps);
		return "current tps : "+schedulerManager.getTps() +"\n "+status;
	}
	
	@RequestMapping(value = "/pause/{jobname}")
	public @ResponseBody String pause(@PathVariable("jobname") String jobname)  {
		Logger.sysLog(LogValues.info, this.getClass().getName(),"inside pause");
		String status = this.schedulerManager.pause(jobname, JobState.JOB_GROUP);
		Logger.sysLog(LogValues.info, this.getClass().getName(),status);
		return status;
	}
	
	@RequestMapping(value = "/resume/{jobname}")
	public @ResponseBody String resume(@PathVariable("jobname") String jobname)  {
		Logger.sysLog(LogValues.info, this.getClass().getName(),"inside resume");
		String status = this.schedulerManager.resume(jobname, JobState.JOB_GROUP);
		Logger.sysLog(LogValues.info, this.getClass().getName(),status);
		return status;
	}
	
	@RequestMapping(value = "/stop/{jobname}")
	public @ResponseBody String stop(@PathVariable("jobname") String jobname)  {
		Logger.sysLog(LogValues.info, this.getClass().getName(),"inside stop");
		String status = this.schedulerManager.stop(jobname, JobState.JOB_GROUP);
		Logger.sysLog(LogValues.info, this.getClass().getName(),status);
		return status;
	}
	
	@RequestMapping(value = "/")
	public String welcome(ModelMap modelMap)  {
		modelMap.addAttribute("list", schedulerManager.refresh());
		return "welcome";
	}
}
