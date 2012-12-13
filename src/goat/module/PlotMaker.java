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

public class PlotMaker extends Module {
	Random generator = new Random();
	// Bank of phrases to use in the plot.
	String p1[] = {
		" neo-noir",
		"n alternate-history",
		"n ancient",
		" post-apocalyptic",
		" dystopian",
		" VR-simulated",
		" metaphorical",
		"n anachronistic",
		" leather-clad",
		" coal-powered",
		" dragon-filled",
		" shrill",
		" sex-saturated",
		" transhumanist",
		" cyberpunk",
		"n interwar",
		" agricultural",
		" utopian",
		" fascist",
		" racist",
		" sexist",
		" utopian",
		" laughably innacurate",
		" unrelentingly criminal",
		" cyberpunk",
		" mechanoid",
		" zombie-filled",
		" all woman",
	};
	String p2[] = {
		"America",
		"Japan",
		"Soviet Russia",
		"Victorian Britain",
		"medieval Europe",
		"Aztec empire",
		"Atlantis",
		"terraformed Mars",
		"Antarctica",
		"one-way spaceflight",
		"Outer Rim world",
		"set from Road Warrior",
		"Dyson sphere",
		"Belle Epoque Paris",
		"Rome",
		"China",
		"Germany",
		"Athens",
		"Sparta",
		"Ancient Realm",
		"Robot Realm",
		"slave planet",
		"Trantor",
		"Frontier",
		"Everytown, USA",
		"Disneyland knockoff",
	};
	String p3[] = {
		"flying message courier",
		"student of metaphysics",
		"milquetoast office drone",
		"schlub with mild OCD",
		"farm boy with dreams",
		"techno-obsessed geek",
		"brooding loner",
		"wisecracking mercenary",
		"idealistic revolutionary",
		"journeyman inventor",
		"collector of oddities",
		"author self-insert",
		"robot seeking out its humanity",
		"far right revolutionary",
		"newly qualified veterinarian",
		"gentleman professor of archaeology",
		"patent clerk",
		"truth-seeking journalist",
		"feckless obscenely wealthy heir",
		"young, trainee preist harbouring secret doubts",
		"nerd virgin",
		"'sperg",
		"Joe Sixpack",
		"doting father",
		"documentarian",
		"reformed ex-convict",
		"magical negro",
		"scam artist",
	};
	String p4[] = {
		" magic diadem",
		"n arcane prophecy",
		" dusty tome",
		" crazy old man",
		" alien artifact",
		" enchanted sword",
		"n otherworldly portal",
		" dream-inducing drug",
		"n encrypted data feed",
		" time-travelling soldier",
		"n exiled angel",
		" talking fish",
		" talking robotic head from the far future",
		" humming monitor-obelisk",
		" skull of a dragon",
		" jewel encrusted crown",
		" plain wooden goblet",
		" secret government programme",
		" the last specimen of a great and near-mythical creature",
		" mathematical insight of great beauty and practical use",
		"n improbably complicated conspiracy",
		"n obvious MacGuffin",
	};
	String p5[] = {
		"a megalomaniacal dictator",
		"a government conspiracy",
		"a profit-obsessed corporation",
		"a sneering wizard",
		"supernatural monsters",
		"computer viruses made real",
		"murderous robots",
		"an army led by a sadist",
		"forces that encourage conformity",
		"a charismatic polititian on the rise",
		"humanity's selfish nature",
		"his own insecurity vis-a-vis girls",
		"bureaucrats from the civil service",
		"a twisted artificial intelligence intent on universal domination",
		"a socialist",
		"an arrogant atheist",
		"Anonymous",
	};
	String p6[] = {
		"a sarcastic female techno-geek",
		"a tomboyish female mechanic",
		"a shape-shifting female assassin",
		"a leather-clad female in shades",
		"a girl who's always loved him",
		"a bookish female scholar with mousy brown hair",
		"a cherubic girl with pigtails and spunk",
		"a female who inexplicably becomes attracted to the damaged protagonist for unstated reasons",
		"a supposedly androgynous robot with an unaccountably sexy female voice and the suggestion of metallic breasts",
		"a furry - some kind of dog in fact - blessed with a disturbingly attractive behind, big eyes, and full lips",
		"an androgynous boy who turns out to be an attractive young girl much to the relief of all",
		"a freakishly tall, leggy, blonde and unfortunately very stupid woman with very good taste in futuristic boots",
		"a nunchuck-wielding femi-ninja",
		"a young female schoolteacher",
		"a chain-smoking, wizened MILF",
		"a sultry widow aching for release",
		"a lost, wanton stripper",
		"a streetsmart orphan girl",
		"a sassy female bounty hunter",
		"a wily gypsy girl",
		"a snooty female research scientist",
		"a cigar chomping butch lesbian",
		"a veteran female soldier who must take medication to keep her sane",
		"a librarian who hides her beauty behind a pair of thick-framed spectacles",
		"a feminine robot from heinlein's fantasies",
		"the leader of a polyamarous anti-capitalist collective",
		"a sheltered Amish girl",
	};
	String p7[] = {
		"wacky pet",
		"welding gear",
		"closet full of assault rifles",
		"reference book",
		"cleavage",
		"facility with magic",
		"condescending tone",
		"discomfort in formal wear",
		"propensity for being captured",
		"ability to ignore the lead character's blatant sexism and hostility towards her liberated sexuality",
		"copy of vogue",
		"interfering, spying duenna",
		"obsession with the bechdel test",
		"amazing martial arts abilities",
		"facility with reading",
		"vast wealth",
		"powerful father",
		"talking car",
		"mechanical third arm",
		"positive attitude",
		"encyclopedic knowledge of Woody Allen films",
		"obsessive boyfriend",
		"racial purity",
	};
	String p8[] = {
		"a fistfight atop a tower",
		"a daring rescue attempt",
		"a heroic sacrifice that no one will ever remember",
		"a philosophical arguement punctuated by violence",
		"a false victory with the promise of future danger",
		"the invocation of a spell at the last possible moment",
		"eternal love professed without irony",
		"the land restored to health",
		"authorial preaching through the mouths of the characters",
		"convoluted nonsense that squanders the readers' goodwill",
		"wish-fulfillment solutions to real-world problems",
		"a cliffhanger for the sake of prompting a series",
		"entirely avoidable tragedy",
		"restoration of a static and possibly repressive cis-phobic society",
		"all of humanity cursed to a bleak and desolate future",
		"a revelation that it was all a dream",
		"a technological singularity",
		"every single character dying",
		"an inappropriately happy ending",
	};

