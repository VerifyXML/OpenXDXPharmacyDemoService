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
 * Portions Copyrighted 2013 VerifyXml.org
 */
package org.verifyxml.openxdx;

import com.oracle.openxdx.XDXFactory;
import com.oracle.openxdx.processor.Processor;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.verifyxml.openxdx.jaxb.providerslookup.Providers;
import org.verifyxml.openxdx.jaxb.vaccineslookup.Vaccines;
import org.xml.sax.InputSource;

/**
 *
 * @author Serge Leontiev <sergeleo@users.sourceforge.net>
 */
@WebService(serviceName = "PharmacyOpenXDXService", targetNamespace = "http://verifyxml.org/")
public class PharmacyOpenXDXDemo {

    private static final Logger LOG = Logger.getLogger(PharmacyOpenXDXDemo.class.getName());
    // Defines JNDI name for Data source
    private static String CHICAGO_DATASOURCE_CONTEXT = "jdbc/CPHARM";
    //CAM Templates
    private static final String PROVIDER_LOOKUP_QUERY_TEMPLATE =
            "/Providers-Lookup.cxf";
    private static final String VACCINES_LOOKUP_QUERY_TEMPLATE =
            "/Pharmacy-Vaccines-Lookup.cxf";
    // Data source
    private DataSource datasourceChicago;

    public PharmacyOpenXDXDemo() {

        // Lookup and set DataSource
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                System.out.println("JNDI problem. Cannot get InitialContext.");
            }
            datasourceChicago = (DataSource) initialContext.lookup(CHICAGO_DATASOURCE_CONTEXT);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "DataSource Error", e);
        }
    }

    @WebMethod
    @WebResult(name = "ProviderResult")
    public Providers doProviderQuery(@WebParam(name = "ZIP") Integer zip) {
        Providers providers = null;
        try {
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(Providers.class);
            Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("$ZIPsearch", Integer.toString(zip));

            providers =
                    (Providers) jaxbUnMarshaller.unmarshal(getOpenXDX(PROVIDER_LOOKUP_QUERY_TEMPLATE, datasourceChicago, tokens));

        } catch (JAXBException ex) {
            Logger.getLogger(PharmacyOpenXDXDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return providers;
    }

    @WebMethod
    @WebResult(name = "VaccinesResult")
    public Vaccines doVaccinesQuery(@WebParam(name = "providerID") Integer providerID) {
        Vaccines vaccines = null;
        try {
            JAXBContext jaxbContext =
                    JAXBContext.newInstance(Vaccines.class);
            Unmarshaller jaxbUnMarshaller = jaxbContext.createUnmarshaller();
            Map<String, String> tokens = new HashMap<String, String>();
            tokens.put("$ProviderID", Integer.toString(providerID));

            vaccines =
                    (Vaccines) jaxbUnMarshaller.unmarshal(getOpenXDX(VACCINES_LOOKUP_QUERY_TEMPLATE, datasourceChicago, tokens));

        } catch (JAXBException ex) {
            Logger.getLogger(PharmacyOpenXDXDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return vaccines;
    }

    private StreamSource getOpenXDX(String cxfFile, DataSource datasource, Map<String, String> tokens) {
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
            processor.setTemplate(new InputSource(PharmacyOpenXDXDemo.class.getResourceAsStream(cxfFile)));

            // Set Data source
            processor.setDataSource(datasource);

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
