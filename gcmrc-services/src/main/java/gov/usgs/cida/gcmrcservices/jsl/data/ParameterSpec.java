package gov.usgs.cida.gcmrcservices.jsl.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
 * @author dmsibley
 */
public class ParameterSpec extends DataSpec {
	private static final long serialVersionUID = -4493630171150826021L;
	private static final Logger log = LoggerFactory.getLogger(ParameterSpec.class);

	public ParameterSpec(String stationName, ParameterCode parameterCode, SpecOptions options) {
		super(stationName, parameterCode, options);
	}

	@Override
	public ColumnMapping[] setupColumnMap() {
		ColumnMapping[] result = null;
		
		if (null != this.stationName && null != this.parameterCode) {
			String parameterColumnName = ColumnMetadata.createColumnName(this.stationName, this.parameterCode);
			result = new ColumnMapping[] {
				new ColumnMapping(C_TSM_DT, C_TSM_DT, ASCENDING_ORDER, S_TSM_DT, null, null, null, "TO_CHAR(" + C_TSM_DT + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null, null),
				new ColumnMapping(parameterColumnName, C_FINAL_VALUE, ASCENDING_ORDER, S_FINAL_VALUE, null, null, null, C_FINAL_VALUE, null, null),
				new ColumnMapping(parameterColumnName + C_RAW_VALUE, C_RAW_VALUE, ASCENDING_ORDER, S_RAW_VALUE, null, null, null, C_RAW_VALUE, null, null),
				new ColumnMapping(parameterColumnName + C_MAIN_QUALIFIER, C_MAIN_QUALIFIER, ASCENDING_ORDER, S_MAIN_QUALIFIER, null, null, null, C_MAIN_QUALIFIER, null, null),
				new ColumnMapping(parameterColumnName + C_DATA_APPROVAL, C_DATA_APPROVAL, ASCENDING_ORDER, S_DATA_APPROVAL, null, null, null, C_DATA_APPROVAL, null, null),
				new ColumnMapping(parameterColumnName + C_MEASUREMENT_GRADE, C_MEASUREMENT_GRADE, ASCENDING_ORDER, S_MEASUREMENT_GRADE, null, null, null, C_MEASUREMENT_GRADE, null, null),
				new ColumnMapping(parameterColumnName + C_DEPLOYMENT, C_DEPLOYMENT, ASCENDING_ORDER, S_DEPLOYMENT, null, null, null, C_DEPLOYMENT, null, null),
				new ColumnMapping(parameterColumnName + C_ICE_AFFECTED, C_ICE_AFFECTED, ASCENDING_ORDER, S_ICE_AFFECTED, null, null, null, C_ICE_AFFECTED, null, null),
				new ColumnMapping(parameterColumnName + C_TURBIDITY_PEGGED, C_TURBIDITY_PEGGED, ASCENDING_ORDER, S_TURBIDITY_PEGGED, null, null, null, C_TURBIDITY_PEGGED, null, null),
				new ColumnMapping(parameterColumnName + C_PROBE_TYPE, C_PROBE_TYPE, ASCENDING_ORDER, S_PROBE_TYPE, null, null, null, C_PROBE_TYPE, null, null),
				new ColumnMapping(parameterColumnName + C_INSTRUMENT, C_INSTRUMENT, ASCENDING_ORDER, S_INSTRUMENT, null, null, null, C_INSTRUMENT, null, null),
				new ColumnMapping(parameterColumnName + C_DATA_LEAD, C_DATA_LEAD, ASCENDING_ORDER, S_DATA_LEAD, null, null, null, C_DATA_LEAD, null, null),
				new ColumnMapping(parameterColumnName + C_RAW_FLAG, C_RAW_FLAG, ASCENDING_ORDER, S_RAW_FLAG, null, null, null, C_RAW_FLAG, null, null),
				new ColumnMapping(parameterColumnName + C_DATA_QUALIFICATION, C_DATA_QUALIFICATION, ASCENDING_ORDER, S_DATA_QUALIFICATION, null, null, null, C_DATA_QUALIFICATION, null, null),
				new ColumnMapping(parameterColumnName + C_ACCURACY_RATING, C_ACCURACY_RATING, ASCENDING_ORDER, S_ACCURACY_RATING, null, null, null, C_ACCURACY_RATING, null, null),
				new ColumnMapping(parameterColumnName + C_SOURCE_FILE_NAME, C_SOURCE_FILE_NAME, ASCENDING_ORDER, S_SOURCE_FILE_NAME, null, null, null, C_SOURCE_FILE_NAME, null, null),
				new ColumnMapping(parameterColumnName + C_SOURCE_UPLOAD_DATE, C_SOURCE_UPLOAD_DATE, ASCENDING_ORDER, S_SOURCE_UPLOAD_DATE, null, null, null, C_SOURCE_UPLOAD_DATE, null, null),
				new ColumnMapping(parameterColumnName + C_NOTES, C_NOTES, ASCENDING_ORDER, S_NOTES, null, null, null, C_NOTES, null, null),
				new ColumnMapping(parameterColumnName + C_ER_VALUE, C_ER_VALUE, ASCENDING_ORDER, S_ER_VALUE, null, null, null, C_ER_VALUE, null, null)
			};
		} else {
			log.trace("setupColumnMap stationName=" + this.stationName + " parameterCode=" + this.parameterCode);
		}
		
		return result;
	}

