package org.neuroml.export;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.LinkedHashMap;

import javax.xml.bind.JAXBException;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.run.RuntimeError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.ParseException;
import org.lemsml.jlems.core.sim.Sim;
import org.lemsml.jlems.core.type.BuildException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.core.type.Unit;
import org.lemsml.jlems.core.xml.XMLException;
import org.lemsml.jlems.io.reader.JarResourceInclusionReader;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.util.JUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroML2Validator;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

public class Utils {
	
	private static Lems lemsWithNML2CompTypes;
    
    private static Lems getLemsWithNML2CompTypes() throws ParseError {
        if (lemsWithNML2CompTypes==null) {
            try {
                NeuroML2Validator nmlv = new NeuroML2Validator();
                String content = JUtil.getRelativeResource(nmlv.getClass(),
                        Main.getNeuroMLExamplesResourcesDir()
                                + "/NML2_AbstractCells.nml");
                String lemsVer = NeuroMLConverter.convertNeuroML2ToLems(content);
                lemsWithNML2CompTypes = readLemsNeuroMLFile(lemsVer).getLems();

            } catch (Exception e) {
                throw new ParseError("Error loading NeuroML CompType definitions!", e);
            }
        }
		return lemsWithNML2CompTypes;
    }
	

	public static String getHeaderComment(String format) {
		String commentString = "    This "+format+" file has been generated by org.neuroml.export (see https://github.com/NeuroML/org.neuroml.export)\n" +
    			"         org.neuroml.export  v"+Main.ORG_NEUROML_EXPORT_VERSION+"\n" +
                "         org.neuroml.model   v"+NeuroMLElements.ORG_NEUROML_MODEL_VERSION+"\n" +
                "         jLEMS               v"+org.lemsml.jlems.io.Main.VERSION;
		return commentString;
	}
	
