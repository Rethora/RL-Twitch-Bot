package com.rethoracle.rltwitchbot;

import org.openqa.selenium.WebDriver;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

public class MessageHandler {
	private TwitchClient c;
	private Screenshotter s;
	private WebDriver d;
	private GoalWatcher gW;

	public MessageHandler(TwitchClient client, WebDriver driver, Screenshotter screenshotter, GoalWatcher goalWatcher) {
		c = client;
		d = driver;
		s = screenshotter;
		gW = goalWatcher;
	}

	public void handleMessage(ChannelMessageEvent event) {
		String message = event.getMessage();
		if (isCommand(message)) {
			String command = getCommand(message).toLowerCase();
			switch (command) {
			case "leave":
				leaveChannel(event);
				break;
			case "stop watching":
				stopWatching(event);
				break;
			case "start watching":
				startWatching(event);
				break;
			case "stop roasting":
				stopRoasting(event);
				break;
			case "start roasting":
				startRoasting(event);
				break;
			case "stop complimenting":
				stopComplimenting(event);
				break;
			case "start complimenting":
				startComplimenting(event);
				break;
			default:
				break;
			}
		}

		if (event.getMessage().equals("!leave")) {
			c.getChat().leaveChannel(event.getChannel().getName());
		}
	}

	private void leaveChannel(ChannelMessageEvent event) {
		c.getChat().leaveChannel(event.getChannel().getName());
	}

	private void stopWatching(ChannelMessageEvent event) {
		d.quit();
		if (!s.isRunning()) {
			s.terminate();
		}
		if (!gW.isRunning()) {
			gW.terminate();
		}
	}

	private void startWatching(ChannelMessageEvent event) {
		// TODO:
	}

	private void stopRoasting(ChannelMessageEvent event) {
		gW.setRoastWhenScoredOn(false);
	}

	private void startRoasting(ChannelMessageEvent event) {
		gW.setRoastWhenScoredOn(true);
	}

	private void stopComplimenting(ChannelMessageEvent event) {
		gW.setComplimentWhenScored(false);
	}

	private void startComplimenting(ChannelMessageEvent event) {
		gW.setComplimentWhenScored(true);
	}

	private boolean isCommand(String message) {
		return message.startsWith("!");
	}

	private String getCommand(String message) {
		return message.substring(1, message.length());
	}
}
