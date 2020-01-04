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

	private long minimizeStartTime;

	private boolean singleNodeSingleWorker;

	private boolean sortColumnsInPhaseOne;
	private boolean sortNegatively;
	private String outputFile;
	private String datasetName;
	private int maxTreeDepth;

	public AlgorithmTimerObject(boolean sortColumnsInPhaseOne, boolean sortNegatively, String outputFile, String datasetName, int maxTreeDepth, boolean singleNodeSingleWorker) {
		this.sortColumnsInPhaseOne = sortColumnsInPhaseOne;
		this.sortNegatively = sortNegatively;
		this.outputFile = outputFile;
		this.datasetName = datasetName;
		this.maxTreeDepth = maxTreeDepth;
		this.singleNodeSingleWorker = singleNodeSingleWorker;
	}

	public AlgorithmTimerObject clone() {
		AlgorithmTimerObject timerObject = new AlgorithmTimerObject(sortColumnsInPhaseOne, sortNegatively, outputFile, datasetName, maxTreeDepth, singleNodeSingleWorker);
		timerObject.tableReadStartTime = tableReadStartTime;
		timerObject.registerStartTime = registerStartTime;
		timerObject.dictionaryStartTime = dictionaryStartTime;
		timerObject.phaseOneStartTime = phaseOneStartTime;
		timerObject.phaseTwoStartTime = phaseTwoStartTime;
		timerObject.finishTime = finishTime;
		timerObject.minimizeStartTime = minimizeStartTime;
		return timerObject;
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

	public void setMinimizeStartTime() {
		minimizeStartTime = now();
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

	public long getPhaseOneCreateRuntime() {
		if(!singleNodeSingleWorker) return 0;
		return minimizeStartTime - phaseOneStartTime;
	}

	public long getPhaseOneMinimizeRuntime() {
		if(!singleNodeSingleWorker) return 0;
		return phaseTwoStartTime - minimizeStartTime;
	}

	public long getPhaseTwoRuntime() {
		return finishTime - phaseTwoStartTime;
	}

	public long getCompleteRuntime() {
		return finishTime - tableReadStartTime;
	}

	public String toSeconds(long time) {
		return time * 0.001 + "s";
//		return time + "s";
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

	public int settingsMaxTreeDepth() {
		return maxTreeDepth;
	}
}
