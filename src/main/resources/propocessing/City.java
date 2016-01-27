package propocessing;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.GeometryType;

public class City {
	private String statefp;
	private String placefp;
	private String placens;
	private String affgeoid;
	private String geoid;
	private String name;
	private String lsad;
	private double aland;
	private double awater;
	private String geometry;
	private String geometryType;

	public City(Feature f) throws Exception {
		setParameter(f);
	}

	public void setParameter(Feature f) throws Exception {
		Collection<Property> cp = f.getProperties();
		for (Property p : cp) {
			setValue(p);
		}
		/*
		 * geometry = f.getDefaultGeometryProperty().getValue(); geometryType =
		 * f.getDefaultGeometryProperty().getType();
		 */
	}

	public String getStatefp() {
		return statefp;
	}

	public String getPlacefp() {
		return placefp;
	}

	public String getPlacens() {
		return placens;
	}

	public String getAffgeoid() {
		return affgeoid;
	}

	public String getGeoid() {
		return geoid;
	}

	public String getName() {
		return name;
	}

	public String getLsad() {
		return lsad;
	}

	public double getAland() {
		return aland;
	}

	public double getAwater() {
		return awater;
	}

	public String getGeometry() {
		return geometry;
	}

	public String getGeometryType() {
		return geometryType;
	}

	private void setValue(Property p) throws Exception {
		Map<Object, Object> moo = p.getUserData();
		String pName = p.getName().toString();
		if ("STATEFP".equalsIgnoreCase(pName)) {
			this.statefp = p.getValue().toString();
			return;
		}
		if ("PLACEFP".equalsIgnoreCase(pName)) {
			this.placefp = p.getValue().toString();
			return;
		}
		if ("PLACENS".equalsIgnoreCase(pName)) {
			this.placens = p.getValue().toString();
			return;
		}
		if ("AFFGEOID".equalsIgnoreCase(pName)) {
			this.affgeoid = p.getValue().toString();
			return;
		}
		if ("GEOID".equalsIgnoreCase(pName)) {
			this.geoid = p.getValue().toString();
			return;
		}
		if ("NAME".equalsIgnoreCase(pName)) {
			this.name = p.getValue().toString();
			return;
		}
		if ("LSAD".equalsIgnoreCase(pName)) {
			this.lsad = p.getValue().toString();
			return;
		}
		if ("ALAND".equalsIgnoreCase(pName)) {
			this.aland = Double.valueOf(p.getValue().toString());
			return;
		}
		if ("AWATER".equalsIgnoreCase(pName)) {
			this.awater = Double.valueOf(p.getValue().toString());
			return;
		}
		if ("geometry".equalsIgnoreCase(pName)) {
			String value = p.getValue().toString();
			/*
			 * String regStr = "(.*)[^(]\\((.*)\\)"; Pattern pattern =
			 * Pattern.compile(regStr); Matcher matcher =
			 * pattern.matcher(value);
			 */
			String[] ss = value.split("\\(", 2);
			if (ss.length == 2) {
				this.geometryType = ss[0].trim();
				this.geometry = ss[1].substring(0, ss[1].length() - 1).trim();
				return;
			}
			System.out.println("geometry error");
			return;
		}
		throw new Exception("type error");
	}
}