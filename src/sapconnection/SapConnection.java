package sapconnection;

import com.sap.conn.jco.AbapException;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.JCoStructure;
import com.sap.conn.jco.JCoTable;
import com.sap.conn.jco.ext.DestinationDataProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

public class SapConnection {
    
     final static String ABAP_AS_POOLED="ABAP_AS_WITH_POOL";
    
    static
    {
        Properties connectProperties=new Properties();
        connectProperties.setProperty(DestinationDataProvider.JCO_ASHOST, "10.10.100.52");
        connectProperties.setProperty(DestinationDataProvider.JCO_SAPROUTER, "/H/168.234.192.205/S/3299");
        connectProperties.setProperty(DestinationDataProvider.JCO_SYSNR, "00");
        connectProperties.setProperty(DestinationDataProvider.JCO_CLIENT,"241");
        connectProperties.setProperty(DestinationDataProvider.JCO_USER, "jrecinos");
        connectProperties.setProperty(DestinationDataProvider.JCO_PASSWD,"Manager$15");
        connectProperties.setProperty(DestinationDataProvider.JCO_LANG,"ES");
        //Numero máximo de connection que puede ser abierto ell destino
        connectProperties.setProperty(DestinationDataProvider.JCO_POOL_CAPACITY,"3");
        
        //Número máximo de connecciones activas
        connectProperties.setProperty(DestinationDataProvider.JCO_PEAK_LIMIT,"10");
        
        createDataFile(ABAP_AS_POOLED,"jcoDestination",connectProperties);     
    }

    static void createDataFile(String name, String suffix, Properties properties)
    {
        File cfg= new File(name+"."+suffix);
        if(!cfg.exists())
        {
            try
            {
                FileOutputStream fos= new FileOutputStream(cfg,false);
                properties.store(fos, "Solo es un test");
                fos.close();
            }catch(Exception e)
            {
                throw new RuntimeException("Unable to create the destination "+cfg.getName(),e);
            }
        }
    }
    
    public static void exeFunctionCall() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        JCoFunction function=destination.getRepository().getFunction("STFC_CONNECTION");
        
        if(function == null)       
            throw new RuntimeException("BAPI_COMPANYCODE_GETLIST not found in SAP.");
            //Se recupera el importparmeterList() y se fija un valor
            function.getImportParameterList().setValue("REQUTEXT","Hello Word desde Sap");
            