	/*
	 * Gets the magnitude of a NeuroML 2 quantity string in SI units (e.g. -60mV -> -0.06)
	 */
	public static float getMagnitudeInSI(String nml2Quantity) throws NeuroMLException {
		
        try {
            DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());
            float val = (float)dq.getValue();

            return val;
        } catch (ParseError ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        } catch (ContentError ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        }
		
	}
    
	/*
	 * Gets the Dimension of a NeuroML 2 quantity string in SI units (e.g. -60mV returns Dimension with values for Voltage)
	 */
	public static Dimension getDimension(String nml2Quantity) throws NeuroMLException {
		
        try {
            DimensionalQuantity dq = QuantityReader.parseValue(nml2Quantity, getLemsWithNML2CompTypes().getUnits());

            return dq.getDimension();
        } catch (ParseError ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        } catch (ContentError ex) {
            throw new NeuroMLException("Problem getting magnitude in SI for: "+nml2Quantity, ex);
        }
		
	}
    
    /*
         For example, ../Pop0[0] returns Pop0; ../Gran/0/Granule_98 returns Gran
    */
    public static String parseCellRefStringForPopulation(String cellRef) {
        System.out.println("Parsing for population: "+cellRef);
        int loc = cellRef.startsWith("../") ? 1 : 0;
        String ref = cellRef.indexOf("/")>=0 ? cellRef.split("/")[loc] : cellRef;
        if (ref.indexOf("[")>=0)
            return ref.substring(0, ref.indexOf("["));
        else
            return ref;
    }
    
    /*
         For example, ../Pop0[0] returns 0; ../Gran/0/Granule_98 returns 0; Gran/1/Granule_98 returns 0
    */
    public static int parseCellRefStringForCellNum(String cellRef) {
        System.out.println("Parsing for cell num: "+cellRef);
        if (cellRef.indexOf("[")>=0) {
            return Integer.parseInt(cellRef.substring(cellRef.indexOf("[")+1, cellRef.indexOf("]")));
        } else {
            int loc = cellRef.startsWith("../") ? 2 : 1;
            String ref = cellRef.split("/")[loc];
            return Integer.parseInt(ref);
        }
    }
    
    public static Unit getSIUnitInNeuroML(Dimension dim) throws NeuroMLException
    {
        try {
            for (Unit unit: getLemsWithNML2CompTypes().getUnits()) {
                if (unit.getDimension().getName().equals(dim.getName()) &&
                    unit.scale==1 && unit.power==0 && unit.offset==0)
                    return unit;
            }
        } catch (ParseError ex) {
            throw new NeuroMLException("Problem finding SI unit for dimension: "+dim, ex);
        }
        
        return null;
    }

	public static Sim readLemsNeuroMLFile(String contents) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPathInJar("/examples");
		JarResourceInclusionReader.addSearchPathInJar("/");
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(contents);
		JUtil.setResourceRoot(NeuroMLConverter.class);
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}

	public static Sim readNeuroMLFile(File f) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError, IOException {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPath(f.getParentFile());
		
		E.info("Reading from: "+f.getAbsolutePath());

    	String nml = FileUtil.readStringFromFile(f);
    	
    	String nmlLems = NeuroMLConverter.convertNeuroML2ToLems(nml);
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(nmlLems);
		
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}
	public static Sim readLemsNeuroMLFile(File f) throws ContentError, ParseError, ParseException, BuildException, XMLException, ConnectionError, RuntimeError {

		JarResourceInclusionReader.addSearchPathInJar("/NeuroML2CoreTypes");
		JarResourceInclusionReader.addSearchPath(f.getParentFile());
		
		E.info("Reading from: "+f.getAbsolutePath());
		
		JarResourceInclusionReader jrir = new JarResourceInclusionReader(f);
		
        Sim sim = new Sim(jrir.read());
            
        sim.readModel();
    	return sim;
		
	}

    public static String replaceInExpression(String expression, String oldVal, String newVal) {
    	expression = " "+expression+" ";
    	String[] pres = new String[]{"\\(","\\+","-","\\*","/","\\^", " "};
        String[] posts = new String[]{"\\)","\\+","-","\\*","/","\\^", " "};

        for(String pre: pres){
            for(String post: posts){
                String o = pre+oldVal+post;
                String n = pre+" "+newVal+" "+post;
	                //E.info("Replacing "+o+" with "+n+": "+formula);
                //if (formula.indexOf(o)>=0) {
                expression = expression.replaceAll(o, n);
                //}
            }
        }
        return expression.trim();
    }

    
    
    public static LinkedHashMap<String,Standalone> convertLemsComponentToNeuroML(Component comp) throws ContentError, JAXBException 
    {
        XMLSerializer xmlSer = XMLSerializer.newInstance();
        String compString = xmlSer.writeObject(comp);
        System.out.println(compString);
        
        NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML("<neuroml xmlns=\"http://www.neuroml.org/schema/neuroml2\"\n" +
"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
"      xsi:schemaLocation=\"http://www.neuroml.org/schema/neuroml2 "+NeuroMLElements.LATEST_SCHEMA_LOCATION+"\">"+compString+"</neuroml>");
        LinkedHashMap<String,Standalone> els = NeuroMLConverter.getAllStandaloneElements(nmlDocument);
        return els;
    }
    
    
    public static Component convertNeuroMLToComponent(Standalone nmlElement) throws NeuroMLException
    {
        Lems lems = convertNeuroMLToSim(nmlElement).getLems();
        
        try {
			return lems.getComponent(nmlElement.getId());
		} catch (ContentError e) {
			throw new NeuroMLException(e);
		}
    }

    public static Sim convertNeuroMLToSim(Standalone nmlElement) throws NeuroMLException
    {
        NeuroMLDocument nml2 = new NeuroMLDocument();
        nml2.setId(nmlElement.getId());
        NeuroMLConverter.addElementToDocument(nml2, nmlElement);
        NeuroMLConverter nmlc;
		try {
			nmlc = new NeuroMLConverter();
		} catch (JAXBException e) {
			throw new NeuroMLException(e);
		}
        String nml2String = nmlc.neuroml2ToXml(nml2);
        String lemsString = NeuroMLConverter.convertNeuroML2ToLems(nml2String);
        Sim sim;
		try {
			sim = Utils.readLemsNeuroMLFile(lemsString);
		} catch (ContentError e) {
			throw new NeuroMLException(e);
		} catch (ParseError e) {
			throw new NeuroMLException(e);
		} catch (ParseException e) {
			throw new NeuroMLException(e);
		} catch (BuildException e) {
			throw new NeuroMLException(e);
		} catch (XMLException e) {
			throw new NeuroMLException(e);
		} catch (ConnectionError e) {
			throw new NeuroMLException(e);
		} catch (RuntimeError e) {
			throw new NeuroMLException(e);
		}
        
        return sim;
    }
    
    public static AbstractList reorderAlphabetically(AbstractList list, boolean ascending)
    {
        if (list.size() > 1)
        {
            for (int j = 1; j < list.size(); j++)
            {

                for (int k = 0; k < j; k++)
                {
                    if (ascending)
                    {
                        if (list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) < 0)
                        {
                            Object earlier = list.get(j);
                            Object later = list.get(k);
                            list.set(j, later);
                            list.set(k, earlier);
                        }
                    }
                    else
                    {
                        if (list.get(j).toString().compareToIgnoreCase(list.get(k).toString()) > 0)
                        {
                            Object earlier = list.get(j);
                            Object later = list.get(k);
                            list.set(j, later);
                            list.set(k, earlier);
                        }
                    }
                }
            }
        }
        return list;
    }
    

    public static void main(String[] args) throws Exception {
    	
    	String expr = "q+instances";
    	E.info("Replaced "+expr+" with "+replaceInExpression(expr, "q", "gg"));
    }

}
