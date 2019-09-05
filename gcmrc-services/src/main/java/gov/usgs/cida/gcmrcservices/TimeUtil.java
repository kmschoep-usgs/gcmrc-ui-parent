package gov.usgs.cida.gcmrcservices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

/**
 *
 * @author dmsibley
 */
public class TimeUtil {

	private static final Logger log = LoggerFactory.getLogger(TimeUtil.class);
	public static final DateTimeZone AZ_TZ = DateTimeZone.forOffsetHours(-7);
	public static final Map<Integer, String> TZ_CODE_LOOKUP;
	public static final DateTimeFormatter INPUT_DATE_FORMAT;
	public static final DateTimeFormatter FILENAME_FORMAT;
	public static final DateTimeFormatter DB_DATE_FORMAT;

	static {
		INPUT_DATE_FORMAT = ISODateTimeFormat.date();

		DateTimeFormatterBuilder d = new DateTimeFormatterBuilder();
		d.appendYear(4, 4);
		d.appendMonthOfYear(2);
		d.appendDayOfMonth(2);
		d.appendHourOfDay(2);
		d.appendMinuteOfHour(2);
		d.appendSecondOfMinute(2);

		FILENAME_FORMAT = d.toFormatter();

		d = new DateTimeFormatterBuilder();
		d.appendYear(4, 4);
		d.appendLiteral("-");
		d.appendMonthOfYear(2);
		d.appendLiteral("-");
		d.appendDayOfMonth(2);
		d.appendLiteral("T");
		d.appendHourOfDay(2);
		d.appendLiteral(":");
		d.appendMinuteOfHour(2);
		d.appendLiteral(":");
		d.appendSecondOfMinute(2);

		DB_DATE_FORMAT = d.toFormatter();
		
		Map<Integer, String> tzLookup = new HashMap<Integer, String>();
		
		tzLookup.put(-5, "EST");
		tzLookup.put(-6, "CST");
		tzLookup.put(-7, "MST");
		tzLookup.put(-8, "PST");
		
		TZ_CODE_LOOKUP = Collections.unmodifiableMap(tzLookup);
	}

	public static DateTimeFormatter getDateFormatter(String key, Integer tz) {
		DateTimeFormatter result = TimeFormats.ISONOZONE.getFormat();
		
		String cleanName = StringUtils.trimToNull(key);
		if (null != cleanName) {
			cleanName = StringUtils.upperCase(cleanName);
			try {
				result = TimeFormats.valueOf(cleanName).getFormat();
			} catch (Exception e) {
				log.debug("No Date Format associated with: " + cleanName);
				try {
					result = DateTimeFormat.forPattern(key);
				} catch (Exception ex) {
					log.info("Could not parse date format: " + key);
				}
			}
		}
		
		if (null != result && null != tz) { 
			result = result.withZone(DateTimeZone.forOffsetHours(tz));
		}
		
		return result;
	}
	
	protected enum TimeFormats {
		ISO(ISODateTimeFormat.dateTimeNoMillis()),
		ISONOZONE(TimeUtil.DB_DATE_FORMAT),
		UTCMILLIS(null);

		private TimeFormats(DateTimeFormatter dtf) {
			this.format = dtf;
		}
		
		protected DateTimeFormatter format;

		public DateTimeFormatter getFormat() {
			return this.format;
		}
	}
}
