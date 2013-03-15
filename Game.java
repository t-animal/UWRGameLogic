import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.TimerTask;

public class Game {
	
	private Date start = null;
	private long halfTimesDuration;
	private int halfTimesCount;
	private int currentHalfTime=0;
	private boolean stoppingTime;
	
	private String blue;
	private String white;
	private int blueScore=0;
	private int whiteScore=0;
	private int blueTeamLeader;
	private int whiteTeamLeader;
	
	private String uwRef1;
	private String uwRef2;
	private String gameLeader;
	private GameLog gameLog;
	
	private Date clockStoppedAt;
	private boolean clockIsStopped = true;
	private long timeWasStoppedFor=0;
	private TimerTask gameTimeTimerTask;

	private long penaltyThrowDuration=45000;
	private boolean penaltyThrowOnResume=false;
	private Date penaltyThrowStartedAt;
	private TimerTask penaltyThrowTimerTask;
	
	private LinkedList<TwoMinutePenaltyTimerTask> twoMinutePenaltyTimerTasks = new LinkedList<TwoMinutePenaltyTimerTask>();

	//assuming max 8 suspended players, 1 penalty throw, 1 time measurement => each needs its own thread in worst case!
	final private ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(10, new DaemonThreadFactory());

	private class DaemonThreadFactory implements ThreadFactory{
		public Thread newThread(Runnable r) { Thread t = new Thread(r); t.setDaemon(true); return t; }
	}
	
	private class TwoMinutePenaltyTimerTask extends TimerTask{
		int playerNumber;
		Game.Colors team;
		long startedAt;
		
		public TwoMinutePenaltyTimerTask(int playerNumber, Game.Colors team, long startedAt){
			super();
			
			this.playerNumber = playerNumber;
			this.team = team;
			this.startedAt = startedAt;
		}
		
		private long getRestTimeMillis(){
			return getCurrentGameTimeMillis()-startedAt;
		}
		
		public int getRestTime(){
			return (int) getRestTimeMillis()/1000;
		}
		
		public void allowPlayerBackIn(){
			//TODO: Spieler wieder reinstellen
			System.out.println("Spieler "+playerNumber+" von Team "+team+" darf wieder mitspielen");
			twoMinutePenaltyTimerTasks.remove(this);
		}
		
		public void run(){
			long restTime = getRestTimeMillis();
			if(restTime<=0){
				allowPlayerBackIn();
			}else{
				timer.schedule(this, restTime, TimeUnit.MILLISECONDS);
				System.out.println("Player "+playerNumber+" of team "+team+" would be allowed in, but game has been stopped. Rechecking in "+restTime);
			}
		}
	}
	
	
	public enum GameAction {
		UNDEFINED, GOAL, FREE_THROW, WARNING, TEAM_WARNING, TWO_MINUTES, PENALTY_THROW, EXCHANGE
	}
	public enum Colors {
		UNDEFINED, BLUE, WHITE
	}
	
	
	/*
	 * Constructor and overrides start here 
	 */
	public Game(String blue, String white, String uwRef1, String uwRef2, String gameLeader, 
					int halfTimesDuration, int halfTimesCount, boolean stoppingTime){
		this.blue = blue;
		this.white = white;
		this.uwRef1 = uwRef1;
		this.uwRef2 = uwRef2;
		this.gameLeader = gameLeader;
		
		this.halfTimesDuration = halfTimesDuration*1000*60;
		this.halfTimesCount = halfTimesCount;
		this.stoppingTime = stoppingTime;
		
		this.gameLog = new GameLog();
	}
	
