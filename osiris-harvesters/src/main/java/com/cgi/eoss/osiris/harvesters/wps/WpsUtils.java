package com.cgi.eoss.osiris.harvesters.wps;

import com.cgi.eoss.osiris.rpc.wps.BoundingBoxDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsOutputDefinition;
import com.cgi.eoss.osiris.rpc.wps.ComplexDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.LiteralDataWpsParam;
import com.cgi.eoss.osiris.rpc.wps.WpsJobStatus;
import net.opengis.wps.x100.StatusType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WpsUtils {

	public static WpsJobStatus getWpsJobStatusFromExecuteResponseStatus(StatusType status) throws WpsException {
		if (status.isSetProcessAccepted()) {
			return WpsJobStatus.ACCEPTED;
		}
		if (status.isSetProcessStarted()) {
			return WpsJobStatus.STARTED;
		}
		if (status.isSetProcessPaused()) {
			return WpsJobStatus.PAUSED;
		}
		if (status.isSetProcessFailed()) {
			return WpsJobStatus.FAILED;
		}
		if (status.isSetProcessSucceeded()) {
			return WpsJobStatus.SUCCEEDED;
		}
		throw new WpsException("Unrecognized WPS status");
	}
	
	public static WpsJobStatus getWpsJobStatusFromClsJobStatus(String status) throws WpsException {
		switch(status.toUpperCase()) {
			case "PENDING": return WpsJobStatus.ACCEPTED;
			case "RUNNING": return WpsJobStatus.STARTED;
			case "FAILED": return WpsJobStatus.FAILED;
			case "CANCELLED": return WpsJobStatus.SUCCEEDED;
			case "TERMINATED": return WpsJobStatus.SUCCEEDED;
			default: throw new WpsException("Unrecognized WPS status");
		}
	}
	
	public static Map<String, ComplexDataParam> rpcComplexParamListToMap(List<ComplexDataWpsParam> complexDataWpsParamList) {
		return complexDataWpsParamList.stream().collect(Collectors.toMap(ComplexDataWpsParam::getParamName, GrpcConversions::fromComplexDataWpsParam));
	}
	
	public static Map<String, LiteralDataParam> rpcLiteralParamListToMap(List<LiteralDataWpsParam> literalDataWpsParamList) {
		return literalDataWpsParamList.stream().collect(Collectors.toMap(LiteralDataWpsParam::getParamName, GrpcConversions::fromLiteralDataWpsParam));
	}
	
	public static Map<String, BoundingBoxDataParam> rpcBoundingBoxParamListToMap(List<BoundingBoxDataWpsParam> boundingBoxDataWpsParamList) {
		return boundingBoxDataWpsParamList.stream().collect(Collectors.toMap(BoundingBoxDataWpsParam::getParamName, GrpcConversions::fromBoundingBoxDataWpsParam));
	}
	
	public static Map<String, ComplexDataOutputDefinition> rpcComplexDataOutputDefinitionListToMap(List<ComplexDataWpsOutputDefinition> complexDataWpsOutputDefinitionList) {
		return complexDataWpsOutputDefinitionList.stream().collect(Collectors.toMap(ComplexDataWpsOutputDefinition::getOutputName, GrpcConversions::fromComplexDataWpsOutputDefinition));
	}

}
