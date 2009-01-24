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
public class Scores implements Comparator<Record> {
    ArrayList<Record> records;

    public Scores() {
        try {
            XMLDecoder XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/unoscores.xml")));
            records = (ArrayList<Record>) XMLdec.readObject();
            XMLdec.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
			System.out.println("Most likely your resources/unoscores.xml file has become corrupted - have a look at it please.");
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public void commit(Player winningPlayer, int score, Player[] players) {
        boolean winMatch = false;
        for (Player player : players) {
            Iterator<Record> it = records.iterator();
            boolean match = false;
            while (it.hasNext()) {
                Record record = (Record) it.next();
                if (!player.isABot)
                    if (record.getName().equals(player.getName())) { //we already have a record of this player
                        record.setGamesEntered(record.getGamesEntered() + 1);
                        match = true;
                    }
                if (!winningPlayer.isABot)
                    if (record.getName().equals(winningPlayer.getName()) && !winMatch) {
                        record.setGamesEntered(record.getGamesEntered() + 1);
                        record.setGamesWon(record.getGamesWon() + 1);
                        record.setTotalScore(record.getTotalScore() + score);
                        if (score > record.getHiScore())
                            record.setHiScore(score);
                        winMatch = true;
                    }
            }
            if (!player.isABot)
                if (!match) {
                    Record newRecord = new Record();
                    newRecord.setName(player.getName());
                    newRecord.setGamesEntered(1);

                    records.add(newRecord);
                }
        }
        if(!winningPlayer.isABot)
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

    public int compare(Record record1, Record record2) {
        if (record1.getTotalScore() > record2.getTotalScore())
            return -1;
        if (record1.getTotalScore() < record2.getTotalScore())
            return 1;
        if (record1.getTotalScore() == record2.getTotalScore())
            return 0;
        return 0;
    }

    public static ArrayList<Record> getRecords() {

        try {
            XMLDecoder XMLdec = new XMLDecoder(new BufferedInputStream(new FileInputStream("resources/unoscores.xml")));
            ArrayList<Record> records = (ArrayList<Record>) XMLdec.readObject();
            return records;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
        }
        return null;
    }
}
