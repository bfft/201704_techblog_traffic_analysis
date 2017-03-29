package de.bfft.traffic.analysis;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to crop a given image and divide it in subimages
 * 
 * @author Andreas Wittmann
 *
 */
public class ImageDivider {
	private static final Logger log = LogManager.getLogger(ImageDivider.class);
	private final List<BufferedImage> images;

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public ImageDivider(BufferedImage _image) throws IOException {
		final BufferedImage image = cropImage(_image, 320, 33);
		this.images = divideImage(image, 4, 4);
	}

	/**
	 * divide a given image to the given number of rows and columns
	 * 
	 * @see http://kalanir.blogspot.de/2010/02/how-to-split-image-into-chunks-java.html
	 * @param image
	 * @param nrows
	 * @param ncols
	 * @return
	 */
	private List<BufferedImage> divideImage(BufferedImage image, int nrows, int ncols) {
		final int chunkWidth = image.getWidth() / ncols;
		final int chunkHeight = image.getHeight() / nrows;

		final List<BufferedImage> images = new ArrayList<>();
		for (int x = 0; x < nrows; x++) {
			for (int y = 0; y < ncols; y++) {
				final BufferedImage tmpImage = new BufferedImage(chunkWidth, chunkHeight, image.getType());

				final Graphics2D gr = tmpImage.createGraphics();
				gr.drawImage(image, 0, 0, chunkWidth, chunkHeight, chunkWidth * y, chunkHeight * x,
						chunkWidth * y + chunkWidth, chunkHeight * x + chunkHeight, null);
				gr.dispose();
				images.add(tmpImage);
			}
		}
		if (log.isDebugEnabled())
			log.info("Splitting done");
		return images;
	}

	/**
	 * cut off top and take only the left side of the image
	 * 
	 * @param image
	 * @param cutoffRight
	 * @param cutoffTop
	 * @return
	 */
	private BufferedImage cropImage(final BufferedImage image, int cutoffRight, int cutoffTop) {
		final BufferedImage imageCutted = new BufferedImage(image.getWidth() - cutoffRight, image.getHeight() - cutoffTop,
				image.getType());

		final Graphics2D grCutted = imageCutted.createGraphics();
		grCutted.drawImage(image, 0, 0, image.getWidth() - cutoffRight, image.getHeight() - cutoffTop, 0, cutoffTop,
				image.getWidth() - cutoffRight, image.getHeight(), null);
		grCutted.dispose();

		if (log.isDebugEnabled())
			log.info("image cropping done");
		return (imageCutted);
	}

	/**
	 * extract rgb data from a given image and scale it to gray
	 * 
	 * @see http://alvinalexander.com/blog/post/java/getting-rgb-values-for-each-pixel-in-image-using-java-bufferedi
	 * @param image
	 * @return
	 */
	static double[] extractData(final BufferedImage image) {
		if (log.isDebugEnabled())
			log.info("Image has dimension: " + image.getWidth() + " x " + image.getHeight());

		final List<RGB> rgbList = new ArrayList<>();
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				final int pixel = image.getRGB(x, y);
				final RGB rgb = new RGB((pixel >> 16) & 0xff, (pixel >> 8) & 0xff, pixel & 0xff);
				rgbList.add(rgb);
			}
		}

		return rgbList.stream().map(item -> item.getGrayscaled()).mapToDouble(Double::doubleValue).toArray();
	}

	public List<BufferedImage> getImages() {
		return images;
	}

	/**
	 * class to store rgb-values and convert it to gray-scale
	 * 
	 * @author Andreas Wittmann
	 *
	 */
	private static class RGB {
		double red;
		double green;
		double blue;
		private final List<Double> list;

		public RGB(double _red, double _green, double _blue) {
			this.red = _red;
			this.green = _green;
			this.blue = _blue;
			this.list = Arrays.asList(red / 255.0, green / 255.0, blue / 255.0);
		}

		double getGrayscaled() {
			return (Collections.min(list) + Collections.max(list)) / (2 * 255.0);
		}

		@Override
		public String toString() {
			return "RGB [red=" + red + ", green=" + green + ", blue=" + blue + ", list=" + list + "]";
		}
	}
}
