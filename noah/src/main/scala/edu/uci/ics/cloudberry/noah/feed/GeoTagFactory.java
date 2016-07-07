package edu.uci.ics.cloudberry.noah.feed;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionFactory;

/**
 * Created by Xikui on 6/21/16.
 */
public class GeoTagFactory implements IFunctionFactory{


    @Override
    public IExternalScalarFunction getExternalFunction(){
        return new GeoTagFunction();
    }
}
