package com.cgi.eoss.osiris.model.internal;

import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.OsirisServiceDescriptor.Relation.RelationType;

public class ParameterRelationTypeToFileRelationTypeUtil {

	public static OsirisFilesRelation.Type fromParameterRelationType(RelationType relationType) {
		switch (relationType) {
		case VISUALIZATION_OF: 
			return OsirisFilesRelation.Type.VISUALIZATION_OF;
		default:
			return null;
		}
	}
}
