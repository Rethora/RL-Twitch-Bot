package com.rethoracle.rltwitchbot;

import com.github.twitch4j.TwitchClient;

import com.github.twitch4j.TwitchClientBuilder;

import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;

import io.github.bonigarcia.wdm.WebDriverManager;

// TODO: 
// dockerize
// rewrite tests
// clean code
// README

public class App {
	public static void main(String[] args) {
		try {
			String configFilePath = System.getenv("CONFIG_FILE_PATH");
			FileInputStream propsInput = new FileInputStream(configFilePath);
			Properties properties = new Properties();
			properties.load(propsInput);

			String channelName = properties.getProperty("TWITCH_CHANNEL_NAME");
//			String width = properties.getProperty("WINDOW_WIDTH");
//			String height = properties.getProperty("WINDOW_HEIGHT");

//			WebDriverManager.firefoxdriver().setup();
//			FirefoxOptions options = new FirefoxOptions();
//			options.addArguments("--headless");
//			options.addArguments(String.format("--width=%s", width));
//			options.addArguments(String.format("--height=%s", height));

//			WebDriver driver = new FirefoxDriver(options);
			WebDriverManager wdm = WebDriverManager.firefoxdriver().browserInDocker().browserVersion("111.0");
			WebDriver driver = wdm.create();

			// open video player
			String url = String.format(
					"https://player.twitch.tv/?channel=%s&muted=true&parent=twitch.tv&player=popout&quality=chunked",
					channelName);
			driver.get(url);
//			driver.manage().window().fullscreen();

			// accept mature content warning
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

			// WebDriver driver = wdm.create();
			// driver.get("https://google.com");
			// FirefoxOptions webDriverOptions = new FirefoxOptions();
			// webDriverOptions.addArguments(
			// String.format("--no-sandbox", "--disable-extensions", "--disable-gpu",
			// "--ignore-certificate-errors",
			// "--whitelisted-ips=''", "--disable-dev-shm-usage", "--window-size=%s,%s"),
			// windowWidth, windowHeight);

			// WebDriverManager.chromedriver().setup();
			// WebDriverManager.firefoxdriver().setup();
			// FirefoxOptions firefoxOptions = new FirefoxOptions();
			// firefoxOptions.addArguments("--headless");
			// firefoxOptions.addArguments(String.format("--width=%s", windowWidth));
			// firefoxOptions.addArguments(String.format("--height=%s", windowHeight));
			// firefoxOptions.addArguments(String.format("--window-size=%s,%s", windowWidth,
			// windowHeight));
			// firefoxOptions.addArguments("--no-sandbox");
			// firefoxOptions.addArguments("--disable-gpu");
			// firefoxOptions.addArguments("--disable-crash-reporter");
			// firefoxOptions.addArguments("--disable-extensions");
			// firefoxOptions.addArguments("--disable-dev-shm-usage");

			// try {
			// URL url = new URL("http://localhost:4444/wd/hub");
			// WebDriver driver = new RemoteWebDriver(url, webDriverOptions);
			// driver.get("https://google.com");
			//// ThreadLocal<RemoteWebDriver> driver = new ThreadLocal<>();
			//// driver.set(new RemoteWebDriver(url, webDriverOptions));
			// System.out.println(driver);
			//// WebDriver driver = new RemoteWebDriver(url, webDriverOptions);
			// // driver.openPlayer(channelName);
			// // driver.manage().window().fullscreen();
			//// driver.get("https://google.com");
			//// File file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			//// System.out.println(file);
			// } catch (MalformedURLException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (WebDriverException e) {
			// e.printStackTrace();
			// } catch (UnsupportedOperationException e) {
			// e.printStackTrace();
			// }

			TwitchClient twitchClient = TwitchClientBuilder.builder()
					.withDefaultAuthToken(new OAuth2Credential("twitch", System.getenv("TWITCH_OAUTH_TOKEN")))
					.withEnableChat(true).build();
			twitchClient.getChat().joinChannel(channelName);

			Queue<File> queue = new LinkedList<File>();
			Recognizer recognizer = new Recognizer();

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
