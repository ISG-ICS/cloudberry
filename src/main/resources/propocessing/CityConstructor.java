package propocessing;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;


public class CityConstructor implements ObjConstructor<City> {
	private static String BASEID = "ID";
	private static String STATE_FP = "statefp";
	private static String PLACE_FP = "place_fp";
	private static String PLACE_NS = "place_ns";
	private static String AFFGEOID = "aff_geo_id";
	private static String GEOID = "geo_id";
	private static String NAME = "name";
	private static String LSAD = "lsad";
	private static String ALAND = "aland";
	private static String AWATER = "awater";
	private static String GEOMETRY = "geometry";
	private static String GEOMETRYTYPE = "geometry_type";

	public String outputStr(Collection<City> lc) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (City c : lc) {
			if ("polygon".equalsIgnoreCase(c.getGeometryType())) {
				if (outputACity(c, c.getGeometry(), i, sb))
					i++;
			} else if ("multipolygon".equalsIgnoreCase(c.getGeometryType())) {
				String[] ss = ToADM.splitGeometry(c.getGeometry());
				for (String s : ss) {
					if (outputACity(c, s, i, sb))
						i++;
				}
				ss = null;
			}
		}
		return sb.toString();
	}

	/**
	 * the city's geometryType must be polygon;
	 * 
	 * @param c
	 * @param i
	 * @param sb
	 */
	public boolean outputACity(City c, String geometry, int i, StringBuilder sb) {
		if ("multipolygon".equalsIgnoreCase(geometry))
			return false;
		sb.append("{");
		ToADM.keyValueToSbWithComma(sb, BASEID, String.valueOf(i));
		ToADM.keyValueToSbWithComma(sb, STATE_FP, ToADM.mkQuote(c.getStatefp()));
		ToADM.keyValueToSbWithComma(sb, PLACE_FP, ToADM.mkQuote(c.getPlacefp()));
		ToADM.keyValueToSbWithComma(sb, PLACE_NS, ToADM.mkQuote(c.getPlacens()));
		ToADM.keyValueToSbWithComma(sb, AFFGEOID,
				ToADM.mkQuote(c.getAffgeoid()));
		ToADM.keyValueToSbWithComma(sb, GEOID, ToADM.mkQuote(c.getGeoid()));
		ToADM.keyValueToSbWithComma(sb, NAME, ToADM.mkQuote(c.getName()));
		ToADM.keyValueToSbWithComma(sb, LSAD, ToADM.mkQuote(c.getLsad()));
		ToADM.keyValueToSbWithComma(sb, ALAND,
				ToADM.mkDoubleConstructor(c.getAland()));
		ToADM.keyValueToSbWithComma(sb, AWATER,
				ToADM.mkDoubleConstructor(c.getAwater()));
		ToADM.keyValueToSb(sb, GEOMETRY,
				ToADM.mkGeoConstructor(geometry, "polygon"));
		sb.append("}\n");
		return true;
	}
}
