package goat.util;

import java.io.IOException;
import java.util.List;



public class ScoresWithMatches extends Scores {

	private Scores channelMatchScores;
	private Scores globalMatchScores;
	//private String[] previousMatchWinner;
	
	
	public ScoresWithMatches(String gameName, String channelName) 
	throws IOException {
		super(gameName, channelName + ".scores");
		channelMatchScores = new Scores(gameName, channelName + ".matchScores");
		globalMatchScores = new Scores(gameName, "matchScores");		
	}
	
	public void endMatch() {
			if(size() == 0)
				return; // endMatch() called before there were any scores
			//System.out.println("endMatch(): round scores not empty");
			String[] winnar = copyOfHighScoreEntry();
			//System.out.println("endMatch(): got copy of High Score entry");
	
			//if(winnar.length < HIGHEST_SCORE + 1) {
			//	System.out.println("Winnar entry is too short...");
			//}
			
			String[] matchWinnar = new String[3];
			//System.out.println("endMatch(): Created new array");
			matchWinnar[NAME] = winnar[NAME];
			//System.out.println("endMatch(): NAME set");
			matchWinnar[HIGHEST_SCORE] = winnar[TOTAL_SCORE];
			//System.out.println("endMatch(): HIGHEST_SCORE set");
			matchWinnar[TOTAL_SCORE] = Integer.toString(1);
			//System.out.println("endMatch(): TOTAL_SCORE set");
			
			//System.out.println("endMatch(): winnar entry fiddled");
			
			//System.out.println("endMatch(): Adding score to channelMatchScores");
			channelMatchScores.add(matchWinnar);
			//System.out.println("endMatch(): Adding score to globalMatchScores");
			globalMatchScores.add(matchWinnar);
			
			//System.out.println("endMatch(): clearing scores");
			clear();
			//previousMatchWinner = matchWinnar;
	}
	
	public List<String> matchScoreTable(int limit) {
		return channelMatchScores.matchScoreTable(limit);
	}
}