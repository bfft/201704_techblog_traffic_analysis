package de.bfft.traffic.analysis;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import hex.genmodel.GenModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.MultinomialModelPrediction;

/**
 * Class to call the H2O generated prediction class using the given double array
 * 
 * @author Andreas Wittmann
 *
 */
public class Prediction {

	private static final Logger log = LogManager.getLogger(Prediction.class);
	private String label;
	private int labelIndex;
	private List<Double> probabilities;
	private List<String> categories;

	/**
	 * Constructor
	 * 
	 */
	public Prediction(double[] data) {
		calc(data);
	}

	private void calc(double[] data) {
		try {
			final GenModel genModel = (GenModel) Class.forName(DRF_model_R_1478847643061_1.class.getName()).newInstance();
			final EasyPredictModelWrapper model = new EasyPredictModelWrapper(genModel);

			final RowData rowData = new RowData();
			final String[] colNames = genModel.getNames();
			if (colNames.length != data.length) {
				throw new IllegalStateException(
						"Length of colNames " + colNames.length + " not equal to length of data " + data.length);
			}
			for (int i = 0; i < data.length; i++) {
				rowData.put(colNames[i], data[i]);
			}
			final MultinomialModelPrediction p = model.predictMultinomial(rowData);
			label = p.label;
			labelIndex = p.labelIndex;
			probabilities = Arrays.stream(p.classProbabilities).boxed().collect(Collectors.toList());
			categories = Arrays.stream(model.getResponseDomainValues()).collect(Collectors.toList());
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			log.error("could not create model!");
			e.printStackTrace();
		}
		catch (final PredictException e) {
			log.error("Could not run prediction");
			e.printStackTrace();
		}

	}

	@Override
	public String toString() {
		return "Prediction [label=" + label + ", labelIndex=" + labelIndex + ", probabilities=" + probabilities
				+ ", categories=" + categories + "]";
	}

	public String getLabel() {
		return label;
	}

	public int getLabelIndex() {
		return labelIndex;
	}

	public List<Double> getProbabilities() {
		return probabilities;
	}

	public List<String> getCategories() {
		return categories;
	}
}
