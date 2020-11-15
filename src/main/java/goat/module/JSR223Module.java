package goat.module;

import goat.core.IrcMessage;
import goat.core.Module;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.Invocable;

import java.io.FileReader;
import java.io.BufferedReader;

//JSR223 is a standard that abstracts plugin scripting languages 
//therefore, this module can wrap ANYTHING. jython, jruby, groovy, rhino 
// - if it supports JSR223 you're golden 
public class JSR223Module extends Module {
	
	ScriptEngine engine;
	Invocable inv;
	Object module;
	
	public JSR223Module(String extension, String moduleScript) throws NoSuchMethodException, ScriptException  {
		//construct interpreter
		engine = new ScriptEngineManager().getEngineByExtension(extension);
		engine.eval(moduleScript);
		inv = (Invocable) engine;
		module = inv.invokeFunction("getInstance");
	}
	
	@Override
	public String[] getCommands() {
		try {
			return (String[]) inv.invokeMethod(module, "getCommands");
		} catch (Exception e) {
			System.out.println("Error with getCommands():" + e.getLocalizedMessage() );
		}
		
		return null;
	}
	
	@Override
	public void processOtherMessage(Message m) {
		try {
			inv.invokeMethod(module, "processOtherMessage", m);
		} catch (Exception e) {
			m.reply(m.getSender() + ": error calling into script:" + e.getLocalizedMessage() );
		}
	}

	@Override
	public void processPrivateMessage(Message m) {
		try {
			inv.invokeMethod(module, "processPrivateMessage", m);
		} catch (Exception e) {
			m.reply(m.getSender() + ": error calling into script:" + e.getLocalizedMessage() );
		}
	}

	@Override
	public void processChannelMessage(Message m) {
		try {
			inv.invokeMethod(module, "processChannelMessage", m);
		} catch (Exception e) {
			m.reply(m.getSender() + ": error calling into script:" + e.getLocalizedMessage() );
		}
	}
	
	public static void main(String[] args) throws Exception {
		BufferedReader bis = new BufferedReader(  new FileReader("/home/bc/workspace/Goat/scripts/PyModule.py"));
		String line;
		String mod="";
		while((line=bis.readLine())!=null) {
			mod+=line+"\n";
		}
		JSR223Module smod = new JSR223Module("py",mod);
		smod.engine.eval(mod);
		Invocable inv = (Invocable) smod.engine;
		Object module = inv.invokeFunction("getInstance");
		IrcMessage msg = new IrcMessage("","","","My goodness, we peeked into a message object from python!");
		inv.invokeMethod(module, "processChannelMessage", msg);
		Object commands = inv.invokeMethod(module, "getCommands");
		System.out.println("Commands:" + commands);
	}

}
