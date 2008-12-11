package goojax.search.patent;

import goojax.search.SearchResult;
import java.text.ParseException;
import java.util.Date;

public class PatentSearchResult extends SearchResult {

	public String applicationDate;
	public String patentNumber;
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
	
	public Date getApplicationDate() {
		return parseDate(applicationDate);
	}

	public void setApplicationDate(Date applicationDate) {
		this.applicationDate = formatDate(applicationDate);
	}

	public String getPatentNumber() {
		return patentNumber;
	}

	public void setPatentNumber(String patentNumber) {
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
