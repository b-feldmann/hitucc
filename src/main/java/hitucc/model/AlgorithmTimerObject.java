package hitucc.model;

import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor(force = true)
public class AlgorithmTimerObject implements Serializable {
	private long dictionaryStartTime;
	private long phaseOneStartTime;
	private long phaseTwoStartTime;
	private long finishTime;

	private long now() {
		return System.currentTimeMillis();
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
		return finishTime - dictionaryStartTime;
	}

	public String toSeconds(long time) {
		return time * 0.001 + "s";
	}
}
