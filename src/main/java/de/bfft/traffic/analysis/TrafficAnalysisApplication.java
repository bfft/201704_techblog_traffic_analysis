package de.bfft.traffic.analysis;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.*;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import scala.Tuple2;

/**
 * Spark Streaming Job to predict the number of cars on a given image from the kafka topic in.webcam.data
 * 
 * @author Andreas Wittmann
 *
 */
public class TrafficAnalysisApplication {
	static final String TOPIC_WEBCAM = "in.webcam.data";
	private static final Logger log = LogManager.getLogger(TrafficAnalysisApplication.class);

	public static void main(String[] args) {
		// set the Log Level to only print errors
		org.apache.log4j.Logger.getLogger("org").setLevel(Level.ERROR);

		// create SparkStreamingContext
		final SparkConf conf = new SparkConf().setMaster("local[*]")
				.setAppName(TrafficAnalysisApplication.class.getName());
		final JavaStreamingContext sc = new JavaStreamingContext(conf, new Duration(60 * 1000));
		sc.checkpoint("/tmp");

		// define topics for consumption
		final Set<String> topics = new HashSet<String>();
		topics.add(TOPIC_WEBCAM);
		log.info("Listening for topics: " + topics);

		// read byte-array data from kafka topic
		final JavaPairDStream<Long, byte[]> inputStream = KafkaUtils
				.createDirectStream(sc, LocationStrategies.PreferConsistent(),
						ConsumerStrategies.<Long, byte[]> Subscribe(topics, getConsumerConfig("TrafficAnalysisApplication")))
				.mapToPair(msg -> new Tuple2<Long, byte[]>(msg.key(), msg.value()));

		// estimate number of cars of the given image
		// 1. Divide image in subimages and estimate 0, 1 or 2 cars on it
		// 2. Reduce-Step: sum up the numbers of cars for the whole image
		final JavaPairDStream<Long, Integer> numberOfCarsByTimestamp = inputStream.flatMapToPair(entry -> {
			final byte[] payload = entry._2;
			final List<Tuple2<Long, Integer>> result = new ArrayList<>();

			final BufferedImage image = ImageIO.read(new ByteArrayInputStream(payload));
			final List<BufferedImage> subimages = new ImageDivider(image).getImages();

			for (final BufferedImage img : subimages) {
				final Prediction prediction = new Prediction(ImageDivider.extractData(img));
				result.add(new Tuple2<Long, Integer>(entry._1, prediction.getLabelIndex()));
			}

			return result.iterator();
		}).reduceByKey((key1, key2) -> key1.intValue() + key2.intValue());

		numberOfCarsByTimestamp.print();
		numberOfCarsByTimestamp.foreachRDD(rdd -> {
			if (rdd != null) {
				final JavaRDD<String> rddJson = rdd.map(m -> {
					final CountData dat = new CountData(new Date(m._1), m._2);
					final ObjectMapper objectMapper = new ObjectMapper();
					objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
					log.info("Got this data: " + dat);
					return objectMapper.writeValueAsString(dat);
				});

				log.info("Save to es index " + "analysis/log");
				JavaEsSpark.saveJsonToEs(rddJson, "analysis/log", getEsConfig());
			}
		});

		sc.start();
		try {
			sc.awaitTermination();
		}
		catch (final Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static Map<String, String> getEsConfig() {
		final Map<String, String> esConfig = new HashMap<>();
		esConfig.put("es.nodes", "localhost");
		esConfig.put("es.port", "9200");
		esConfig.put("es.index.auto.create", "true");
		esConfig.put("es.http.timeout", "5m");
		return esConfig;
	}

	/**
	 * Create Kafka-Consumer-Config
	 * 
	 * @return
	 */
	public static Map<String, Object> getConsumerConfig(String canonicalName) {
		final Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.LongDeserializer.class.getCanonicalName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				org.apache.kafka.common.serialization.ByteArrayDeserializer.class.getCanonicalName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, canonicalName);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.TRUE);
		props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 60 * 1000);
		props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024 * 5);
		log.info("Kafka consumer properties: " + props.toString());
		return props;
	}

	/**
	 * Class to store the count data with timestamp and count
	 * 
	 * @author Andreas Wittmann
	 *
	 */
	private static class CountData implements Serializable {
		private static final long serialVersionUID = 7517340248145922836L;

		private final Date t;
		private final Integer count;

		public CountData(Date _t, Integer _count) {
			this.t = _t;
			this.count = _count;
		}

		@Override
		public String toString() {
			return "CountData [t=" + t + ", count=" + count + "]";
		}
	}
}
