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
package org.verifyxml.rest.pharmacyservices;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import org.verifyxml.jaxb.pharmacyupdate.PharmacyUpdate;
import org.verifyxml.jaxb.providerlist.ProviderList;
import org.verifyxml.jaxb.vaccinedetails.VaccineDetails;
import org.xml.sax.InputSource;

/**
 *
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@Path("/Exchange")
public class PharmacyExchangeResource {

    private static final Logger LOG = Logger.getLogger(PharmacyExchangeResource.class.getName());
    // OpenXDX Handler
    private OpenXDXHandler openXDXHandler;

    /**
     * Default constructor. Initializes OpenXDX Handler
     */
    public PharmacyExchangeResource() throws Exception {
        openXDXHandler = new OpenXDXHandler();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_XML)
    public ProviderList getPharmacies() {
        ProviderList providerList = null;
        try {
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(ProviderList.class);
            Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();

            providerList = (ProviderList) jaxbUnMarshaller.unmarshal(openXDXHandler.getOpenXDX(OpenXDXHandler.PROVIDER_LIST_TEMPLATE, null));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in XML Web Service", e);
        }
        return providerList;
    }

    @GET
    @Path("/details")
    @Produces(MediaType.APPLICATION_XML)
    public VaccineDetails getPharmacyDetails(@QueryParam("ProviderID") int providerId) {
        VaccineDetails vaccineDetails = null;

        try {
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(VaccineDetails.class);
            Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();

            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("$ProviderID", Integer.toString(providerId));

            vaccineDetails =
                    (VaccineDetails) jaxbUnMarshaller.unmarshal(openXDXHandler.getOpenXDX(OpenXDXHandler.VACCINE_DETAILS_TEMPLATE, tokens));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in XML Web Service", e);
        }
        return vaccineDetails;
    }

    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_XML)
    public Response updatePharmacy(PharmacyUpdate pharmacyUpdate) {
        // Set Response builder
        Response.ResponseBuilder response;

        ByteArrayOutputStream xmlOut = null;
        try {
            // Marshall object to stream

            xmlOut = new ByteArrayOutputStream();
            JAXBContext jaxbContext = JAXBContext.newInstance(PharmacyUpdate.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.marshal(pharmacyUpdate, xmlOut);

            // Publish data through OpenXDX
            openXDXHandler.putOpenXDX(OpenXDXHandler.PHARMACY_UPDATE_TEMPLATE, new InputSource(new ByteArrayInputStream(xmlOut.toByteArray())));

            response = Response.ok();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error in XML Web Service", e);
            response = Response.serverError();
        } finally {
            if (xmlOut != null) {
                IOUtils.closeQuietly(xmlOut);
            }
        }

        return response.build();
    }
}
