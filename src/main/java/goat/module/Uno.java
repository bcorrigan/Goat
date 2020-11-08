/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.module;

import goat.Goat;
import goat.uno.*;
import goat.uno.cards.*;
import goat.core.Constants;
import goat.core.Module;
import goat.core.Message;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Handles IO between Goat and Uno
 *
 * @author bc
 */
public class Uno extends Module implements Output {
    boolean playing; //true if playing a game
    boolean waiting; //true if a game has been started but only one player has joined it
    boolean hasJoined; //signifies whether it is safe to start the game
    Game game;
    Message target;        //the target channel, effectively. The message that started the game
    String longReply;            //Reply string for multiple events
    private static final String[] SPACES = new String[20];    //spaces for formatting purposes
    private static final String BG = Constants.COLCODE + ",1";  //bg colour
    private static final String NORMAL = Constants.WHITE + BG; //reset to normal + BG

    private ExecutorService pool = Goat.modController.getPool();
    
    static {
        for (int i = 0; i < 20; i++) {
            SPACES[i] = " ";
            for (int j = 0; j < i - 1; j++) {
                SPACES[i] += " ";
            }
        }
    }
    
    public void processChannelMessage(Message m) {
        if (waiting || playing)
            if (m.getModCommand().equalsIgnoreCase("botjoin")) {
                String[] botNames = m.getModTrailing().split(" ");
                for (String botName : botNames) {
                    if (getPlayer(botName) == null) {
                        if (game.players.size() > 10) {
                            m.reply("Fuck off, we're full. ");
                            return;
                        }
                        if (botName.trim().length() == 0)
                            continue;
                        game.join(botName, true);
                        hasJoined = true;
                    }
                }
            }
        if (!playing) {
            if (m.getModCommand().equalsIgnoreCase("uno") && !waiting) {
                target = m;
                game = new Game(this, m.getSender());
                longReply = "";
                waiting = true;
                pool.execute(new GameStarter(this));
            }
            if (waiting)
                if (m.getModCommand().equalsIgnoreCase("join"))
                    if (getPlayer(m.getSender()) == null) {
                        game.join(m.getSender(), false);
                        hasJoined = true;
                    }
        } else {
            if (!game.needColour) {
                if (m.getModCommand().equalsIgnoreCase("play") 
                		|| m.getModCommand().equalsIgnoreCase("p")) {
                    try {
                        Player player = getPlayer(m.getSender());
                        if (player != null) {
                            Card card = getCard(m.getModTrailing(), player);
                            if (card != null) {
                                game.play(player, card);
                            }
                        }
                    } catch (InvalidCardColourException e) {
                        e.printStackTrace();
                    } catch (InvalidCardNumberException e) {
                        e.printStackTrace();
                    }
                } else if (m.getModCommand().equalsIgnoreCase("draw") 
                		|| m.getModCommand().equalsIgnoreCase("<")) {
                    Player player = getPlayer(m.getSender());
                    if (player != null)
                        game.draw(player);
                } else if (m.getModCommand().equalsIgnoreCase("pass") 
                		|| m.getModCommand().equalsIgnoreCase(">")) {
                    Player player = getPlayer(m.getSender());
                    if (player != null)
                        game.pass(player);
                }
            } else {
                if (m.getModCommand().equalsIgnoreCase("red") 
                		|| m.getModCommand().equalsIgnoreCase("r"))
                    game.setColour(getPlayer(m.getSender()), Card.RED);
                else if (m.getModCommand().equalsIgnoreCase("blue") 
                		|| m.getModCommand().equalsIgnoreCase("b"))
                    game.setColour(getPlayer(m.getSender()), Card.BLUE);
                else if (m.getModCommand().equalsIgnoreCase("green") 
                		|| m.getModCommand().equalsIgnoreCase("g"))
                    game.setColour(getPlayer(m.getSender()), Card.GREEN);
                else if (m.getModCommand().equalsIgnoreCase("red") 
                		|| m.getModCommand().equalsIgnoreCase("r"))
                    game.setColour(getPlayer(m.getSender()), Card.RED);
                else if (m.getModCommand().equalsIgnoreCase("yellow") 
                		|| m.getModCommand().equalsIgnoreCase("y"))
                    game.setColour(getPlayer(m.getSender()), Card.YELLOW);
            }
            if (m.getModCommand().equalsIgnoreCase("join")) {
                if (getPlayer(m.getSender()) == null)
                    game.join(m.getSender(), false);
            } else if (m.getModCommand().equalsIgnoreCase("hand") 
            		|| m.getModCommand().equalsIgnoreCase("h")) {
                Player player = getPlayer(m.getSender());
                if (player != null)
                    game.hand(player);
            } else if (m.getModCommand().equalsIgnoreCase("show") 
            		|| m.getModCommand().equalsIgnoreCase("s")) {
                game.show();
            } else if (m.getModCommand().equalsIgnoreCase("stopgame")) {
                m.reply(Constants.BOLD + "Uno" + Constants.BOLD + " has been stopped.");
                playing = false;
            } else if (m.getModCommand().equalsIgnoreCase("order")) {
                game.order();
            }
        }

        if (m.getModCommand().equalsIgnoreCase("unoscore")) {
            ArrayList records = Scores.getRecords();
            if (records.size() == 0) {
                m.reply("Nobody's got any scores yet :(");
                return;
            }
            Iterator it = records.iterator();
            int lScore = 0, lHScore = 0, lgW = 0, lgE = 0, lNick = 0;
            int i=0;
            while (it.hasNext()) {
                i++;
                if(i>20)
                    break;
                goat.uno.Record record = (goat.uno.Record) it.next();
                if (record.getName().length() > lNick)
                    lNick = record.getName().length();
                if (Integer.toString(record.getHiScore()).length() > lHScore)
                    lHScore = Integer.toString(record.getHiScore()).length();
                if (Integer.toString(record.getTotalScore()).length() > lScore)
                    lScore = Integer.toString(record.getTotalScore()).length();
                if (Integer.toString(record.getGamesWon()).length() > lgW)
                    lgW = Integer.toString(record.getGamesWon()).length();
                if (Integer.toString(record.getGamesEntered()).length() > lgE)
                    lgE = Integer.toString(record.getGamesEntered()).length();

            }
            m.reply("   " + Constants.UNDERLINE + "Name" + SPACES[lNick + 3 - 4]
                            + "HiScore" + SPACES[lHScore + 7 - 7]
                            + "Won" + SPACES[lgW + 3 - 3]
                            + "Games" + SPACES[lgE + 5 - 5]
                            + "Ratio  "
                            + "TotalScore");

            it = records.iterator();
            int count = 0;
            while (it.hasNext()) {
                if(count>19) break;
                goat.uno.Record record = (goat.uno.Record) it.next();
                count++;
                String ratio;
                if (record.getGamesEntered() > 0) {
                    ratio = Float.toString((float) record.getGamesWon() / (float) record.getGamesEntered() * 100f);
                    if (ratio.length() == 3)
                        ratio += "%   ";
                    else if (ratio.length() == 4)
                        ratio += "%  ";
                    else if (ratio.length() > 4)
                        ratio = ratio.substring(0, 5) + "% ";
                } else
                    ratio = "0%   ";
                m.reply(Constants.BOLD + count + Constants.BOLD + SPACES[3 - Integer.toString(count).length()] + record.getName()
                                + SPACES[lNick + 3 - record.getName().length()]
                                + record.getHiScore() + SPACES[lHScore + 7 - Integer.toString(record.getHiScore()).length()]
                                + record.getGamesWon() + SPACES[lgW + 3 - Integer.toString(record.getGamesWon()).length()]
                                + record.getGamesEntered() + SPACES[lgE + 5 - Integer.toString(record.getGamesEntered()).length()]
                                + ratio + "  "
                                + record.getTotalScore());
            }

        }

    }

