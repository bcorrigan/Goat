package goat.core;

public class IrcUser {
	private String nick = "";
	private boolean isRegistered;
	private String ident = "";
	private String hostmask = "";
	private String realName = "";
	private int hopcount = 0;
	private boolean isAway;
	private boolean isSysop;
	
	public IrcUser() {};
	
	public static IrcUser getNewInstanceFromWHOReply(Message m) {
		IrcUser ret = null;
		if( !(Constants.RPL_WHOREPLY == Integer.parseInt(m.getCommand())))
			return null;
		String[] params = m.getParams().split("\\s+");
		if(params.length < 7)
			return null;
		ret = new IrcUser();
		ret.ident = params[2];
		ret.hostmask = params[3];
		ret.nick = params[5];
		String flags = params[6];
		if(flags.contains("G"))
			ret.isAway = true;
		if(flags.contains("r"))
			ret.isRegistered = true;
		if(flags.contains("*"))
			ret.isSysop = true;
		if(params.length > 7)
			ret.hopcount = Integer.parseInt(params[7].substring(1));
		if(params.length > 8) {
			for(int i=8; i < params.length; i++)
				ret.realName += " " + params[i];
			ret.realName = ret.realName.substring(1);
		}
		return ret;
	}

	public String getNick() {
		return nick;
	}

	public void setNick(String nick) {
		this.nick = nick;
	}

	public boolean isRegistered() {
		return isRegistered;
	}

	public void setRegistered(boolean isRegistered) {
		this.isRegistered = isRegistered;
	}

	public String getIdent() {
		return ident;
	}

	public void setIdent(String ident) {
		this.ident = ident;
	}

	public String getHostmask() {
		return hostmask;
	}

	public void setHostmask(String hostmask) {
		this.hostmask = hostmask;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public int getHopcount() {
		return hopcount;
	}

	public void setHopcount(int hopcount) {
		this.hopcount = hopcount;
	}

	public boolean isAway() {
		return isAway;
	}

	public void setAway(boolean isAway) {
		this.isAway = isAway;
	}

	public boolean isSysop() {
		return isSysop;
	}

	public void setSysop(boolean isSysop) {
		this.isSysop = isSysop;
	}
}
