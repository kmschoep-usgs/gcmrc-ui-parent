package gov.usgs.cida.gcmrcservices.mb.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zmoore
 */
public class DurationCurve {	
	private final List<DurationCurvePoint> points;
	private final int groupId;
	private final String siteName;
	private final String binType;
	private final String gapMinutesPercent;
	private final DurationCurveConsecutiveGap consecutiveGap;

	public DurationCurve(List<DurationCurvePoint> pts, String site, int group, String bin, String gapMinutes, DurationCurveConsecutiveGap  consecGap) {
		if(pts == null) {
			points = new ArrayList<>();
		} else {
			points = pts;
		}
		groupId = group;
		siteName = site;
		binType = bin.equalsIgnoreCase("LOG_BINS") ? "log" : "lin";
		gapMinutesPercent = gapMinutes;
		consecutiveGap = consecGap;
	}
	
	public List<DurationCurvePoint> getPoints() {
		return points;
	}
	
	public int getGroupId() {
		return groupId;
	}
	
	public String getBinType() {
		return binType;
	}
	
	public String getSiteName() {
		return siteName;
	}
	
	public String getGapMinutesPercent() {
		return gapMinutesPercent;
	}
	
	public DurationCurveConsecutiveGap getConsecutiveGap() {
		return consecutiveGap;
	}
	
	public List<Double> extractBinValues() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getBinValue());
		}
		
		return toReturn;
	}
	
	public List<Double> extractCumulativeBinPercs() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getCumulativeBinPerc());
		}
		
		return toReturn;
	}
	
	public List<Double> extractInBinMinutes() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getInBinMinutes());
		}
		
		return toReturn;
	}
	
	public List<Double> extractCumulativeInBinMinutes() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getCumulativeInBinMinutes());
		}
		
		return toReturn;
	}
	
	public List<Double> extractLowBounds() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getLowBound());
		}
		
		return toReturn;
	}
	
	public List<Double> extractHighBounds() {
		List<Double> toReturn = new ArrayList<>();
		
		for(DurationCurvePoint point : points){
			toReturn.add(point.getHighBound());
		}
		
		return toReturn;
	}
}
