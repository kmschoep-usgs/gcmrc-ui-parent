package gov.usgs.cida.gcmrcservices.nude.transform;

import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.filter.ColumnTransform;
import gov.usgs.cida.nude.resultset.inmemory.TableRow;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley, zmoore
 */
public class ValueRangeLimiterTransform implements ColumnTransform {
	private static final Logger log = LoggerFactory.getLogger(ValueRangeLimiterTransform.class);
	private final static String DELIMITER = ";";

	protected BigDecimal lowerLimit;
	protected BigDecimal upperLimit;
	
	protected final Column inColumn;

	public ValueRangeLimiterTransform(Column inColumn, BigDecimal lowerLimit, BigDecimal upperLimit) {
		this.inColumn = inColumn;
		this.lowerLimit = lowerLimit;
		this.upperLimit = upperLimit;
	}
	
	public BigDecimal limitValue(BigDecimal inVal) {
		BigDecimal result = inVal;
		if (null != inVal) {
			if (null != lowerLimit && lowerLimit.compareTo(inVal) > 0) {
				result = lowerLimit;
			} else if (null !=  upperLimit && upperLimit.compareTo(inVal) < 0) {
				result = upperLimit;
			}
		}
		return result;
	}
	
	@Override
	public String transform(TableRow tr) {
		String result = null;
		
		try {
			result = tr.getValue(inColumn);
			String inStr = tr.getValue(inColumn);
			List<String> values = Arrays.asList(StringUtils.split(inStr, DELIMITER));
			List<String> results = new LinkedList<>();
			for (String val : values) {
				BigDecimal inVal = new BigDecimal(val);
				results.add(limitValue(inVal).toPlainString());
			}
			result = StringUtils.join(results, DELIMITER);
		} catch (Exception e) {
			log.trace("Could not limit value", e);
		}
		
		return result;
	}
}
