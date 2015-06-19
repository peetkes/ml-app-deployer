package com.marklogic.rest.util;

import org.jdom2.Namespace;

/**
 * Provides some convenience methods for the XML response from resources endpoints
 */
public class ResourcesFragment extends Fragment {

    public ResourcesFragment(Fragment f) {
        super(f);
    }

    public ResourcesFragment(String xml, Namespace[] namespaces) {
        super(xml, namespaces);
    }

    public int getResourceCount() {
        return Integer.parseInt(getElementValues(
                "/node()/*[local-name(.) = 'list-items']/*[local-name(.) = 'list-count']").get(0));
    }

    public boolean resourceExists(String resourceIdOrName) {
        String xpath = "/node()/*[local-name(.) = 'list-items']/node()"
                + "[*[local-name(.) = 'nameref'] = '%s' or *[local-name(.) = 'idref'] = '%s']";
        xpath = String.format(xpath, resourceIdOrName, resourceIdOrName);
        return elementExists(xpath);
    }
}
