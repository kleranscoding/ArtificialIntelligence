/**
 * ArffParser.java
 * Clarence Cheung
 * created originally in 2017
 *
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


/**
 * This class reads in files of ARFF format. 
 * It removes quotation marks when needed
 */


public class ArffParser {

    public ArffParser() { }

    // create DataSets from reading in file
    public ArffDataSet createSets(String file) {
		
        ArffDataSet dataSet= new ArffDataSet();
		
        try {
            // use BufferRead to read in file
            BufferedReader lines= new BufferedReader(new FileReader(file));
			
            List<Map<String,Integer>> mapAttrLoc= new ArrayList<Map<String,Integer>>();
			
            int numLineCount=0, dataIndex=0;
            String relation= "";	

            while (lines.ready()) {
				
                String dataline= lines.readLine();
				
                // check if comment, relation, attribute or data
                char firstChar= dataline.charAt(0);
				
                // skip comments
                if (firstChar=='%') {
                    continue;
				
                // find if attributes
                } else if (firstChar=='@') {
					
                    // split line as array
                    String[] header= dataline.split("\\s+");
					
                    if (header[0].equals("@relation") || header[0].equals("@Relation") || header[0].equals("@RELATION")) {
                        if (header.length>1) {
                            StringBuilder sb= new StringBuilder(header[1]);
                            relation= sb.toString();
                        }
                        dataSet.addRelation(relation);
				
                    } else if (header[0].equals("@attribute") || header[0].equals("@Attribute") || header[0].equals("@ATTRIBUTE")) {
                        dataIndex++;

                        Map<String,Integer> map= new HashMap<String,Integer>();
						
                        // get attributes
                        String attributes= header[1].substring(1, header[1].length()-1);
						
                        // get attributes values
                        StringBuilder sb= new StringBuilder();
                        for (int i=3;i<header.length; i++) { sb.append(header[i]); }
                        String[] attributeValues= sb.toString().substring(0,sb.length()-1).split(",");
						
                        // remove quotation marks
                        for (int i=0;i<attributeValues.length;i++) {
                            String strVal= attributeValues[i];
                            if ((strVal.charAt(0)=='\'' && strVal.charAt(strVal.length()-1)=='\'') || (strVal.charAt(0)=='"' && strVal.charAt(strVal.length()-1)=='"')) {
                                attributeValues[i]= strVal.substring(1,strVal.length()-1); }
                            map.put(attributeValues[i],i);
                        }
						
                        // check if attribute is class/label
                        if (attributes.equals("class") || attributes.equals("Class") || attributes.equals("CLASS") ) {
                            dataSet.addLabels(attributeValues);
                        } else {
                            dataSet.addAttribute(attributes, attributeValues);
                            mapAttrLoc.add(map);
                        }
						
                    } else if (header[0].equals("@data") || header[0].equals("@Data") || header[0].equals("@DATA")) {
                        continue;
                    } 
					
                // data sets
                } else {
                    numLineCount++;
	
                    // get data values
                    String[] dataValues= dataline.split("\\s*,");
                    for (int i=0;i<dataValues.length;i++) {
                        String strVal= dataValues[i];
                        if (strVal.charAt(0)=='\'' && strVal.charAt(strVal.length()-1)=='\'') {
                            dataValues[i]= strVal.substring(1,strVal.length()-1); }
                    }
                    dataSet.addInstance(dataValues);
                }

            } // end of while

            dataSet.addMapAttrLoc(mapAttrLoc);
            lines.close();
	
            return dataSet;

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }
		
}
