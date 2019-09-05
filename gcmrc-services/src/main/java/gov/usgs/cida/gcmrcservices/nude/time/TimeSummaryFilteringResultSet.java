package gov.usgs.cida.gcmrcservices.nude.time;

import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.column.ColumnGrouping;
import gov.usgs.cida.nude.resultset.inmemory.PeekingResultSet;
import gov.usgs.cida.nude.resultset.inmemory.TableRow;
import gov.usgs.cida.nude.time.DateRange;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class TimeSummaryFilteringResultSet extends PeekingResultSet {
	private static final Logger log = LoggerFactory.getLogger(TimeSummaryFilteringResultSet.class);
	
	protected final DateRange drc;
	protected final ResultSet in;
	
	protected final LinkedList<DateTime> requestedTimes;
	protected final Period duration;
	
	protected final LinkedList<TableRow> bufferedRows;
	
	/**
	 * A set of columns exempt from time summarizing.
	 * Anytime this column has a value, it will add the row.
	 */
	protected final Set<Column> exemptCols;
	
	public TimeSummaryFilteringResultSet(ResultSet input, ColumnGrouping colGroup, Set<Column> exemptColumns, DateRange drc) {
		try {
			this.closed = input.isClosed();
		} catch (Exception e) {
			this.closed = true;
		}
		
		this.in = input;
		this.drc = drc;
		
		this.bufferedRows = new LinkedList<TableRow>();
		
		this.requestedTimes = new LinkedList<DateTime>();
		Period period = Period.ZERO;
		if (DateRange.RangeType.DISCRETE == this.drc.type) {
			this.requestedTimes.addAll(this.drc.getDiscreteTimesteps());
			period = this.drc.getAcceptibleLag();
		}
		this.duration = period;
		
		this.columns = colGroup;
		
		Set<Column> exempt = new HashSet<Column>();
		if (null != exemptColumns) {
			for (Column col : exemptColumns) {
				if (this.columns.getColumns().contains(col)) {
					exempt.add(col);
				} else {
					log.trace("Could not find exempt column '" + col.getName() + "' in ColumnGrouping");
				}
			}
		}
		this.exemptCols = Collections.unmodifiableSet(exempt);
	}
	
	@Override
	protected void addNextRow() throws SQLException {
		List<TableRow> nextRow = new LinkedList<TableRow>();
		
		if (DateRange.RangeType.DISCRETE == this.drc.type) {
			while (nextRow.isEmpty() && !in.isClosed() && !requestedTimes.isEmpty()) {
				if (in.next()) {
					//build next input row, and then sync
					TableRow nextInRow = null;
					try {
						nextInRow = TableRow.buildTableRow(in);
					} catch (Exception e) {
						log.error("Could not build TableRow from in", e);
					}

					addToNextVals(nextInRow);
				} else {
					in.close();
				}
				
				syncReqTimes();
				
				if (isReadyToFlush() || in.isClosed()) {
					nextRow.addAll(buildNextOutRows());
				} else {
					log.trace("Continue trying to get new rows.");
				}
			}
		} else {
			if (!in.isClosed()) {
				if (in.next()) {
					nextRow.add(TableRow.buildTableRow(in));
				} else {
					in.close();
				}
			}
		}
		
		if (!nextRow.isEmpty()) {
			this.nextRows.addAll(nextRow);
		} else {
			log.trace("Could not add a next row");
		}
	}
	
	protected boolean isReadyToFlush() {
		boolean result = false;
		DateTime reqTime = requestedTimes.peekFirst();
		DateTime latestTime = getPrimaryKey(bufferedRows.peekLast());
		
		if (null != reqTime && null != latestTime) {
			DateTime cutoff = reqTime.plus(duration);
			result = cutoff.compareTo(latestTime) <= 0;
		} else {
			log.trace("isReadyToFlush We ran out of something... " + "req:" + reqTime + " next:" + latestTime);
		}
		
		return result;
	}
	
	/**
	 * adds a row into the nextVals if it's after the next cutoff
	 * @param nextInRow 
	 */
	protected void addToNextVals(TableRow nextInRow) {
		if (null != nextInRow) {
			this.bufferedRows.add(nextInRow);
		}
	}
	
	protected boolean syncReqTimes() {
		boolean result = false; //things synced?
		
		if (!requestedTimes.isEmpty() && !bufferedRows.isEmpty()) {
			DateTime firstTime = getPrimaryKey(bufferedRows.peekFirst());
			
			for (DateTime reqTime = requestedTimes.peekFirst(); 
				null != reqTime 
					&& reqTime.compareTo(firstTime) <= 0 
					&& reqTime.plus(duration).compareTo(firstTime) <= 0; 
				reqTime = requestedTimes.peekFirst()) {
				requestedTimes.pollFirst();
			}
		}
		
		return result;
	}
	
	/**
	 * Creates the list of outgoing rows that abide by the following rules:
	 * <ul>
	 * <li>The row contains a temporal maximum or minimum in any column</li>
	 * <li>The row contains a non-null value in a Column exempt from summarizing</li>
	 * </ul>
	 * @return 
	 */
	protected List<TableRow> buildNextOutRows() {
		List<TableRow> result = new LinkedList<TableRow>();
		SortedSet<TableRow> sortedResult = new TreeSet<TableRow>();
		Map<Column, TableRow> maxOutRows = new HashMap<Column, TableRow>();
		Map<Column, TableRow> minOutRows = new HashMap<Column, TableRow>();
		Set<TableRow> exemptRows = new HashSet<TableRow>();
		
		for (DateTime reqTime = requestedTimes.peekFirst(); 
				sortedResult.isEmpty() && null != reqTime; 
				reqTime = requestedTimes.peekFirst()) {
			DateTime cutoffTime = reqTime.plus(duration);
			int rowsBeforeCutoffTime = -1;
			int rowsWithinTimeFrame = -1;
			for (TableRow t : bufferedRows) {
				DateTime nextTime = getPrimaryKey(t);
				if (cutoffTime.compareTo(nextTime) > 0) {
					rowsBeforeCutoffTime++;
					if (reqTime.compareTo(nextTime) <= 0) {
						rowsWithinTimeFrame++;
						syncMaxesAndMins(maxOutRows, minOutRows, t);
						syncExemptColumns(exemptRows, t);
					} else {
						log.trace("our next time is not a part of this time period.");
					}
				} else {
					log.trace("reqTime is before nextTime");
				}
			}
			
				if (-1 < rowsWithinTimeFrame) {
					sortedResult.addAll(maxOutRows.values());
					sortedResult.addAll(minOutRows.values());
					sortedResult.addAll(exemptRows);
			} else { 
				log.trace("we didn't get anything this time");
			}
			
			//clean up all vals before the cutoff
			for (; -1 < rowsBeforeCutoffTime; rowsBeforeCutoffTime--) {
				bufferedRows.removeFirst();
			}
			
			requestedTimes.pollFirst();
		}
		
		result.addAll(sortedResult);
		
		return result;
	}
	
	/**
	 * Uses this row as the starting point for all columns to calculate their
	 * temporal minimums and maximums
	 * @param outRow
	 * @param t 
	 */
	protected void initOutRow(Map<Column, TableRow> outRow, TableRow t) {
		for (Column col : this.columns.getColumns()) {
			if (!col.equals(this.columns.getPrimaryKey())) {
				outRow.put(col, t);
			}
		}
	}
	
	/**
	 * ugh... Operates via Side Effects!
	 * 
	 * Takes the earliest min/max
	 * @param maxOutRow
	 * @param minOutRow
	 * @param t 
	 */
	protected void syncMaxesAndMins(Map<Column, TableRow> maxOutRow, Map<Column, TableRow> minOutRow, TableRow t) {
		if (maxOutRow.isEmpty()) {
			initOutRow(maxOutRow, t);
		}
		if (minOutRow.isEmpty()) {
			initOutRow(minOutRow, t);
		}
		for (Column col : this.columns.getColumns()) {
			if (!col.equals(this.columns.getPrimaryKey())) {
				TableRow currMaxRow = max(col, maxOutRow.get(col), t);
				maxOutRow.put(col, currMaxRow);
				
				TableRow currMinRow = min(col, minOutRow.get(col), t);
				minOutRow.put(col, currMinRow);
			}
		}
	}
	
	protected void syncExemptColumns(Set<TableRow> exemptRows, TableRow t) {
		for (Column col : this.exemptCols) {
			if (null != t && null != t.getValue(col)) {
				exemptRows.add(t);
			}
		}
	}
	
	protected TableRow max(Column col, TableRow a, TableRow b) {
		TableRow result = a;
		
		String aVal = StringUtils.trimToNull(a.getValue(col));
		String bVal = StringUtils.trimToNull(b.getValue(col));

		try {
			if (null == aVal || (null != bVal && (new BigDecimal(bVal)).compareTo(new BigDecimal(aVal)) > 0)) {
				result = b;
			}
		} catch (Exception e) {
			log.trace("Exception for aVal:" + aVal + " bVal:" + bVal, e);
		}
		
		
		return result;
	}
	
	protected TableRow min(Column col, TableRow a, TableRow b) {
		TableRow result = a;
		
		String aVal = StringUtils.trimToNull(a.getValue(col));
		String bVal = StringUtils.trimToNull(b.getValue(col));

		try {
			if (null == aVal || (null != bVal && (new BigDecimal(bVal)).compareTo(new BigDecimal(aVal)) < 0)) {
				result = b;
			}
		} catch (Exception e) {
			log.trace("Exception for aVal:" + aVal + " bVal:" + bVal, e);
		}
		
		return result;
	}

	@Override
	public void close() throws SQLException {
		try {
			in.close();
		} finally {
			this.closed = true;
		}
	}

	@Override
	public String getCursorName() throws SQLException {
		throwIfClosed(this);
		return in.getCursorName();
	}
	
	public static DateTime getPrimaryKey(TableRow t) {
		DateTime result = null;
		
		if (null != t) {
			Column primaryKeyCol = t.getColumns().getPrimaryKey();
			String primKeyStr = t.getValue(primaryKeyCol);
			Long primKeyLong = new Long(primKeyStr);
			
			result = new DateTime(primKeyLong);
		}
		
		return result;
	}
}
