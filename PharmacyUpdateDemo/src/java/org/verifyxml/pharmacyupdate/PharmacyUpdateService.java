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
 * Session Bean for PrimeFaces JSF
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@ManagedBean
@SessionScoped
public class PharmacyUpdateService implements Serializable{
  
    private static final Logger LOG = Logger.getLogger(PharmacyUpdateService.class.getName());
    
    // Bean properties
    private transient List<PharmaciesDetails> pharmaciesList;
    private transient PharmaciesDetails selectedPharmacy;
    private transient List<VaccineDetails.VaccinesDetails> vaccineDetailsList = new ArrayList<VaccineDetails.VaccinesDetails>();   
    
    /**
     * Load available list of pharmacies from RESTful service
     * 
     * @return 
     */
    public String doPharmaciesLoad() {
        try {
            
            // Set Handler
            OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
            
            // Gel the list
            pharmaciesList = openXDXService.getProviderList();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "System error", e);
        }
        return null;
    }
    
    /**
     * Load vaccine details for selected pharmacy
     * 
     * @return 
     */
    public String doVaccineDetailLoad() {
        
        // Check if pharmacy has been selected
        if (selectedPharmacy != null) {
            try {
                
                // Set Handler
                OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
                
                // Get the list
                vaccineDetailsList = openXDXService.getVaccineDetailsList(selectedPharmacy.getProviderID());
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "System error", e);
            }
        }
        return null;
    }
    
    /**
     * Perform RESTful POST call in order to update vaccine details info
     * 
     * @return 
     */
    public String doVaccineUpdate() {
        
        // Check if vaccile list is available
        if (vaccineDetailsList != null) {
            
            // Set Faces Context for messages
            FacesContext context = FacesContext.getCurrentInstance();

            // Build PharmacyUpdate object for RESTful POST
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
                
                // Call update service
                openXDXService.updatePharmacy(pharmacyUpdate);
                
                // Load updated data
                doVaccineDetailLoad();
                
                // Set success message
                context.addMessage(null, new FacesMessage("Successful", "Records has been updated."));
            } catch (Exception e) {
                // Set error message
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
