package gov.usgs.cida.gcmrcservices.jsl.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.cida.gcmrcservices.column.ColumnMetadata;
import gov.usgs.cida.gcmrcservices.nude.Endpoint;
import gov.usgs.webservices.jdbc.spec.mapping.ColumnMapping;
import gov.usgs.webservices.jdbc.spec.mapping.SearchMapping;
import gov.usgs.webservices.jdbc.spec.mapping.WhereClauseType;
import gov.usgs.webservices.jdbc.util.CleaningOption;

/**
 *
 * @author thongsav
 */
public class DischargeErrorSpec extends DataSpec {
	private static final long serialVersionUID = 1L;
	
	private static final Logger log = LoggerFactory.getLogger(DischargeErrorSpec.class);

	public DischargeErrorSpec(String stationName, ParameterCode parameterCode, SpecOptions options) {
		super(stationName, parameterCode, options);
	}

	@Override
	public ColumnMapping[] setupColumnMap() {
		ColumnMapping[] result = null;
		
		if (null != this.stationName && null != this.parameterCode) {
			result = new ColumnMapping[] {
				new ColumnMapping(ParameterSpec.C_TSM_DT, ParameterSpec.C_TSM_DT, ASCENDING_ORDER, ParameterSpec.S_TSM_DT, null, null, null, "CASE WHEN USE_LAGGED = 'true' THEN TO_CHAR(" + C_LAGGED_SAMPLE_START_DT + ", 'YYYY-MM-DD\"T\"HH24:MI:SS') ELSE TO_CHAR(" + C_SAMPLE_START_DT + ", 'YYYY-MM-DD\"T\"HH24:MI:SS') END", null, null),
				new ColumnMapping(ColumnMetadata.createColumnName(this.stationName, this.parameterCode), C_FINAL_VALUE_NAME, ASCENDING_ORDER, C_FINAL_VALUE_NAME, null, null, null, "CASE WHEN ERROR_PERCENT IS NOT NULL THEN (CASE WHEN ERRORBAR_LOWER_VA < 0 THEN 0 ELSE ERRORBAR_LOWER_VA END) || ';' || RESULT_VA || ';' || ERRORBAR_UPPER_VA ELSE RESULT_VA::character END", null, null),
				new ColumnMapping(C_SITE_NAME, S_SITE_NAME),
				new ColumnMapping(C_GROUP_NAME, S_GROUP_NAME),
				new ColumnMapping(C_METHOD_NAME, S_METHOD_NAME),
				new ColumnMapping(C_FINAL_VALUE_NAME, S_FINAL_VALUE_NAME),
				new ColumnMapping(C_UPPER_ERROR_NAME, C_UPPER_ERROR_NAME),
				new ColumnMapping(C_LOWER_ERROR_NAME, C_LOWER_ERROR_NAME)
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
			new SearchMapping(S_METHOD_NAME, C_METHOD_NAME, null, WhereClauseType.equals, CleaningOption.addedWildcardSearch, null, null),
			new SearchMapping(Endpoint.BEGIN_KEYWORD, C_SAMPLE_START_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " >= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null),
			new SearchMapping(Endpoint.END_KEYWORD, C_SAMPLE_START_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " <= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null)
		};
		
		return result;
	}

	@Override
	public String setupTableName() {
		StringBuilder result = new StringBuilder();

		result.append("(");
		result.append("  SELECT");
		result.append("  coalesce(s.nwis_site_no, s.short_name) SITE_NAME,");
		result.append("  DED.AVERAGE_DATE AS SAMP_START_DT,");
		result.append("  DED.AVERAGE_DATE + format('%s %s',SS.TIME_LAG_SECONDS,'seconds')::interval AS LAGGED_SAMP_START_DT,");
		result.append("  'false' AS USE_LAGGED,"); //Hack? I don't know why the query is generated wanting these columns
		result.append("  DED.RAW_VALUE RAW_VALUE,");
		result.append("  DED.FINAL_VALUE RESULT_VA,");
		result.append("  DED.ERROR_PERCENT,");
		result.append("  CASE");
		result.append("    WHEN DED.ERROR_PERCENT IS NULL");
		result.append("    THEN NULL");
		result.append("    ELSE DED.FINAL_VALUE * (DED.ERROR_PERCENT * 0.01 + 1)"); 
		result.append("  END AS ERRORBAR_UPPER_VA,");
		result.append("  CASE");
		result.append("    WHEN DED.ERROR_PERCENT IS NULL");
		result.append("    THEN NULL");
		result.append("    ELSE DED.FINAL_VALUE * (1 - DED.ERROR_PERCENT * 0.01)");
		result.append("  END AS ERRORBAR_LOWER_VA,");
		result.append("  DED.METHOD,");
		result.append("  DED.GROUP_ID,");
		result.append("  G.NAME AS GROUP_NAME");
		result.append(" FROM DISCHARGE_ERROR_DATA DED");
		result.append("   left join site_star s");
		result.append("     on DED.site_id = s.site_id");
		result.append("   left join group_name g");
		result.append("     on DED.group_id = g.group_id");
		result.append("   left join subsite_star ss");
		result.append("     on ss.site_id = s.site_id");

		if(this.parameterCode != null && this.parameterCode.sampleMethod != null) {
			String sqlCleanMethod = cleanSql(this.parameterCode.sampleMethod);
			result.append(" where");
			result.append("   DED.METHOD = '" + sqlCleanMethod + "'");
		}
			
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
		if (obj instanceof DischargeErrorSpec) {
			DischargeErrorSpec rhs = (DischargeErrorSpec) obj;
			return new EqualsBuilder()
					.append(this.stationName, rhs.stationName)
					.append(this.parameterCode, rhs.parameterCode)
					.isEquals();
		}
		return false;
	}
	
	public static final String C_SAMPLE_START_DT = "samp_start_dt";
	public static final String C_LAGGED_SAMPLE_START_DT = "lagged_samp_start_dt";
	public static final String C_METHOD_NAME = "method";
	public static final String C_FINAL_VALUE_NAME = "result_va";
	public static final String C_UPPER_ERROR_NAME = "errorbar_upper_va";
	public static final String C_LOWER_ERROR_NAME = "errorbar_lower_va";
	
	public static final String S_SAMPLE_START_DT = "SAMP_START_DT";
	public static final String S_LAGGED_SAMPLE_START_DT = "LAGGED_SAMP_START_DT";
	public static final String S_FINAL_VALUE_NAME = "FINAL_VALUE";
	public static final String S_METHOD_NAME = "METHOD";
	
	public static final String S_SITE_NAME = "site";
	public static final String C_SITE_NAME = "site_name";
	public static final String S_GROUP_NAME = "groupName";
	public static final String C_GROUP_NAME = "group_name";
	
	public static String cleanSql(String input) {
		if (input == null) {
			return null;
		}
		String output = input.trim();
		output = output.replace("'", "");
		output = output.replace("|", "");
		output = output.replace(";", "");

		return output;
	}
}
