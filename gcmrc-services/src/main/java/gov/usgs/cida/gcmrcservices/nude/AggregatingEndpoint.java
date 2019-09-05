package gov.usgs.cida.gcmrcservices.nude;

import static gov.usgs.cida.gcmrcservices.TimeUtil.TZ_CODE_LOOKUP;
import static gov.usgs.cida.gcmrcservices.column.ColumnResolver.getCustomName;
import static gov.usgs.cida.gcmrcservices.column.ColumnResolver.getStation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import gov.usgs.cida.gcmrcservices.column.ColumnMetadata;
import gov.usgs.cida.gcmrcservices.column.ColumnMetadata.SpecEntry;
import gov.usgs.cida.gcmrcservices.nude.time.CutoffTimesPlanStep;
import gov.usgs.cida.gcmrcservices.nude.time.TimeColumnReq;
import gov.usgs.cida.gcmrcservices.nude.time.TimeConfig;
import gov.usgs.cida.gcmrcservices.nude.time.TimeSummaryFilteringPlanStep;
import gov.usgs.cida.gcmrcservices.nude.transform.ReplaceValueTransform;
import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.column.ColumnGrouping;
import gov.usgs.cida.nude.filter.ColumnTransform;
import gov.usgs.cida.nude.filter.FilterStageBuilder;
import gov.usgs.cida.nude.filter.FilterStep;
import gov.usgs.cida.nude.filter.NudeFilter;
import gov.usgs.cida.nude.filter.NudeFilterBuilder;
import gov.usgs.cida.nude.out.mapping.ColumnToXmlMapping;
import gov.usgs.cida.nude.plan.PlanStep;
import gov.usgs.cida.nude.resultset.inmemory.TableRow;
import gov.usgs.cida.nude.time.DateRange;
import gov.usgs.webservices.jdbc.spec.Spec;

/**
 *
 * @author dmsibley
 */
public class AggregatingEndpoint extends SpecEndpoint {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(AggregatingEndpoint.class);
	
	public static final String BAD_DATA_KEYWORD = "BAD_BAD_DATA";
	public static final String MINUS_999 = "-999";
	
	@Override
	public List<PlanStep> configurePlan(UUID requestId, List<String> stations, Iterable<Spec> specs, Multimap<Column, Column> mux, TimeConfig timeConfig, boolean noDataFilter) {
		LinkedList<PlanStep> result = new LinkedList<PlanStep>();
		
		result.add(new DBConnectorPlanStep(time, specs, sqlProvider, requestId));
			
		Collection<Column> muxedCols = mux.values();
		List<Column> interestingColumns = new LinkedList<Column>();
		interestingColumns.add(time);
		interestingColumns.addAll(muxedCols);
		
		DateRange requestedCutoff = new DateRange(timeConfig.getCutoffBefore(), timeConfig.getCutoffAfter());
		result.add(new CutoffTimesPlanStep(time, new ColumnGrouping(interestingColumns), requestedCutoff));
		
		if (noDataFilter) {
			result.add(new FilterStep(buildReplaceValueFilter(new ColumnGrouping(interestingColumns), interestingColumns, MINUS_999, BAD_DATA_KEYWORD)));
		}
		
		Set<Column> exemptColumns = new HashSet<Column>();
		for (String station : stations) {
			for (Entry<String, ColumnMetadata> ent : resolver.buildQErrorCols().entrySet()) {
				exemptColumns.addAll(getExemptColumns(ent.getValue(), station));
			}
			for (Entry<String, ColumnMetadata> ent : resolver.buildQWParametersCols().entrySet()) {
				exemptColumns.addAll(getExemptColumns(ent.getValue(), station));
			}
			for (Entry<String, ColumnMetadata> ent : resolver.buildBedMaterialParametersCols().entrySet()) {
				exemptColumns.addAll(getExemptColumns(ent.getValue(), station));
			}
		}
		
		NudeFilterBuilder offsettingLoadBuilder = new NudeFilterBuilder(new ColumnGrouping(interestingColumns));
		offsettingLoadBuilder.addFilterStage(new FilterStageBuilder(offsettingLoadBuilder.getCurrOutCols()).buildFilterStage());
		for (String station : stations) {
			for (Entry<String, ColumnMetadata> ent : resolver.buildCumulativeParametersCols().entrySet()) {
				final Column derivedMux = ent.getValue().getColumn(station);
				final Column derivedCol = ent.getValue().getInternalColumn(station);
				if (mux.keySet().contains(derivedMux)) {
					offsettingLoadBuilder.addFilterStage(new FilterStageBuilder(offsettingLoadBuilder.getCurrOutCols())
							.addTransform(derivedCol, new ColumnTransform() {
								private BigDecimal offset = null;
								
								@Override
								public String transform(TableRow row) {
									String result = null;
									
									String raw = row.getValue(derivedCol);
									if (StringUtils.isNotBlank(raw)) {
										BigDecimal in = new BigDecimal(raw);
										
										if (null == this.offset) {
											this.offset = in;
										}
										
										if (null != this.offset) {
											result = in.subtract(this.offset).toPlainString();
										}
									}
									
									return result;
								}
							})
							.buildFilterStage());
				}
			}
		}
		result.add(new FilterStep(offsettingLoadBuilder.buildFilter()));
		
		
		if (null != timeConfig.getInterp()) {
			result.add(new InterpolatingFilterPlanStep(new ColumnGrouping(interestingColumns), exemptColumns, BAD_DATA_KEYWORD, timeConfig.getInterp().getEvery()));
		}
		
		if (null != timeConfig.getDownscale()) {
			result.add(new TimeSummaryFilteringPlanStep(new ColumnGrouping(interestingColumns), exemptColumns, timeConfig.getDownscale()));
		}
		
		String badOutput = MINUS_999;
		if (noDataFilter) {
			badOutput = null;
		}
		result.add(new FilterStep(buildReplaceValueFilter(new ColumnGrouping(interestingColumns), interestingColumns, BAD_DATA_KEYWORD, badOutput)));
		
		return result;
	}
	
