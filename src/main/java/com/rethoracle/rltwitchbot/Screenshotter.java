package com.rethoracle.rltwitchbot;

import java.io.File;
import java.util.Queue;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class Screenshotter extends Thread {
	private WebDriver d;
	private Queue<File> q;
	private int in;
	private volatile boolean running;

	public Screenshotter(WebDriver driver, Queue<File> queue) {
		d = driver;
		q = queue;
		in = 1000;
	}

	public Screenshotter(WebDriver driver, Queue<File> queue, int interval) {
		d = driver;
		q = queue;
		in = interval;
	}

	public int getInterval() {
		return in;
	}

	public void terminate() {
		running = false;
		interrupt();
	}

	public boolean isRunning() {
		return running;
	}

	public void handleRun() {
		File file = ((TakesScreenshot) d).getScreenshotAs(OutputType.FILE);
		q.add(file);
		try {
			Thread.sleep(in);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		running = true;
		while (running) {
			handleRun();
		}
	}
}
