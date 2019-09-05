package gov.usgs.cida.gcmrcservices.nude.transform;

import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.filter.ColumnTransform;
import gov.usgs.cida.nude.resultset.inmemory.TableRow;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class ReplaceValueTransform implements ColumnTransform {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ReplaceValueTransform.class);

	protected final Column col;
	protected final String inValue;
	protected final String outValue;
	public ReplaceValueTransform(Column inCol, String inValue, String outValue) {
		this.col = inCol;
		this.inValue = inValue;
		this.outValue = outValue;
	}

	@Override
	public String transform(TableRow row) {
		String result = row.getValue(col);
		
		if (StringUtils.equals(inValue, result)) {
			result = outValue;
		}
		
		return result;
	}
}
