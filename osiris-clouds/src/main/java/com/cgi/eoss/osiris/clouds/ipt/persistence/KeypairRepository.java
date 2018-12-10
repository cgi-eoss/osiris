package com.cgi.eoss.osiris.clouds.ipt.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.io.Serializable;

@Repository
public interface KeypairRepository extends CrudRepository<Keypair, Serializable>{
     
}
