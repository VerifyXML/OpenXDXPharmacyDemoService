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
 * Portions VerifyXML.org
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
import org.verifyxml.jaxb.ProvidersFullLookup;

/**
 * OpenXDX Handler
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
public class OpenXDXServiceHandler {
    private static final Logger LOG = Logger.getLogger(OpenXDXServiceHandler.class.getName());   
    
    /**
     * Get list of Providers List by Zip and Vaccine Type
     * @param zip Zip Codes list. Can be one or multiple. Use commas to separate multiple zip codes i.e. 60624,60619
     * @param vaccType Vaccine Type Codes list. Optional parameter to narrow down the list of Providers. Can be one or multiple. Use commas to separate multiple zip codes i.e. 1,2,3
     * @return ProvidersFullLookup object
     */
    public ProvidersFullLookup getPharmacyData(String zip, String vaccType){
        ProvidersFullLookup providerLookup = null;
        // Do RESTful call
        String xml = doProviderQuery(zip, vaccType);
        
        if(xml != null){
            try{
                // Unmarshall JAXB
                JAXBContext jaxbContext = JAXBContext.newInstance(ProvidersFullLookup.class);
                providerLookup = (ProvidersFullLookup) jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(xml.getBytes()));               
            }catch (Exception e){
                LOG.log(Level.SEVERE, "Unable to process OpenXDX XML", e);
            }
        }                
        
        return providerLookup;
    }
    
    private String doProviderQuery(String zip, String vaccType) {
        String xml = null;
  
        PharmacyXMLResource_JerseyClient client = new PharmacyXMLResource_JerseyClient();
        try{
            xml = client.getXml(String.class, zip, vaccType);
        }catch (Exception e){
            LOG.log(Level.SEVERE, "Unable to retrieve data from OpenXDX RESTful services", e);
        }
        client.close();
        return xml; 
    
    }

    /**
     * Jersey Client implementation for OpenXDX RESTful Web Services call
     */
    static class PharmacyXMLResource_JerseyClient {
        private static final String SERVICE_PROPERTIES = "/service.properties";
        private static final String XML_PATH = "XML";
        private static final String ZIP_PARAM = "zip";
        private static final String VACC_TYPE_PARAM = "vaccType";
        private static final String XML_URI_PROP = "XML_Service_URL";
        
        private WebResource webResource;
        private Client client;
  
        public PharmacyXMLResource_JerseyClient(WebResource webResource, Client client) {
            this.webResource = webResource;
            this.client = client;
        }

        public PharmacyXMLResource_JerseyClient() {
            
            Properties service_prop = new Properties();
            try {
                service_prop.load(PharmacyXMLResource_JerseyClient.class.getResourceAsStream(SERVICE_PROPERTIES));
            } catch (IOException ex) {
                Logger.getLogger(OpenXDXServiceHandler.class.getName()).log(Level.SEVERE, "Unable to load properties file", ex);
            }
            com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig();
            client = Client.create(config);
            webResource = client.resource(service_prop.getProperty(XML_URI_PROP)).path(XML_PATH);
        }

        public <T> T getXml(Class<T> responseType, String zip, String vaccType) throws UniformInterfaceException {
            WebResource resource = webResource;
            if (zip != null) {
                resource = resource.queryParam(ZIP_PARAM, zip);
            }
            if (vaccType != null) {
                resource = resource.queryParam(VACC_TYPE_PARAM, vaccType);
            }
            return resource.accept(javax.ws.rs.core.MediaType.APPLICATION_XML).get(responseType);
        }

        public void close() {
            client.destroy();
        }
    }   
    
}
