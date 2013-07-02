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
package org.verifyxml.rest.pharmacyservices;

import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 * Sample JSON RESTful Web Service for Pharmacy Search Demo.
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@Path("/JSON")
public class PharmacyJSONResource {
    private static final Logger LOG = Logger.getLogger(PharmacyJSONResource.class.getName());    
    
    // OpenXDX Handler
    private OpenXDXHandler openXDXHandler;    
   
    /**
     * Default constructor. Initializes OpenXDX Handler
     */
    public PharmacyJSONResource() {
        openXDXHandler = new OpenXDXHandler();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJSON(@QueryParam("zip") String zip, @QueryParam("vaccType") String vaccType) {
        // Set Response builder
        Response.ResponseBuilder response;
        try{
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("$ZIPsearch", zip);        
            if(vaccType == null){
                tokens.put("$VaccineTypeID", "NULL");
            }else{
                tokens.put("$VaccineTypeID", vaccType);
            }
            String xml = openXDXHandler.getOpenXDX(openXDXHandler.providerLookupTemplateFile, tokens);
            if(xml != null){
                response = Response.ok(convertXMLtoJSON(xml), MediaType.APPLICATION_JSON);           
            }else{
                response = Response.serverError();           
            }
        }catch (Exception e){
            response = Response.serverError();           
            LOG.log(Level.SEVERE, "Error in JSON Web Service", e);
        }
        return response.build();
    }
    
    private String convertXMLtoJSON(String xml) throws IOException{
        String jsonResult = null;
        ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        /*
         * If we want to insert JSON array boundaries for multiple elements,
         * we need to set the <code>autoArray</code> property.
         * If our XML source was decorated with <code>&lt;?xml-multiple?&gt;</code>
         * processing instructions, we'd set the <code>multiplePI</code>
         * property instead.
         * With the <code>autoPrimitive</code> property set, element text gets
         * automatically converted to JSON primitives (number, boolean, null).
         */
        JsonXMLConfig config = new JsonXMLConfigBuilder()
                .autoArray(true)
                .autoPrimitive(true)
                .prettyPrint(true)
                .build();
        try {
                /*
                 * Create reader (XML).
                 */
                XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);

                /*
                 * Create writer (JSON).
                 */
                XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(output);

                /*
                 * Copy events from reader to writer.
                 */
                writer.add(reader);

                /*
                 * Close reader/writer.
                 */
                reader.close();
                writer.close();
                
                jsonResult = output.toString("UTF-8");
        } catch (XMLStreamException ex) {
            Logger.getLogger(PharmacyJSONResource.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PharmacyJSONResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
                /*
                 * As per StAX specification, XMLEventReader/Writer.close() doesn't close
                 * the underlying stream.
                 */
                output.close();
                input.close();
        }
        return jsonResult;
    }
}
