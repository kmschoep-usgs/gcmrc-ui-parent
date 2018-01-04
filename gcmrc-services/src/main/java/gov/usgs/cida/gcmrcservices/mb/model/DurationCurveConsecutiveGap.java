package gov.usgs.cida.gcmrcservices.mb.model;

/**
 *
 * @author kmschoep
 */
public class DurationCurveConsecutiveGap {
	
	private String gapTime;
	private String gapUnit;

	public DurationCurveConsecutiveGap() {
	}
	
	public DurationCurveConsecutiveGap(String gapTime, String gapUnit) {
		this.gapTime = gapTime;
		this.gapUnit = gapUnit;
	}
	
	public String getGapTime() {
		return gapTime;
	}
	
	public String getGapUnit() {
		return gapUnit;
	}
	
	public void setGapTime(String val) {
		gapTime = val;
	}
	
	public void setGapUnit(String val) {
		gapUnit = val;
	}
}
