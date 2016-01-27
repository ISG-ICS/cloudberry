package propocessing;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;


public class CityConstructor implements ObjConstructor<City> {
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
		for (City c : lc) {
			sb.append("{");
			ToADM.keyValueToSbWithComma(sb, STATE_FP, ToADM.mkQuote(c.getStatefp()));
			ToADM.keyValueToSbWithComma(sb, PLACE_FP, ToADM.mkQuote(c.getPlacefp()));
			ToADM.keyValueToSbWithComma(sb, PLACE_NS, ToADM.mkQuote(c.getPlacens()));
			ToADM.keyValueToSbWithComma(sb, AFFGEOID, ToADM.mkQuote(c.getAffgeoid()));
			ToADM.keyValueToSbWithComma(sb, GEOID, ToADM.mkQuote(c.getGeoid()));
			ToADM.keyValueToSbWithComma(sb, NAME, ToADM.mkQuote(c.getName()));
			ToADM.keyValueToSbWithComma(sb, LSAD, ToADM.mkQuote(c.getLsad()));
			ToADM.keyValueToSbWithComma(sb,ALAND,ToADM.mkDoubleConstructor(c.getAland()));
			ToADM.keyValueToSbWithComma(sb, AWATER, ToADM.mkDoubleConstructor(c.getAwater()));
			ToADM.keyValueToSb(sb, GEOMETRY, ToADM.mkGeoConstructor(c.getGeometry(),c.getGeometryType()));
			sb.append("}\n");
		}
		return sb.toString();
	}
}
