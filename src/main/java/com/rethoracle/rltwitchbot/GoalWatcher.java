package com.rethoracle.rltwitchbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;

import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import com.github.twitch4j.TwitchClient;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class GoalWatcher extends Thread {
	private Recognizer r;
	private Queue<File> q;
	private WebDriver d;
	private TwitchClient tc;
	private String cn;
	private String rlName;
	private String tmOneName;
	private String tmTwoName;
	private ArrayList<String> complimentMessageList;
	private ArrayList<String> roastMessageList;
	private boolean shouldCompliment;
	private boolean shouldComplimentForTeammates;
	private boolean shouldRoast;
	private volatile boolean running;

	public GoalWatcher(Recognizer recognizer, Queue<File> queue, WebDriver driver, TwitchClient twitchClient,
			Properties properties) {
		r = recognizer;
		q = queue;
		d = driver;
		tc = twitchClient;
		shouldCompliment = Boolean.parseBoolean(properties.getProperty("COMPLIMENT_BY_DEFAULT"));
		shouldComplimentForTeammates = Boolean.parseBoolean(properties.getProperty("COMPLIMENT_BY_DEFAULT_TEAMMATES"));
		shouldRoast = Boolean.parseBoolean(properties.getProperty("ROAST_BY_DEFAULT"));
		cn = properties.getProperty("TWITCH_CHANNEL_NAME");
		rlName = properties.getProperty("ROCKET_LEAGUE_NAME");
		tmOneName = properties.getProperty("TEAMMATE_ONE_NAME");
		tmTwoName = properties.getProperty("TEAMMATE_TWO_NAME");
		initializeComplimentMessageList();
		initializeRoastMessageList();
	}

	private void initializeComplimentMessageList() {
		complimentMessageList = getMessageList(EMessageType.COMPLIMENT);
	}

	private void initializeRoastMessageList() {
		roastMessageList = getMessageList(EMessageType.ROAST);
	}

	public void setComplimentWhenScored(boolean complimentWhenScored) {
		shouldCompliment = complimentWhenScored;
	}

	public void setComplimentWhenTeammateScored(boolean complimentWhenTeammateScored) {
		shouldComplimentForTeammates = complimentWhenTeammateScored;
	}

	public void setRoastWhenScoredOn(boolean roastWhenScoredOn) {
		shouldRoast = roastWhenScoredOn;
	}

	public boolean isComplimentingWhenScored() {
		return shouldCompliment;
	}

	public boolean isComplimentingWhenTeammatesScored() {
		return shouldComplimentForTeammates;
	}

	public boolean isRoastingWhenScoredOn() {
		return shouldRoast;
	}

	/**
	 * Reads the messages from messageType's file and returns a shuffled list
	 */
	private ArrayList<String> getMessageList(EMessageType messageType) {
		ArrayList<String> messageList = new ArrayList<>();
		File messageFile;

		switch (messageType) {
			case COMPLIMENT:
				messageFile = new File(System.getenv("COMPLIMENTS_FILE_PATH"));
				break;
			case ROAST:
				messageFile = new File(System.getenv("ROASTS_FILE_PATH"));
				break;
			default:
				return messageList;
		}

		try {
			Scanner scanner = new Scanner(messageFile);
			while (scanner.hasNextLine()) {
				String message = scanner.nextLine();
				messageList.add(message);
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Collections.shuffle(messageList);
		return messageList;
	}

	public void terminate() {
		running = false;
		interrupt();
	}

	public boolean isRunning() {
		return running;
	}

	private boolean didScore(String text) {
		String scoredText = "scored!";

		if (text.replaceAll("\\W+", "").length() < scoredText.length()) {
			return false;
		}

		int weight = FuzzySearch.weightedRatio(scoredText, text);
		if (weight >= 75) {
			return true;
		}

		return false;
	}

	private EWhoScored whoScored(String text) {
		EWhoScored whoScored;

		if (isRlNameInText(rlName, text)) {
			whoScored = EWhoScored.STREAMER;
		} else if (isRlNameInText(tmOneName, text) || isRlNameInText(tmTwoName, text)) {
			whoScored = EWhoScored.TEAMMATE;
		} else {
			whoScored = EWhoScored.UNKNOWN;
		}

		return whoScored;
	}

	private boolean isRlNameInText(String rlName, String text) {
		int weight = FuzzySearch.weightedRatio(rlName, text);

		return weight >= 75;
	}

	private String extractTextFromScreenshot(File screenshot) {
		String text = "";

		try {
			BufferedImage screenshotBuffer = ImageIO.read(screenshot);
			BufferedImage subBuffer = r.getSubBuffer(screenshotBuffer, d);
			BufferedImage binarizedBuffer = r.binarize(subBuffer);
			text = r.doOCR(binarizedBuffer).trim();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TesseractException e) {
			e.printStackTrace();
		}

		return text;
	}

	public void handleRun() {
		if (!q.isEmpty()) {
			File screenshot = null;
			String text = "";

			try {
				screenshot = q.remove();
				text = extractTextFromScreenshot(screenshot);
				screenshot.delete();
				screenshot = null;
			} catch (NoSuchElementException e) {
				e.printStackTrace();
			}

			if (!text.equals("")) {
				// * reset list if empty
				if (complimentMessageList.isEmpty()) {
					initializeComplimentMessageList();
				}
				if (roastMessageList.isEmpty()) {
					initializeRoastMessageList();
				}

				if (didScore(text)) {
					EWhoScored scorer = EWhoScored.OPPONENT;

					EWhoScored s = whoScored(text);

					if (s != EWhoScored.UNKNOWN) {
						scorer = s;
					} else {
						LocalTime startTime = LocalTime.now();
						LocalTime stopTime = startTime.plusSeconds(5);

						// * check text for the next 5 seconds
						while (LocalTime.now().isBefore(stopTime)) {
							if (!q.isEmpty()) {
								try {
									screenshot = q.remove();
									text = extractTextFromScreenshot(screenshot);
									screenshot.delete();
									screenshot = null;
								} catch (NoSuchElementException e) {
									e.printStackTrace();
								}
							}

							if (!text.equals("")) {
								s = whoScored(text);
								if (s != EWhoScored.UNKNOWN) {
									scorer = s;
									break;
								}
							}
						}
					}

					switch (scorer) {
						case STREAMER:
							if (shouldCompliment) {
								String compliment = complimentMessageList.get(0);
								complimentMessageList.remove(0);
								// System.out.println("COMPLIMENT: " + compliment);
								tc.getChat().sendMessage(cn, compliment);
							}
							break;
						case TEAMMATE:
							if (shouldComplimentForTeammates) {
								String compliment = complimentMessageList.get(0);
								complimentMessageList.remove(0);
								// System.out.println("COMPLIMENT: " + compliment);
								tc.getChat().sendMessage(cn, compliment);
							}
							break;
						case OPPONENT:
							if (shouldRoast) {
								String roast = roastMessageList.get(0);
								roastMessageList.remove(0);
								// System.out.println("ROAST: " + roast);
								tc.getChat().sendMessage(cn, roast);
							}
							break;
						default:
							break;
					}

					try {
						Thread.sleep(6000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					for (File file : q) {
						file.delete();
					}
					q.clear();
					return;
				}
			}
		}
	}

	public void run() {
		running = true;
		while (running) {
			handleRun();
		}
	}

}

enum EMessageType {
	ROAST, COMPLIMENT
}

enum EWhoScored {
	STREAMER, TEAMMATE, OPPONENT, UNKNOWN
}
