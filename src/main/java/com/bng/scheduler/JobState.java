package com.bng.scheduler;


public class JobState {
	public static final String DBName = "ivr_data";
	public static final String FAILED_REASON_PREFIX = "failedreason_";
	public static final String SCHEDULED = "scheduled";
	public static final String RUNNING = "running";
	public static final String RETRY = "retry";
	public static final String PAUSED = "paused";
	public static final String PROCESSED = "processed";
	public static final String STOPPED = "stopped";
	public static final String EXPIRED = "expired";
	public static final String FAILED = "failed";
	public static final String PICKED = "picked";
	public static final String JOB_GROUP = "group1";
	
	public static final String MSISDN_STATE_TO_CORE_ENGINE = "to_core_engine";
	public static final String MSISDN_STATE_FAILED = "failed";
//	public static void main(String[] args) {
//		int i= JobState.JobRunningState.failed.ordinal();
//		for (JobRunningState string : JobRunningState.values()) {
//			System.out.println(string.ordinal());
//		}
//	}
	
	public static enum JobRunningState{
        initial,
        retry,
        retryStarcopy,
        jobover,
        failed;
        public static final JobRunningState values[] = values();
    }
}