    public void processPrivateMessage(Message m) {

    }

    public int messageType() {
        return WANT_COMMAND_MESSAGES;
    }

    public String[] getCommands() {
        return new String[]{"red", "blue", "green", "yellow", "play", "draw", "pass", "uno", "join", "hand", "show", "stopgame", "remove", "leave", "order", "unoscore", "p", "<", ">", "r", "g", "b", "y", "h", "s", "botjoin"};
    }

    private Player getPlayer(String name) {
        if (game.currentPlayer.getName().equals(name)) {
            return game.currentPlayer;
        }
        LinkedList players = game.players;
        Iterator it = players.iterator();
        for (Object player1 : players) {
            Player player = (Player) player1;
            if (player.getName().equals(name))
                return player;
        }
        return null;
    }

    private Card getCard(String modTrailing, Player player) throws InvalidCardColourException, InvalidCardNumberException {
        Card card = newCard(modTrailing);
        if (card == null)
            return null;
        ArrayList cards = player.getHand();
        Iterator it = cards.iterator();
        for (Object card1 : cards) {
            Card playerCard = (Card) card1;
            if (card.getColour() == playerCard.getColour() && card.getNumber() == playerCard.getNumber() && card.getType() == playerCard.getType())
                return playerCard;
        }
        return null;
    }

