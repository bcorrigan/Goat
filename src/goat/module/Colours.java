package goat.module;

import goat.core.Constants;
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
   public String[] getCommands() {
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
		String c = m.getModCommand() ;
		String msg = "" ;
		if (c.equals("colours") || c.equals("colourguide")) {
			msg = "" ;
			for (int i=0; i<16; i++) {
				msg += colourStrings[i] + i + " ";
			}
		} else if(c.equals("colour")) {
			String t = m.getModTrailing().trim() ;
			msg = "Colours are: white, black, dark blue, dark green, red, brown, purple, olive, yellow, green, communism, teal, cyan, blue, magenta, dark gray, and light gray." ;
			if (t.equalsIgnoreCase("white")) 
				msg = Constants.WHITE + "white: " + Constants.WHITE.substring(1) ;
			else if (t.equalsIgnoreCase("black"))
				msg = Constants.BLACK +"black: " + Constants.BLACK.substring(1) ;
			else if (t.equalsIgnoreCase("dark blue"))
				msg = Constants.DARK_BLUE + "dark blue: " + Constants.DARK_BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("dark green"))
				msg = Constants.DARK_GREEN + "dark green: " + Constants.DARK_GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("red"))
				msg = Constants.RED + "red: " + Constants.RED.substring(1) ;
			else if (t.equalsIgnoreCase("brown"))
				msg = Constants.BROWN + "brown: " + Constants.BROWN.substring(1) ;
			else if (t.equalsIgnoreCase("purple"))
				msg = Constants.PURPLE + "purple: " + Constants.PURPLE.substring(1) ;
			else if (t.equalsIgnoreCase("olive"))
				msg = Constants.OLIVE + "olive: " + Constants.OLIVE.substring(1) ;
			else if (t.equalsIgnoreCase("yellow"))
				msg = Constants.YELLOW + "yellow: " + Constants.YELLOW.substring(1) ;
			else if (t.equalsIgnoreCase("green"))
				msg = Constants.GREEN + "green: " + Constants.GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("teal"))
				msg = Constants.TEAL + "teal: " + Constants.TEAL.substring(1) ;
			else if (t.equalsIgnoreCase("cyan"))
				msg = Constants.CYAN + "cyan: " + Constants.CYAN.substring(1) ;
			else if (t.equalsIgnoreCase("blue"))
				msg = Constants.BLUE + "blue: " + Constants.BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("magenta"))
				msg = Constants.MAGENTA + "magenta: " + Constants.MAGENTA.substring(1) ;
			else if (t.equalsIgnoreCase("dark gray"))
				msg = Constants.DARK_GRAY + "dark gray: " + Constants.DARK_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("light gray"))
				msg = Constants.LIGHT_GRAY + "light gray: " + Constants.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("communism"))
				msg = Constants.RED + "COMMUNISM!" ;
			else if (t.equalsIgnoreCase("adequacy"))
				msg = Constants.LIGHT_GRAY + "adequacy: " + Constants.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("homosexuality"))
				msg = homosexualise("HOMOSEXUALITY!!!!") ;
			else if (t.equalsIgnoreCase("camouflage"))
				msg = camouflage("Camouflage") ;
		} else if (c.equals("colourise")) {
			String text = Constants.removeFormattingAndColors(m.getModTrailing()).trim() ;
			int numColours = colourStrings.length ;
			int colourNum = text.hashCode() ;
			if (colourNum < 0) 
				colourNum = -colourNum ;
			colourNum = colourNum % (numColours + 3) ;
			//System.out.println("selecting colour for \"" + text + "\", hash=" + text.hashCode() + ", colorNum=" + text.hashCode() % (numColours + 3) );
			if (colourNum < numColours )
				msg = colourStrings[colourNum] + text ;
			else if (numColours + 0 == colourNum)
				msg = homosexualise(text) ;
			else if (numColours + 1 == colourNum)
				msg = camouflage(text) ;
			else if (numColours + 2 == colourNum) 
				msg = Constants.RED + text.toUpperCase() + "!!!" ;
			else
				msg = "I think something has gone wrong in my innards" ;
		}
		m.createReply(msg).send() ;
	}
	
	private String homosexualise(String arg) {
		String[] rainbow = { Constants.RED, Constants.YELLOW, Constants.DARK_GREEN, Constants.BLUE, Constants.PURPLE } ;
		String out = "" ;
		for (int i=0; i<arg.length(); i++) 
			out += rainbow[i % rainbow.length] + arg.charAt(i) ;
		return out ;
	}

	private String camouflage(String arg) {
		String[] camo = { Constants.DARK_GRAY, Constants.DARK_GREEN, Constants.DARK_GREEN, Constants.OLIVE, Constants.OLIVE, Constants.OLIVE, Constants.WHITE, Constants.BLACK } ;
		String out = "" ;
		for (int i=0; i<arg.length(); i++) 
			out += camo[rand.nextInt(camo.length)] + arg.charAt(i) ;
		return out ;
	}

	public static void main(String[] arg) {
	}
}
