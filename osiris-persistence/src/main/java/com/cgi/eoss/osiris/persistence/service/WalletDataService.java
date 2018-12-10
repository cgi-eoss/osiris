package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.Wallet;
import com.cgi.eoss.osiris.model.WalletTransaction;

public interface WalletDataService extends
        OsirisEntityDataService<Wallet> {
    Wallet findByOwner(User user);
    void transact(WalletTransaction transaction);
    void creditBalance(Wallet wallet, int amount);
}
