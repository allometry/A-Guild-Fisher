import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.quirlion.script.Script;
import com.quirlion.script.types.Area;
import com.quirlion.script.types.Location;
import com.quirlion.script.types.NPC;
import com.quirlion.script.types.Thing;

public class AGuildFisher extends Script {
	private int npcShark = 313, fishCaught = 0;
	private Area northDock = new Area(new Location(2605, 3426), new Location(2598, 3419));
	private Area guildBank = new Area(new Location(2587, 3424), new Location(2585, 3420));
	private Location[] northDockPath = { new Location(2586, 3422), new Location(2591, 3420), new Location(2596, 3420), new Location(2599, 3422) };
	private NPC currentFishingArea = null;
	private long startTime, failsafeTime = 0;
	private BufferedImage basketImage;
	private BufferedImage clockImage;
	private Thread fishingHoleMonitor;
	private Runnable fishingHole;
	private int regularHarpoon = 311;
	private boolean runThread = true;
	
	public class FishingHole implements Runnable {
		public void run() {
			while(runThread) {
				if(players.getCurrent().isInArea(northDock)) {
					if(currentFishingArea == null)
						currentFishingArea = npcs.getNearestByID(npcShark);
					
					if(npcs.getAt(currentFishingArea.getLocation()) == null)
						currentFishingArea = npcs.getNearestByID(npcShark);
					
					if(npcs.getAt(currentFishingArea.getLocation()).getID() != npcShark)
						currentFishingArea = npcs.getNearestByID(npcShark);
					
					if(players.getCurrent().getAnimation() == -1 && !players.getCurrent().isMoving()) {
						if(failsafeTime == 0) {
							failsafeTime = System.currentTimeMillis() + 15000;
						}
						
						if(System.currentTimeMillis() > failsafeTime) {
							currentFishingArea = npcs.getNearestByID(npcShark);
							failsafeTime = 0;
						}
					} else {
						failsafeTime = 0;
					}
				}
			}
			
			log("Fishing Hole Monitor Thread Stopping...");
		}
	}
	
	@Override
	public int loop() {
		if(players.getCurrent().getAnimation() == -1 && !inventory.isFull() && players.getCurrent().isInArea(northDock) && currentFishingArea != null) {
			currentFishingArea.click("Harpoon");
			return 3000;
		}
		
		if(inventory.isFull() && !players.getCurrent().isInArea(guildBank)) {
			walker.walkPathMM(Location.reversePath(northDockPath));
			return 1500;
		}
		
		if(inventory.isFull() && players.getCurrent().isInArea(guildBank)) {
			if(!bank.isOpen()) {
				Thing booth = things.getNearest(49018);
				
				if(booth != null)
					booth.click("Quickly");
				
				return 2000;
			} else {
				bank.depositAllExcept(regularHarpoon);
				
				return 2000;
			}
		}
		
		if(!inventory.isFull() && !players.getCurrent().isInArea(northDock)) {
			walker.walkPathMM(northDockPath);
			return 1500;
		}
		
		return 1;
	}
	
	@Override
	public void onStart() {
		startTime = System.currentTimeMillis();

		try {
			basketImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/basket.png"));
			clockImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/clock.png"));
		} catch (IOException e) {
			logStackTrace(e);
		}
		
		fishingHole = new FishingHole();
		
		fishingHoleMonitor = new Thread(fishingHole);
		fishingHoleMonitor.start();
	}
	
	@Override
	public void onStop() {
		runThread = false;
		log("Stopping Allometry Guild Fisher...");
		
		return ;
	}
	
	@Override
	public void paint(Graphics g2) {
		if(!players.getCurrent().isLoggedIn() || players.getCurrent().isInLobby()) return ;
		
		Graphics2D g = (Graphics2D)g2;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//Rectangles
		RoundRectangle2D clockBackground = new RoundRectangle2D.Float(
				interfaces.getMinimap().getRealX() - 144,
				20,
				89,
				26,
				5,
				5);
		
		RoundRectangle2D scoreboardBackground = new RoundRectangle2D.Float(
				20,
				20,
				89,
				26,
				5,
				5);
		
		g.setColor(new Color(0, 0, 0, 127));
		g.fill(clockBackground);
		g.fill(scoreboardBackground);
		
		//Text
		g.setColor(Color.white);
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		
		NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
		
		g.drawString(nf.format(fishCaught), 48, 39);
		
		if(startTime == 0)
			g.drawString("Loading", interfaces.getMinimap().getRealX() - 139, 37);
		else
			g.drawString(millisToClock(System.currentTimeMillis() - startTime), interfaces.getMinimap().getRealX() - 139, 37);
		
		//Images
		ImageObserver observer = null;
		g.drawImage(basketImage, 25, 25, observer);
		g.drawImage(clockImage, interfaces.getMinimap().getRealX() - 75, 25, observer);
		
		return ;
	}
	
	@Override
	public void serverMessageReceived(String message) {
		if (message.contains("shark"))
			fishCaught++;
		if (message.contains("swordfish"))
			fishCaught++;
		if (message.contains("lobster"))
			fishCaught++;
		if (message.contains("tuna"))
			fishCaught++;
		
		return;
	}
	
	private String millisToClock(long milliseconds) {
		long seconds = (milliseconds / 1000), minutes = 0, hours = 0;
		
		if (seconds >= 60) {
			minutes = (seconds / 60);
			seconds -= (minutes * 60);
		}
		
		if (minutes >= 60) {
			hours = (minutes / 60);
			minutes -= (hours * 60);
		}
		
		return (hours < 10 ? "0" + hours + ":" : hours + ":")
				+ (minutes < 10 ? "0" + minutes + ":" : minutes + ":")
				+ (seconds < 10 ? "0" + seconds : seconds);
	}

}