    private Card newCard(String modTrailing) throws InvalidCardColourException, InvalidCardNumberException {
        String[] words = modTrailing.toLowerCase().split(" ");
        try {
            if (words.length == 0)
                return null;
            if (words.length == 1) {
                if (words[0].startsWith("w"))
                    return new Wild();
            } else if (words[0].startsWith("g")) {    //green
                if (words[1].startsWith("d")) //draw two
                    return new Draw(Card.GREEN);
                if (words[1].startsWith("r")) //reverse
                    return new Reverse(Card.GREEN);
                if (words[1].startsWith("s")) //skip
                    return new Skip(Card.GREEN);
                int number = Integer.parseInt(words[1]);    //might have to try/catch this
                return new NormalCard(Card.GREEN, number);
            } else if (words[0].startsWith("r")) {    //red
                if (words[1].startsWith("d")) //draw two
                    return new Draw(Card.RED);
                if (words[1].startsWith("r")) //reverse
                    return new Reverse(Card.RED);
                if (words[1].startsWith("s")) //skip
                    return new Skip(Card.RED);
                int number = Integer.parseInt(words[1]);    //might have to try/catch this
                return new NormalCard(Card.RED, number);
            } else if (words[0].startsWith("y")) {    //yellow
                if (words[1].startsWith("d")) //draw two
                    return new Draw(Card.YELLOW);
                if (words[1].startsWith("r")) //reverse
                    return new Reverse(Card.YELLOW);
                if (words[1].startsWith("s")) //skip
                    return new Skip(Card.YELLOW);
                int number = Integer.parseInt(words[1]);    //might have to try/catch this
                return new NormalCard(Card.YELLOW, number);
            } else if (words[0].startsWith("b")) {    //blue
                if (words[1].startsWith("d")) //draw two
                    return new Draw(Card.BLUE);
                if (words[1].startsWith("r")) //reverse
                    return new Reverse(Card.BLUE);
                if (words[1].startsWith("s")) //skip
                    return new Skip(Card.BLUE);
                int number = Integer.parseInt(words[1]);    //might have to try/catch this
                return new NormalCard(Card.BLUE, number);
            } else if (words[0].startsWith("w")) {  //wild&WDF
                if (words[1].startsWith("d"))
                    return new WDF();
            }
        } catch (NumberFormatException nfe) {
            return null;    //bad number
        }
        return null;
    }

