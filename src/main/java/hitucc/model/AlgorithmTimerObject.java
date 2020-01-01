package hitucc.model;

import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor(force = true)
public class AlgorithmTimerObject implements Serializable {
	private long tableReadStartTime;
	private long registerStartTime;
	private long dictionaryStartTime;
	private long phaseOneStartTime;
	private long phaseTwoStartTime;
	private long finishTime;

	private boolean sortColumnsInPhaseOne;
	private boolean sortNegatively;
	private String outputFile;
	private String datasetName;

	public AlgorithmTimerObject(boolean sortColumnsInPhaseOne, boolean sortNegatively, String outputFile, String datasetName) {
		this.sortColumnsInPhaseOne = sortColumnsInPhaseOne;
		this.sortNegatively = sortNegatively;
		this.outputFile = outputFile;
		this.datasetName = datasetName;
	}

	private long now() {
		return System.currentTimeMillis();
	}

	public void setTableReadStartTime() {
		tableReadStartTime = now();
	}

	public void setRegisterStartTime() {
		registerStartTime = now();
	}

	public void setDictionaryStartTime() {
		dictionaryStartTime = now();
	}

	public void setPhaseOneStartTime() {
		phaseOneStartTime = now();
	}

	public void setPhaseTwoStartTime() {
		phaseTwoStartTime = now();
	}

	public void setFinishTime() {
		finishTime = now();
	}

	public long getTableReadRuntime() {
		return registerStartTime - tableReadStartTime;
	}

	public long getRegisterRuntime() {
		return dictionaryStartTime - registerStartTime;
	}

	public long getDictionaryRuntime() {
		return phaseOneStartTime - dictionaryStartTime;
	}

	public long getPhaseOneRuntime() {
		return phaseTwoStartTime - phaseOneStartTime;
	}

	public long getPhaseTwoRuntime() {
		return finishTime - phaseTwoStartTime;
	}

	public long getCompleteRuntime() {
		return finishTime - tableReadStartTime;
	}

	public String toSeconds(long time) {
		return time * 0.001 + "s";
	}

	public boolean settingsSortColumnsInPhaseOne() {
		return sortColumnsInPhaseOne;
	}

	public boolean settingsSortNegatively() {
		return sortNegatively;
	}

	public String settingsOutputFile() {
		return outputFile;
	}

	public String settingsDatasetName() {
		return datasetName;
	}
}
