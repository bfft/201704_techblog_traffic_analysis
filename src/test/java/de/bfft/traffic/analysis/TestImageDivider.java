package de.bfft.traffic.analysis;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test ImageDivider class
 * 
 * @author Andreas Wittmann
 *
 */
public class TestImageDivider {

	/**
	 * test the extractData method with an 4 x 4 pixel image and compare with the result from R
	 * 
	 * @throws IOException
	 */
	@Test
	public void testImageDividerExtractData16Pixel() throws IOException {
		final double[] dataActual = getDataFromImage("16pixel.jpg");
		final double[] dataExpected = getExpectedDataFromR("16pixelData.txt").get(0);
		Assert.assertTrue(dataActual.length == dataExpected.length);
		for (int i = 0; i < dataActual.length; i++) {
			Assert.assertEquals(dataExpected[i], dataActual[i], 1.0e-12);
		}
	}

	/**
	 * test the extractData method with an 9 x 9 pixel image and compare with the result from R
	 * 
	 * @throws IOException
	 */
	@Test
	public void testImageDividerExtractData81Pixel() throws IOException {
		final double[] dataActual = getDataFromImage("81pixel.jpg");
		final double[] dataExpected = getExpectedDataFromR("81pixelData.txt").get(0);
		Assert.assertTrue(dataActual.length == dataExpected.length);
		for (int i = 0; i < dataActual.length; i++) {
			Assert.assertEquals(dataExpected[i], dataActual[i], 1.0e-12);
		}
	}

	private List<double[]> getExpectedDataFromR(String filename) throws IOException, FileNotFoundException {
		final String path = new File("").getAbsolutePath();
		String[] splitted = null;
		final List<double[]> data = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(path + "\\testdata\\" + filename)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				splitted = line.split(" ");
				data.add(Arrays.stream(splitted).mapToDouble(Double::parseDouble).toArray());
			}
		}
		return data;
	}

	private double[] getDataFromImage(final String filename) throws IOException {
		final String path = new File("").getAbsolutePath() + "\\testdata\\";
		final File file = new File(path + filename);
		final BufferedImage image = ImageIO.read(file);
		return ImageDivider.extractData(image);
	}

}
