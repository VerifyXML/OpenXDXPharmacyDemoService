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

import com.oracle.openxdx.XDXFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Sample OpenXDX handler class for Pharmacy Search Demo. Provides support for invocation of OpenXDX API calls
 * 
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
public class OpenXDXHandler {
    
    private static final Logger LOG = Logger.getLogger(OpenXDXHandler.class.getName());
    
    // Defines JNDI name for Data source
    private static String CHICAGO_DATASOURCE_CONTEXT = "jdbc/CPHARM";

    // OpenXDX variables
    private static final String OPEN_XDX_DUMP_XML = "-open-xdx-dump.xml";

    //CAM Templates
    private static final String PROVIDER_LOOKUP_QUERY_TEMPLATE = "/ProvidersFullLookup.cxf";
    private static final String PROVIDER_LIST_TEMPLATE = "/ProviderList.cxf";
    private static final String VACCINE_DETAILS_TEMPLATE = "/VaccineDetails.cxf";
    public File providerLookupTemplateFile;
    public File providerListTemplateFile;
    public File vaccineDetailsTemplateFile;
    
    // Data source
    private DataSource datasourceChicago;
    
    /**
     * The main constructor. This constructor initiates DataSource object from JNDI.
     */
    public OpenXDXHandler() {        
        
        // Lookup and set DataSource
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                System.out.println("JNDI problem. Cannot get InitialContext.");
            }
            datasourceChicago = (DataSource)initialContext.lookup(CHICAGO_DATASOURCE_CONTEXT);

            providerLookupTemplateFile = extractResource(PROVIDER_LOOKUP_QUERY_TEMPLATE);
            providerListTemplateFile = extractResource(PROVIDER_LIST_TEMPLATE);
            vaccineDetailsTemplateFile = extractResource(VACCINE_DETAILS_TEMPLATE);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot initialize OpenXDXHandler", e);
        }
    }
    
    /**
     * Retrieve OpenXDX data. 
     * 
     * @param cxfFile CAM Template File
     * @param tokens Parameter tokens
     * @return OpenXDX XML Object as String
     */
    public String getOpenXDX(File cxfFile, Map<String, String> tokens) {
        
        String xmlString = null;
        
        // Set Output Stream for XML
        ByteArrayOutputStream out = null;
        
        // Set temporary data file
        File dataOutput = null;
        
        try {
            out = new ByteArrayOutputStream();
            
            // Get new Processor
            com.oracle.openxdx.processor.Processor processor = XDXFactory.newProcessor();

            // Define data output and final result files
            dataOutput =
                    File.createTempFile(this.getClass().getName(), OPEN_XDX_DUMP_XML);

            // Set CAM Template
            processor.setTemplate(cxfFile);

            // Set Data source
            processor.setDataSource(datasourceChicago);

            // Extract data
            processor.extract(dataOutput, tokens);

            // Generate Open-XDX XML
            StreamResult result = new StreamResult(out);            
            processor.generate(dataOutput, result);        
            // Set XML String
            xmlString = out.toString();
        } catch (Exception e) {
           LOG.log(Level.SEVERE, "Unable to extract OpenXDX Data", e);
        } finally {
            if(dataOutput != null)
                dataOutput.deleteOnExit();
            if(out != null){
                try {
                    out.close();
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }               
            }
        }
        return xmlString;
    }
    
    /**
     * Extracting application resource to file system
     * 
     * @param resourceName
     * @return File object
     */
    private File extractResource(String resourceName) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(this.getClass().getName(), ".cxf");
            // read resource into InputStream
            InputStream inputStream =
                this.getClass().getResourceAsStream(resourceName);

            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(tmpFile);

            int read;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            inputStream.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, null, e);
        }
        return tmpFile;
    }
     
    @Override
    protected void finalize() throws Throwable {        
        providerLookupTemplateFile.deleteOnExit();
        providerListTemplateFile.deleteOnExit();
        vaccineDetailsTemplateFile.deleteOnExit();
        super.finalize();
    }   
}
