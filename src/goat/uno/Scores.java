/*
 * Copyright (c) 2004 Barry Corrigan. All Rights Reserved.
 */
package goat.uno;

import java.util.*;
import java.io.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

/**
 * Stores scoretable.
 * <p/>
 * Fields: name, win rate, games won, hiscore, total score
 *
 * @author bc
 */
public class Scores implements Comparator {
    ArrayList records;

    public Scores() {
        try {
            XMLDecoder XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/unoscores.xml")));
            records = (ArrayList) XMLdec.readObject();
            XMLdec.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public void commit(Player winningPlayer, int score, Player[] players) {
        boolean winMatch = false;
        for (int i = 0; i < players.length; i++) {
            Iterator it = records.iterator();
            boolean match = false;

            while (it.hasNext()) {                    //this is all completely wrong!!
                Record record = (Record) it.next();
                if (record.getName().equals(players[i].getName())) { //we already have a record of this player
                    record.setGamesEntered(record.getGamesEntered() + 1);
                    match = true;
                }
                if (record.getName().equals(winningPlayer.getName()) && !winMatch) {
                    record.setGamesEntered(record.getGamesEntered() + 1);
                    record.setGamesWon(record.getGamesWon() + 1);
                    record.setTotalScore(record.getTotalScore() + score);
                    if (score > record.getHiScore())
                        record.setHiScore(score);
                    winMatch = true;
                }
            }
            if (!match) {
                Record newRecord = new Record();
                newRecord.setName(players[i].getName());
                newRecord.setGamesEntered(1);

                records.add(newRecord);
            }
        }
        if (!winMatch) {
            Record newRecord = new Record();
            newRecord.setName(winningPlayer.getName());
            newRecord.setGamesEntered(1);
            newRecord.setGamesWon(1);
            newRecord.setTotalScore(score);
            newRecord.setHiScore(score);
            records.add(newRecord);
        }
        //now zap out the records to a file
        Collections.sort(records, this);
        try {
            XMLEncoder XMLenc = new XMLEncoder(new BufferedOutputStream(new FileOutputStream("resources/unoscores.xml")));
            XMLenc.writeObject(records);
            XMLenc.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }

    }

    public int compare(Object o, Object o1) {
        Record record1 = (Record) o;
        Record record2 = (Record) o1;
        if (record1.getTotalScore() > record2.getTotalScore())
            return -1;
        if (record1.getTotalScore() < record2.getTotalScore())
            return 1;
        if (record1.getTotalScore() == record2.getTotalScore())
            return 0;
        return 0;
    }

    public static ArrayList getRecords() {

        try {
            XMLDecoder XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/unoscores.xml")));
            ArrayList records = (ArrayList) XMLdec.readObject();
            return records;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
