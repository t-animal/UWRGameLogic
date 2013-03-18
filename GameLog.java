import java.util.ArrayList;
import java.util.Date;


public class GameLog {

	public class Entry implements Comparable<Entry> {
		//TODO:Halbzeiten
		Integer logTime;
		Game.Colors logTeam;
		Integer logPlayer;
		Game.GameAction logAction;
		String logComment;
		int exchangeIn;
		int exchangeOut;

		/*
		 * For any incident except exchanges
		 */
		public Entry(Integer logTime, Game.Colors logTeam, Integer logPlayer,
				Game.GameAction logAction, String logComment) {
			this.logTime = logTime;
			this.logTeam = logTeam;
			this.logPlayer = logPlayer;
			this.logAction = logAction;
			this.logComment = logComment;
		}

		/*
		 * For exchanges
		 */
		public Entry(Integer logTime, Game.Colors logTeam, int exchangeIn,
				int exchangeOut) {
			this.logTime = logTime;
			this.logTeam = logTeam;
			this.exchangeIn = exchangeIn;
			this.exchangeOut = exchangeOut;
		}

		/*
		 * For when the incident cannot yet be determined
		 */
		public Entry(Integer logTime) {
			this.logTime = logTime;
			this.logTeam = Game.Colors.UNDEFINED;
			this.logAction = Game.GameAction.UNDEFINED;
		}

		@Override
		public int compareTo(Entry o) {
			return logTime.compareTo(o.logTime);

		}

		public String toString() {
			if (logAction != Game.GameAction.EXCHANGE) {
				return logTime + ": " + logAction + " fuer/von Spieler Nummer "
						+ logPlayer + " vom Team " + logTeam + "; "
						+ logComment;
			} else {
				return logTime + ": Wechsel bei Team " + logTeam + ": Spieler "
						+ exchangeIn + " wechselt ein fuer Spieler "
						+ exchangeOut;
			}
		}

	}

	ArrayList<Entry> logList = new ArrayList<Entry>();

	private int insertNewLogEntry(Entry newEntry) {
		// TODO: Eventuell sortierung hier?
		logList.add(newEntry);
		System.err.println(new Date()+" new log entry created");
		return logList.size();
	}

	public int createNewLogEntry(int gameTime) {
		return insertNewLogEntry(new Entry(gameTime));

	}

	public int createNewLogEntry(int gameTime, Game.Colors team, int player,
			Game.GameAction incident, String comment) {
		return insertNewLogEntry(new Entry(gameTime, team, player, incident,
				comment));
	}

	public void logNewestIncident(Game.Colors team, int player,
			Game.GameAction incident, String comment) {
		logIncident(logList.size() - 1, team, player, incident, comment);
	}

	public void logIncident(int logNumber, Game.Colors team, int player,
			Game.GameAction incident, String comment) {
		Entry e = logList.get(logNumber);
		e.logTeam = team;
		e.logPlayer = player;
		e.logAction = incident;
		e.logComment = comment;
	}

	public int createNewExchange(int gameTime, Game.Colors team, int playerIn,
			int playerOut, String comment) {
		return insertNewLogEntry(new Entry(gameTime, team, playerIn, playerOut));
	}
	
	public void deleteNewestIncident(){
		logList.remove(logList.size()-1);
	}
	
	public void deleteIncident(int logNumber){
		logList.remove(logNumber);
	}
	
	public void printToConsole(){
		for(Entry e: logList){
			System.out.println(e.toString());
		}
	}
}
