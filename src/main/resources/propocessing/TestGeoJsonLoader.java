package propocessing;

public class TestGeoJsonLoader {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//change the following paths as you want;
		String admPath = System.getProperty("user.dir");
		String jsonPath = System.getProperty("user.dir") + "/src/main/resources/Geojson/";

		List<City> lc = new ArrayList<City>();
		try {
			File afile = new File(jsonPath);
			readCity(afile, lc);
		} catch (IOException e) {
			System.out.print("read ");
			System.out.print(afile.getName());
			System.out.println(" error!");
			return;
		}

		CityConstructor cc = new CityConstructor();
		String result = cc.outputStr(lc);
		BufferedWriter bw = new BufferedWriter(new FileWriter(admPath + "/citys.adm"));
		bw.write(result);
		bw.close();
		System.out.println("GeoJson has transfered to ADM file.");
	}
}
