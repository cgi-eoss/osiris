import com.cgi.eoss.osiris.wps.OsirisServicesClient;

import java.util.HashMap;

public class ${id} {

    public static int ${id}(HashMap conf, HashMap inputs, HashMap outputs) {
        return OsirisServicesClient.launch("${id}", conf, inputs, outputs);
    }

}