	protected static NudeFilter buildReplaceValueFilter(ColumnGrouping outColumns, Collection<Column> columnsToFilter, String inValue, String outValue) {
		NudeFilterBuilder result = new NudeFilterBuilder(outColumns);
		
		FilterStageBuilder ndfsb = new FilterStageBuilder(result.getCurrOutCols());
		for(Column c : columnsToFilter) {
			ndfsb.addTransform(c, new ReplaceValueTransform(c, inValue, outValue));
		}
		result.addFilterStage(ndfsb.buildFilterStage());
		
		return result.buildFilter();
	}
	
	protected Set<Column> getExemptColumns(ColumnMetadata col, String station) {
		Set<Column> result = new HashSet<Column>();
		if (null != col && null != station) {
			for (SpecEntry ent : col.getSpecEntries()) {
				result.add(ent.getColumn(station));
			}
		}
		return result;
	}

	@Override
	public ColumnToXmlMapping[] createSpecifiedColumnMappings(ListMultimap<String, String> params) {
		LinkedList<ColumnToXmlMapping> colMaps = new LinkedList<ColumnToXmlMapping>();

		boolean isDownload = getIsDownload(params);
		boolean timezoneInHeader = !"false".equals(getParameter(params, TIMEZONE_IN_HEADER_KEYWORD, "false"));
		TimeConfig timeConfig = getDateRange(params);
		
		String timeDisplayName = "time";
		if (timezoneInHeader) {
			String timezoneCode = TZ_CODE_LOOKUP.get(-7);
			
			String tzStr = getParameter(params, TIMEZONE_KEYWORD);
			if (StringUtils.isNotBlank(tzStr)) {
				try {
					int timezone = Integer.parseInt(tzStr);
					String tzCode = TZ_CODE_LOOKUP.get(timezone);
					if (null != tzCode) {
						timezoneCode = tzCode;
					} else {
						log.debug("Unknown tzcode");
					}
				} catch (NumberFormatException e) {
					log.debug("Timezone " + tzStr + " is invalid.");
				}
			}
			
			timeDisplayName = "time (" + timezoneCode + ")";
		}
		
		List<String> userCols = params.get(COLUMN_KEYWORD);
		for (String colName : userCols) {
			ColumnMetadata cmd = resolver.resolveColumn(colName);
			String station = getStation(colName);
			String customName = getCustomName(colName);
			
			if (null != cmd && null != station) {
				colMaps.add(cmd.getMapping(station, isDownload, customName));
			} else {
				//try time
				TimeColumnReq timeCol = TimeColumnReq.parseTimeColumn(colName, timeDisplayName, timeConfig.getTimezone());
				
				if (null != timeCol) {
					colMaps.add(timeCol.getMapping());
				} else {
					log.debug("No column by the name of: " + colName);
				}
			}
		}
		
		if (colMaps.isEmpty()) {
			colMaps.add(new ColumnToXmlMapping("ERROR","ERROR"));
		}
		
		return colMaps.toArray(new ColumnToXmlMapping[0]);
	}
	
}
