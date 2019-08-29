package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.rpc.wps.BoundingBoxDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsOutputDefinition;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.LiteralDataWpsParam;
import com.google.common.base.Strings;

public class GrpcConversions {
	
	private GrpcConversions() {
		
	}
	
	public static ComplexDataParam fromComplexDataWpsParam(ComplexDataWpsParam complexDataWpsParam) {
		String encoding = complexDataWpsParam.getEncoding();
		if(Strings.isNullOrEmpty(encoding)) {
			encoding = null;
		}
		String schema = complexDataWpsParam.getSchema();
		if(Strings.isNullOrEmpty(schema)) {
			schema = null;
		}
		return new ComplexDataParam(encoding, 
				schema, 
				complexDataWpsParam.getMimeType(),
				complexDataWpsParam.getParamValue());
	}
	
	public static LiteralDataParam fromLiteralDataWpsParam(LiteralDataWpsParam literalDataWpsParam) {
		return new LiteralDataParam(literalDataWpsParam.getParamValue());
	}
	
	public static BoundingBoxDataParam fromBoundingBoxDataWpsParam(BoundingBoxDataWpsParam boundingBoxDataWpsParam) {
		return new BoundingBoxDataParam(boundingBoxDataWpsParam.getLowerCorner(), 
				boundingBoxDataWpsParam.getUpperCorner(), 
				boundingBoxDataWpsParam.getCrs(),
				boundingBoxDataWpsParam.getDimensions());
	}
	
	public static ComplexDataOutputDefinition fromComplexDataWpsOutputDefinition(ComplexDataWpsOutputDefinition complexDataWpsOutputDefinition) {
		String encoding = complexDataWpsOutputDefinition.getEncoding();
		if(Strings.isNullOrEmpty(encoding)) {
			encoding = null;
		}
		String schema = complexDataWpsOutputDefinition.getSchema();
		if(Strings.isNullOrEmpty(schema)) {
			schema = null;
		}
		return new ComplexDataOutputDefinition(encoding, 
				schema, 
				complexDataWpsOutputDefinition.getMimeType());
	}

}