            try
            {
                function.execute(destination);
            }
            catch(AbapException e)
            {
                System.out.println(e.toString());
                return;
            }
            System.out.println("STFC_CONNECTION finished: ");
            System.out.println("Echo: "+function.getExportParameterList().getString("ECHOTEXT"));
            System.out.println("Response: "+function.getExportParameterList().getString("RESPTEXT"));
            System.out.println();
        
    }
    
    public static void step4WorkWithTable() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        JCoFunction function = destination.getRepository().getFunction("BAPI_COMPANYCODE_GETLIST");
        if(function == null)
            throw new RuntimeException("BAPI_COMPANYCODE_GETLIST not found in SAP.");

        try
        {
            function.execute(destination);
        }
        catch(AbapException e)
        {
            System.out.println(e.toString());
            return;
        }
        
        JCoStructure returnStructure = function.getExportParameterList().getStructure("RETURN");
        if (! (returnStructure.getString("TYPE").equals("")||returnStructure.getString("TYPE").equals("S"))  )   
        {
           throw new RuntimeException(returnStructure.getString("MESSAGE"));
        }
        
        JCoTable codes = function.getTableParameterList().getTable("COMPANYCODE_LIST");
        for (int i = 0; i < codes.getNumRows(); i++) 
        {
            codes.setRow(i);
            System.out.println("Código compañia: "+codes.getString("COMP_CODE") + '\t' +"Nombre compañia: "+ codes.getString("COMP_NAME"));
        }

        //move the table cursor to first row
        codes.firstRow();
        for (int i = 0; i < codes.getNumRows(); i++, codes.nextRow()) 
        {
            function = destination.getRepository().getFunction("BAPI_COMPANYCODE_GETDETAIL");
            if (function == null) 
                throw new RuntimeException("BAPI_COMPANYCODE_GETDETAIL not found in SAP.");

            function.getImportParameterList().setValue("COMPANYCODEID", codes.getString("COMP_CODE"));
            
            //We do not need the addresses, so set the corresponding parameter to inactive.
            //Inactive parameters will be  either not generated or at least converted.  
            function.getExportParameterList().setActive("COMPANYCODE_ADDRESS",false);
            
            try
            {
                function.execute(destination);
            }
            catch (AbapException e)
            {
                System.out.println(e.toString());
                return;
            }

            returnStructure = function.getExportParameterList().getStructure("RETURN");
            if (! (returnStructure.getString("TYPE").equals("") ||
                   returnStructure.getString("TYPE").equals("S") ||
                   returnStructure.getString("TYPE").equals("W")) ) 
            {
                throw new RuntimeException(returnStructure.getString("MESSAGE"));
            }
            
            JCoStructure detail = function.getExportParameterList().getStructure("COMPANYCODE_DETAIL");
            
            System.out.println("código: "+detail.getString("COMP_CODE") + '\t' +
                               "Country: "+detail.getString("COUNTRY") + '\t' +
                               "Ciudad: "+detail.getString("CITY"));
        }//for
    }
    

    public static void step4WorkWithTable2() throws JCoException
    {
        JCoDestination destination = JCoDestinationManager.getDestination(ABAP_AS_POOLED);
        JCoFunction function = destination.getRepository().getFunction("BAPI_MATERIAL_GETLIST");
        if(function == null)
            throw new RuntimeException("BAPI_MATERIAL_GETLIST not found in SAP.");

        try
        {
            function.execute(destination);
        }
        catch(AbapException e)
        {
            System.out.println(e.toString());
            return;
        }
        
        JCoStructure returnStructure = function.getImportParameterList().getStructure("MAXROWS");
        if (! (returnStructure.getString("TYPE").equals("")||returnStructure.getString("TYPE").equals("S"))  )   
        {
           throw new RuntimeException(returnStructure.getString("MESSAGE"));
        }
        
        JCoTable codes = function.getTableParameterList().getTable("BAPIMATLIST");
        for (int i = 0; i < codes.getNumRows(); i++) 
        {
            codes.setRow(i);
            System.out.println("Material: "+codes.getString("MATERIA") + '\t' +"MATL_DESC: "+ codes.getString("MATL_DESC"));
        }
/*
        //move the table cursor to first row
        codes.firstRow();
        for (int i = 0; i < codes.getNumRows(); i++, codes.nextRow()) 
        {
            function = destination.getRepository().getFunction("BAPI_COMPANYCODE_GETDETAIL");
            if (function == null) 
                throw new RuntimeException("BAPI_COMPANYCODE_GETDETAIL not found in SAP.");

            function.getImportParameterList().setValue("COMPANYCODEID", codes.getString("COMP_CODE"));
            
            //We do not need the addresses, so set the corresponding parameter to inactive.
            //Inactive parameters will be  either not generated or at least converted.  
            function.getExportParameterList().setActive("COMPANYCODE_ADDRESS",false);
            
            try
            {
                function.execute(destination);
            }
            catch (AbapException e)
            {
                System.out.println(e.toString());
                return;
            }

            returnStructure = function.getExportParameterList().getStructure("RETURN");
            if (! (returnStructure.getString("TYPE").equals("") ||
                   returnStructure.getString("TYPE").equals("S") ||
                   returnStructure.getString("TYPE").equals("W")) ) 
            {
                throw new RuntimeException(returnStructure.getString("MESSAGE"));
            }
            
            JCoStructure detail = function.getExportParameterList().getStructure("COMPANYCODE_DETAIL");
            
            System.out.println("código: "+detail.getString("COMP_CODE") + '\t' +
                               "Country: "+detail.getString("COUNTRY") + '\t' +
                               "Ciudad: "+detail.getString("CITY"));
        }//for
        */
    }
    
    public static void main(String[] args) throws JCoException {
        exeFunctionCall();
        step4WorkWithTable();
        //step4WorkWithTable2();
    }
}
