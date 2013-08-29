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

import com.oracle.openxdx.XDXFactory;
import com.oracle.openxdx.processor.Processor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

/**
 * Sample OpenXDX handler class for Pharmacy Search Demo. Provides support for invocation of OpenXDX API calls
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
public class OpenXDXHandler {
    
    private static final Logger LOG = Logger.getLogger(OpenXDXHandler.class.getName());
    
    // Defines JNDI name for Data source
    private static String CHICAGO_DATASOURCE_CONTEXT = "jdbc/CPHARM";

    //CAM Templates
    public static final String PROVIDER_LOOKUP_QUERY_TEMPLATE = "/ProvidersFullLookup.cxf";
    public static final String PROVIDER_LIST_TEMPLATE = "/ProviderList.cxf";
    public static final String VACCINE_DETAILS_TEMPLATE = "/VaccineDetails.cxf";
    public static final String PHARMACY_UPDATE_TEMPLATE = "/PharmacyUpdate.cxf";
    
    // Data source
    private DataSource datasourceChicago;
    
    /**
     * The main constructor. This constructor initiates DataSource object from JNDI.
     */
    public OpenXDXHandler() throws Exception {        
        
        // Lookup and set DataSource
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                System.out.println("JNDI problem. Cannot get InitialContext.");
            }
            datasourceChicago = (DataSource)initialContext.lookup(CHICAGO_DATASOURCE_CONTEXT);

        } catch (Exception e) {            
            LOG.log(Level.SEVERE, "Cannot initialize OpenXDXHandler", e);
            throw new Exception(e);
        }
    }
    
    public void putOpenXDX(String cxfFile, InputSource xml) throws Exception{
        try {
            
            // Get new Processor
            com.oracle.openxdx.processor.Processor processor = XDXFactory.newProcessor();

            // Set CAM Template
            processor.setTemplate(new InputSource(OpenXDXHandler.class.getResourceAsStream(cxfFile)));

            // Set Data source
            processor.setDataSource(datasourceChicago);

            // Extract data
            processor.publish(xml);

        } catch (Exception e) {
           LOG.log(Level.SEVERE, "Unable to publish OpenXDX Data", e);
           throw new Exception(e);
        }
    }
    
    /**
     * Retrieve OpenXDX data. 
     * 
     * @param cxfFile CAM Template File
     * @param tokens Parameter tokens
     * @return OpenXDX XML Object as String
     */
    public StreamSource getOpenXDX(String cxfFile, Map<String, String> tokens) throws Exception {
        StreamSource xdxSource = null;

        // Set Output Stream for XML
        ByteArrayOutputStream outOpenXDX = null;

        // Set temporary data
        ByteArrayOutputStream dataExtract = null;
        try {

            outOpenXDX = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(outOpenXDX);

            // Get new Processor
            Processor processor = XDXFactory.newProcessor();

            // Define data output stream
            dataExtract = new ByteArrayOutputStream();

            // Set CAM Template
            processor.setTemplate(new InputSource(OpenXDXHandler.class.getResourceAsStream(cxfFile)));

            // Set Data source
            processor.setDataSource(datasourceChicago);

            // Extract data
            processor.extract(new OutputStreamWriter(dataExtract), tokens);

            // Generate Open-XDX XML
            processor.generate(new InputSource(new ByteArrayInputStream(dataExtract.toByteArray())), result);

            // Set XML Source
            xdxSource = new StreamSource(new ByteArrayInputStream(outOpenXDX.toByteArray()));

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "OpenXDX Error", e);
        } finally {
            if (outOpenXDX != null) {
                IOUtils.closeQuietly(outOpenXDX);
            }
            if (dataExtract != null) {
                IOUtils.closeQuietly(dataExtract);
            }
        }
        return xdxSource;
    }
    
}
