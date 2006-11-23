/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.module;

import goat.uno.*;
import goat.uno.cards.*;
import goat.core.Module;
import goat.core.Message;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Handles IO between Goat and Uno
 *
 * @author bc
 */
public class Uno extends Module implements Output, Runnable {
    boolean playing; //true if playing a game
    boolean waiting; //true if a game has been started but only one player has joined it
    boolean hasJoined; //signifies whether it is safe to start the game
    Game game;
    Message target;        //the target channel, effectively. The message that started the game
    String longReply;            //Reply string for multiple events
    private static final String[] SPACES = new String[20];    //spaces for formatting purposes
    private static final String BG = Message.COLCODE + ",1";  //bg colour
    private static final String NORMAL = Message.WHITE + BG; //reset to normal + BG

    static {
        for (int i = 0; i < 20; i++) {
            SPACES[i] = " ";
            for (int j = 0; j < i - 1; j++) {
                SPACES[i] += " ";
            }
        }
    }

    public void processChannelMessage(Message m) {
        m.modCommand = m.modCommand.toLowerCase();
        if (waiting || playing)
            if (m.modCommand.equals("botjoin")) {
                String[] botNames = m.modTrailing.split(" ");
                for (int i = 0; i < botNames.length; i++) { 
                    if (getPlayer(botNames[i]) == null) {
                        if (game.players.size() > 10) {
                            m.createReply("Fuck off, we're full. ").send();
                            return;
                        }
						if(botNames[i].trim().length() == 0) 
							continue;
                        game.join(botNames[i], true);
                        hasJoined = true;
                    }
                }
            }
        if (!playing) {
            if (m.modCommand.equals("uno") && !waiting) {
                target = m;
                game = new Game(this, m.sender);
                longReply = "";
                Thread t = new Thread(this);
                t.start();
                waiting = true;
            }
            if (waiting)
                if (m.modCommand.equals("join"))
                    if (getPlayer(m.sender) == null) {
                        game.join(m.sender, false);
                        hasJoined = true;
                    }
        } else {
            if (!game.needColour) {
                if (m.modCommand.equals("play") || m.modCommand.equals("p")) {
                    try {
                        Player player = getPlayer(m.sender);
                        if (player != null) {
                            Card card = getCard(m.modTrailing, player);
                            if (card != null) {
                                game.play(player, card);
                            }
                        }
                    } catch (InvalidCardColourException e) {
                        e.printStackTrace();
                    } catch (InvalidCardNumberException e) {
                        e.printStackTrace();
                    }
                } else if (m.modCommand.equals("draw") || m.modCommand.equals("<")) {
                    Player player = getPlayer(m.sender);
                    if (player != null)
                        game.draw(player);
                } else if (m.modCommand.equals("pass") || m.modCommand.equals(">")) {
                    Player player = getPlayer(m.sender);
                    if (player != null)
                        game.pass(player);
                }
            } else {
                if (m.modCommand.equals("red") || m.modCommand.equals("r"))
                    game.setColour(getPlayer(m.sender), Card.RED);
                else if (m.modCommand.equals("blue") || m.modCommand.equals("b"))
                    game.setColour(getPlayer(m.sender), Card.BLUE);
                else if (m.modCommand.equals("green") || m.modCommand.equals("g"))
                    game.setColour(getPlayer(m.sender), Card.GREEN);
                else if (m.modCommand.equals("red") || m.modCommand.equals("r"))
                    game.setColour(getPlayer(m.sender), Card.RED);
                else if (m.modCommand.equals("yellow") || m.modCommand.equals("y"))
                    game.setColour(getPlayer(m.sender), Card.YELLOW);
            }
            if (m.modCommand.equals("join")) {
                if (getPlayer(m.sender) == null)
                    game.join(m.sender, false);
            } else if (m.modCommand.equals("hand") || m.modCommand.equals("h")) {
                Player player = getPlayer(m.sender);
                if (player != null)
                    game.hand(player);
            } else if (m.modCommand.equals("show") || m.modCommand.equals("s")) {
                game.show();
            } else if (m.modCommand.equals("stopgame")) {
                m.createReply(Message.BOLD + "Uno" + Message.BOLD + " has been stopped.").send();
                playing = false;
            } else if (m.modCommand.equals("order")) {
                game.order();
            }
        }

        if (m.modCommand.equals("unoscore")) {
            ArrayList records = Scores.getRecords();
            if (records.size() == 0) {
                m.createReply("Nobody's got any scores yet :(").send();
                return;
            }
            Iterator it = records.iterator();
            int lScore = 0, lHScore = 0, lgW = 0, lgE = 0, lNick = 0;
            int i=0;
            while (it.hasNext()) {
                i++;
                if(i>20)
                    break;
                Record record = (Record) it.next();
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
            m.createReply("   " + Message.UNDERLINE + "Name" + SPACES[lNick + 3 - 4]
                            + "HiScore" + SPACES[lHScore + 7 - 7]
                            + "Won" + SPACES[lgW + 3 - 3]
                            + "Games" + SPACES[lgE + 5 - 5]
                            + "Ratio  "
                            + "TotalScore").send();

            it = records.iterator();
            int count = 0;
            while (it.hasNext()) {
                if(count>19) break;
                Record record = (Record) it.next();
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
                m.createReply(Message.BOLD + count + Message.BOLD + SPACES[3 - Integer.toString(count).length()] + record.getName()
                                + SPACES[lNick + 3 - record.getName().length()]
                                + record.getHiScore() + SPACES[lHScore + 7 - Integer.toString(record.getHiScore()).length()]
                                + record.getGamesWon() + SPACES[lgW + 3 - Integer.toString(record.getGamesWon()).length()]
                                + record.getGamesEntered() + SPACES[lgE + 5 - Integer.toString(record.getGamesEntered()).length()]
                                + ratio + "  "
                                + record.getTotalScore()).send();
            }

        }

    }

    public void processPrivateMessage(Message m) {

    }

    public int messageType() {
        return WANT_COMMAND_MESSAGES;
    }

    public static String[] getCommands() {
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
                        return Message.RED + "Red " + card.getNumber() + NORMAL;
                    case Card.BLUE:
                        return Message.BLUE + "Blue " + card.getNumber() + NORMAL;
                    case Card.YELLOW:
                        return Message.YELLOW + "Yellow " + card.getNumber() + NORMAL;
                    case Card.GREEN:
                        return Message.GREEN + "Green " + card.getNumber() + NORMAL;
                }
            case Card.REVERSE:
                switch (card.getColour()) {
                    case Card.RED:
                        return Message.RED + "Red Reverse" + NORMAL;
                    case Card.BLUE:
                        return Message.BLUE + "Blue Reverse" + NORMAL;
                    case Card.YELLOW:
                        return Message.YELLOW + "Yellow Reverse" + NORMAL;
                    case Card.GREEN:
                        return Message.GREEN + "Green Reverse" + NORMAL;
                }
            case Card.SKIP:
                switch (card.getColour()) {
                    case Card.RED:
                        return Message.RED + "Red Skip" + NORMAL;
                    case Card.BLUE:
                        return Message.BLUE + "Blue Skip" + NORMAL;
                    case Card.YELLOW:
                        return Message.YELLOW + "Yellow Skip" + NORMAL;
                    case Card.GREEN:
                        return Message.GREEN + "Green Skip" + NORMAL;
                }
            case Card.DRAW:
                switch (card.getColour()) {
                    case Card.RED:
                        return Message.RED + "Red Draw Two" + NORMAL;
                    case Card.BLUE:
                        return Message.BLUE + "Blue Draw Two" + NORMAL;
                    case Card.YELLOW:
                        return Message.YELLOW + "Yellow Draw Two" + NORMAL;
                    case Card.GREEN:
                        return Message.GREEN + "Green Draw Two" + NORMAL;
                }
            case Card.FAKE:
                switch (card.getColour()) {
                    case Card.RED:
                        return Message.RED + "Red" + NORMAL;
                    case Card.BLUE:
                        return Message.BLUE + "Blue" + NORMAL;
                    case Card.YELLOW:
                        return Message.YELLOW + "Yellow" + NORMAL;
                    case Card.GREEN:
                        return Message.GREEN + "Green" + NORMAL;
                }
            case Card.WILD:
                return Message.YELLOW + 'W' + Message.BLUE + 'i' + Message.RED + 'l' + Message.GREEN + 'd' + NORMAL;
            case Card.WDF:
                return Message.YELLOW + 'W' + Message.BLUE + 'i' + Message.RED + 'l' + Message.GREEN + "d "
                        + Message.BLUE + 'D' + Message.YELLOW + 'r' + Message.GREEN + 'a' + Message.RED + "w "
                        + Message.GREEN + 'F' + Message.RED + 'o' + Message.BLUE + 'u' + Message.YELLOW + 'r' + NORMAL;
        }
        return null;
    }

    public void gameInit() {
        target.createReply(Message.BOLD + "Uno" + Message.BOLD + " has started!").send();
    }

    public void playerStart(Player player) {
        target.createReply(Message.BOLD + player.getName() + Message.BOLD + " is ready to play uno!").send();
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
        target.createReply(longReply).send();
        target.createReply(Message.BOLD + player.getName() + Message.YELLOW + " has won Uno!!!").send();
        target.createReply(Message.BOLD + player.getName() + Message.YELLOW + " got " + score + " points.").send();
        target.createReply("This is everybody's hand: ").send();
        for (int i = 0; i < players.length; i++) {
            ArrayList hand = players[i].getHand();
            Iterator it = hand.iterator();
            String cards = "";
            while (it.hasNext()) {
                cards += getStringForCard((Card) it.next()) + ' ';
            }
            target.createReply(NORMAL + Message.BOLD + players[i].getName() + Message.BOLD + ": " + cards).send();
        }
        playing = false;
    }

    public void swapDrawAndDiscard() {
        new Message("", "ACTION", target.channame, "turns over the discard pile and makes it the draw pile.").send();
    }

    public void playerDrewCard(Player player, Card card) {
        if (!playing)
            return;
        target.createReply(Message.BOLD + player.getName() + Message.BOLD + " has drawn a card.").send();
        if (!player.isABot)
            new Message("", "NOTICE", player.getName(), NORMAL + "You drew: " + getStringForCard(card)).send();
    }

    public void playerPassed(Player player) {
        if (!playing)
            return;
        target.createReply(Message.BOLD + player.getName() + Message.BOLD + " has passed.").send();
    }

    public void nextPlayer(Player player) {
        if (!playing)
            return;
        if (longReply == null)
            target.createReply("It is now the turn of " + Message.BOLD + player.getName() + Message.BOLD + '.').send();
        else {
            target.createReply(NORMAL + longReply + "It is now the turn of " + Message.BOLD + player.getName() + Message.BOLD + '.').send();
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
        target.createReply(NORMAL + getStringForCard(card) + " is up.").send();
    }

    public void normalPlay(Player player, Card card) {
        if (!playing)
            return;
        if (longReply == null)
            longReply = Message.BOLD + player.getName() + Message.BOLD + " has played a " + getStringForCard(card) + ". ";
        else
            longReply = Message.BOLD + player.getName() + Message.BOLD + " has played a " + getStringForCard(card) + ". ";
    }

    public void playerSkipped(Player player) {
        if (!playing)
            return;
        if (longReply == null)
            longReply = Message.BOLD + player.getName() + Message.BOLD + " is skipped! ";
        else
            longReply += Message.BOLD + player.getName() + Message.BOLD + " is skipped! ";
    }

    public void playerReversed(Player player) {

    }

    public void drawnCards(Player player, Card[] cards) {
        String reply = "";
        if (longReply == null)
            longReply = Message.BOLD + player.getName() + Message.BOLD + " draws " + cards.length + " cards. ";
        else
            longReply += Message.BOLD + player.getName() + Message.BOLD + " draws " + cards.length + " cards. ";
        if(player.isABot)
            return;
        for (int i = 0; i < cards.length; i++)
            reply += ' ' + getStringForCard(cards[i]);
        new Message("", "NOTICE", player.getName(), NORMAL + "You drew:" + reply).send();
    }

    public void chooseColour(Player player) {
        if (longReply != null) {
            target.createReply(longReply).send();
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
        target.createReply(NORMAL + "The colour is now " + getStringForCard(colour) + '.').send();
    }

    public void playerHasUno(Player player) {
        if (!playing)
            return;
        if (game.currentPlayer.isABot) {
            if (longReply == null)
                longReply = NORMAL + Message.BOLD + player.getName() + Message.BOLD + " has " + Message.YELLOW + 'U' + Message.BLUE + 'N' + Message.RED + 'O' + NORMAL + "!! ";
            else
                longReply += NORMAL + Message.BOLD + player.getName() + Message.BOLD + " has " + Message.YELLOW + 'U' + Message.BLUE + 'N' + Message.RED + 'O' + NORMAL + "!! ";
            return;
        }
        target.createReply(NORMAL + Message.BOLD + player.getName() + Message.BOLD + " has " + Message.YELLOW + 'U' + Message.BLUE + 'N' + Message.RED + 'O' + NORMAL + "!!").send();
    }

    public void order(Player[] allPlayers, int[] noCards) {
        String reply = "";
        for (int i = 0; i < allPlayers.length; i++) {
            reply += Message.BOLD + allPlayers[i].getName() + Message.BOLD + '(' + noCards[i] + ") ";
        }
        target.createReply(NORMAL + reply).send();
    }

    public void run() {
        target.createReply("Waiting for other players to join..").send();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
        }

        target.createReply(Message.BOLD + "10 secs..").send();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
        if (hasJoined) {
            target.createReply("Starting the game now!").send();
            hasJoined = false;
            playing = true;
            waiting = false;
        } else {
            target.createReply("Not enough players have joined to start the game :( Abandoning it..").send();
            waiting = false;
            playing = false;
            hasJoined = false;
        }
    }
}
