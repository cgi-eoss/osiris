package com.cgi.eoss.osiris.persistence.dao;

import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.Wallet;

public interface WalletDao extends OsirisEntityDao<Wallet> {
    Wallet findOneByOwner(User user);
}
