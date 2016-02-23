package propocessing;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ToADM {

	public static final SimpleDateFormat ADMDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd");
	public static final SimpleDateFormat ADMTimeFormat = new SimpleDateFormat(
			"HH:mm:ss.SSS");

	public static String mkADMConstructor(String constructor, String content) {
		return constructor + "(\"" + content + "\")";
	}

	public static String mkInt64Constructor(long value) {
		return mkADMConstructor("int64", String.valueOf(value));
	}

	public static String mkDoubleConstructor(double value) {
		return mkADMConstructor("double", String.valueOf(value));
	}

	public static String mkDateConstructor(Date jdate) {
		return "date(\"" + ADMDateFormat.format(jdate) + "\")";
	}

	public static String mkDateTimeConstructor(Date jdate) {
		return "datetime(\"" + ADMDateFormat.format(jdate) + "T"
				+ ADMTimeFormat.format(jdate) + "\")";
	}

	public static void keyValueToSb(StringBuilder sb, String key, String val) {
		sb.append(mkQuote(key)).append(":").append(val.replaceAll("\\s+", " "));
	}

	public static String mkQuote(String key) {
		// TODO Auto-generated method stub
		return "\"" + key + "\"";
	}

	public static void keyValueToSbWithComma(StringBuilder sb, String key,
			String val) {
		keyValueToSb(sb, key, val);
		sb.append(", ");
	}

	public static String[] splitGeometry(String geometry) {
		String[] ss = geometry.split("\\)\\)(,?)");
		StringBuilder sb = null;
		for (int i = 0; i < ss.length; i++) {
			sb = new StringBuilder();
			String s = ss[i].replaceAll("\\(", "");
			ss[i] = sb.append("(").append(s.trim()).append(")").toString();
		}
		return ss;
	}

	public static String mkGeoConstructor(String geometry, String geometryType) {
		// TODO Auto-generated method stub
		if ("polygon".equalsIgnoreCase(geometryType)) {
			if (geometry.length() >= 2) {
				String[] temps = geometry.substring(1, geometry.length() - 1)
						.trim().split(",");
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < temps.length; i++) {
					if (i > 0)
						sb.append(" ");
					sb.append(temps[i].trim().replaceAll(" ", ","));
				}
				return mkADMConstructor(geometryType, sb.toString());
			}
		}
		if ("multipolygon".equalsIgnoreCase(geometryType)) {
			String[] ss = geometry.split("\\)\\)(,?)");
			StringBuilder sb = new StringBuilder();
			sb.append("{{");
			for (int i = 0; i < ss.length; i++) {
				if (i > 0) {
					sb.append(',');
				}
				String s = ss[i].replaceAll("\\(", "");
				String[] temps = s.trim().split(",");
				StringBuilder sb2 = new StringBuilder();
				for (int j = 0; j < temps.length; j++) {
					if (i > 0)
						sb2.append(" ");
					sb2.append(temps[j].trim().replaceAll(" ", ","));
				}
				sb.append(mkADMConstructor("polygon", sb2.toString()));
			}
			sb.append("}}");
			return sb.toString();
		}
		System.out.println("GEO type error!");
		return "error";
	}
}
