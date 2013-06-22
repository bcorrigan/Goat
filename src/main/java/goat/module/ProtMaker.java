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

import goat.core.Message;
import goat.core.Module;

import java.util.Random;

public class ProtMaker extends Module {
    Random generator = new Random();
    // Bank of phrases to use in the plot.
    String kungfuSettingTypes[] = {
        "colonial",
        "ancient",
        "timeless",
        "alternate-history",
        "anachronistic",
        "agricultural",
        "utopian",
        "oppressed",
        "capitalistic",
        "modern day",
        "feudal",
        "agrarian",
    };

    String kungfuSettings[] = {
        "Japan",
        "China",
        "Thailand",
        "Hong Kong",
        "Shanghai",
        "Tokyo",
        "Cambodia",
        "Vietnam",
        "Taipei",
        "Taiwan"
    };

    String kungfuStyles[] = {
        "kung fu",
        "muay thai",
        "karate",
        "wushu",
        "judo",
        "jiu jitsu",
        "bushido",
        "grecco roman wrestling",
        "sumo wrestling",
        "tai chi",
        "ikibana",
    };

    String kungfuHeroTypes[] = {
        "simple",
        "old",
        "ancient",
        "young",
        "middle-aged",
        "bumbling",
        "fat",
        "scrawny",
        "prophesied",
        "magical",
        "disgraced",
        "widowed",
        "wizened",
        "blind",
        "crippled",
        "drunken"
    };


    String kungfuHeros[] = {
        "farmer",
        "fisherman",
        "monk",
        "local party official",
        "samurai",
        "ronin",
        "mercenary",
        "thief",
        "student",
        "cowboy",
        "laborer",
        "soldier",
        "assassin",
    };

    String kungfuMaguffins[] = {
        "government corruption",
        "secret shipment of contraband",
        "theft of ancient heirloom",
        "crazy old man",
        "underground resistance movement",
        "partially overheard conversation",
        "beautiful but forbidden slave girl",
        "murder",
        "evidence of prot against emperor",
        "angry spirits",
        "illegal martial arts contest",
    };

    String kungfuVillains[] = {
        "gang of toughs",
        "interantional criminal organization",
        "local warlord",
        "his older brother",
        "white imperialists",
        "supernatural monsters",
        "evil monks",
        "ancient wizard-ninja",
        "army led by sadist",
        "treaure hunters",
    };

    String kungfuCompanions[] = {
        "cute shy girl",
        "cute dangerous girl",
        "fearless, useless girl",
        "best friend",
        "childhood rival",
        "white man",
        "Chris Rock",
        "cranky, decrepit old man",
        "domineering, screeching old woman",
        "kindly teacher",
        "his father's sword",
        "fat, friendly innkeeper",
    };

    String kungfuClimaxes[] = {
        "allegory of government unity",
        "heroic sacrifice",
        "fight atop tower",
        "daring rescue",
        "restore land to peasant",
        "tragedy",
        "death of hero",
        "death of all enemy",
        "romance tragedy",
        "big fight",
        "luck for fool",
        "death fight",
        "fight on mountain",
        "fight on boat",
        "fight in tower",
        "fight with monster",
        "fight with sword",
        "fight in forest",
        "blow up fort",
        "explosion"
    };

    // Bank of word fragments to use for generation of the title
    String kungfuTitleAdjectives[] = {
        "Iron",
        "Dark",
        "Last",
        "First",
        "Middle",
        "Final",
        "Steel",
        "Crouching",
        "Hidden",
        "Surprising",
        "Unknown",
        "Black",
        "White",
        "Lost",
        "Sharp",
        "Blind",
        "Flying",
        "Sudden",
        "Dying",
    };

    String kungfuTitleNouns[] = {
        "Fist",
        "Dragon",
        "Monkey",
        "Tiger",
        "Mantis",
        "Idol",
        "Blade",
        "Sword",
        "War",
        "Panda",
        "Foot",
        "Dagger",
        "Path",
        "Way",
        "Death",
        "Eye",
    };

    @Override
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
        reply += kungfuSettingTypes[generator.nextInt(kungfuSettingTypes.length)];
        reply += " ";
        reply += kungfuSettings[generator.nextInt(kungfuSettings.length)];
        reply += ", ";
        reply += kungfuHeroTypes[generator.nextInt(kungfuHeroTypes.length)];
        reply += " ";
        reply += kungfuHeros[generator.nextInt(kungfuHeros.length)];
        reply += " who study ";
        reply += kungfuStyles[generator.nextInt(kungfuStyles.length)];
        reply += " stumble across ";
        reply += kungfuMaguffins[generator.nextInt(kungfuMaguffins.length)];
        reply += " which spur him to conflict with ";
        reply += kungfuVillains[generator.nextInt(kungfuVillains.length)];
        reply += " with help of ";
        reply += kungfuCompanions[generator.nextInt(kungfuCompanions.length)];
        reply += ", culminate in ";
        reply += kungfuClimaxes[generator.nextInt(kungfuClimaxes.length)];
        reply += ".";

        reply += " Your title: \"";
        if (arg.equals("") || arg == null ) {
            reply += "The ";
            reply += kungfuTitleAdjectives[generator.nextInt(kungfuTitleAdjectives.length)];
            reply += " ";
            reply += kungfuTitleNouns[generator.nextInt(kungfuTitleNouns.length)];
        } else {
            reply += arg;
        }
        reply += "\"";


        m.reply(reply);
    }
}
