package goat.module;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: bc
 * Date: Apr 26, 2004
 * Time: 8:49:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class CompareScores implements Comparator {
    public int compare(Object o, Object o1) {
        String[] entry1 = (String[]) o;
        String[] entry2 = (String[]) o1;
        int score1 = Integer.parseInt(entry1[1]);
        int score2 = Integer.parseInt(entry2[1]);
        if(score1>score2)
            return -1;
        if(score1<score2)
            return 1;
        if(score1==score2)
            return 0;
        return 0;
    }
}