    private String getStringForCard(Card card) {
        switch (card.getType()) {
            case Card.NORMALCARD:
                switch (card.getColour()) {
                    case Card.RED:
                        return Constants.RED + "Red " + card.getNumber() + NORMAL;
                    case Card.BLUE:
                        return Constants.BLUE + "Blue " + card.getNumber() + NORMAL;
                    case Card.YELLOW:
                        return Constants.YELLOW + "Yellow " + card.getNumber() + NORMAL;
                    case Card.GREEN:
                        return Constants.GREEN + "Green " + card.getNumber() + NORMAL;
                }
            case Card.REVERSE:
                switch (card.getColour()) {
                    case Card.RED:
                        return Constants.RED + "Red Reverse" + NORMAL;
                    case Card.BLUE:
                        return Constants.BLUE + "Blue Reverse" + NORMAL;
                    case Card.YELLOW:
                        return Constants.YELLOW + "Yellow Reverse" + NORMAL;
                    case Card.GREEN:
                        return Constants.GREEN + "Green Reverse" + NORMAL;
                }
            case Card.SKIP:
                switch (card.getColour()) {
                    case Card.RED:
                        return Constants.RED + "Red Skip" + NORMAL;
                    case Card.BLUE:
                        return Constants.BLUE + "Blue Skip" + NORMAL;
                    case Card.YELLOW:
                        return Constants.YELLOW + "Yellow Skip" + NORMAL;
                    case Card.GREEN:
                        return Constants.GREEN + "Green Skip" + NORMAL;
                }
            case Card.DRAW:
                switch (card.getColour()) {
                    case Card.RED:
                        return Constants.RED + "Red Draw Two" + NORMAL;
                    case Card.BLUE:
                        return Constants.BLUE + "Blue Draw Two" + NORMAL;
                    case Card.YELLOW:
                        return Constants.YELLOW + "Yellow Draw Two" + NORMAL;
                    case Card.GREEN:
                        return Constants.GREEN + "Green Draw Two" + NORMAL;
                }
            case Card.FAKE:
                switch (card.getColour()) {
                    case Card.RED:
                        return Constants.RED + "Red" + NORMAL;
                    case Card.BLUE:
                        return Constants.BLUE + "Blue" + NORMAL;
                    case Card.YELLOW:
                        return Constants.YELLOW + "Yellow" + NORMAL;
                    case Card.GREEN:
                        return Constants.GREEN + "Green" + NORMAL;
                }
            case Card.WILD:
                return Constants.YELLOW + 'W' + Constants.BLUE + 'i' + Constants.RED + 'l' + Constants.GREEN + 'd' + NORMAL;
            case Card.WDF:
                return Constants.YELLOW + 'W' + Constants.BLUE + 'i' + Constants.RED + 'l' + Constants.GREEN + "d "
                        + Constants.BLUE + 'D' + Constants.YELLOW + 'r' + Constants.GREEN + 'a' + Constants.RED + "w "
                        + Constants.GREEN + 'F' + Constants.RED + 'o' + Constants.BLUE + 'u' + Constants.YELLOW + 'r' + NORMAL;
        }
        return null;
    }

    public void gameInit() {
        target.reply(Constants.BOLD + "Uno" + Constants.BOLD + " has started!");
    }

    public void playerStart(Player player) {
        target.reply(Constants.BOLD + player.getName() + Constants.BOLD + " is ready to play uno!");
    }

    public void hand(Player player) {
        ArrayList cards = player.getHand();
        Iterator it = cards.iterator();
        String reply = "";
        while (it.hasNext()) {
            reply += ' ' + getStringForCard((Card) it.next());
        }
        new Message("", "NOTICE", player.getName(), NORMAL + reply).send();
    }

    public void noSuchCard(Player player) {
        new Message("", "NOTICE", player.getName(), "You don't have that card.").send();
    }

    public void playerCantPlay(Player player) {
        new Message("", "NOTICE", player.getName(), "You can't play that card.").send();
    }

    public void playerWon(Player player, int score, Player[] players) {
        //flush longReply first
        target.reply(longReply);
        target.reply(Constants.BOLD + player.getName() + Constants.YELLOW + " has won Uno!!!");
        target.reply(Constants.BOLD + player.getName() + Constants.YELLOW + " got " + score + " points.");
        target.reply("This is everybody's hand: ");
        for (Player player1 : players) {
            ArrayList hand = player1.getHand();
            Iterator it = hand.iterator();
            String cards = "";
            while (it.hasNext()) {
                cards += getStringForCard((Card) it.next()) + ' ';
            }
            target.reply(NORMAL + Constants.BOLD + player1.getName() + Constants.BOLD + ": " + cards);
        }
        playing = false;
    }

    public void swapDrawAndDiscard() {
        new Message("", "ACTION", target.getChanname(), "turns over the discard pile and makes it the draw pile.").send();
    }

    public void playerDrewCard(Player player, Card card) {
        if (!playing)
            return;
        target.reply(Constants.BOLD + player.getName() + Constants.BOLD + " has drawn a card.");
        if (!player.isABot)
            new Message("", "NOTICE", player.getName(), NORMAL + "You drew: " + getStringForCard(card)).send();
    }

    public void playerPassed(Player player) {
        if (!playing)
            return;
        target.reply(Constants.BOLD + player.getName() + Constants.BOLD + " has passed.");
    }

    public void nextPlayer(Player player) {
        if (!playing)
            return;
        if (longReply == null)
            target.reply("It is now the turn of " + Constants.BOLD + player.getName() + Constants.BOLD + '.');
        else {
            target.reply(NORMAL + longReply + "It is now the turn of " + Constants.BOLD + player.getName() + Constants.BOLD + '.');
            longReply = "";
        }
        if (!player.isABot)
            hand(player);
    }

