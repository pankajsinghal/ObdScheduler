package com.bng.entity;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;


public class MsisdnMapper implements RowMapper<Msisdn>{

	@Override
	public Msisdn mapRow(ResultSet rs, int rowNum) throws SQLException {
		Msisdn msisdn = new Msisdn();
		ResultSetMetaData rsMetaData = rs.getMetaData();
		int numberOfColumns = rsMetaData.getColumnCount();

		// get the column names; column indexes start from 1
		for (int i = 1; i < numberOfColumns + 1; i++) {
		    String columnName = rsMetaData.getColumnName(i);
		    // Get the name of the column's table name
		    switch (columnName) {
			case "id":
				msisdn.setId(rs.getInt("id"));
				break;
			case "msisdn":
				msisdn.setMsisdn(rs.getString("msisdn"));
				break;
			case "status":
				msisdn.setStatus(rs.getString("status"));
				break;
			case "failedreason_status":
				msisdn.setFailedreason_status(rs.getString("failedreason_status"));
				break;
			case "reason":
				msisdn.setReason(rs.getString("reason"));
				break;
			default:
				break;
			}
		}
		return msisdn;
	}
	
}
