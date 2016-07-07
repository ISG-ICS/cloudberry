package edu.uci.ics.cloudberry.noah.feed;

/**
 * Created by Xikui on 6/16/16.
 */

import edu.uci.ics.cloudberry.gnosis.USGeoGnosis;
import edu.uci.ics.cloudberry.noah.adm.Tweet;
import org.apache.asterix.common.exceptions.AsterixException;
import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.library.java.JObjects;
import org.apache.asterix.external.util.Datatypes;
import org.json.JSONException;
import org.json.JSONObject;
import play.libs.Scala;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GeoTagFunction implements IExternalScalarFunction {

    public Map<Object, File> geoMap = new HashMap<>();
    public USGeoGnosis usGeoGnosis;
    private StringBuilder sb;
    JObjects.JString strBuffer;
    JObjects.JInt intBuffer;
    JObjects.JLong longBuffer;

    @Override
    public void initialize(IFunctionHelper functionHelper){
        geoMap.put(1,new File("/Volumes/Storage/Users/Xikui/Work/cloudberry/neo/public/data/state.json"));
        geoMap.put(2, new File("/Volumes/Storage/Users/Xikui/Work/cloudberry/neo/public/data/county.json"));
        geoMap.put(3, new File("/Volumes/Storage/Users/Xikui/Work/cloudberry/neo/public/data/city.json"));
        usGeoGnosis = new USGeoGnosis(Scala.asScala(geoMap));
        sb = new StringBuilder();
        strBuffer = new JObjects.JString("");
        intBuffer = new JObjects.JInt(0);
        longBuffer =  new JObjects.JLong(0);
    }

    @Override
    public void deinitialize(){

    }


    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception {
        JObjects.JRecord inputRecord = (JObjects.JRecord) functionHelper.getArgument(0);
        JObjects.JRecord outputRecord = (JObjects.JRecord) functionHelper.getResultObject();
        JObjects.JRecord outGeoTag = outputRecord.getRecordByName("geoTag");

        System.out.println("External Function starts: "+((JObjects.JLong)inputRecord.getValueByName("id")).getValue());
        getGeoTag(inputRecord,outGeoTag);

        outputRecord.setField(Datatypes.Tweet.ID, inputRecord.getValueByName(Datatypes.Tweet.ID));
        outputRecord.setField("geoTag",outGeoTag);
        functionHelper.setResult(outputRecord);
    }


    private void Json2JRecord(String Jstr, JObjects.JRecord attr) throws JSONException, AsterixException {
        JSONObject geoJson = new JSONObject(Jstr);
        Iterator attrNameIter = geoJson.keys();
        while (attrNameIter.hasNext()){
            String attrName = (String) attrNameIter.next();
            Object attrVal = geoJson.get(attrName);
            if(attrVal==null){
                continue;
            }
            if(attrVal instanceof String){
                strBuffer.setValue((String) attrVal);
                attr.setField(attrName, strBuffer);
            }
            else if(attrVal instanceof Integer){
                intBuffer.setValue((Integer) attrVal);
                attr.setField(attrName, intBuffer);
            }
            else if(attrVal instanceof Long){
                longBuffer.setValue((Long) attrVal);
                attr.setField(attrName, longBuffer);
            }
        }
    }

    private boolean getGeoTag(JObjects.JRecord inputRecord, JObjects.JRecord geoTag) throws Exception{
        sb.setLength(0);
        JObjects.JRecord place;
        JObjects.JString location;
        place = (JObjects.JRecord) inputRecord.getValueByName("place");
//        System.out.println("C_Code "+place.getStringByName("country_code").getValue());
        location = inputRecord.getStringByName("location");
        if(place!=null){
            if(Tweet.textMatchPlace(sb, place, usGeoGnosis)){
                Json2JRecord(sb.toString(),geoTag);
                return true;
            }
        }
        else if(location!=null){
            System.out.println(location.toString());
            if(Tweet.exactPointLookup(sb,location.toString(),usGeoGnosis)){
                Json2JRecord(sb.toString(),geoTag);
            }
        }
        return false;
    }
}
