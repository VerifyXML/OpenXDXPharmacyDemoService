package org.verifyxml.openxdx;

import com.oracle.openxdx.XDXFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.transform.stream.StreamResult;

/**
 *
 * @author serega
 */
@WebService(serviceName = "PharmacyOpenXDXService", targetNamespace = "http://verifyxml.org/")
public class PharmacyOpenXDXDemo {
    // Defines JNDI name for Data source
    private static String CHICAGO_DATASOURCE_CONTEXT = "jdbc/CPHARM";

    // OpenXDX variables
    private static final String OPEN_XDX_DUMP_XML = "-open-xdx-dump.xml";
    private static final String OPEN_XDX_XML = "-open-xdx.xml";

    //CAM Templates
    private static final String PROVIDER_LOOKUP_QUERY_TEMPLATE =
        "/Providers-Lookup.cxf";
    private static final String VACCINES_LOOKUP_QUERY_TEMPLATE =
        "/Pharmacy-Vaccines-Lookup.cxf";
    private File providerTemplateFile;
    private File vaccinesTemplateFile;


    // Data source
    private DataSource datasourceChicago;

    public PharmacyOpenXDXDemo() {

        // Lookup and set DataSource
        try {
            Context initialContext = new InitialContext();
            if (initialContext == null) {
                System.out.println("JNDI problem. Cannot get InitialContext.");
            }
            datasourceChicago = (DataSource)initialContext.lookup(CHICAGO_DATASOURCE_CONTEXT);

            providerTemplateFile = extractResource(PROVIDER_LOOKUP_QUERY_TEMPLATE);
            vaccinesTemplateFile = extractResource(VACCINES_LOOKUP_QUERY_TEMPLATE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @WebMethod
    @WebResult(name = "ProviderResult")
    public String doProviderQuery(@WebParam(name = "ZIP") Integer zip) {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("$ZIPsearch", Integer.toString(zip));
        return getOpenXDX(providerTemplateFile, datasourceChicago, tokens);
    }

    @WebMethod
    @WebResult(name = "VaccinesResult")
    public String doVaccinesQuery(@WebParam(name = "providerID")
        Integer providerID) {
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("$ProviderID", Integer.toString(providerID));
        return getOpenXDX(vaccinesTemplateFile, datasourceChicago, tokens);
    }

    private String getOpenXDX(File cxfFile, DataSource datasource, Map<String, String> tokens) {
        String openXDXSource = null;
        File dataOutput = null;
        File openXDXFile = null;

        try {
            // Get new Processor
            com.oracle.openxdx.processor.Processor processor = XDXFactory.newProcessor();

            // Define data output and final result files
            dataOutput =
                    File.createTempFile(this.getClass().getName(), OPEN_XDX_DUMP_XML);

            openXDXFile =
                    File.createTempFile(this.getClass().getName(), OPEN_XDX_XML);

            // Set CAM Template
            processor.setTemplate(cxfFile);

            // Set Data source
            processor.setDataSource(datasource);

            // Extract data
            processor.extract(dataOutput, tokens);

            // Generate Open-XDX XML
            processor.generate(dataOutput, new StreamResult(openXDXFile));

            // Return Open-XDX as a String
            openXDXSource =
                    new Scanner(openXDXFile).useDelimiter("\\Z").next();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataOutput.deleteOnExit();
            openXDXFile.deleteOnExit();
        }
        return openXDXSource;
    }

    private File extractResource(String resourceName) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile(this.getClass().getName(), ".cxf");
            // read resource into InputStream
            InputStream inputStream =
                this.getClass().getResourceAsStream(resourceName);

            // write the inputStream to a FileOutputStream
            OutputStream out = new FileOutputStream(tmpFile);

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            inputStream.close();
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tmpFile;
    }

    @Override
    protected void finalize() throws Throwable {
        providerTemplateFile.deleteOnExit();
        vaccinesTemplateFile.deleteOnExit();
        super.finalize();
    }
}
