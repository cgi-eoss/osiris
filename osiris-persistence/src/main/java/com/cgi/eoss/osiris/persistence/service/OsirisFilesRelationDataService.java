package com.cgi.eoss.osiris.persistence.service;

import java.util.Set;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.OsirisFilesRelation.Type;

public interface OsirisFilesRelationDataService extends
        OsirisEntityDataService<OsirisFilesRelation> {

	Set<OsirisFilesRelation> findBySourceFileAndType(OsirisFile osirisFile, Type relationType);
	
	Set<OsirisFilesRelation> findByTargetFileAndType(OsirisFile osirisFile, Type relationType);
   
}
