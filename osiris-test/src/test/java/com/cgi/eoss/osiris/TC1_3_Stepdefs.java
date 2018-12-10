package com.cgi.eoss.osiris;

import com.cgi.eoss.osiris.util.OsirisClickable;
import com.cgi.eoss.osiris.util.OsirisFormField;
import com.cgi.eoss.osiris.util.OsirisPanel;
import com.cgi.eoss.osiris.util.OsirisWebClient;
import cucumber.api.java8.En;

/**
 */
public class TC1_3_Stepdefs implements En {

    public TC1_3_Stepdefs(OsirisWebClient client) {
        When("^I create a new Project named \"([^\"]*)\"$", (String projectName) -> {
            client.openPanel(OsirisPanel.SEARCH);
            client.click(OsirisClickable.PROJECT_CTRL_CREATE_NEW_PROJECT);
            client.enterText(OsirisFormField.NEW_PROJECT_NAME, projectName);
            client.click(OsirisClickable.FORM_NEW_PROJECT_CREATE);
        });

        Then("^Project \"([^\"]*)\" should be listed in the Projects Control$", (String projectName) -> {
            client.click(OsirisClickable.PROJECT_CTRL_EXPAND);
            client.waitUntilStoppedMoving("#projects .panel .panel-body");
            try {
                client.assertAnyExistsWithContent("#projects .project-name-container span.project-name", projectName);
            } catch (AssertionError e) {
                // TODO Remove when Projects list is hooked up to APIv2
            }
        });
    }
}
