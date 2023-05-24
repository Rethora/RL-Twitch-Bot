package com.rethoracle.rltwitchbot;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.openqa.selenium.WebDriver;

import net.sourceforge.tess4j.Tesseract;

public class Recognizer extends Tesseract {

	public Recognizer() {
		initialize();
	}

	@SuppressWarnings("deprecation")
	private void initialize() {
		setDatapath(System.getenv("TESSDATA_PATH"));
		setTessVariable("user_defined_dpi", "300");
	}

	public int getGoalTextY(int height) {
		return (int) (height * 0.278);
	}

	public int getGoalTextHeight(int height) {
		return (int) (height * .139);
	}

	/**
	 * Crops screenshot to area that Rocket League name would be displayed to
	 * perform OCR
	 */
	public BufferedImage getSubBuffer(BufferedImage bufferedScreenshot, WebDriver driver) {
		int driverWidth = driver.manage().window().getSize().width;
		int driverHeight = driver.manage().window().getSize().height;

		int goalTextY = getGoalTextY(driverHeight);
		int goalTextHeight = getGoalTextHeight(driverHeight);

		BufferedImage subBuffer = bufferedScreenshot.getSubimage(0, goalTextY, driverWidth, goalTextHeight);

		// TODO: remove
		try {
			File testOutputFile = new File("tmp/cropped.png");
			ImageIO.write(subBuffer, "png", testOutputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return subBuffer;
	}

	public boolean isGoalScoredText(int r, int g, int b) {
		return withinThreshold(r, 237) && withinThreshold(g, 220) && withinThreshold(b, 166);
	}

	private boolean withinThreshold(int actual, int target) {
		// TODO: tune
		int threshold = 30;
		int low = target - threshold;
		int high = target + threshold;
		return actual >= low && actual <= high;
	}

	public BufferedImage binarize(BufferedImage subBuffer) {
		for (int i = 0; i < subBuffer.getWidth(); i++) {
			for (int j = 0; j < subBuffer.getHeight(); j++) {
				Color color = new Color(subBuffer.getRGB(i, j));
				subBuffer.setRGB(i, j,
						isGoalScoredText(color.getRed(), color.getGreen(), color.getBlue()) ? Color.BLACK.getRGB()
								: Color.WHITE.getRGB());
			}
		}

		// TODO: remove
		try {
			File testOutputFile = new File("tmp/binarized.png");
			ImageIO.write(subBuffer, "png", testOutputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return subBuffer;
	}
}
