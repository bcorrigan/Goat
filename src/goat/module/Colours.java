package goat.module;

import goat.core.Module;
import goat.core.Message;
import java.util.Random ;

/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 *	@author encontrado
 * 
 * @version $id$
 */

public class Colours extends Module {

	private String[] colourStrings ;
	private Random rand = new Random() ;
	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public static String[] getCommands() {
		return new String[]{"colour", "colours", "colourguide", "colourise" };
   }
	
	public Colours() {
		//set up colour-by-numbers
		colourStrings = new String[16] ;
		for (int i=0; i<16; i++) {
			colourStrings[i] = "\u0003";
			if (i < 10) 
				colourStrings[i] += "0" ;
			colourStrings[i] += i ;
		}
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		String c = m.modCommand ;
		String msg = "" ;
		if (c.equals("colours") || c.equals("colourguide")) {
			msg = "" ;
			for (int i=0; i<16; i++) {
				msg += colourStrings[i] + i + " ";
			}
		} else if(c.equals("colour")) {
			String t = m.modTrailing.trim() ;
			msg = "Colours are: white, black, dark blue, dark green, red, brown, purple, olive, yellow, green, communism, teal, cyan, blue, magenta, dark gray, and light gray." ;
			if (t.equalsIgnoreCase("white")) 
				msg = Message.WHITE + "white: " + Message.WHITE.substring(1) ;
			else if (t.equalsIgnoreCase("black"))
				msg = Message.BLACK +"black: " + Message.BLACK.substring(1) ;
			else if (t.equalsIgnoreCase("dark blue"))
				msg = Message.DARK_BLUE + "dark blue: " + Message.DARK_BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("dark green"))
				msg = Message.DARK_GREEN + "dark green: " + Message.DARK_GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("red"))
				msg = Message.RED + "red: " + Message.RED.substring(1) ;
			else if (t.equalsIgnoreCase("brown"))
				msg = Message.BROWN + "brown: " + Message.BROWN.substring(1) ;
			else if (t.equalsIgnoreCase("purple"))
				msg = Message.PURPLE + "purple: " + Message.PURPLE.substring(1) ;
			else if (t.equalsIgnoreCase("olive"))
				msg = Message.OLIVE + "olive: " + Message.OLIVE.substring(1) ;
			else if (t.equalsIgnoreCase("yellow"))
				msg = Message.YELLOW + "yellow: " + Message.YELLOW.substring(1) ;
			else if (t.equalsIgnoreCase("green"))
				msg = Message.GREEN + "green: " + Message.GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("teal"))
				msg = Message.TEAL + "teal: " + Message.TEAL.substring(1) ;
			else if (t.equalsIgnoreCase("cyan"))
				msg = Message.CYAN + "cyan: " + Message.CYAN.substring(1) ;
			else if (t.equalsIgnoreCase("blue"))
				msg = Message.BLUE + "blue: " + Message.BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("magenta"))
				msg = Message.MAGENTA + "magenta: " + Message.MAGENTA.substring(1) ;
			else if (t.equalsIgnoreCase("dark gray"))
				msg = Message.DARK_GRAY + "dark gray: " + Message.DARK_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("light gray"))
				msg = Message.LIGHT_GRAY + "light gray: " + Message.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("communism"))
				msg = Message.RED + "COMMUNISM!" ;
			else if (t.equalsIgnoreCase("adequacy"))
				msg = Message.LIGHT_GRAY + "adequacy: " + Message.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("homosexuality"))
				msg = homosexualise("HOMOSEXUALITY!!!!") ;
			else if (t.equalsIgnoreCase("camouflage"))
				msg = camouflage("Camouflage") ;
		} else if (c.equals("colourise")) {
			String text = m.modTrailing ;
			m.removeFormattingAndColors() ;
			text = m.modTrailing.trim() ;
			int numColours = colourStrings.length ;
			int colourNum = text.hashCode() ;
			if (colourNum < 0) 
				colourNum = -colourNum ;
			colourNum = colourNum % (numColours + 3) ;
			System.out.println("selecting colour for \"" + text + "\", hash=" + text.hashCode() + ", colorNum=" + text.hashCode() % (numColours + 3) );
			if (colourNum < numColours )
				msg = colourStrings[colourNum] + text ;
			else if (numColours + 0 == colourNum)
				msg = homosexualise(text) ;
			else if (numColours + 1 == colourNum)
				msg = camouflage(text) ;
			else if (numColours + 2 == colourNum) 
				msg = Message.RED + text.toUpperCase() + "!!!" ;
			else
				msg = "I think something has gone wrong in my innards" ;
		}
		m.createReply(msg).send() ;
	}
	
	private String homosexualise(String arg) {
		String[] rainbow = { Message.RED, Message.YELLOW, Message.DARK_GREEN, Message.BLUE, Message.PURPLE } ;
		String out = "" ;
		for (int i=0; i<arg.length(); i++) 
			out += rainbow[i % rainbow.length] + arg.charAt(i) ;
		return out ;
	}

	private String camouflage(String arg) {
		String[] camo = { Message.DARK_GRAY, Message.DARK_GREEN, Message.DARK_GREEN, Message.OLIVE, Message.OLIVE, Message.OLIVE, Message.WHITE, Message.BLACK } ;
		String out = "" ;
		for (int i=0; i<arg.length(); i++) 
			out += camo[rand.nextInt(camo.length)] + arg.charAt(i) ;
		return out ;
	}

	public static void main(String[] arg) {
	}
}
