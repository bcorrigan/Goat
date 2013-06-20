package goat.module;
/*
 * This code is lifted WHOLESALE from "DoorBot" - see:
 * https://github.com/lietk12/DoorBot/blob/master/src/Modules/FictionGenerator/FictionGenerator.java
 *
 * And integrated into the goat way.
 *
 * This will randomly generate a story plot out of 214,990,848 possible plots.
 * Based on http://wondermark.com/554/
 *
 * Reworked to make martial arts plot lines.
 *
 * Copyright 2009 John French, Ethan Li, Alex Kohanski
 *
 * This file is part of DoorBot.
 * Doorbot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DoorBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DoorBot.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.*;
import java.io.*;
import goat.core.*;

public class ProtMaker extends Module {
	Random generator = new Random();
	// Bank of phrases to use in the plot.
	String p1[] = {
		"a colonized",
    "an ancient",
    "a timeless",
		"an alternate-history",
		"an ancient",
		"a post-apocalyptic",
		"a metaphorical",
		"an anachronistic",
		"an agricultural",
		"a utopian",
		"a laughably innacurate",
		"an oppressed",
    "a capitalistic",
    "a modern day",
	};
	String p2[] = {
    "Japan",
    "China",
    "Thailand",
    "Hong Kong",
	};
	String p3[] = {
    "a farmer",
    "a fisher",
    "an old man",
    "a monk",
    "a local party official",
    "a samurai",
    "a ronin",
    "a blind man",
    "a disgraced mercenary",
	};
	String p4[] = {
    "government corruption",
    "a secret shipment of contraband",
    "the theft of an ancient heirloom",
		"a crazy old man",
		"an underground resistance movement",
		"a partially overheard conversation",
		"a beautiful but forbidden slave girl",
		"a murder",
	};
	String p5[] = {
    "a gang of toughs",
    "an interntional criminal organization",
    "a local warlord",
    "his own brother",
    "white imperialists",
		"supernatural monsters",
    "evil monks",
    "an ancient wizard-ninja",
		"an army led by a sadist",
	};
	String p6[] = {
    "a cute, but shy girl",
    "a cute and surprisingly dangerous girl",
    "a fearless, but ultimately useless girl",
	};
	String p8[] = {
    "an allegory about how only a united government can serve its people",
    "a heroic sacrifice for the greater good",
		"a fistfight atop a tower",
		"a daring rescue attempt",
		"the land restored to health",
		"entirely avoidable tragedy",
		"the death of every single character",
    "the death of dozens of nameless foes",
		"a romance that ends tragically due only to wounded pride",
		"an intense but pointless denouement that answers no questions",
    "a bumbling fool finally redeeming himself",
	};

	// Bank of word fragments to use for generation of the title
	String t1[] = {
    "Iron",
    "Dark",
    "Last",
    "Final",
    "Steel",
    "Crouching",
    "Hidden",
    "Kung fu",
    "First",
    "Surprising",
    "Unknown",
		"Black",
		"White",
    "Lost",
    "Sharp",
    "Blind",
	};
	String t2[] = {
    "Fist",
    "Dragon",
    "Monkey",
    "Tiger",
    "Idol",
    "Blade",
    "Sword",
		"War",
	};

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	@Override
	public String[] getCommands() {
		return new String[]{"prot"};
	}
	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}
	@Override
	public void processChannelMessage(Message m) {

		String arg = m.getModTrailing().trim();

		String reply = m.getSender() + ": In ";
		reply += p1[generator.nextInt(p1.length)];
		reply += " ";
		reply += p2[generator.nextInt(p2.length)];
		reply += ", ";
		reply += p3[generator.nextInt(p3.length)];
		reply += " who studies martial arts stumbles across ";
		reply += p4[generator.nextInt(p4.length)];
		reply += " which spurs him into conflict with ";
		reply += p5[generator.nextInt(p5.length)];
		reply += " with the help of ";
		reply += p6[generator.nextInt(p6.length)];
		reply += ", culminating in ";
		reply += p8[generator.nextInt(p8.length)];
		reply += ".";

		reply += " Your title is: \"";
		if (arg.equals("") || arg == null ) {
			reply += "The ";
			reply += t1[generator.nextInt(t1.length)];
      reply += " ";
			reply += t2[generator.nextInt(t2.length)];
		} else {
			reply += arg;
		}
		reply += "\"";

    reply = reply.replace("l", "r");
    reply = reply.replace("L", "R");

		m.reply(reply);
	}
}
