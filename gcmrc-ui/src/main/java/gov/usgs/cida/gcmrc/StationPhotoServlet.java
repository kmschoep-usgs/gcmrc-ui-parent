package gov.usgs.cida.gcmrc;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dmsibley
 */
public class StationPhotoServlet extends HttpServlet {
	private static final long serialVersionUID = 4986068507640412410L;

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(StationPhotoServlet.class);
	
	public static final String BASE_PATH = "img";
	public static final String WIDTH_KEYWORD = "width";
	public static final String HEIGHT_KEYWORD = "height";
	public static final String NETWORK_KEYWORD = "network";
	public static final String STATION_KEYWORD = "station";
	public static final String FILE_KEYWORD = "picture";

	public static final String CLEAR_CACHE_PARAM = "clearCache";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/////TODO!!!!!!!!!!! When you get back to implementing this, add in the Application Initializer
//		Boolean clearCache;
//		Date currentDate = new Date();
//		String imageUrlPath;
//		ImageFactory imgFactory;
//		Image image;
//		Integer width, height;
//		File sourceImage;
//		File responseImage = null;
//		URL imageUrl;
//		
//		Map<String, String[]> params = req.getParameterMap();
//		Map<String, String> pathVars = PathUtil.calculateRestOfURI(req.getRequestURI(), BASE_PATH, WIDTH_KEYWORD, HEIGHT_KEYWORD, NETWORK_KEYWORD, STATION_KEYWORD, FILE_KEYWORD);
//
//		try {
//			// Pull in our parameters
//			clearCache = Boolean.parseBoolean(getParameter(params, CLEAR_CACHE_PARAM, "false"));
//			imageUrlPath = URLDecoder.decode(StringUtils.stripToEmpty(request.getParameter(URL_PARAMETER)), "UTF-8");
//			width = Integer.parseInt(StringUtils.defaultIfBlank(request.getParameter(WIDTH_PARAM), "0"));
//			height = Integer.parseInt(StringUtils.defaultIfBlank(request.getParameter(HEIGHT_PARAM), "-1"));
//		} catch (Exception e) {
//			LOG.info("Bad Params");
//		}
//		
//		// Check for valid parameters
//		if (StringUtils.isBlank(imageUrlPath)) {
//			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter URL cannot be blank");
//			return;
//		} else if (!urlValidator.isValid(imageUrlPath)) {
//			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter URL is not valid");
//			return;
//		} else {
//			imageUrl = new URL(imageUrlPath);
//		}
//
//		imgFactory = new ImageFactory(imageUrl);
//
//		try {
//			synchronized (this) {
//				// Before we start work, if set up to do so, we clear the cache directory.
//				// If the cache directory does not exist, there will be no attempt to
//				// clear it
//				if (clearCache) {
//					ImageProxyFileUtils.cleanCacheSubdirectory(imageUrl);
//				}
//				
//				image = imgFactory.getImage();
//			}
//
//			if (image == null) {
//				throw new IOException("Image object came back from getImage() null");
//			} else if (image.getPath() == null) {
//				throw new IOException("Image path came back from getImage() null");
//			} else if (!image.getPath().exists()) {
//				throw new IOException("Image path came back from getImage() nonexistent");
//			}
//		} catch (IOException ex) {
//			LOG.error("Could not create Image object. Forwarding user to original URL", ex);
//			response.sendRedirect(imageUrlPath);
//			return;
//		}
//
//		// We attain a compression rate from the request.
//		if (image.isCompressable() && StringUtils.isNotBlank(request.getParameter(COMPRESSION_PARAM))) {
//			try {
//				sourceImage = compressImage(request, image);
//			} catch (NumberFormatException nfe) {
//				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter " + COMPRESSION_PARAM + " must be an integer [0,1]");
//				return;
//			}
//		} else {
//			// The image will not be compressed so we use the original image
//			sourceImage = image.getPath();
//		}
//
//		try {
//			responseImage = new File(ImageProxyFileUtils.getWorkDirectory() + File.separator + Long.toString(currentDate.getTime()) + "-" + image.getHash() + "." + image.getFormat());
//
//			// Do we need to resize? 
//			if (width > 0 && height != 0) {
//				try {
//					byte[] resizedImageByteArray = image.resize(width, height);
//					FileUtils.writeByteArrayToFile(responseImage, resizedImageByteArray);
//				} catch (UnsupportedOperationException uoe) {
//					LOG.info("Image type {} does not support resizing. Retaining original size.");
//					FileUtils.copyFile(sourceImage, responseImage);
//				}
//			} else {
//				FileUtils.copyFile(sourceImage, responseImage);
//			}
//
//
//			try {
//				response.setContentType(image.getContentType());
//				FileUtils.copyFile(responseImage, response.getOutputStream());
//			} catch (IOException ex) {
//				LOG.error("Could not send object back to client", ex);
//				response.sendRedirect(imageUrlPath);
//			} finally {
//				response.flushBuffer();
//			}
//
//		} finally {
//			FileUtils.deleteQuietly(responseImage);
//		}
	}
	
	public static String getParameter(Map<String, String[]> map, String key) {
		return getParameter(map, key, null);
	}

	public static String getParameter(Map<String, String[]> map, String key, String defaultValue) {
		String result = null;

		String[] vals = map.get(key);
		if (null != vals && 0 < vals.length) {
			result = StringUtils.trimToNull(vals[0]);
		}

		if (null == result) {
			result = defaultValue;
		}

		return result;
	}
	
}
