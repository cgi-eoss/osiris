package com.cgi.eoss.osiris.persistence.service;

import static com.cgi.eoss.osiris.model.QOsirisFilesRelation.osirisFilesRelation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.OsirisFilesRelation.Type;
import com.cgi.eoss.osiris.persistence.dao.OsirisEntityDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisFilesRelationDao;
import com.querydsl.core.types.Predicate;
@Service
@Transactional(readOnly = true)
public class JpaOsirisFilesRelationDataService extends AbstractJpaDataService<OsirisFilesRelation> implements OsirisFilesRelationDataService {

	
	private final OsirisFilesRelationDao dao;
	 
	@Autowired
    public JpaOsirisFilesRelationDataService(OsirisFilesRelationDao osirisFileRelationDao) {
        this.dao = osirisFileRelationDao;
    }
	
	@Override
	OsirisEntityDao<OsirisFilesRelation> getDao() {
		return dao;
	}

	@Override
	Predicate getUniquePredicate(OsirisFilesRelation entity) {
		return osirisFilesRelation.sourceFile.eq(entity.getSourceFile())
				.and(osirisFilesRelation.sourceFile.eq(entity.getTargetFile()))
				.and(osirisFilesRelation.type.eq(entity.getType()));
	}

	@Override
	public Set<OsirisFilesRelation> findBySourceFileAndType(OsirisFile osirisFile, Type relationType) {
		return dao.findBySourceFileAndType(osirisFile, relationType);
	}
	
	@Override
	public Set<OsirisFilesRelation> findByTargetFileAndType(OsirisFile osirisFile, Type relationType) {
		return dao.findByTargetFileAndType(osirisFile, relationType);
	}

    
}
