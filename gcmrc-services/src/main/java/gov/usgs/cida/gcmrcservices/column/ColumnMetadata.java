package gov.usgs.cida.gcmrcservices.column;

import gov.usgs.cida.gcmrcservices.jsl.data.BedSedimentSpec;
import gov.usgs.cida.gcmrcservices.jsl.data.DischargeErrorSpec;
import gov.usgs.cida.gcmrcservices.jsl.data.ParameterCode;
import gov.usgs.cida.gcmrcservices.jsl.data.ParameterSpec;
import gov.usgs.cida.gcmrcservices.jsl.data.QWDataSpec;
import gov.usgs.cida.gcmrcservices.jsl.data.SpecOptions;
import gov.usgs.cida.nude.column.Column;
import gov.usgs.cida.nude.column.SimpleColumn;
import gov.usgs.cida.nude.out.mapping.ColumnToXmlMapping;
import gov.usgs.webservices.jdbc.spec.Spec;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class ColumnMetadata {
	private static final Logger log = LoggerFactory.getLogger(ColumnMetadata.class);

	protected final String pcode;
	protected final ParameterCode parameterCode;
	protected final String columnTitle;
	protected final List<SpecEntry> specEntries;
	
	public ColumnMetadata(String pcode, String columnTitle, SpecEntry... specs) {
		this.pcode = pcode;
		this.parameterCode = ParameterCode.parseParameterCode(pcode);
		this.columnTitle = columnTitle;
		
		List<SpecEntry> se = new ArrayList<SpecEntry>();
		if (null != specs && 0 < specs.length) {
			se.addAll(Arrays.asList(specs));
		}
		
		this.specEntries = Collections.unmodifiableList(se);
	}
	
	/**
	 * Give a pCode if not a download, give descriptive names if is.
	 * @param station
	 * @param isDownload
	 * @return 
	 */
	public ColumnToXmlMapping getMapping(String station, boolean isDownload) {
		return getMapping(station, isDownload, null);
	}
	
	private static String hashString(String message, Integer places) {
		String result = null;
		
		if (null != message) {
			String algorithm = "SHA-256";
			String encoding = "UTF-8";
			try {
				MessageDigest sha = MessageDigest.getInstance(algorithm);
				sha.update(message.getBytes(encoding));
				byte[] digest = sha.digest();
				result = Hex.encodeHexString(digest);
			} catch (NoSuchAlgorithmException e) {
				log.error("Could not get " + algorithm + " Algorithm");
			} catch (UnsupportedEncodingException ex) {
				log.error("Could not get " + encoding + " Encoding");
			}
			
			if (null == result) {
				result = "" + message.hashCode();
			}
			
			if (null != places) {
				result = result.substring(0, places);
			}
		}
		
		return result;
	}
	
	public static String createColumnName(String station, ParameterCode parameterCode) {
		String result = null;
		String ancillaryColumn = null;
		ParameterCode tempParameterCode = null;

		if (null != station && null != parameterCode) {
			if (parameterCode.sampleMethod.startsWith("iceAffected") || 
				parameterCode.sampleMethod.startsWith("notes")) {
				tempParameterCode = ParameterCode.parseParameterCode("inst!" + parameterCode.groupName);			
			}
			else {
				tempParameterCode = parameterCode;
			}

			switch (parameterCode.sampleMethod)
			{
				case "iceAffected": 
					ancillaryColumn = ParameterSpec.C_ICE_AFFECTED;
					break;
				
				case "notes": 
					ancillaryColumn = ParameterSpec.C_NOTES;
					break;

				default: 
					ancillaryColumn = "";
					break;
			}
			
			result = ("S" + hashString(station, 5) + "P" + hashString(tempParameterCode.toString(), 5) + ancillaryColumn).toLowerCase();		
		}
		
		return result;
	}
	
	
	/**
	 * Give a pCode if not a download, give descriptive names if is.
	 * @param station
	 * @param isDownload
	 * @param customName
	 * @return 
	 */
	public ColumnToXmlMapping getMapping(String station, boolean isDownload, String customName) {
		ColumnToXmlMapping result = null;
		
		String inName = createColumnName(station, this.parameterCode);
		String outName = getDefaultOutName(station, isDownload);
		
		String cleanCustomName = StringUtils.trimToNull(customName);
		if (null != cleanCustomName) {
			outName = StringUtils.replace(cleanCustomName, "*default*", outName);
		}
		
		result = new ColumnToXmlMapping(inName, outName);
		
		return result;
	}
	
	protected String getDefaultOutName(String station, boolean isDownload) {
		String result = null;
		
		String tag = this.pcode;
		if (isDownload) {
			tag = this.columnTitle;
		}
			
		result = tag + "-" + station;
		
		return result;
	}
	
	public List<SpecEntry> getSpecEntries() {
		return this.specEntries;
	}
	
	public Column getColumn(String station) {
		Column result = null;
		
		result = new SimpleColumn(pcode + "-" + station);
		
		return result;
	}
	
	public Column getInternalColumn(String station) {
		Column result = null;
		
		result = new SimpleColumn(createColumnName(station, this.parameterCode));
		
		return result;
	}
	
	public String getPCode() {
		return this.pcode;
	}
	
	public static class SpecEntry {
		public static enum SpecType {
			PARAM,
			LABDATA,
			BEDMATERIAL,
			DISCHARGEERROR;
		}
		
		public final ParameterCode parameterCode;
		public final SpecType specType;

		public SpecEntry(ParameterCode parameterCode, SpecType specType) {
			this.parameterCode = parameterCode;
			this.specType = specType;
		}
		
		public Column getColumn(String station) {
			Column result = null;

			result = new SimpleColumn(createColumnName(station, this.parameterCode));

			return result;
		}
		
		public Spec getSpec(String station, SpecOptions specOptions) {
			Spec result = null;
			
			switch (this.specType) {
				case PARAM:
					result = new ParameterSpec(station, this.parameterCode, specOptions);
					break;
				case LABDATA:
					result = new QWDataSpec(station, this.parameterCode, specOptions);
					break;
				case BEDMATERIAL:
					result = new BedSedimentSpec(station, this.parameterCode, specOptions);
					break;
				case DISCHARGEERROR:
					result = new DischargeErrorSpec(station, this.parameterCode, specOptions);
					break;
			}

			return result;
		}
	}
}
