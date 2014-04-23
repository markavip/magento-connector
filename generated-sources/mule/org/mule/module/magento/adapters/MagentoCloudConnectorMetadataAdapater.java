
package org.mule.module.magento.adapters;

import javax.annotation.Generated;
import org.mule.api.MetadataAware;
import org.mule.module.magento.MagentoCloudConnector;


/**
 * A <code>MagentoCloudConnectorMetadataAdapater</code> is a wrapper around {@link MagentoCloudConnector } that adds support for querying metadata about the extension.
 * 
 */
@Generated(value = "Mule DevKit Version 3.5.0-SNAPSHOT", date = "2014-04-23T03:07:34-05:00", comments = "Build master.1920.518defc")
public class MagentoCloudConnectorMetadataAdapater
    extends MagentoCloudConnectorCapabilitiesAdapter
    implements MetadataAware
{

    private final static String MODULE_NAME = "Magento";
    private final static String MODULE_VERSION = "2.1.2-SNAPSHOT";
    private final static String DEVKIT_VERSION = "3.5.0-SNAPSHOT";
    private final static String DEVKIT_BUILD = "master.1920.518defc";
    private final static String MIN_MULE_VERSION = "3.5";

    public String getModuleName() {
        return MODULE_NAME;
    }

    public String getModuleVersion() {
        return MODULE_VERSION;
    }

    public String getDevkitVersion() {
        return DEVKIT_VERSION;
    }

    public String getDevkitBuild() {
        return DEVKIT_BUILD;
    }

    public String getMinMuleVersion() {
        return MIN_MULE_VERSION;
    }

}
