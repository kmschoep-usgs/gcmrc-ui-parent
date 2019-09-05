package gov.usgs.cida.gcmrcservices.jsl.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 *
 * @author dmsibley
 */
public class ParameterCode {
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory.getLogger(ParameterCode.class);
	
	public final String sampleMethod;
	public final String groupName;

	public ParameterCode(String sampleMethod, String groupName) {
		this.sampleMethod = sampleMethod;
		this.groupName = groupName;
	}
	
	public static ParameterCode parseParameterCode(String parseThis) {
		ParameterCode result = null;
		
		String sampleMethod = null;
		String tsGrpNm = null;
		if (null != parseThis) {
			String[] split = parseThis.split("!");
			if (null != split && 0 < split.length) {
				sampleMethod = split[0];
				if (1 < split.length) {
					tsGrpNm = split[1];
				}
			}
			result = new ParameterCode(sampleMethod, tsGrpNm);
		}
		
		
		return result;
	}

	@Override
	public String toString() {
		String result = null;
		
		result = MoreObjects.toStringHelper(ParameterCode.class)
				.add("sampleMethod", this.sampleMethod)
				.add("groupName", this.groupName)
				.toString();
		
		return result;
	}

	@Override
	public int hashCode() {
		int result = new HashCodeBuilder()
				.append(this.sampleMethod)
				.append(this.groupName)
				.toHashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj instanceof ParameterCode) {
			ParameterCode rhs = (ParameterCode) obj;
			return new EqualsBuilder()
					.append(this.sampleMethod, rhs.sampleMethod)
					.append(this.groupName, rhs.groupName)
					.isEquals();
		}
		return false;
	}
}
