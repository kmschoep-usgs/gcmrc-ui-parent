package gov.usgs.cida.gcmrcservices.nude.time;

import gov.usgs.cida.gcmrcservices.TimeUtil;
import gov.usgs.cida.gcmrcservices.jsl.data.ParameterSpec;
import gov.usgs.cida.nude.out.mapping.ColumnToXmlMapping;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class TimeColumnReq {
	private static final Logger log = LoggerFactory.getLogger(TimeColumnReq.class);
	
	public static final String TIME_COLUMN_NAME = "time";
	
	protected final String displayName;
	protected final String timeFormat;
	protected final Integer timezoneOffset;

	public TimeColumnReq(String displayName, String timeFormat, Integer timezoneOffset) {
		this.displayName = displayName;
		if (null == timeFormat) {
			timeFormat = "ISONOZONE";
		}
		this.timeFormat = timeFormat;
		this.timezoneOffset = timezoneOffset;
	}
	
	public ColumnToXmlMapping getMapping() {
		ColumnToXmlMapping result = null;
		
		String inName = ParameterSpec.C_TSM_DT;
		
		if (null != this.getFormatter()) {
			 inName = inName + this.getFormat().hashCode();
		}
		
		result = new ColumnToXmlMapping(inName, displayName);
		
		return result;
	}
	
	public String getFormat() {
		return this.timeFormat;
	}
	
	public DateTimeFormatter getFormatter() {
		DateTimeFormatter result = TimeUtil.getDateFormatter(this.timeFormat, this.timezoneOffset);
		
		return result;
	}
	
	public static TimeColumnReq parseTimeColumn(String colName, String displayName, Integer timezoneOffset) {
		TimeColumnReq result = null;
		
		String cleanCol = StringUtils.trimToNull(colName);
		String cleanDisplayName = StringUtils.trimToNull(displayName);
		if (null == cleanDisplayName) {
			cleanDisplayName = TIME_COLUMN_NAME;
		}
		
		if (null != cleanCol && StringUtils.startsWithIgnoreCase(cleanCol, TIME_COLUMN_NAME)) {
			String[] cleanSplit = cleanCol.split("!");
			String timeFormat = null;
			if (1 < cleanSplit.length) {
				timeFormat = StringUtils.trimToNull(cleanSplit[1]);
			}
			if (2 < cleanSplit.length && null != StringUtils.trimToNull(cleanSplit[2])) {
				String parsedDisplayName = StringUtils.trimToNull(cleanSplit[2]);
				cleanDisplayName = parsedDisplayName.replace("*default*", cleanDisplayName);
			}
			result = new TimeColumnReq(cleanDisplayName, timeFormat, timezoneOffset);
		} else {
			log.trace("This isn't a time column request: " + cleanCol);
		}
		
		return result;
	}
}
