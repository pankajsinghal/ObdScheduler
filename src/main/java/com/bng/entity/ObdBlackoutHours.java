package com.bng.entity;

// Generated 5 Mar, 2014 2:37:46 PM by Hibernate Tools 4.0.0

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import static javax.persistence.GenerationType.IDENTITY;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * ObdBlackoutHours generated by hbm2java
 */
@Entity
@Table(name = "obd_blackout_hours")
public class ObdBlackoutHours implements java.io.Serializable {

	private Integer id;
	private Date blackoutStart;
	private Date blackoutEnd;
	private Set<Service> services = new HashSet<Service>(0);

	public ObdBlackoutHours() {
	}

	public ObdBlackoutHours(Date blackoutStart, Date blackoutEnd) {
		this.blackoutStart = blackoutStart;
		this.blackoutEnd = blackoutEnd;
	}

	public ObdBlackoutHours(Date blackoutStart, Date blackoutEnd,
			Set<Service> services) {
		this.blackoutStart = blackoutStart;
		this.blackoutEnd = blackoutEnd;
		this.services = services;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Temporal(TemporalType.TIME)
	@Column(name = "blackout_start", nullable = false, length = 8)
	public Date getBlackoutStart() {
		return this.blackoutStart;
	}

	public void setBlackoutStart(Date blackoutStart) {
		this.blackoutStart = blackoutStart;
	}

	@Temporal(TemporalType.TIME)
	@Column(name = "blackout_end", nullable = false, length = 8)
	public Date getBlackoutEnd() {
		return this.blackoutEnd;
	}

	public void setBlackoutEnd(Date blackoutEnd) {
		this.blackoutEnd = blackoutEnd;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "obdBlackoutHours")
	public Set<Service> getServices() {
		return this.services;
	}

	public void setServices(Set<Service> services) {
		this.services = services;
	}

}
