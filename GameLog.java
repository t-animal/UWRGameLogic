import java.util.ArrayList;
import java.util.Date;

import Game.GameAction;

//TODO: GameLogEntry-Klasse fuer sortierbare Liste!
public class GameLog {
	private ArrayList<Long> logTime = new ArrayList<Long>();
	private ArrayList<Game.Colors> logTeam = new ArrayList<Game.Colors>(); 
	private ArrayList<Integer> logPlayer = new ArrayList<Integer>(); 
	private ArrayList<Game.GameAction> logAction = new ArrayList<Game.GameAction>();
	private ArrayList<String> logComment = new ArrayList<String>();
	private ArrayList<Integer[]> exchanges = new ArrayList<Integer[]>();
	
	public GameLog(){
		
	}
	
	public int createNewLogEntry(long gameTime){
		logTime.add(gameTime);
		logTeam.add(Game.Colors.UNDEFINED);
		logPlayer.add(-1);
		logAction.add(Game.GameAction.UNDEFINED);
		logComment.add("");
		
		return logTime.size();
	}
	
	public void logNewestIncident(Game.Colors team,  int player, Game.GameAction incident, String comment){
		logIncident(logTime.size()-1, team, player, incident, comment);
	}
	
	public void logIncident(int logNumber, Game.Colors team, int player, Game.GameAction incident, String comment){
		logTeam.set(logNumber, team);
		logPlayer.set(logNumber, player);
		logAction.set(logNumber, incident);
		logComment.set(logNumber, comment);
	}
	
	public Game.GameAction logExchange(long gameTime, Game.Colors team, int playerIn, int playerOut, String comment){
		logTime.add(gameTime);
		logTeam.add(team);
		logPlayer.add(exchanges.size());
		logAction.add(Game.GameAction.EXCHANGE);
		Integer[] players = {playerIn, playerOut};
		exchanges.add(players);
	}
}
