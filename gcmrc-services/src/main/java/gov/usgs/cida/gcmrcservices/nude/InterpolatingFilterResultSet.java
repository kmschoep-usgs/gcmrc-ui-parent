package gov.usgs.cida.gcmrcservices.nude;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import gov.usgs.cida.gcmrcservices.TableRowUtil;
import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.column.ColumnGrouping;
import gov.usgs.cida.nude.resultset.inmemory.PeekingResultSet;
import gov.usgs.cida.nude.resultset.inmemory.TableRow;

/**
 *
 * @author dmsibley
 */
public class InterpolatingFilterResultSet extends PeekingResultSet {
	private static final Logger log = LoggerFactory.getLogger(InterpolatingFilterResultSet.class);
	
	protected ResultSet in;
	protected Period gapLimit;
	protected LinkedList<TableRow> queuedRows;
	protected Map<Column, Tuple<Long, BigDecimal>> lastVals;
	
	/**
	 * A set of columns exempt from time summarizing.
	 * Anytime this column has a value, it will add the row.
	 */
	protected final Set<Column> exemptCols;
	protected final String exemptValueKeyword;
	
	public InterpolatingFilterResultSet(ResultSet input, ColumnGrouping cg, Set<Column> exemptColumns, String exemptValueKeyword, Period gapLimit) {
		try {
			this.closed = input.isClosed();
		} catch (Exception e) {
			this.closed = true;
		}
		
		this.in = input;
		this.columns = cg;
		
		this.gapLimit = gapLimit;
		
		queuedRows = new LinkedList<TableRow>();
		
		lastVals = new HashMap<Column, Tuple<Long, BigDecimal>>();
		for (Column col : cg) {
			if (!cg.getPrimaryKey().equals(col)) {
				lastVals.put(col, null);
			}
		}
		
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
		
		this.exemptValueKeyword = exemptValueKeyword;
	}
	
	protected static TableRow badDataFill(TableRow fillThis, TableRow checkFromThis, String badDataVal) {
		TableRow result = fillThis;
		// **Forward** Bad Data Fill
		if (null != fillThis && null != checkFromThis) {
			for (Entry<Column, String> ent : checkFromThis.getEntries()) {
				String nowVal = result.getValue(ent.getKey());
				if (null == nowVal && StringUtils.equals(badDataVal, ent.getValue())) {
					result = TableRowUtil.replace(result, ent.getKey(), badDataVal);
				} 
			}
		}
		return result;
	}

	@Override
	protected void addNextRow() throws SQLException {
		/*
		 * See GCMRC-353 for why there are two (a forward and a backward) bad data fills spaced out here
		 */
		boolean nothingSentOut = true;
		while(nothingSentOut && in.next()) {
			if (!in.isAfterLast()) {
				TableRow now = TableRow.buildTableRow(in);
				if (!queuedRows.isEmpty()) {
					now = badDataFill(now, queuedRows.getLast(), this.exemptValueKeyword);
				} else {
					log.trace("no queued rows to fill bad data from");
				}
				//Make a new row in queuedRows
				queuedRows.add(now);
				
				//Go through the columns with values:
				Column pkCol = now.getColumns().getPrimaryKey();
				Long pkVal = new Long(now.getValue(pkCol));
//				log.trace("added " + pkVal);
				for (Entry<Column, String> ent : now.getEntries()) {
					if (StringUtils.isNotEmpty(ent.getValue()) &&
						!pkCol.equals(ent.getKey())) {
						try {
							Column col = ent.getKey();
							BigDecimal nowVal = new BigDecimal(ent.getValue());
							Tuple<Long, BigDecimal> nowTup = buildTuple(pkVal, nowVal);
							//and check between lastvals and now for needed interp entries
							Tuple<Long, BigDecimal> lastTup = lastVals.get(col);
							if (lastTup != null) {
								TableRow itTR = null;
								Long itPk = null;
								boolean isDone = false;
								LinkedList<TableRow> q = new LinkedList<TableRow>(this.queuedRows);
								while (!isDone && 0 < q.size()) {
									itTR = q.pollLast();
									itPk = new Long(itTR.getValue(pkCol));
									if (!lastTup.getX().equals(itPk)) {
										if (!nowTup.getX().equals(itPk)) {
//											log.trace("interp " + col + " at " + itPk);
											// **Backward** Bad Data Fill
											if (this.queuedRows.size() > q.size() + 1) {
												itTR = badDataFill(itTR, this.queuedRows.get(q.size() + 1), exemptValueKeyword);
											} else {
												log.error("How the heck did we get here if you don't have more values than this?");
											}
											//calc interp entries
											Tuple<Long, BigDecimal> itTup = calcLinearInterp(lastTup, itPk, nowTup);
											//insert interp entries into rows
											TableRow interpTR = buildInterpTR(col, itTup.getY(), itTR, this.exemptValueKeyword);
											//Fix the Row
											this.queuedRows.set(q.size(), interpTR);
										}
									} else {
										isDone = true;
									}
								}
							}
							if (!this.exemptCols.contains(col)) {
								//swap last vals with now vals
								lastVals.put(ent.getKey(), nowTup);
							}
						} catch (NumberFormatException e) {
							log.trace("Not a BigDecimal: '" + ent.getValue() + "', passing it through.");
							
						} catch (Exception e) {
							log.debug("Exception Caught", e);
						}
					}
				}
				
				if (0 < this.queuedRows.size()) {
					//check gapLimit on old rows
					DateTime nowDT = new DateTime(pkVal);
					DateTime qDT = new DateTime(new Long(this.queuedRows.peek().getValue(pkCol)));
					DateTime gap = nowDT.minus(gapLimit);
					while (null != this.queuedRows.peek() && (gap.isEqual(qDT) || gap.isAfter(qDT))) {
//						log.trace(qDT +" gapped by "+ nowDT);
						Long queuedPk = new Long(this.queuedRows.peek().getValue(pkCol));
						////ifGapped
						////remove gapped values from last vals
						for (Entry<Column, String> ent : this.queuedRows.peek().getEntries()) {
							Tuple<Long, BigDecimal> lastTup = lastVals.get(ent.getKey());
							if (null != lastTup && lastTup.getX().equals(queuedPk)) {
								lastVals.put(ent.getKey(), null);
							}
						}
//						log.trace("" + this.queuedRows.peek());
						////send gapped rows to nextRows
						this.nextRows.add(this.queuedRows.poll());
						nothingSentOut = false;
//						log.trace("" + lastVals);
						qDT = new DateTime(new Long(this.queuedRows.peek().getValue(pkCol)));
						gap = nowDT.minus(gapLimit);
					}
				}
			}
		}
		if (in.isAfterLast() && 0 < this.queuedRows.size()) {
			//Send out all remaining rows
			this.nextRows.addAll(this.queuedRows);
			this.queuedRows.clear();
			nothingSentOut = false;
		}
	}
	
