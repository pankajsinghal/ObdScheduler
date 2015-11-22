package com.bng.entity;

// Generated 13 Nov, 2013 1:13:35 PM by Hibernate Tools 3.4.0.CR1

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Mxgraph generated by hbm2java
 */
@Entity
@Table(name = "mxgraph", uniqueConstraints = @UniqueConstraint(columnNames = {
		"service_name", "shortcode" }))
public class Mxgraph implements java.io.Serializable {

	private int id;
	private Mxdata mxdata;
	private String serviceName;
	private short produtionFlag;
	private Date productionDate;
	private String type;
	private String shortcode;
	private String callType;
	private Set<Service> services = new HashSet<Service>(0);
	private Set<MxgraphVersion> mxgraphVersions = new HashSet<MxgraphVersion>(0);

	public Mxgraph() {
	}

	public Mxgraph(int id, Mxdata mxdata, String serviceName,
			short produtionFlag, Date productionDate, String type,
			String shortcode, String callType) {
		this.id = id;
		this.mxdata = mxdata;
		this.serviceName = serviceName;
		this.produtionFlag = produtionFlag;
		this.productionDate = productionDate;
		this.type = type;
		this.shortcode = shortcode;
		this.callType = callType;
	}

	public Mxgraph(int id, Mxdata mxdata, String serviceName,
			short produtionFlag, Date productionDate, String type,
			String shortcode, String callType, Set<Service> services,
			Set<MxgraphVersion> mxgraphVersions) {
		this.id = id;
		this.mxdata = mxdata;
		this.serviceName = serviceName;
		this.produtionFlag = produtionFlag;
		this.productionDate = productionDate;
		this.type = type;
		this.shortcode = shortcode;
		this.callType = callType;
		this.services = services;
		this.mxgraphVersions = mxgraphVersions;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mxdata_id", nullable = false)
	public Mxdata getMxdata() {
		return this.mxdata;
	}

	public void setMxdata(Mxdata mxdata) {
		this.mxdata = mxdata;
	}

	@Column(name = "service_name", nullable = false)
	public String getServiceName() {
		return this.serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	@Column(name = "prodution_flag", nullable = false)
	public short getProdutionFlag() {
		return this.produtionFlag;
	}

	public void setProdutionFlag(short produtionFlag) {
		this.produtionFlag = produtionFlag;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "production_date", nullable = false, length = 23)
	public Date getProductionDate() {
		return this.productionDate;
	}

	public void setProductionDate(Date productionDate) {
		this.productionDate = productionDate;
	}

	@Column(name = "type", nullable = false)
	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Column(name = "shortcode", nullable = false, length = 10)
	public String getShortcode() {
		return this.shortcode;
	}

	public void setShortcode(String shortcode) {
		this.shortcode = shortcode;
	}

	@Column(name = "call_type", nullable = false, length = 10)
	public String getCallType() {
		return this.callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "mxgraph")
	public Set<Service> getServices() {
		return this.services;
	}

	public void setServices(Set<Service> services) {
		this.services = services;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "mxgraph")
	public Set<MxgraphVersion> getMxgraphVersions() {
		return this.mxgraphVersions;
	}

	public void setMxgraphVersions(Set<MxgraphVersion> mxgraphVersions) {
		this.mxgraphVersions = mxgraphVersions;
	}

}
