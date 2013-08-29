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
package org.verifyxml.pharmacyupdate.handler;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.verifyxml.jaxb.exchange.pharmacyupdate.PharmacyUpdate;
import org.verifyxml.jaxb.exchange.providerlist.ProviderList;
import org.verifyxml.jaxb.exchange.vaccinedetails.VaccineDetails;
import org.verifyxml.jaxb.exchange.vaccinedetails.VaccineDetails.VaccinesDetails;

/**
 * OpenXDX Services Handler
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
public class OpenXDXServiceHandler {

    private static final Logger LOG = Logger.getLogger(OpenXDXServiceHandler.class.getName());  
    
    public List<ProviderList.PharmaciesDetails> getProviderList() {
        List<ProviderList.PharmaciesDetails> xml = null;
  
        PharmacyExchangeResource_JerseyClient client = new PharmacyExchangeResource_JerseyClient();
        try{
            xml = client.getPharmacies(ProviderList.class).getPharmaciesDetails();
        }catch (Exception e){
            LOG.log(Level.SEVERE, "Unable to retrieve data from OpenXDX RESTful services", e);
        }
        client.close();
        return xml; 
    
    }
    
    public List<VaccinesDetails> getVaccineDetailsList(int providerID){
         List<VaccinesDetails> xml = null;
  
        PharmacyExchangeResource_JerseyClient client = new PharmacyExchangeResource_JerseyClient();
        try{
            xml = client.getPharmacyDetails(VaccineDetails.class, Integer.toString(providerID)).getVaccinesDetails();
        }catch (Exception e){
            LOG.log(Level.SEVERE, "Unable to retrieve data from OpenXDX RESTful services", e);
        }
        client.close();
        return xml; 
    }

    public void updatePharmacy(PharmacyUpdate pharmacyUpdate){
        PharmacyExchangeResource_JerseyClient client = new PharmacyExchangeResource_JerseyClient();
        try{
            client.updatePharmacy(pharmacyUpdate);
        }catch (Exception e){
            LOG.log(Level.SEVERE, "Unable to update data through OpenXDX RESTful services", e);
        }
        client.close();
    }
    
    static class PharmacyExchangeResource_JerseyClient {

        private WebResource webResource;
        private Client client;
        private static final String BASE_URI = "http://verifyxml.org/restOpenXDXPharmacy/resources";

        public PharmacyExchangeResource_JerseyClient() {
            com.sun.jersey.api.client.config.ClientConfig config = new com.sun.jersey.api.client.config.DefaultClientConfig();
            client = Client.create(config);
            webResource = client.resource(BASE_URI).path("Exchange");
        }

        public <T> T getPharmacies(Class<T> responseType) throws UniformInterfaceException {
            WebResource resource = webResource;
            resource = resource.path("list");
            return resource.accept(javax.ws.rs.core.MediaType.APPLICATION_XML).get(responseType);
        }

        public ClientResponse updatePharmacy(Object requestEntity) throws UniformInterfaceException {
            return webResource.path("update").type(javax.ws.rs.core.MediaType.APPLICATION_XML).post(ClientResponse.class, requestEntity);
        }

        public <T> T getPharmacyDetails(Class<T> responseType, String ProviderID) throws UniformInterfaceException {
            WebResource resource = webResource;
            if (ProviderID != null) {
                resource = resource.queryParam("ProviderID", ProviderID);
            }
            resource = resource.path("details");
            return resource.accept(javax.ws.rs.core.MediaType.APPLICATION_XML).get(responseType);
        }

        public void close() {
            client.destroy();
        }
    }
    
}
