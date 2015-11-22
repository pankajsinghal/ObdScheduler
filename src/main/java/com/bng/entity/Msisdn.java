package com.bng.entity;

public class Msisdn {

	private Integer id;
	private String cli;
	private String msisdn;
	private String status;
	private String failedreason_status;
	private String reason;

	public Msisdn() {
	}

	public Msisdn(Integer id, String cli, String msisdn, String status,
			String failedreason_status, String reason) {
		super();
		this.id = id;
		this.cli = cli;
		this.msisdn = msisdn;
		this.status = status;
		this.failedreason_status = failedreason_status;
		this.reason = reason;
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCli() {
		return cli;
	}

	public void setCli(String cli) {
		this.cli = cli;
	}

	public String getMsisdn() {
		return this.msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getFailedreason_status() {
		return failedreason_status;
	}

	public void setFailedreason_status(String failedreason_status) {
		this.failedreason_status = failedreason_status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@Override
	public String toString() {
		return "Msisdn [id=" + id + ", cli=" + cli + ", msisdn=" + msisdn
				+ ", status=" + status + ", failedreason_status="
				+ failedreason_status + ", reason=" + reason + "]";
	}

}
