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
		"a neo-noir",
		"an alternate-history",
		"an ancient",
		"a post-apocalyptic",
		"a dystopian",
		"a VR-simulated",
		"a metaphorical",
		"an anachronistic",
		"a leather-clad",
		"a coal-powered",
		"a dragon-filled",
		"a shrill",
		"a sex-saturated",
		"a transhumanist",
		"a cyberpunk",
		"an interwar",
		"an agricultural",
		"a utopian",
		"a fascist",
		"a racist",
		"a sexist",
		"a utopian",
		"a laughably innacurate",
		"an unrelentingly criminal",
		"a cyberpunk",
		"a mechanoid",
		"a zombie-filled",
		"an all woman",
		"a nudist",
		"an oppressive",
		"a heteronormative",
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
		"Old West",
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
		"feckless but obscenely wealthy heir",
		"trainee priest harbouring secret doubts",
		"nerd virgin",
		"'sperg",
		"Joe Sixpack",
		"doting father",
		"documentarian",
		"reformed ex-convict",
		"magical negro",
		"scam artist",
		"FBI agent in deep cover",
		"pimp with a heart of gold",
		"anthropomorphic horse",
	};
	String p4[] = {
		"a magic diadem",
		"an arcane prophecy",
		"a dusty tome",
		"a crazy old man",
		"an alien artifact",
		"an enchanted sword",
		"an otherworldly portal",
		"a dream-inducing drug",
		"an encrypted data feed",
		"a time-travelling soldier",
		"an exiled angel",
		"a talking fish",
		"a talking robotic head from the far future",
		"a humming monitor-obelisk",
		"a skull of a dragon",
		"a jewel encrusted crown",
		"a plain wooden goblet",
		"a secret government programme",
		"a great and near-mythical creature thought to be extinct",
		"a mathematical insight of great beauty and practical use",
		"an improbably complicated conspiracy",
		"an obvious MacGuffin",
		"an additional ten commandments",
		"a priceless bejeweled dildo",
		"an incredible deal on car insurance",
		"a bag of money",
		"a secret passageway",
		"a mind control device",
		"a rising caliphate",
		"a terrorist plot",
		"a superhero's secret identity",
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
		"the prophet of a suicide cult",
		"a vast right-wing conspiracy",
		"the leftist media bias",
		"a sleeper cell of jihadis",
		"his evil twin",
		"a nasty bout of depression",
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
		"a post-op transexual genderqueer womyn",
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
		"superior racial background",
		"case of the mondays",
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
		"the death of every single character",
		"an inappropriately happy ending",
		"a quarter hour of gratuitous explosions",
		"an enlightened understanding of different cultures",
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
		"Mega",
		"Anti",
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
		"run",
		"fall",
		"path",
		"freeze",
		"crash",
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
		String reply = m.getSender() + ": In ";
		reply += p1[generator.nextInt(p1.length)];
		reply += " ";
		reply += p2[generator.nextInt(p2.length)];
		reply += ", a young ";
		reply += p3[generator.nextInt(p3.length)];
		reply += " stumbles across ";
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
		reply += "\"";

		m.reply(reply);
	}
}
