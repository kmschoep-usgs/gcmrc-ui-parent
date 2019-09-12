package gov.usgs.cida.gcmrc.util;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class PathUtil {

	private static final Logger log = LoggerFactory.getLogger(PathUtil.class);

	public static String calculateRelativePath(final String requestURI, final String basePath) {
		StringBuilder relativePath = new StringBuilder();
		
		String cleanBasePath = "";
		if (null != basePath) {
			cleanBasePath = basePath.trim();
		}
		if (cleanBasePath.endsWith("/")) {
			cleanBasePath = cleanBasePath.substring(0, cleanBasePath.length() - 1);
		}
		
		String path = "";
		if (null != requestURI) {
			path = requestURI.trim();
		}
		
		if (!path.equals(cleanBasePath) && path.contains(cleanBasePath)) {
			path = path.substring(path.indexOf(cleanBasePath) + cleanBasePath.length() + 1);
		}
		
		if (StringUtils.isNotEmpty(path)) {
			int pathCount = path.split("/", -1).length;
			for (int i = 1; i < pathCount; i++) {
				relativePath.append("../");
			}
		}
		
		return relativePath.toString();
	}
	
	public static Map<String, String> calculateRestOfURI(final String requestURI, final String basePath, final String... args) {
		Map<String, String> result = new HashMap<String, String>();

		String cleanBasePath = "";
		if (null != basePath) {
			cleanBasePath = basePath.trim();
			cleanBasePath = StringUtils.strip(cleanBasePath, "/");
			while (StringUtils.contains(cleanBasePath, "//")) {
				cleanBasePath = StringUtils.replace(cleanBasePath, "//", "/");
			}
		}

		String cleanPath = StringUtils.trimToNull(requestURI);
		if (null != cleanPath) {
			cleanPath = StringUtils.strip(cleanPath, "/");
			while (StringUtils.contains(cleanPath, "//")) {
				cleanPath = StringUtils.replace(cleanPath, "//", "/");
			}
			try {
				cleanPath = URLDecoder.decode(cleanPath, "UTF-8");
			} catch (Exception e) {
				log.error("Exception when trying to decode cleanPath: " + cleanPath + " for exception: " + e.getMessage() );
			}
			
		}
		if (null != cleanPath && !cleanPath.equals(cleanBasePath) && cleanPath.contains(cleanBasePath)) {
			cleanPath = StringUtils.trimToNull(StringUtils.strip(cleanPath.substring(cleanPath.indexOf(cleanBasePath) + cleanBasePath.length()), "/"));
			
			if (null != cleanPath) {
				String[] splitPath = StringUtils.split(cleanPath, "/");
				
				List<String> argList = Arrays.asList(args);
				Iterator<String> argIt = argList.iterator();
				int missingKeyIndex = 1;
				for (String nextVal : splitPath) {
					if (StringUtils.isNotBlank(nextVal)) {
						String nextKey = null;
						if (argIt.hasNext()) {
							nextKey = StringUtils.trimToNull(argIt.next());
						}
						
						if (null == nextKey) {
							nextKey = "unknown[" + missingKeyIndex + "]";
						}
						result.put(nextKey, nextVal);
					}
				}
			}
		}

		return result;
	}
}