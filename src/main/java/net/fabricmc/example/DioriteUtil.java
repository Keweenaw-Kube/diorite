package net.fabricmc.example;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class DioriteUtil {
  public static boolean isURLValid(String u) {
		try {
			new URL(u).toURI();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// https://stackoverflow.com/a/29213105/2129808
	public static String getQueryParamsFrom(Map<String, String> map) {
		return map.entrySet().stream()
    	.map(p -> urlEncodeUTF8(p.getKey()) + "=" + urlEncodeUTF8(p.getValue()))
    	.reduce((p1, p2) -> p1 + "&" + p2)
    	.orElse("");
	}

	private static String urlEncodeUTF8(String s) {
		try {
				return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
				throw new UnsupportedOperationException(e);
		}
	}
}