    public void showTopDiscardCard(Player player, Card card) {
        if (card.getType() == Card.WILD || card.getType() == Card.WDF) {
            card = game.colourCard;
        }
        new Message("", "NOTICE", player.getName(), NORMAL + getStringForCard(card) + " is up.").send();
    }

    public void showTopDiscardCardEverybody(Card card) {

        if (card.getType() == Card.WILD || card.getType() == Card.WDF) {
            card = game.colourCard;
        }
        target.reply(NORMAL + getStringForCard(card) + " is up.");
    }

    public void normalPlay(Player player, Card card) {
        if (!playing)
            return;
        if (longReply == null)
            longReply = Constants.BOLD + player.getName() + Constants.BOLD + " has played a " + getStringForCard(card) + ". ";
        else
            longReply = Constants.BOLD + player.getName() + Constants.BOLD + " has played a " + getStringForCard(card) + ". ";
    }

    public void playerSkipped(Player player) {
        if (!playing)
            return;
        if (longReply == null)
            longReply = Constants.BOLD + player.getName() + Constants.BOLD + " is skipped! ";
        else
            longReply += Constants.BOLD + player.getName() + Constants.BOLD + " is skipped! ";
    }

    public void playerReversed(Player player) {

    }

    public void drawnCards(Player player, Card[] cards) {
        String reply = "";
        if (longReply == null)
            longReply = Constants.BOLD + player.getName() + Constants.BOLD + " draws " + cards.length + " cards. ";
        else
            longReply += Constants.BOLD + player.getName() + Constants.BOLD + " draws " + cards.length + " cards. ";
        if(player.isABot)
            return;
        for (Card card : cards) reply += ' ' + getStringForCard(card);
        new Message("", "NOTICE", player.getName(), NORMAL + "You drew:" + reply).send();
    }

    public void chooseColour(Player player) {
        if (longReply != null) {
            target.reply(longReply);
            longReply = null;
        }
        if (!player.isABot)
            new Message("", "NOTICE", player.getName(), "Please choose a colour.").send();
    }

    public void chosenColour(Colour colour) {
        if (!playing)
            return;
        if (game.currentPlayer.isABot) {
            if (longReply == null)
                longReply = NORMAL + "The colour is now " + getStringForCard(colour) + ". ";
            else
                longReply += NORMAL + "The colour is now " + getStringForCard(colour) + ". ";
            return;
        }
        target.reply(NORMAL + "The colour is now " + getStringForCard(colour) + '.');
    }

    public void playerHasUno(Player player) {
        if (!playing)
            return;
        if (game.currentPlayer.isABot) {
            if (longReply == null)
                longReply = NORMAL + Constants.BOLD + player.getName() + Constants.BOLD + " has " + Constants.YELLOW + 'U' + Constants.BLUE + 'N' + Constants.RED + 'O' + NORMAL + "!! ";
            else
                longReply += NORMAL + Constants.BOLD + player.getName() + Constants.BOLD + " has " + Constants.YELLOW + 'U' + Constants.BLUE + 'N' + Constants.RED + 'O' + NORMAL + "!! ";
            return;
        }
        target.reply(NORMAL + Constants.BOLD + player.getName() + Constants.BOLD + " has " + Constants.YELLOW + 'U' + Constants.BLUE + 'N' + Constants.RED + 'O' + NORMAL + "!!");
    }

    public void order(Player[] allPlayers, int[] noCards) {
        String reply = "";
        for (int i = 0; i < allPlayers.length; i++) {
            reply += Constants.BOLD + allPlayers[i].getName() + Constants.BOLD + '(' + noCards[i] + ") ";
        }
        target.reply(NORMAL + reply);
    }

    private class GameStarter implements Runnable {
    	private Uno game;
    	GameStarter(Uno uno) {
    		game = uno;
    	}
    	public void run() {
    		game.target.reply("Waiting for other players to join..");
    		try {
    			Thread.sleep(20000);
    		} catch (InterruptedException e) {
    		}

    		game.target.reply(Constants.BOLD + "10 secs..");

    		try {
    			Thread.sleep(10000);
    		} catch (InterruptedException e) {
    		}
    		if (hasJoined) {
    			game.target.reply("Starting the game now!");
    			game.hasJoined = false;
    			game.playing = true;
    			game.waiting = false;
    		} else {
    			game.target.reply("Not enough players have joined to start the game :( Abandoning it..");
    			game.waiting = false;
    			game.playing = false;
    			game.hasJoined = false;
    		}
    	}
    }
}
