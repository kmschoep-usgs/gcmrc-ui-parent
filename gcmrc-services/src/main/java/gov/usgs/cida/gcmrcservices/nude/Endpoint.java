package gov.usgs.cida.gcmrcservices.nude;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import gov.usgs.cida.gcmrcservices.TimeUtil;
import gov.usgs.cida.gcmrcservices.column.ColumnResolver;
import gov.usgs.cida.gcmrcservices.nude.time.TimeConfig;
import gov.usgs.cida.nude.out.Closers;
import gov.usgs.cida.nude.out.Dispatcher;
import gov.usgs.cida.nude.out.StreamResponse;
import gov.usgs.cida.nude.out.TableResponse;
import gov.usgs.cida.nude.out.mapping.ColumnToXmlMapping;
import gov.usgs.cida.nude.plan.Plan;
import gov.usgs.cida.nude.provider.sql.SQLProvider;
import gov.usgs.cida.nude.time.DateRange;
import gov.usgs.webservices.framework.basic.MimeType;

/**
 *
 * @author dmsibley
 */
public abstract class Endpoint extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(Endpoint.class);
	public static final String COLUMN_KEYWORD = "column[]";
	public static final String BEGIN_KEYWORD = "beginPosition";
	public static final String END_KEYWORD = "endPosition";
	/**
	 * determines the acceptable gap for linearly interpolating
	 */
	public static final String EVERY_KEYWORD = "every";
	/**
	 * the time period over which to run the downscale algorithm (min/max by default)
	 */
	public static final String DOWNSCALE_KEYWORD = "downscale";
	public static final String NODATA_FILTER_KEYWORD = "noDataFilter";
	public static final String INTERVAL_KEYWORD = "timeInt";
	
	public static final String DOWNLOAD_KEYWORD = "download";
	public static final String OUTPUT_FORMAT_KEYWORD = "output";
	public static final String TIME_FORMAT_KEYWORD = "timeFormat";
	public static final String TIMEZONE_KEYWORD = "tz";
	public static final String TIMEZONE_IN_HEADER_KEYWORD = "tzInHeader";
	public static final String CUTOFF_AFTER_KEYWORD = "cutoffAfter";
	public static final String CUTOFF_BEFORE_KEYWORD = "cutoffBefore";
	
	protected SQLProvider sqlProvider;
	protected ColumnResolver resolver;
	
	@Override
	public void init() throws ServletException {
		sqlProvider = new SQLProvider("java:comp/env/jdbc/gcmrcDS");
		sqlProvider.init();
		super.init();
	}
	
	@Override
	public void destroy() {
		if (null != sqlProvider) {
			sqlProvider.destroy();
		}
		super.destroy();
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UUID requestUUID = UUID.randomUUID();
		try {
			resolver = new ColumnResolver(sqlProvider);

			log.info("Started Request " + requestUUID.toString() + " " + req.getRequestURI() + "?" + req.getQueryString());

			Map<String, String[]> reqArrayMap = req.getParameterMap();
			ListMultimap<String, String> reqMap = ArrayListMultimap.<String, String>create();
			for (Map.Entry<String, String[]> ent : reqArrayMap.entrySet()) {
				reqMap.putAll(ent.getKey(), Arrays.asList(ent.getValue()));
			}

			Plan plan = createPlan(requestUUID, reqMap);
			OutputConfig outConfig = createOutputConfig(reqMap);

			dispatchPlan(plan, resp, outConfig);
		} finally {
			SQLProvider.closeConnection(requestUUID);
			log.info("Ended Request " + requestUUID.toString());
		}
	}
	
	public abstract Plan createPlan(UUID requestId, ListMultimap<String, String> params);
	
	public OutputConfig createOutputConfig(ListMultimap<String, String> params) {
		OutputConfig result = null;

		String fill = null;

		ColumnToXmlMapping[] columnMappings = createSpecifiedColumnMappings(params);

		boolean isDownload = getIsDownload(params);

		String userMime = getParameter(params, OUTPUT_FORMAT_KEYWORD, "tab");
		log.debug(userMime);
		MimeType mime = MimeType.lookup(userMime);
		if (null == mime) {
			mime = MimeType.TAB;
		}
		log.debug(mime.getMimeType());
		

		result = new OutputConfig(fill, columnMappings, false, isDownload, mime);

		return result;
	}
	
	public abstract ColumnToXmlMapping[] createSpecifiedColumnMappings(ListMultimap<String, String> params);
	
	public static String getParameter(ListMultimap<String, String> map, String key) {
		return getParameter(map, key, null);
	}

	public static String getParameter(ListMultimap<String, String> map, String key, String defaultValue) {
		String result = null;

		List<String> vals = map.get(key);
		if (null != vals && 0 < vals.size()) {
			result = StringUtils.trimToNull(vals.get(0));
		}

		if (null == result) {
			result = defaultValue;
		}

		return result;
	}
	
	public static String getOptionParameter(ListMultimap<String, String> map, String key, String defaultValue, String... otherOptions) {
		String result = null;
		
		String pendingResult = getParameter(map, key, defaultValue);
		
		if (null != otherOptions && 0 < otherOptions.length) {
			if (null == pendingResult || !(StringUtils.equals(defaultValue, pendingResult) || Arrays.asList(otherOptions).contains(pendingResult))) {
				log.trace("invalid option '" + pendingResult + "'. setting to default '" + defaultValue + "'.");
				pendingResult = defaultValue;
			} else {
				log.trace("valid option " + pendingResult);
			}
		} else {
			log.trace("no other options");
		}
		
		result = pendingResult;
		
		return result;
	}
	
	public static List<String> getStations(ListMultimap<String, String> params) {
		List<String> result = new ArrayList<String>();
		Set<String> toGo = new HashSet<String>();
		
		List<String> columnNames = params.get(COLUMN_KEYWORD);
		for (String colName : columnNames) {
			String station = ColumnResolver.getStation(colName);
			if (null != station) {
				toGo.add(station);
			}
		}
		
		result.addAll(toGo);
		return result;
	}

	public static void dispatchPlan(Plan plan, HttpServletResponse resp, OutputConfig outConfig) throws IOException {
		ResultSet rsResp = null;
		Writer out = null;
		try {
			resp.setCharacterEncoding("utf-8");
			resp.setContentType(outConfig.getMimeType());

			if (outConfig.isDownload()) {
				resp.setHeader("Content-Disposition", "attachment; filename=\"gcmrc"
						+ DateTime.now().toString(TimeUtil.FILENAME_FORMAT) + "." + outConfig.getStyle().getFileSuffix() + "\"");
			}
			resp.flushBuffer();
			out = resp.getWriter();

			try {
				rsResp = Plan.runPlan(plan);
			} catch (Exception e) {
				log.error("Could not run plan!", e);
			}

			if (null != rsResp) {
				TableResponse tr = new TableResponse(rsResp, outConfig.getFill(), outConfig.getColumnMappings(), outConfig.isTransparentMode());
				StreamResponse sr = null;
				try {
					sr = Dispatcher.buildFormattedResponse(outConfig.getStyle(), tr);
				} catch (SQLException ex) {
					log.error("Exception while creating StreamResponse", ex);
				} catch (XMLStreamException ex) {
					log.error("Exception while creating StreamResponse", ex);
				}

				if (null != sr && null != out) {
					StreamResponse.dispatch(sr, out);
					out.flush();
				}
			} else {
				log.debug("Tried dispatching null ResultSet");
			}
		} finally {
			Closers.closeQuietly(rsResp);
			Closers.closeQuietly(out);
		}
	}

	public static TimeConfig getDateRange(ListMultimap<String, String> params) {
		TimeConfig result = null;

		/**
		 * Date range config.
		 */
		String beginTime = getParameter(params, BEGIN_KEYWORD);
		String endTime = getParameter(params, END_KEYWORD);
		String every = getParameter(params, EVERY_KEYWORD);
		String downscale = getParameter(params, DOWNSCALE_KEYWORD);
		
		String cutoffBefore = getParameter(params, CUTOFF_BEFORE_KEYWORD);
		String cutoffAfter = getParameter(params, CUTOFF_AFTER_KEYWORD);
		
		String timeInt = getParameter(params, INTERVAL_KEYWORD);
		int acceptPeriod = 24;
		if (!StringUtils.isEmpty(timeInt)) {
			acceptPeriod = Integer.parseInt(timeInt);
		}
		
		int tz = -7;
		String timezone = getParameter(params, TIMEZONE_KEYWORD);
		if (!StringUtils.isEmpty(timezone)) {
			try {
				tz = Integer.parseInt(timezone);
			} catch (NumberFormatException e) {
				log.debug("Timezone '" + timezone + "' is not valid.");
			}
		} else {
			log.debug("Using default timezone -7");
		}
		
		DateTimeZone dtz = DateTimeZone.forOffsetHours(tz);
		
		DateRange dateRange = null;
		dateRange = DateRange.parseContinuousDateRange(beginTime, endTime, TimeUtil.INPUT_DATE_FORMAT, dtz);
		
		DateRange interpDateRange = null;
		if (StringUtils.isNotEmpty(every)) {
			//Prefill seems to act as a time shift? really freakin weird.  give it ZERO
			interpDateRange = DateRange.createRegularDiscreteDateRange(dateRange, every, acceptPeriod, Period.ZERO, dtz);
		}

		DateRange downscaleDateRange = null;
		if (StringUtils.isNotEmpty(downscale)) {
			downscaleDateRange = DateRange.createRegularDiscreteDateRange(dateRange, downscale, acceptPeriod, Period.ZERO, dtz);
		}
		
		DateTime cutoffBeforeDT = null;
		DateTime cutoffAfterDT = null;
		
		try {
			if (null != cutoffBefore) {
				cutoffBeforeDT = DateTime.parse(cutoffBefore + "-0700", ISODateTimeFormat.dateTimeNoMillis());
			}
		} catch (Exception e) {
			log.debug("couldn't parse cutoffBefore: " + cutoffBefore);
		}
		try {
			if (null != cutoffAfter) {
				cutoffAfterDT = DateTime.parse(cutoffAfter + "-0700", ISODateTimeFormat.dateTimeNoMillis());
			}
		} catch (Exception e) {
			log.debug("couldn't parse cutoffAfter: " + cutoffAfter);
		}
		
		result = new TimeConfig(dateRange, interpDateRange, downscaleDateRange, cutoffBeforeDT, cutoffAfterDT, tz);
		
		return result;
	}
	
	public static boolean getIsDownload(ListMultimap<String, String> params) {
		boolean result = false;
		
		String userDownload = getParameter(params, DOWNLOAD_KEYWORD, "false");
		if (!StringUtils.equalsIgnoreCase(userDownload, "false")) {
			result = true;
		}
		
		return result;
	}
}