	// Bank of word fragments to use for generation of the title
	String t1[] = {
		"Chrono",
		"Neuro",
		"Aero",
		"Cosmo",
		"Reve",
		"Necro",
		"Cyber",
		"Astro",
		"Psycho",
		"Steam",
		"Meta",
		"Black",
		"White",
		"Power",
		"Vibro",
		"Dark",
		"Death",
		"Inner",
		"Jingo",
	};
	String t2[] = {
		"punk",
		"mech",
		"noiac",
		"poli",
		"naut",
		"phage",
		"droid",
		"bot",
		"blade",
		"tron",
		"mancer",
		"War",
		"man",
		"mage",
	};
	String t3[] = {
		"s",
		"",
	};

	public int messageType() {
		return WANT_COMMAND_MESSAGES;
	}

	@Override
	public String[] getCommands() {
		return new String[]{"plot"};
	}
	@Override
	public void processPrivateMessage(Message m) {
		processChannelMessage(m);
	}
	@Override
	public void processChannelMessage(Message m) {
		//plot is args free.
		String reply = m.getSender() + ": In a";
		reply += p1[generator.nextInt(p1.length)];
		reply += " ";
		reply += p2[generator.nextInt(p2.length)];
		reply += ", a young ";
		reply += p3[generator.nextInt(p3.length)];
		reply += " stumbles across a";
		reply += p4[generator.nextInt(p4.length)];
		reply += " which spurs him into conflict with ";
		reply += p5[generator.nextInt(p5.length)];
		reply += " with the help of ";
		reply += p6[generator.nextInt(p6.length)];
		reply += " and her ";
		reply += p7[generator.nextInt(p7.length)];
		reply += ", culminating in ";
		reply += p8[generator.nextInt(p8.length)];
		reply += ".";

		reply += " Your title is: \"";
		reply += "The ";
		reply += t1[generator.nextInt(t1.length)];
		reply += t2[generator.nextInt(t2.length)];
		reply += t3[generator.nextInt(t3.length)];
		reply += "\"";

		m.reply(reply);
	}
}
