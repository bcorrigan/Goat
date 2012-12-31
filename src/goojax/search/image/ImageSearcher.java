package goojax.search.image;

import static java.net.URLEncoder.encode;


import goojax.search.AbstractSearcher;



import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Iterator;

public class ImageSearcher extends AbstractSearcher<ImageSearchResponse> {

	public enum ImageSize {
		ICON    ("icon"),
		SMALL   ("small"),    // At dev time, Google claims
		MEDIUM  ("medium"),   // that small, medium,
		LARGE   ("large"),    // large, and xlarge
		XLARGE  ("xlarge"),   // are all considered to be "medium"
		XXLARGE ("xxlarge"),
		HUGE    ("huge");
		
		String urlCode;
		ImageSize(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	public enum ImageColor {
		MONO  ("mono"),
		GRAY  ("gray"),
		GREY  ("gray"),
		COLOR ("color");
		
		String urlCode;
		ImageColor(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	public enum ImageType {
		FACE ("face");
		String urlCode;
		ImageType(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	public enum FileType {
		JPG ("jpg"),
		GIF ("gif"),
		PNG ("png"),
		BMP ("bmp");
		String urlCode;
		FileType(String urlCode) {
			this.urlCode = urlCode;
		}
	}
	
	public SafeSearch safeSearch = null;
	public ImageSize imageSize = null;
	public ImageColor imageColor = null;
	public ImageType imageType = null;
	public FileType fileType = null;
	public String site = null;
	
	public ImageSearcher() {
		super();
	}
	
	public String encodeExtraSearchOpts() {
		ArrayList<String> tokes = new ArrayList<String>();

		try {
			if(safeSearch != null)
				tokes.add("safe=" + safeSearch.urlCode);
			if(imageSize != null)
				tokes.add("imgsz=" + imageSize.urlCode);
			if(imageColor != null)
				tokes.add("imgc=" + imageColor.urlCode);
			if(imageType != null)
				tokes.add("imgtype" + imageType.urlCode);
			if(fileType != null)
				tokes.add("as_filetype=" + fileType.urlCode);
			if(null != site && ! site.matches("\\s*"))
				tokes.add("as_sitesearch=" + encode(site, encoding));
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return "";
		}
		String ret = "";
		Iterator<String> iter = tokes.iterator();
		if(iter.hasNext())
			ret += iter.next();
		while(iter.hasNext())
			ret += "&" + iter.next();
		return ret;	
	}

	public SearchType getSearchType() {
		return SearchType.IMAGES;
	}

	public SafeSearch getSafeSearch() {
		return safeSearch;
	}

	public void setSafeSearch(SafeSearch safeSearch) {
		this.safeSearch = safeSearch;
	}

	public ImageSize getImageSize() {
		return imageSize;
	}

	public void setImageSize(ImageSize imageSize) {
		this.imageSize = imageSize;
	}

	public ImageColor getImageColor() {
		return imageColor;
	}

	public void setImageColor(ImageColor imageColor) {
		this.imageColor = imageColor;
	}

	public ImageType getImageType() {
		return imageType;
	}

	public void setImageType(ImageType imageType) {
		this.imageType = imageType;
	}

	public FileType getFileType() {
		return fileType;
	}

	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}
}
