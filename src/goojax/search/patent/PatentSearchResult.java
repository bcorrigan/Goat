package goojax.search.patent;

import goojax.search.SearchResult;
import java.util.Date;

public class PatentSearchResult extends SearchResult {

	public Date applicationDate;
	public Long patentNumber;
	public String patentStatus; //TODO getter and setter using PatentStatus enum
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
	public PatentStatus getStatus() {
		return PatentStatus.statusFromCode(patentStatus);
	}
	public void setPatentStatus(PatentStatus status) {
		patentStatus = status.code;
	}
}
