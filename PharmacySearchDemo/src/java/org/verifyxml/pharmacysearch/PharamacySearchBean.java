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
package org.verifyxml.pharmacysearch;

import org.verifyxml.pharmacysearch.handler.OpenXDXServiceHandler;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.geocodes.postalcodes.Geonames;
import org.geocodes.postalcodes.Geonames.Code;
import org.primefaces.event.map.OverlaySelectEvent;
import org.primefaces.event.map.StateChangeEvent;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;
import org.verifyxml.jaxb.ProvidersFullLookup;
import org.verifyxml.jaxb.ProvidersFullLookup.PharmaciesDetails;
import org.verifyxml.pharmacysearch.handler.GeoSearchHandler;

/**
 * Session Bean for PrimeFaces JSF
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@ManagedBean
@SessionScoped
public class PharamacySearchBean implements Serializable {
    private static final Logger LOG = Logger.getLogger(PharamacySearchBean.class.getName());
    
    private static final String ARROW_IMAGE_URL = "http://www.google.com/mapfiles/arrow.png";
    private static final String CHICAGO_LATLONG = "41.876719, -87.649612";

    // Bean properties
    private int zipSearch;
    private int searchRadius = 0;
    private transient List<String> selectedVaccines;
    private Map<String,String> vaccines;
    private transient List<PharmaciesDetails> pharmaciesDetails;
    private transient Map<String,Marker> resultMarkers;
    private transient MapModel mapModel;    
    private Marker marker;  
    private LatLng mapCenter;
    private int zoomLevel = 12;
    private transient String selectedProviderId;  
        
    /**
     * Creates a new instance of PharamacySearchBean
     */
    public PharamacySearchBean() {
        // Init list of Vaccines
        vaccines = new HashMap<String,String>();
        vaccines.put("Flu", "1");
        vaccines.put("Coccal", "2");
        vaccines.put("Shingles", "3");
        vaccines.put("Hepatitis", "4");
        vaccines.put("HPV", "5");
        vaccines.put("MMR", "6");
        vaccines.put("TD", "7");
        vaccines.put("Varicella", "8");
    }
    
    public int getZipSearch() {
        return zipSearch;
    }

    public void setZipSearch(int zipSearch) {
        this.zipSearch = zipSearch;
    }
    
    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int searchRadius) {
        this.searchRadius = searchRadius;
    }

    public List<String> getSelectedVaccines() {
        return selectedVaccines;
    }

    public void setSelectedVaccines(List<String> selectedVaccines) {
        this.selectedVaccines = selectedVaccines;
    }

    public Map<String, String> getVaccines() {
        return vaccines;
    }

    public List<PharmaciesDetails> getPharmaciesDetails() {
        return pharmaciesDetails;
    }

    /**
     * Performs Zip Search
     * @return 
     */
    public String doZipSearch() {
        // Reset map and providers
        mapModel = new DefaultMapModel();
        resultMarkers = new HashMap<String, Marker>();
        pharmaciesDetails = new ArrayList<PharmaciesDetails>();
        mapCenter = null;
        zoomLevel = 12;
        
        if(this.zipSearch > 0){
            try{
                // Set Handlers
                OpenXDXServiceHandler openXDXService = new OpenXDXServiceHandler();
                GeoSearchHandler geoSearchService = new GeoSearchHandler();
                
                // Build Vaccine Types List
                String vTypes = "NULL";
                if(selectedVaccines != null && selectedVaccines.size() > 0){
                    StringBuilder vaccTypes = new StringBuilder();
                    int i = 1;
                    for(String vacType:selectedVaccines){
                        vaccTypes.append(vacType);
                        if(i < selectedVaccines.size()){
                            vaccTypes.append(",");
                        }
                        i++;
                    }
                    vTypes = vaccTypes.toString();                
                }

                // Get Zip codes for specified radius
                Geonames geocodes = geoSearchService.getGeoCodes(String.format("%05d",zipSearch), Integer.toString(searchRadius));

                // Build Zip codes list
                StringBuilder zipList = new StringBuilder();
                if(geocodes != null){                    
                    int j = 1;            
                    for(Code geocode:geocodes.getCode()){
                        zipList.append(geocode.getPostalcode());
                        if(j < geocodes.getCode().size()){
                                zipList.append(",");
                            }
                            j++;
                    }
                }else{
                    zipList.append(zipSearch);
                }

                if(zipList.length() > 0){
                    
                    ProvidersFullLookup providersLookup = openXDXService.getPharmacyData(zipList.toString(), vTypes);
                    
                    if(providersLookup != null && !providersLookup.getPharmaciesDetails().isEmpty()){
                        pharmaciesDetails = providersLookup.getPharmaciesDetails();
                        if(pharmaciesDetails != null && !pharmaciesDetails.isEmpty()){
                            for(PharmaciesDetails details:pharmaciesDetails){
                                Marker pharmacyMarker = null;
                                //FIXME temporary check for dummy data
                                if(!details.getProviderName().getValue().equals("string")){
                                    pharmacyMarker =  new Marker(
                                        new LatLng(
                                            Double.parseDouble(details.getYcoordValue().toString()), 
                                            Double.parseDouble(details.getXcoordValue().toString())), 
                                        details.getProviderName().getValue(), details);
                                }
                                if(pharmacyMarker != null){
                                    resultMarkers.put(Integer.toString(details.getProviderID()), pharmacyMarker);
                                    mapModel.addOverlay(pharmacyMarker);  
                                }
                            }
                            if(!resultMarkers.isEmpty()){
                                mapCenter = new LatLng(Double.parseDouble(pharmaciesDetails.get(0).getYcoordValue().toString()), 
                                        Double.parseDouble(pharmaciesDetails.get(0).getXcoordValue().toString()));
                                zoomLevel = 15;
                                resultMarkers.get(Integer.toString(pharmaciesDetails.get(0).getProviderID())).setIcon(ARROW_IMAGE_URL);
                            }
                        }
                    }                   
                }
            }catch (Exception e){
                LOG.log(Level.SEVERE, "System error", e);
            }
        }
        return null;
    }

    public MapModel getMapModel() {        
        return mapModel;
    }
    
    public void onMarkerSelect(OverlaySelectEvent event) {  
        marker = (Marker) event.getOverlay();  
    }  
      
    public Marker getMarker() {  
        return marker;  
    } 
    
    public PharmaciesDetails getMarkerPharmacyDetails() {
        PharmaciesDetails pharmacy = null;
        if(marker != null && marker.getData() != null)
            pharmacy = (PharmaciesDetails) marker.getData();
        return pharmacy;  
    } 

    public Marker getResultMarker(String providerID){
        return resultMarkers.get(providerID);
    }
    
    public String onResultSelect() {
        // Reset markers
        if(resultMarkers != null){
            for(Marker m:resultMarkers.values()){
                m.setIcon("");
            }
            if(selectedProviderId != null && resultMarkers.containsKey(selectedProviderId)){
                resultMarkers.get(selectedProviderId).setIcon(ARROW_IMAGE_URL);      
            }
        }
        return null;
    }  

    public String getSelectedProviderId() {
        return selectedProviderId;
    }

    public void setSelectedProviderId(String selectedProviderId) {
        if(resultMarkers != null){
            for(Marker m:resultMarkers.values()){
                m.setIcon("");
            }
            if(resultMarkers.containsKey(selectedProviderId)){
                resultMarkers.get(selectedProviderId).setIcon(ARROW_IMAGE_URL); 
                mapCenter = resultMarkers.get(selectedProviderId).getLatlng();
                zoomLevel = 15;
            }
        }
        this.selectedProviderId = selectedProviderId;
        
    }

    public void onMapStateChange(StateChangeEvent event) {  
        mapCenter = event.getCenter();  
        zoomLevel = event.getZoomLevel();          
    }

    public String getLatLong() {        
        if(mapCenter != null ){
            return Double.toString(mapCenter.getLat()) + ", " + Double.toString(mapCenter.getLng());
        }else{
            return CHICAGO_LATLONG;
        }
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
    
}
