package goat.module;

import goat.core.Module;
import goat.core.Message;

/**
 * Copyright (c) 2004 Robot Slave Enterprise Solutions
 * 
 *	@author encontrado
 * 
 * @version $id$
 */

public class Colours extends Module {

	
	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}
   public String[] getCommands() {
		return new String[]{"colour", "colours", "colourguide" };
   }
	
	public Colours() {
	}

	public void processPrivateMessage(Message m) {
		processChannelMessage(m) ;
	}

	public void processChannelMessage(Message m) {
		String c = m.modCommand ;
		if (c.equals("colours") || c.equals("colourguide")) {
			String line = "\u0003000 \u0003011 \u0003022 \u0003033 " +
			              "\u0003044 \u0003055 \u0003066 \u0003077 " +
							  "\u0003088 \u0003099 \u00031010 \u00031111 " +
							  "\u00031212 \u00031313 \u00031414 \u00031515" ;
			m.createReply(line).send() ;
		} else if(c.equals("colour")) {
			String t = m.modTrailing.trim() ;
			String msg = "Colours are: white, black, dark blue, dark green, red, brown, purple, olive, yellow, green, communism, teal, cyan, blue, magenta, dark grey, and light grey." ;
			if (t.equalsIgnoreCase("white")) 
				msg = m.WHITE + "white: " + m.WHITE.substring(1) ;
			else if (t.equalsIgnoreCase("black"))
				msg = m.BLACK +"black: " + m.BLACK.substring(1) ;
			else if (t.equalsIgnoreCase("dark blue"))
				msg = m.DARK_BLUE + "dark blue: " + m.DARK_BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("dark green"))
				msg = m.DARK_GREEN + "dark green: " + m.DARK_GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("red"))
				msg = m.RED + "red: " + m.RED.substring(1) ;
			else if (t.equalsIgnoreCase("brown"))
				msg = m.BROWN + "brown: " + m.BROWN.substring(1) ;
			else if (t.equalsIgnoreCase("purple"))
				msg = m.PURPLE + "purple: " + m.PURPLE.substring(1) ;
			else if (t.equalsIgnoreCase("olive"))
				msg = m.OLIVE + "olive: " + m.OLIVE.substring(1) ;
			else if (t.equalsIgnoreCase("yellow"))
				msg = m.YELLOW + "yellow: " + m.YELLOW.substring(1) ;
			else if (t.equalsIgnoreCase("green"))
				msg = m.GREEN + "green: " + m.GREEN.substring(1) ;
			else if (t.equalsIgnoreCase("teal"))
				msg = m.TEAL + "teal: " + m.TEAL.substring(1) ;
			else if (t.equalsIgnoreCase("cyan"))
				msg = m.CYAN + "cyan: " + m.CYAN.substring(1) ;
			else if (t.equalsIgnoreCase("blue"))
				msg = m.BLUE + "blue: " + m.BLUE.substring(1) ;
			else if (t.equalsIgnoreCase("magenta"))
				msg = m.MAGENTA + "magenta: " + m.MAGENTA.substring(1) ;
			else if (t.equalsIgnoreCase("dark gray"))
				msg = m.DARK_GRAY + "dark gray: " + m.DARK_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("light gray"))
				msg = m.LIGHT_GRAY + "light gray: " + m.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("communism"))
				msg = m.RED + "COMMUNISM!" ;
			else if (t.equalsIgnoreCase("adequacy"))
				msg = m.LIGHT_GRAY + "adequacy: " + m.LIGHT_GRAY.substring(1) ;
			else if (t.equalsIgnoreCase("homosexuality"))
				msg = m.RED + "H" +
					   m.YELLOW + "O" +
						m.DARK_GREEN + "M" +
						m.BLUE + "O" +
						m.PURPLE + "S" +
						m.RED + "E" +
					   m.YELLOW + "X" +
						m.DARK_GREEN + "U" +
						m.BLUE + "A" +
						m.PURPLE + "L" +
						m.RED + "I" +
					   m.YELLOW + "T" +
						m.DARK_GREEN + "Y" +
						m.BLUE + "!" +
						m.PURPLE + "!" +
						m.RED + "!" ;
			m.createReply(msg).send() ;
		}
	}

	public static void main(String[] arg) {
	}
}
