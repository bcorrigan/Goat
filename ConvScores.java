import java.util.*;
import java.io.*;


public class ConvScores {
    public static void main(String[] args) throws Exception {
        ArrayList scores = new ArrayList();
        FileInputStream in = new FileInputStream("scores");
        ObjectInputStream s = new ObjectInputStream(in);
        scores = (ArrayList) s.readObject();
        ArrayList newScores = new ArrayList();
        in.close();
        Iterator it = scores.iterator();
        while(it.hasNext()) {
            String[] entry = (String[]) it.next();
            entry[1]=Integer.toString((int) (Double.parseDouble(entry[1])));
            entry[2]=Integer.toString((int) (Double.parseDouble(entry[2])));
            newScores.add(entry);
        }
        FileOutputStream out = new FileOutputStream("scoresInt");
        ObjectOutputStream t = new ObjectOutputStream(out);
        t.writeObject(newScores);
        t.flush();
        t.close();
        out.flush();
        out.close();
        System.out.println("Done!");
    }
}
