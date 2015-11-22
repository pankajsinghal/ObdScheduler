/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bng.core.jsonBean;

import java.io.Serializable;

/**
 *
 * @author richa
 */

/**
 * Event class will always be passed by telephony to coreEngine. 
 * This class will have a method which will insert the seekBytes and seekFilePath in db.
 */
public class Event implements Serializable{
    private int vId;
    private String aPartyMsisdn = "";
    private String bPartyMsisdn = "";
    private int event;
    private int subEvent;
    private String ivrCode = "";//ShortCode
    private int hardware;
    private int callType;
    private String ip = "";
    private String seekBytes = "";
    private String filePath = "";    
    private int reason;
    private int subEventCause;
    private String dtmfBuffer = "";
    private String service = "";
    private String serviceName = "";
    private String transId;  
    private int cic;
    private String protocol;
    private String coreToTelephony = "";
    private String obdlist=""; 

    public int getvId() {
		return vId;
	}

	public void setvId(int vId) {
		this.vId = vId;
	}

    public String getaPartyMsisdn() {
		return aPartyMsisdn;
	}

	public void setaPartyMsisdn(String aPartyMsisdn) {
		this.aPartyMsisdn = aPartyMsisdn;
	}

	public String getbPartyMsisdn() {
		return bPartyMsisdn;
	}

	public void setbPartyMsisdn(String bPartyMsisdn) {
		this.bPartyMsisdn = bPartyMsisdn;
	}

	public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public int getSubEvent() {
        return subEvent;
    }

    public void setSubEvent(int subEvent) {
        this.subEvent = subEvent;
    }

    public String getIvrCode() {
        return ivrCode;
    }

    public void setIvrCode(String ivrCode) {
        this.ivrCode = ivrCode;
    }

    public int getHardware() {
        return hardware;
    }

    public void setHardware(int hardware) {
        this.hardware = hardware;
    }

    public int getCallType() {
        return callType;
    }

    public void setCallType(int callType) {
        this.callType = callType;
    }

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSeekBytes() {
		return seekBytes;
	}

	public void setSeekBytes(String seekBytes) {
		this.seekBytes = seekBytes;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public int getReason() {
		return reason;
	}

	public void setReason(int reason) {
		this.reason = reason;
	}

	public int getSubEventCause() {
		return subEventCause;
	}

	public void setSubEventCause(int subEventCause) {
		this.subEventCause = subEventCause;
	}	

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getCoreToTelephony() {
		return coreToTelephony;
	}

	public void setCoreToTelephony(String coreToTelephony) {
		this.coreToTelephony = coreToTelephony;
	}

	public String getDtmfBuffer() {
		return dtmfBuffer;
	}

	public void setDtmfBuffer(String dtmfBuffer) {
		this.dtmfBuffer = dtmfBuffer;
	}

	public String getTransId() {
		return transId;
	}

	public void setTransId(String transId) {
		this.transId = transId;
	}

	public int getCic() {
		return cic;
	}

	public void setCic(int cic) {
		this.cic = cic;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getObdlist() {
		return obdlist;
	}

	public void setObdlist(String obdlist) {
		this.obdlist = obdlist;
	}

	@Override
	public String toString() {
		return "Event [vId=" + vId + ", aPartyMsisdn=" + aPartyMsisdn
				+ ", bPartyMsisdn=" + bPartyMsisdn + ", event=" + event
				+ ", subEvent=" + subEvent + ", ivrCode=" + ivrCode
				+ ", hardware=" + hardware + ", callType=" + callType + ", ip="
				+ ip + ", seekBytes=" + seekBytes + ", filePath=" + filePath
				+ ", reason=" + reason + ", subEventCause=" + subEventCause
				+ ", dtmfBuffer=" + dtmfBuffer + ", service=" + service
				+ ", serviceName=" + serviceName + ", transId=" + transId
				+ ", cic=" + cic + ", protocol=" + protocol
				+ ", coreToTelephony=" + coreToTelephony + ", obdlist="
				+ obdlist + "]";
	}

	
}
