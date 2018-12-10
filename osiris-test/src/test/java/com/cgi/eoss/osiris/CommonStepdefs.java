package com.cgi.eoss.osiris;

import com.cgi.eoss.osiris.util.OsirisPage;
import com.cgi.eoss.osiris.util.OsirisWebClient;
import cucumber.api.java8.En;

public class CommonStepdefs implements En {

    public CommonStepdefs(OsirisWebClient client) {
        Given("^I am on the \"([^\"]*)\" page$", (String page) -> {
            client.load(OsirisPage.valueOf(page));
        });

        Given("^I am logged in as \"([^\"]*)\" with role \"([^\"]*)\"$", (String username, String role) -> {
            client.loginAs(username, role);
        });

        Given("^the default services are loaded$", () -> {
            client.oneShotApiPost("/contentAuthority/services/restoreDefaults");
            client.oneShotApiPost("/contentAuthority/services/wps/syncAllPublic");
        });
    }

}