	public String toString(){
		int curTime=getCurrentGameTime();
		return blue+" vs "+white+": "+blueScore+" - "+whiteScore+" The game is currently "+(isStopped()?"stopped":"running")+
					" at "+(curTime/60)+"m"+(curTime%60)+"s in halftime #"+currentHalfTime+".";
	}
	
	
	/*
	 * Game time related stuff from here on
	 */
	public void start(){
		if(currentHalfTime == halfTimesCount){
			System.out.println("Spiel zu Ende - alle Halbzeiten gespielt.");
			return;
		}
		start = new Date();
		currentHalfTime++;
		
		gameTimeTimerTask = new TimerTask(){
			public void run(){
				boolean penaltyThrowRunning = penaltyThrowRunning();
				if(!penaltyThrowRunning && getCurrentGameTimeMillis()>=halfTimesDuration){
					//TODO abhupen
					start = null;
					timeWasStoppedFor = 0;
					unsuspendAllPlayers();
					System.out.println(new Date()+" Halbzeit zu ende! ");
				}else if(penaltyThrowRunning){
					timer.schedule(this, penaltyThrowDuration-getCurrentPenaltyThrowTimeMillis(), TimeUnit.MILLISECONDS);
					System.out.println(new Date()+" Halbzeit zu ende, aber Strafwurf laeuft noch! => Warte "+penaltyThrowDuration+"-"+getCurrentPenaltyThrowTimeMillis());
				}else{
					timer.schedule(this, halfTimesDuration-getCurrentGameTimeMillis(), TimeUnit.MILLISECONDS);
					System.out.println(new Date()+" Halbzeit zu ende, aber Zeit war "+timeWasStoppedFor+"ms angehalten!");
				}
			}
		};
		
		timer.schedule(gameTimeTimerTask, halfTimesDuration, TimeUnit.MILLISECONDS);
	}
	
	public boolean isStarted(){
		return start != null;
	}

	public void reset(){
		start = null;
		currentHalfTime = 0;
		gameTimeTimerTask.cancel();
		penaltyThrowTimerTask.cancel();
		unsuspendAllPlayers();
		//todo clear log
	}
	
	public void stopClock(){
		clockStoppedAt = new Date();
	}
	
	public boolean isStopped(){
		return start != null && clockStoppedAt != null;
	}
	
	public void resumeClock(){
		timeWasStoppedFor += new Date().getTime()-clockStoppedAt.getTime();
		clockStoppedAt = null;
	}

	public int getCurrentGameTime(){
		return (int) getCurrentGameTimeMillis()/1000;
	}
	
	private long getCurrentGameTimeMillis(){
		if(isStarted()){
			if(isStopped()){
				return clockStoppedAt.getTime()-start.getTime()-timeWasStoppedFor;
			}else{
				return new Date().getTime()-start.getTime()-timeWasStoppedFor;
			}
		}
		
		return 0;
	}

	
	/*
	 * Interaktion with referee from here on
	 */
	public void refereeInteraktion(){
		if(!isStarted()){
			start();
			return;
		}
		
		gameLog.createNewLogEntry(getCurrentGameTime());
		
		if(stoppingTime){
			if(!isStopped()){
				stopClock();
			}else{
				resumeClock();
			}
		}
		
		if(penaltyThrowRunning()){
			cancelPenaltyThrow();
		}
		
		if(penaltyThrowOnResume){
			startPenaltyThrow();
		}
	}
	
	public void decision(Game.Colors team,  int player, Game.GameAction incident, String comment){
		gameLog.logNewestIncident(team, player, incident, comment);
		switch(incident){
		case PENALTY_THROW:
			if(!isStopped()){
				System.out.println("Warning: Free throw given AFTER game was started.");
				stopClock();
				startWithPenaltyThrow();
				resumeClock();
			}
			startWithPenaltyThrow();
			break;
		case TWO_MINUTES:
			suspendPlayer(player, team);
			break;
		case GOAL:
			if(team == Colors.WHITE){
				whiteScore++;
				unsuspendPlayer(Colors.BLUE);
			}else{
				blueScore++;
				unsuspendPlayer(Colors.WHITE);
			}
		}
	}
	
	
	/*
	 * Penalty throw related stuff from here on
	 */
	
	public void startPenaltyThrow(){
		penaltyThrowOnResume = false;
		penaltyThrowStartedAt = new Date();
		penaltyThrowTimerTask = new TimerTask(){
			public void run(){
				//TODO: Abhupen
				System.out.println(new Date()+" Strafwurf zu ende!");
				cancelPenaltyThrow();
			}
		};
		timer.schedule(penaltyThrowTimerTask, penaltyThrowDuration, TimeUnit.MILLISECONDS);
	}
	
