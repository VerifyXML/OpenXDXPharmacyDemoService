/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.verifyxml.pharmacysearch.handler;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import org.geocodes.postalcodes.Geonames;

/**
 * GeoSearch Handler
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
public class GeoSearchHandler {
    private static final Logger LOG = Logger.getLogger(GeoSearchHandler.class.getName());
    
    /**
     * Get list of Zip Codes with proximity
     * 
     * @param zip Original Zip Code
     * @param radius Proximity Radius
     * @return Geonames object
     */
    public Geonames getGeoCodes(String zip, String radius){
        
        Geonames geonames = null;
        // Do RESTful call
        String xml = doGeoSearchQuery(zip, radius);
        
        if(xml != null){
            try{
                // Unmarshall JAXB
                JAXBContext jaxbContext = JAXBContext.newInstance(Geonames.class);
                geonames = (Geonames) jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes()));               
            }catch (Exception e){
                LOG.log(Level.SEVERE, "Unable to process GeoNames XML", e);
            }
        }                  
       
        return geonames;
    }
    
    private String doGeoSearchQuery(String zip, String radius) {
        String xml = null;
  
        PharmacyXMLResource_JerseyClient client = new PharmacyXMLResource_JerseyClient();
        try{
            xml = client.getXml(String.class, zip, radius);
        }catch (Exception e){
            LOG.log(Level.SEVERE, "Unable to retrieve data from GeoNames RESTful services", e);
        }
        client.close();
        return xml;     
    }
    
    /**
     * Jersey Client implementation for GeoNames RESTful Web Services call
     */
    static class PharmacyXMLResource_JerseyClient {
        private static final String SERVICE_PROPERTIES = "/service.properties";
        private static final String FIND_POSTAL_CODE_PATH = "findNearbyPostalCodes";
        private static final String POSTAL_CODE_PARAM = "postalcode";
        private static final String RADIUS_PARAM = "radius";
        private static final String COUNTRY_CODE_PARAM = "country";
        private static final String USERNAME_PARAM = "username";
        private static final String STYLE_PARAM = "style";
        private static final String MAXROWS_PARAM = "maxRows";
        private static final String ZIP_URI_PROP = "ZIP_Service_URL";
        private static final String USERNAME_PROP = "ZIP_Service_User";
        private static final String MAXRADIUS_PROP = "ZIP_Service_MaxRadius";
        private static final String MAXROWS_PROP = "ZIP_Service_MaxRows";
        public static final String DEFAULT_RADIUS = "30";
        public static final String DEFAULT_COUNTRY = "US";
        public static final String XML_TYPE = "short";
        
        private WebResource webResource;
        private Client client;
        private Properties service_prop = new Properties();
  
        public PharmacyXMLResource_JerseyClient(WebResource webResource, Client client) {
            this.webResource = webResource;
            this.client = client;
        }

        public PharmacyXMLResource_JerseyClient() {            
            
            try {
                service_prop.load(OpenXDXServiceHandler.PharmacyXMLResource_JerseyClient.class.getResourceAsStream(SERVICE_PROPERTIES));
            } catch (IOException ex) {
                Logger.getLogger(OpenXDXServiceHandler.class.getName()).log(Level.SEVERE, "Unable to load properties file", ex);
            }
            com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig();
            client = Client.create(config);
            webResource = client.resource(service_prop.getProperty(ZIP_URI_PROP)).path(FIND_POSTAL_CODE_PATH);
        }

        public <T> T getXml(Class<T> responseType, String zip, String radius) throws UniformInterfaceException {
            WebResource resource = webResource;
            if (zip != null) {
                resource = resource.queryParam(POSTAL_CODE_PARAM, zip);
            }
            if (radius != null) {
                if(Integer.parseInt(radius) <= Integer.parseInt(service_prop.getProperty(MAXRADIUS_PROP))){
                    resource = resource.queryParam(RADIUS_PARAM, radius);
                }else{
                    resource = resource.queryParam(RADIUS_PARAM, DEFAULT_RADIUS);
                }
            }
            resource = resource.queryParam(COUNTRY_CODE_PARAM, DEFAULT_COUNTRY);
            resource = resource.queryParam(STYLE_PARAM, XML_TYPE);
            resource = resource.queryParam(MAXROWS_PARAM, service_prop.getProperty(MAXROWS_PROP));
            resource = resource.queryParam(USERNAME_PARAM, service_prop.getProperty(USERNAME_PROP));
            return resource.accept(javax.ws.rs.core.MediaType.APPLICATION_XML).get(responseType);
        }

        public void close() {
            client.destroy();
        }
    }      
}
