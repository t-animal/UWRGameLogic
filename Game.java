import java.util.LinkedList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Game {
	
	private Date start;
	private long halfTimesDuration;
	private int halfTimesCount;
	private int currentHalfTime=0;
	private boolean stoppingTime;
	
	private String blue;
	private String white;
	private int blueTeamLeader;
	private int whiteTeamLeader;
	
	private String uwRef1;
	private String uwRef2;
	private String gameLeader;
	private GameLog gameLog;
	
	private Date clockStoppedAt;
	private boolean clockIsStopped;
	private long timeWasStoppedFor=0;
	private TimerTask gameTimeTimerTask;

	private long penaltyThrowTime=45000;
	private boolean penaltyThrowOnResume=false;
	private Date penaltyThrowStartedAt;
	private TimerTask penaltyThrowTimerTask;
	
	private LinkedList<TwoMinutePenaltyTimerTask> twoMinutePenaltyTimerTasks =
										new LinkedList<TwoMinutePenaltyTimerTask>();
	

	final private Timer timer = new Timer();
	
	private class TwoMinutePenaltyTimerTask extends TimerTask{
		int playerNumber;
		Game.Colors team;
		
		public TwoMinutePenaltyTimerTask(int playerNumber, Game.Colors team){
			super();
			
			this.playerNumber = playerNumber;
			this.team = team;
		}
		
		public void run(){
			//TODO: Spieler wieder reinstellen
			System.out.println("Spieler "+playerNumber+" von Team "+team+"darf wieder mitspielen");
		}
	}
	

	public enum GameAction {
		UNDEFINED, GOAL, FREE_THROW, WARNING, TEAM_WARNING, TWO_MINUTES, PENALTY_THROW, EXCHANGE
	}
	
	public enum Colors {
		UNDEFINED, BLUE, WHITE
	}
	
	
	public Game(String blue, String white, String uwRef1, String uwRef2, String gameLeader, 
					long halfTimesDuration, int halfTimesCount, boolean stoppingTime){
		this.blue = blue;
		this.white = white;
		this.uwRef1 = uwRef1;
		this.uwRef2 = uwRef2;
		this.gameLeader = gameLeader;
		
		this.halfTimesDuration = halfTimesDuration;
		this.halfTimesCount = halfTimesCount;
		this.stoppingTime = stoppingTime;
		
		this.gameLog = new GameLog();
	}

	public void refereeInteraktion(){
		if(!isRunning()){
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
	}
	
	
	public void start(){
		start = new Date();
		clockIsStopped = false;
		currentHalfTime++;
		
		gameTimeTimerTask = new TimerTask(){
			public void run(){
				if(!penaltyThrowRunning() && getCurrentGameTime()>halfTimesDuration){
					//TODO abhupen
					start = null;
					System.out.println("Halbzeit zu ende!");
				}else if(penaltyThrowRunning()){
					timer.schedule(this, penaltyThrowTime-getCurrentPenaltyThrowTime());//TODO:Darf hier nicht negativ werden
				}else{
					timer.schedule(this, halfTimesDuration-getCurrentGameTime());//TODO:Darf hier nicht negativ werden
				}
			}
		};
		
		timer.schedule(gameTimeTimerTask, halfTimesDuration+50);
	}
	
	public void reset(){
		start = null;
		currentHalfTime = 0;
		gameTimeTimerTask.cancel();
		penaltyThrowTimerTask.cancel();
		//todo clear log
	}
	
	public boolean isRunning(){
		return start != null;
	}
		
	public void stopClock(){
		Date now = new Date();
		clockStoppedAt = now;
		clockIsStopped = true;
		penaltyThrowStartedAt = null;
		penaltyThrowTimerTask.cancel();
	}
	
	public boolean isStopped(){
		return clockIsStopped;
	}
	
	public void resumeClock(){
		timeWasStoppedFor += new Date().getTime()-clockStoppedAt.getTime();
		if(penaltyThrowOnResume){
			penaltyThrowOnResume = false;
			penaltyThrowStartedAt = new Date();
			penaltyThrowTimerTask = new TimerTask(){
				public void run(){
					//TODO: Abhupen
					System.out.println("Strafwurf zu ende!");
					stopClock();
				}
			};
			timer.schedule(penaltyThrowTimerTask, penaltyThrowTime);
		}
		clockIsStopped = false;
	}
	
	public void decision(Game.Colors team,  int player, Game.GameAction incident, String comment){
		gameLog.logNewestIncident(team, player, incident, comment);
	}
	
	public void startWithPenaltyThrow(){
		penaltyThrowOnResume = true;
	}
	
	public boolean penaltyThrowRunning(){
		return penaltyThrowStartedAt != null;
	}
	
	public long getCurrentPenaltyThrowTime(){
		if(!penaltyThrowRunning())
			return 0;
		return penaltyThrowStartedAt.getTime()+penaltyThrowTime-new Date().getTime();
	}
	
	public void suspendPlayer(int playerNumber, Game.Colors team){
		TwoMinutePenaltyTimerTask task = new TwoMinutePenaltyTimerTask(playerNumber, team);
		twoMinutePenaltyTimerTasks.add(task);
		timer.schedule(task, 2*60*1000);
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
		task.run();
	}
	
	public long getCurrentGameTime(){
		if(isRunning()){
			if(isStopped()){
				return start.getTime()-clockStoppedAt.getTime()-timeWasStoppedFor;
			}else{
				return start.getTime()-timeWasStoppedFor;
			}
		}
		
		return 0;
	}
}
