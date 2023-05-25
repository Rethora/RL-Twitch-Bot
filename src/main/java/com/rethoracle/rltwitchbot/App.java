package com.rethoracle.rltwitchbot;

import com.github.twitch4j.TwitchClient;

import com.github.twitch4j.TwitchClientBuilder;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;

import io.github.bonigarcia.wdm.WebDriverManager;

public class App {
	public static void main(String[] args) {
		try {
			String configFilePath = System.getenv("CONFIG_FILE_PATH");
			FileInputStream propsInput = new FileInputStream(configFilePath);
			Properties properties = new Properties();
			properties.load(propsInput);

			String channelName = properties.getProperty("TWITCH_CHANNEL_NAME");

			WebDriverManager.firefoxdriver().setup();
			FirefoxOptions options = new FirefoxOptions();
			options.addArguments("--headless");

			WebDriver driver = new FirefoxDriver(options);

			// * open video player
			String url = String.format(
					"https://player.twitch.tv/?channel=%s&muted=true&parent=twitch.tv&player=popout&quality=chunked",
					channelName);
			driver.get(url);
			driver.manage().window().fullscreen();

			// * accept mature content warning if it pops up
			List<WebElement> buttons = driver.findElements(By.tagName("button"));
			for (WebElement button : buttons) {
				if (button.getText().toLowerCase().equals("start watching")) {
					try {
						button.click();
					} catch (StaleElementReferenceException e) {
						e.printStackTrace();
					}
				}
			}

			TwitchClient twitchClient = TwitchClientBuilder.builder()
					.withChatAccount(new OAuth2Credential("twitch", System.getenv("TWITCH_OAUTH_TOKEN")))
					.withEnableChat(true).build();
			twitchClient.getChat().joinChannel(channelName);

			Queue<File> queue = new LinkedList<File>();
			Recognizer recognizer = new Recognizer(properties);

			int screenshotSleep = Integer.parseInt(properties.getProperty("SCREENSHOT_SLEEP"));

			Screenshotter screenshotter = new Screenshotter(driver, queue, screenshotSleep);
			GoalWatcher goalWatcher = new GoalWatcher(recognizer, queue, driver, twitchClient, properties);

			MessageHandler messageHandler = new MessageHandler(twitchClient, driver, screenshotter, goalWatcher);

			screenshotter.start();
			goalWatcher.start();

			twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
				messageHandler.handleMessage(event);
			});
		} catch (FileNotFoundException e) {
			System.out.println("No config.properties file!");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Invalid config.properties!");
			e.printStackTrace();
		}
	}
}
