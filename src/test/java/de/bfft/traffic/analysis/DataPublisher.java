package de.bfft.traffic.analysis;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Publish already scraped images to Kafka
 * 
 * @author wittmaan
 *
 */
public class DataPublisher implements AutoCloseable {

	private static final Logger log = LogManager.getLogger(DataPublisher.class);
	final KafkaProducer<Long, byte[]> kafkaProducer;
	final Properties properties;

	/**
	 * Constructor
	 */
	public DataPublisher() {
		this.properties = new Properties();
		properties.put(ProducerConfig.CLIENT_ID_CONFIG, "DataPublisher");
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
		properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024 * 5);

		log.info("Kafka producer properties: " + properties.toString());
		log.trace("Creating kafka producer...");
		this.kafkaProducer = new KafkaProducer<>(properties, new LongSerializer(), new ByteArraySerializer());
	}

	public static void main(String[] args) throws Exception {
		try (DataPublisher dataPublisher = new DataPublisher()) {
			dataPublisher.runLive();
		}
	}

	/**
	 * Live scraping the webcam image from BayernInfo every minute
	 * 
	 * @throws Exception
	 */
	private void runLive() throws Exception {
		final URL url = new URL("http://www.bayerninfo.de/webcams/images/A9Munich-Nuremberg/mobil200004.jpg");

		while (true) {
			final BufferedImage image = ImageIO.read(url);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", baos);
			final byte[] payload = baos.toByteArray();

			final ProducerRecord<Long, byte[]> msg = new ProducerRecord<Long, byte[]>(
					TrafficAnalysisApplication.TOPIC_WEBCAM, System.currentTimeMillis(), payload);
			log.info("publishing signal " + msg);
			kafkaProducer.send(msg);

			// Wait 1 Minute
			Thread.sleep(60 * 1000L);
		}
	}

	/**
	 * Take already scraped images and send it to kafka
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void runReplay() throws Exception {
		final String filePath = new File("").getAbsolutePath();
		final File[] files = new File(filePath + "\\data").listFiles();
		for (int i = 0; i < files.length; i++) {

			final BufferedImage image = ImageIO.read(files[i]);
			final String dateTime = files[i].getName().substring(16, 30);
			final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			final Date t = dateFormat.parse(dateTime);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", baos);
			final byte[] payload = baos.toByteArray();

			final ProducerRecord<Long, byte[]> msg = new ProducerRecord<Long, byte[]>(
					TrafficAnalysisApplication.TOPIC_WEBCAM, t.getTime(), payload);
			log.info("publishing signal " + msg);
			kafkaProducer.send(msg);

			// Wait 1 Minute
			Thread.sleep(60 * 1000L);
		}

	}

	@Override
	public void close() throws Exception {
		if (kafkaProducer != null) {
			log.info("Closing kafka producer...");
			kafkaProducer.close();
		}
	}
}
