import java.util.*;
import java.io.*;

public class ScoreList {
    public static void main(String[] args) throws Exception {
        ArrayList scores = new ArrayList();
	    FileInputStream in = new FileInputStream("scores");
	    ObjectInputStream s = new ObjectInputStream(in);
	    scores = (ArrayList) s.readObject();
	    in.close();
        System.out.println("Name    HighScore    TotalScore");
        for(int i=0;i<scores.size();i++) {
            String[] entry = (String[]) scores.get(i);
            System.out.println(entry[0] + "    " + entry[2] + "    " + entry[1]);
        }
    }
}																   
