package propocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.Feature;
import org.geotools.feature.FeatureIterator;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GeoJsonLoader {
	private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = System.getProperty("user.dir");
		List<City> lc = new ArrayList<City>();
		for (int i = 1; i < 80; i++) {
			String str = String.format("%02d", i);
			try {
				readCity("D:/testGeo/" + str + ".json", lc);
			} catch (IOException e) {
				System.out.println("read " + str + ".json" + " error!");
			}
		}

		CityConstructor cc = new CityConstructor();
		String result = cc.outputStr(lc);
		BufferedWriter bw = new BufferedWriter(new FileWriter(path + "/citys.adm"));
		bw.write(result);
		bw.close();
	}

	private static void readCity(String dir, List<City> lc) throws IOException {
		String s = readGeoJsonStr(dir);
		List<Feature> features = getFeature(s);
		for (Feature f : features) {
			try {
				lc.add(new City(f));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.print(".");
		}
		return;
	}

	public static List<Feature> getFeature(String s) throws IOException {
		FeatureJSON fjson = new FeatureJSON();
		FeatureIterator featureIterator = fjson.readFeatureCollection(s).features();

		List<Feature> lf = new ArrayList<Feature>();
		while (featureIterator.hasNext()) {
			lf.add(featureIterator.next());
		}
		return lf;
	}

	public static String readGeoJsonStr(String dir) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(dir));
		StringBuffer sb = new StringBuffer();
		String lineTxt = null;
		while ((lineTxt = br.readLine()) != null) {
			sb.append(lineTxt);
		}
		br.close();
		String s = sb.toString();
		return s;
	}

}
