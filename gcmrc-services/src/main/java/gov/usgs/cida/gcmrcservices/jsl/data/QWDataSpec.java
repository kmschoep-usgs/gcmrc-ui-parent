package gov.usgs.cida.gcmrcservices.jsl.data;

import gov.usgs.cida.gcmrcservices.column.ColumnMetadata;
import gov.usgs.cida.gcmrcservices.nude.Endpoint;
import gov.usgs.webservices.jdbc.spec.mapping.ColumnMapping;
import gov.usgs.webservices.jdbc.spec.mapping.SearchMapping;
import gov.usgs.webservices.jdbc.spec.mapping.WhereClauseType;
import gov.usgs.webservices.jdbc.util.CleaningOption;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class QWDataSpec extends DataSpec {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(QWDataSpec.class);

	public QWDataSpec(String stationName, ParameterCode parameterCode, SpecOptions options) {
		super(stationName, parameterCode, options);
	}

	@Override
	public ColumnMapping[] setupColumnMap() {
		ColumnMapping[] result = null;
		
		if (null != this.stationName && null != this.parameterCode) {
			result = new ColumnMapping[] {
				new ColumnMapping(C_SAMPLE_ID, S_SAMPLE_ID),
				new ColumnMapping(ParameterSpec.C_TSM_DT, ParameterSpec.C_TSM_DT, ASCENDING_ORDER, ParameterSpec.S_TSM_DT, null, null, null, "CASE WHEN USE_LAGGED = 'true' THEN TO_CHAR(" + C_LAGGED_SAMPLE_START_DT + ", 'YYYY-MM-DD\"T\"HH24:MI:SS') ELSE TO_CHAR(" + C_SAMPLE_START_DT + ", 'YYYY-MM-DD\"T\"HH24:MI:SS') END", null, null),
				new ColumnMapping(ColumnMetadata.createColumnName(this.stationName, this.parameterCode), C_RESULT_VA, ASCENDING_ORDER, S_RESULT_VA, null, null, null, "CASE WHEN TOTAL_95CONF IS NOT NULL THEN (CASE WHEN ERRORBAR_LOWER_VA < 0 THEN 0 ELSE ERRORBAR_LOWER_VA END) || ';' || RESULT_VA || ';' || ERRORBAR_UPPER_VA ELSE RESULT_VA::character END", null, null),
				new ColumnMapping(C_SITE_NAME, S_SITE_NAME),
				new ColumnMapping(C_SAMPLE_METHOD, S_SAMPLE_METHOD),
				new ColumnMapping(C_GROUP_NAME, S_GROUP_NAME)
			};
		} else {
			log.trace("setupColumnMap stationName=" + this.stationName + " parameterCode=" + this.parameterCode);
		}
		
		return result;
	}

	@Override
	public SearchMapping[] setupSearchMap() {
		SearchMapping[] result = new SearchMapping[] {
			new SearchMapping(ParameterSpec.S_SITE_NAME, C_SITE_NAME, null, WhereClauseType.equals, null, null, null),
			new SearchMapping(ParameterSpec.S_GROUP_NAME, C_GROUP_NAME, null, WhereClauseType.equals, null, null, null),
			new SearchMapping(S_SAMPLE_METHOD, C_SAMPLE_METHOD, null, WhereClauseType.equals, null, null, null),
			new SearchMapping(Endpoint.BEGIN_KEYWORD, C_SAMPLE_START_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " >= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null),
			new SearchMapping(Endpoint.END_KEYWORD, C_SAMPLE_START_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " <= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null)
		};
		
		return result;
	}

	@Override
	public String setupTableName() {
		StringBuilder result = new StringBuilder();

		result.append("(");
		result.append("  SELECT QWS.SAMPLE_ID,");
		result.append("  coalesce(s.nwis_site_no, s.short_name) site_name,");
		result.append("  QWS.AVERAGE_DATE AS SAMP_START_DT,");
		result.append("  QWS.AVERAGE_DATE + format('%s %s',SS.TIME_LAG_SECONDS,'seconds')::interval AS LAGGED_SAMP_START_DT,");
		result.append("  'true' AS USE_LAGGED,");
		result.append("  QWR.SUSPSED_VALUE RESULT_VA,");
		result.append("  CASE");
		result.append("    WHEN QWR.BIAS_CORRECTION IS NULL");
		result.append("    THEN QWR.SUSPSED_VALUE + QWR.TOTAL_95CONF");
		result.append("    ELSE QWR.SUSPSED_VALUE + QWR.TOTAL_95CONF + QWR.BIAS_CORRECTION");
		result.append("  END AS ERRORBAR_UPPER_VA,");
		result.append("  CASE");
		result.append("    WHEN QWR.BIAS_CORRECTION IS NULL");
		result.append("    THEN QWR.SUSPSED_VALUE - QWR.TOTAL_95CONF");
		result.append("    ELSE QWR.SUSPSED_VALUE - QWR.TOTAL_95CONF + QWR.BIAS_CORRECTION");
		result.append("  END AS ERRORBAR_LOWER_VA,");
		result.append("  SMS.SAMPLE_METHOD,");
		result.append("  QWR.TOTAL_95CONF,");
		result.append("  QWR.GROUP_ID,");
		result.append("  G.NAME AS GROUP_NAME,");
		result.append("  QWS.SERVE");
		result.append(" FROM SAMPLE_STAR QWS,");
		result.append("  RESULT_STAR QWR,");
		result.append("  SITE_STAR S,");
		result.append("  SAMPLE_METHOD_STAR SMS,");
		result.append("  SUBSITE_STAR SS,");
		result.append("  GROUP_NAME G");
		result.append(" WHERE QWS.SAMPLE_ID      = QWR.SAMPLE_ID");
		result.append("  AND QWS.SITE_ID          = S.SITE_ID");
		result.append("  AND QWS.SAMPLE_METHOD_ID = SMS.SAMPLE_METHOD_ID");
		result.append("  AND QWS.SUBSITE_ID       = SS.SUBSITE_ID");
		result.append("  AND QWR.GROUP_ID         = G.GROUP_ID");
		result.append("  AND QWS.SERVE = 'YES'");
		result.append(") T_A_INNER");

		return result.toString();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append("TODO I'm a " + this.stationName + " station " + this.parameterCode.toString() + " Lab Data Spec!")
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj instanceof QWDataSpec) {
			QWDataSpec rhs = (QWDataSpec) obj;
			return new EqualsBuilder()
					.append(this.stationName, rhs.stationName)
					.append(this.parameterCode, rhs.parameterCode)
					.isEquals();
		}
		return false;
	}
	
	public static final String C_SAMPLE_ID = "sample_id";
	public static final String C_SAMPLE_START_DT = "samp_start_dt";
	public static final String C_LAGGED_SAMPLE_START_DT = "lagged_samp_start_dt";
	public static final String C_RESULT_VA = "result_va";
	
	public static final String S_SAMPLE_ID = "SAMPLE_ID";
	public static final String S_SAMPLE_START_DT = "SAMP_START_DT";
	public static final String S_LAGGED_SAMPLE_START_DT = "LAGGED_SAMP_START_DT";
	public static final String S_RESULT_VA = "RESULT_VA";
	
	public static final String S_SITE_NAME = "site";
	public static final String C_SITE_NAME = "site_name";
	public static final String S_SAMPLE_METHOD = "sampleMethod";
	public static final String C_SAMPLE_METHOD = "sample_method";
	public static final String S_GROUP_NAME = "groupName";
	public static final String C_GROUP_NAME = "group_name";
}
