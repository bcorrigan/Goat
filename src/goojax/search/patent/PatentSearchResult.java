package goojax.search.patent;

import goojax.search.SearchResult;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

public class PatentSearchResult extends SearchResult {

	public String applicationDate;
	public Long patentNumber;
	public String patentStatus;
	public String assignee;
	public String tbUrl;
	
	public PatentSearchResult() {
		super();
	}
	
	public enum PatentStatus {
		PENDING ("filed"),
		ISSUED ("issued");
		
		String code;
		private PatentStatus(String code) {
			this.code = code;
		}
		public static PatentStatus statusFromCode(String code) {
			PatentStatus ret = null;
			for(PatentStatus ps: PatentStatus.values()) 
				if(ps.code.equalsIgnoreCase(code)) {
					ret = ps;
					break;
				}
			return ret;
		}
	}
	
	public PatentStatus getPatentStatus() {
		return PatentStatus.statusFromCode(patentStatus);
	}
	
	public void setPatentStatus(PatentStatus status) {
		patentStatus = status.code;
	}

	static final String DATE_FORMAT_STRING = "EEE, dd MMM yyyy HH:mm:ss Z";
	static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_STRING);
	
	public Date getApplicationDate() {
		Date ret = null;
		try {
			if(applicationDate != null)
				ret = sdf.parse(applicationDate);
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
		return ret;
	}

	public void setApplicationDate(Date applicationDate) {
		this.applicationDate = sdf.format(applicationDate);
	}

	public Long getPatentNumber() {
		return patentNumber;
	}

	public void setPatentNumber(Long patentNumber) {
		this.patentNumber = patentNumber;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public String getTbUrl() {
		return tbUrl;
	}

	public void setTbUrl(String tbUrl) {
		this.tbUrl = tbUrl;
	}
}