	protected static Tuple<Long, BigDecimal> calcLinearInterp(final Tuple<Long, BigDecimal> xyA, final Long x, final Tuple<Long, BigDecimal> xyB) {
		Tuple<Long, BigDecimal> result = null;
		if (null != xyA && xyA.isValid() && null != x && null != xyB && xyB.isValid()) {
			BigDecimal y = null;
			
			y = xyA.getY().add(
					(xyB.getY().subtract(xyA.getY())).multiply(
					(new BigDecimal(x).subtract(new BigDecimal(xyA.getX()))).divide(
					new BigDecimal(xyB.getX()).subtract(new BigDecimal(xyA.getX())), MathContext.DECIMAL32)
					), MathContext.DECIMAL32);
			
			result = buildTuple(x, y);
		} else {
			log.info("Missing argument! xyA:" + xyA + " x:" + x + " xyB:" + xyB);
		}
		return result;
	}
	
	protected static TableRow buildInterpTR(Column col, BigDecimal val, TableRow base, String exemptValueKeyword) {
		TableRow result = base;
		
		if (null != col && null != val && null != base && 
				!(null != exemptValueKeyword && exemptValueKeyword.equals(base.getValue(col)))) {
			Map<Column, String> modMap = new HashMap<Column, String>();
			modMap.putAll(base.getMap());
			modMap.put(col, val.toPlainString());
			
			result = new TableRow(base.getColumns(), modMap);
		}
		
		return result;
	}

	@Override
	public String getCursorName() throws SQLException {
		throwIfClosed(this);
		return in.getCursorName();
	}
	
	@Override
	public void close() throws SQLException {
		try {
			in.close();
		} finally {
			this.closed = true;
		}
	}
	
	/**
	 * if everybody uses this, we can enforce having null or valid tuples, never invalid tuples
	 * @param x
	 * @param y
	 * @return 
	 */
	protected static Tuple<Long, BigDecimal> buildTuple(Long x, BigDecimal y) {
		Tuple<Long, BigDecimal> result = null;
		
		if (null != x && null != y) {
			result = new Tuple<Long, BigDecimal>(x, y);
		}
		
		return result;
	}
	
	/**
	 * Neither Arg should be null
	 * @param <X>
	 * @param <Y> 
	 */
	protected static class Tuple<X, Y> {
		protected final X x;
		protected final Y y;
		
		protected Tuple(X x, Y y) {
			this.x = x;
			this.y = y;
		}

		public X getX() {
			return this.x;
		}

		public Y getY() {
			return this.y;
		}
		
		public boolean isValid() {
			return ((null != this.x) && (null != this.y));
		}
		
		@Override
		protected Object clone() throws CloneNotSupportedException {
			return new Tuple<X, Y>(this.getX(), this.getY());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) { return false; }
			if (obj == this) { return true; }
			if (obj instanceof Tuple) {
				Tuple<?, ?> rhs = (Tuple<?, ?>) obj;
				return new EqualsBuilder()
						.append(this.getX(), rhs.getX())
						.append(this.getY(), rhs.getY())
						.isEquals();
			}
			return false;
		}

		@Override
		public int hashCode() {
			return new HashCodeBuilder(3221, 743)
					.append(this.getX())
					.append(this.getY())
					.toHashCode();
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("X", this.getX())
					.add("Y", this.getY())
					.toString();
		}
		
	}
}
