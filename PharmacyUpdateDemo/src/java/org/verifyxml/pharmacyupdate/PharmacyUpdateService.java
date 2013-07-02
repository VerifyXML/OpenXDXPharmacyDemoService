/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.verifyxml.pharmacyupdate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.verifyxml.jaxb.exchange.pharmacyupdate.ObjectFactory;
import org.verifyxml.jaxb.exchange.pharmacyupdate.PharmacyUpdate;
import org.verifyxml.jaxb.exchange.providerlist.ProviderList.PharmaciesDetails;
import org.verifyxml.jaxb.exchange.vaccinedetails.VaccineDetails;
import org.verifyxml.jaxb.exchange.vaccinedetails.VaccineDetails.VaccinesDetails;
import org.verifyxml.pharmacyupdate.handler.OpenXDXServiceHandler;

/**
 *
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@ManagedBean
@SessionScoped
public class PharmacyUpdateService implements Serializable{
  
    private static final Logger LOG = Logger.getLogger(PharmacyUpdateService.class.getName());
    
    private transient List<PharmaciesDetails> pharmaciesList;
    private transient PharmaciesDetails selectedPharmacy;
    private transient List<VaccineDetails.VaccinesDetails> vaccineDetailsList = new ArrayList<VaccineDetails.VaccinesDetails>();   
    
    public String doPharmaciesLoad() {
        try {
            // Set Handler
            OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
            pharmaciesList = openXDXService.getProviderList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "System error", e);
        }
        return null;
    }
    
    public String doVaccineDetailLoad() {
        if (selectedPharmacy != null) {
            try {
                // Set Handler
                OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
                vaccineDetailsList = openXDXService.getVaccineDetailsList(selectedPharmacy.getProviderID());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "System error", e);
            }
        }
        return null;
    }
    
    public String doVaccineUpdate() {
        if (vaccineDetailsList != null) {
            FacesContext context = FacesContext.getCurrentInstance();

            ObjectFactory factory = new ObjectFactory();
            PharmacyUpdate pharmacyUpdate = factory.createPharmacyUpdate();
            for (VaccinesDetails vaccDetails : vaccineDetailsList) {
                PharmacyUpdate.VaccinesDetails vaccinesDetails = factory.createPharmacyUpdateVaccinesDetails();
                vaccinesDetails.setUniqueID(vaccDetails.getUniqueID());
                vaccinesDetails.setProviderID(vaccDetails.getProviderID());
                vaccinesDetails.setInStockCode(vaccDetails.getInStockCode().getValue());
                pharmacyUpdate.getVaccinesDetails().add(vaccinesDetails);
            }

            try {
                // Set Handler
                OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
                openXDXService.updatePharmacy(pharmacyUpdate);
                doVaccineDetailLoad();
                context.addMessage(null, new FacesMessage("Successful", "Records has been updated."));
            } catch (Exception e) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Unable to update records. See server log for details."));
                LOG.log(Level.SEVERE, "System error", e);
            }
        }
        
        return null;
    }
    
    public List<PharmaciesDetails> getPharmaciesList() {
        return pharmaciesList;
    }   

    public PharmaciesDetails getSelectedPharmacy() {
        return selectedPharmacy;
    }

    public void setSelectedPharmacy(PharmaciesDetails selectedPharmacy) {
        this.selectedPharmacy = selectedPharmacy;
    }

    public List<VaccineDetails.VaccinesDetails> getVaccineDetailsList() {
        return vaccineDetailsList;
    }    
    
}