	@Override
	public SearchMapping[] setupSearchMap() {
		SearchMapping[] result = new SearchMapping[] {
			new SearchMapping(S_SITE_NAME, C_SITE_NAME, null, WhereClauseType.equals, null, null, null),
			new SearchMapping(S_GROUP_NAME, C_GROUP_NAME, null, WhereClauseType.equals, null, null, null),
			new SearchMapping(Endpoint.BEGIN_KEYWORD, C_TSM_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " >= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null),
			new SearchMapping(Endpoint.END_KEYWORD, C_TSM_DT, null, WhereClauseType.special, CleaningOption.none, FIELD_NAME_KEY + " <= to_timestamp(" + USER_VALUE_KEY + ", 'YYYY-MM-DD\"T\"HH24:MI:SS')", null)
		};
		
		return result;
	}

	@Override
	public String setupTableName() {
		StringBuilder result = new StringBuilder();
		
		result.append("  (SELECT MEASUREMENT_DATE AS TSM_DT,");
		result.append("    FINAL_VALUE,");
		result.append("    RAW_VALUE,");
		result.append("    MAIN_QUALIFIER,");
		result.append("    DATA_APPROVAL,");
		result.append("    MEASUREMENT_GRADE,");
		result.append("    DEPLOYMENT,");
		result.append("    ICE_AFFECTED,");
		result.append("    TURBIDITY_PEGGED,");
		result.append("    PROBE_TYPE,");
		result.append("    INSTRUMENT,");
		result.append("    DATA_LEAD,");
		result.append("    RAW_FLAG,");
		result.append("    DATA_QUALIFICATION,");
		result.append("    ACCURACY_RATING,");
		result.append("    SOURCE_FILE_NAME,");
		result.append("    SOURCE_UPLOAD_DATE,");
		result.append("    NOTES,");
		result.append("    ER_VALUE,");
		result.append("    SITE_NAME,");
		result.append("    GROUP_NAME");
		result.append("  FROM TIME_SERIES_STAR");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT MEASUREMENT_QUALIFIER_ID, NAME AS MAIN_QUALIFIER_NAME, CODE AS MAIN_QUALIFIER FROM MEASUREMENT_QUALIFIER_STAR");
		result.append("    ) T_MAIN_QUALIFIER");
		result.append("  ON TIME_SERIES_STAR.ICE_AFFECTED_ID = T_MAIN_QUALIFIER.MEASUREMENT_QUALIFIER_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT DATA_APPROVAL_ID, NAME AS DATA_APPROVAL_NAME, CODE AS DATA_APPROVAL FROM DATA_APPROVAL_STAR");
		result.append("    ) T_DATA_APPROVAL");
		result.append("  ON TIME_SERIES_STAR.DATA_APPROVAL_ID = T_DATA_APPROVAL.DATA_APPROVAL_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT MEASUREMENT_GRADE_ID, NAME AS MEASUREMENT_GRADE_NAME, CODE AS MEASUREMENT_GRADE FROM MEASUREMENT_GRADE_STAR");
		result.append("    ) T_MEASUREMENT_GRADE");
		result.append("  ON TIME_SERIES_STAR.MEASUREMENT_GRADE_ID = T_MEASUREMENT_GRADE.MEASUREMENT_GRADE_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT DEPLOYMENT_ID, NAME AS DEPLOYMENT FROM DEPLOYMENT_STAR");
		result.append("    ) T_DEPLOYMENT");
		result.append("  ON TIME_SERIES_STAR.DEPLOYMENT_ID = T_DEPLOYMENT.DEPLOYMENT_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT ICE_AFFECTED_ID, NAME AS ICE_AFFECTED FROM ICE_AFFECTED_STAR");
		result.append("    ) T_ICE");
		result.append("  ON TIME_SERIES_STAR.ICE_AFFECTED_ID = T_ICE.ICE_AFFECTED_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT PROBE_TYPE_ID, NAME AS PROBE_TYPE FROM PROBE_TYPE_STAR");
		result.append("    ) T_PROBE_TYPE");
		result.append("  ON TIME_SERIES_STAR.PROBE_TYPE_ID = T_PROBE_TYPE.PROBE_TYPE_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT INSTRUMENT_ID, NAME AS INSTRUMENT FROM INSTRUMENT_STAR");
		result.append("    ) T_INSTRUMENT");
		result.append("  ON TIME_SERIES_STAR.INSTRUMENT_ID = T_INSTRUMENT.INSTRUMENT_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT DATA_LEAD_ID, NAME AS DATA_LEAD FROM DATA_LEAD_STAR");
		result.append("    ) T_DATA_LEAD");
		result.append("  ON TIME_SERIES_STAR.DATA_LEAD_ID = T_DATA_LEAD.DATA_LEAD_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT RAW_FLAG_ID, CODE AS RAW_FLAG FROM RAW_FLAG_STAR");
		result.append("    ) T_RAW_FLAG");
		result.append("  ON TIME_SERIES_STAR.RAW_FLAG_ID = T_RAW_FLAG.RAW_FLAG_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT DATA_QUALIFICATION_ID, NAME AS DATA_QUALIFICATION FROM DATA_QUALIFICATION_STAR");
		result.append("    ) T_DATA_QUALIFICATION");
		result.append("  ON TIME_SERIES_STAR.DATA_QUALIFICATION_ID = T_DATA_QUALIFICATION.DATA_QUALIFICATION_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT ACCURACY_RATING_ID, CODE AS ACCURACY_RATING FROM ACCURACY_RATING_STAR");
		result.append("    ) T_ACCURACY_RATING");
		result.append("  ON TIME_SERIES_STAR.ACCURACY_RATING_ID = T_ACCURACY_RATING.ACCURACY_RATING_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT SOURCE_ID, ORIGINAL_NAME AS SOURCE_FILE_NAME, UPLOAD_DATE AS SOURCE_UPLOAD_DATE FROM SOURCE_STAR");
		result.append("    ) T_SOURCE");
		result.append("  ON TIME_SERIES_STAR.SOURCE_ID = T_SOURCE.SOURCE_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT SITE_ID,");
		result.append("      coalesce(nwis_site_no, short_name) SITE_NAME");
		result.append("    FROM SITE_STAR");
		result.append("    ) SITE");
		result.append("  ON TIME_SERIES_STAR.SITE_ID = SITE.SITE_ID");
		result.append("  LEFT OUTER JOIN");
		result.append("    (SELECT GROUP_ID, NAME AS GROUP_NAME FROM GROUP_NAME");
		result.append("    ) T_GROUP");
		result.append("  ON TIME_SERIES_STAR.GROUP_ID = T_GROUP.GROUP_ID");
		result.append("  ) T_A_MAIN");

		return result.toString();
	}

	
	@Override
	public int hashCode() {
		return new HashCodeBuilder()
				.append("TODO I'm a " + this.stationName + " " + this.parameterCode.toString() + " param Spec!")
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (obj == this) { return true; }
		if (obj instanceof ParameterSpec) {
			ParameterSpec rhs = (ParameterSpec) obj;
			return new EqualsBuilder()
					.append(this.stationName, rhs.stationName)
					.append(this.parameterCode, rhs.parameterCode)
					.isEquals();
		}
		return false;
	}
	
	public static final String C_TSM_DT = "tsm_dt";
	public static final String S_TSM_DT = "TSM_DT";
	public static final String C_FINAL_VALUE = "final_value";
	public static final String S_FINAL_VALUE = "FINAL_VALUE";
	public static final String C_SITE_NAME = "site_name";
	public static final String S_SITE_NAME = "SITE_NAME";
	public static final String C_GROUP_NAME = "group_name";
	public static final String S_GROUP_NAME = "GROUP_NAME";
	
	public static final String C_RAW_VALUE = "raw_value";
	public static final String S_RAW_VALUE = "RAW_VALUE";
	public static final String C_MAIN_QUALIFIER = "main_qualifier";
	public static final String S_MAIN_QUALIFIER = "MAIN_QUALIFIER";
	public static final String C_DATA_APPROVAL = "data_approval";
	public static final String S_DATA_APPROVAL = "DATA_APPROVAL";
	public static final String C_MEASUREMENT_GRADE = "measurement_grade";
	public static final String S_MEASUREMENT_GRADE = "MEASUREMENT_GRADE";
	public static final String C_DEPLOYMENT = "deployment";
	public static final String S_DEPLOYMENT = "DEPLOYMENT";
	public static final String C_ICE_AFFECTED = "ice_affected";
	public static final String S_ICE_AFFECTED = "ICE_AFFECTED";
	public static final String C_TURBIDITY_PEGGED = "turbidity_pegged";
	public static final String S_TURBIDITY_PEGGED = "TURBIDITY_PEGGED";
	public static final String C_PROBE_TYPE = "probe_type";
	public static final String S_PROBE_TYPE = "PROBE_TYPE";
	public static final String C_INSTRUMENT = "instrument";
	public static final String S_INSTRUMENT = "INSTRUMENT";
	public static final String C_DATA_LEAD = "data_lead";
	public static final String S_DATA_LEAD = "DATA_LEAD";
	public static final String C_RAW_FLAG = "raw_flag";
	public static final String S_RAW_FLAG = "RAW_FLAG";
	public static final String C_DATA_QUALIFICATION = "data_qualification";
	public static final String S_DATA_QUALIFICATION = "DATA_QUALIFICATION";
	public static final String C_ACCURACY_RATING = "accuracy_rating";
	public static final String S_ACCURACY_RATING = "ACCURACY_RATING";
	public static final String C_SOURCE_FILE_NAME = "source_file_name";
	public static final String S_SOURCE_FILE_NAME = "SOURCE_FILE_NAME";
	public static final String C_SOURCE_UPLOAD_DATE = "source_upload_date";
	public static final String S_SOURCE_UPLOAD_DATE = "SOURCE_UPLOAD_DATE";
	public static final String C_NOTES = "notes";
	public static final String S_NOTES = "NOTES";
	public static final String C_ER_VALUE = "er_value";
	public static final String S_ER_VALUE = "ER_VALUE";
}
