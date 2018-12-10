package com.cgi.eoss.osiris.services;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.Set;
import org.junit.Test;
import com.cgi.eoss.osiris.model.OsirisService;

public class DefaultOsirisServicesTest {

    @Test
    public void getDefaultServices() throws Exception {
        Set<OsirisService> defaultServices = DefaultOsirisServices.getDefaultServices();
        assertThat(defaultServices.size(), is(3));
   }

}