/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