	public void cancelPenaltyThrow(){
		penaltyThrowStartedAt = null;
		penaltyThrowTimerTask.cancel();
	}
	

	public void startWithPenaltyThrow(){
		penaltyThrowOnResume = true;
	}
	
	public boolean penaltyThrowRunning(){
		return penaltyThrowStartedAt != null;
	}
	
	private long getCurrentPenaltyThrowTimeMillis(){
		if(!penaltyThrowRunning())
			return penaltyThrowDuration;
		return new Date().getTime()-penaltyThrowStartedAt.getTime();
	}
	
	
	/*
	 * 2 minute suspension related stuff from here on
	 */
	
	public void suspendPlayer(int playerNumber, Game.Colors team){
		TwoMinutePenaltyTimerTask task = new TwoMinutePenaltyTimerTask(playerNumber, team, getCurrentGameTimeMillis());
		twoMinutePenaltyTimerTasks.add(task);
		timer.schedule(task, 2*60*1000, TimeUnit.MILLISECONDS);
	}
	
	public void unsuspendPlayer(Game.Colors team){		
		int i=-1;
		try{
			while(twoMinutePenaltyTimerTasks.get(++i).team!=team);
		}catch(IndexOutOfBoundsException e){
			return;
		}
		
		TwoMinutePenaltyTimerTask task = twoMinutePenaltyTimerTasks.remove(i);
		task.cancel();
		task.allowPlayerBackIn();
	}
	
	private void unsuspendAllPlayers(){
		for(TwoMinutePenaltyTimerTask task: twoMinutePenaltyTimerTasks){
			task.cancel();
			task.allowPlayerBackIn();
		}
		twoMinutePenaltyTimerTasks.clear();
	}
	
	
	/*
	 * Some basic testing from here on
	 */
	
	public static void main(String[] args) throws IOException{
		
		System.out.println(new Date()+" Starting a test game of 2 times 3 Minute with stopping time.");
		System.out.println(new Date()+" Honk via <ENTER>.");
		System.out.println(new Date()+" Decisions via:  <P(B|W)> Penalty throw against B or W.");
		System.out.println(new Date()+"                 <T(B|W)#> two Minutes against B or W, # is playerno (one digit).");
		System.out.println(new Date()+"                 <G(B|W)#> Goal for B or W, # is scorerno (one digit)");
		System.out.println(new Date()+"                 <X(B|W)##> exchange in Team B or W. First # is playerno in, second is out.\n");
		
		Game g = new Game("Bamberg1", "Bamberg2", "foo", "bar", "bla", 3, 2, true);
		
		System.out.println("Start: "+g);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String s;
	    while ((s = in.readLine()) != null){
	    	if(s.length() == 0){
	    		g.refereeInteraktion();
	    		System.out.println(new Date()+" Honking. Game state: "+g);
	    		
	    	}else
	    	try{
		    	switch(s.charAt(0)){
		    	case 'P':
		    		g.decision(s.charAt(1)=='W'?Colors.WHITE:Colors.BLUE, -1, GameAction.PENALTY_THROW, "");
		    		break;
		    	case 'T':
		    		g.decision(s.charAt(1)=='W'?Colors.WHITE:Colors.BLUE, s.charAt(2)-'0', GameAction.TWO_MINUTES, "");
		    		break;
		    	case 'G':
		    		g.decision(s.charAt(1)=='W'?Colors.WHITE:Colors.BLUE, s.charAt(2)-'0', GameAction.GOAL, "");
		    		break;
		    	case 'X':
		    		g.gameLog.logExchange(g.getCurrentGameTime(),s.charAt(1)=='W'?Colors.WHITE:Colors.BLUE, s.charAt(2)-'0', s.charAt(3)-'0', "");
		    	}	
	    	}catch(IndexOutOfBoundsException e){
		    	System.out.println("Please honk before filing a decision!");
		    	//TODO: Might fuck up log. =>CHeck there 
		    }
	    }
	}
}
