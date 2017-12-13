/**
 * ArffDataSet.java
 * Clarence Cheung
 * created originally in Spring 2015
 * modified in 2017
 *
 * Disclaimer: Some files and codes are adapted and modified from CS540 Fall 2014 (DataSet.java)
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Original description: 
 * This class organizes the information of a data set into simple structures.
 * To speed up program performance, the label value of an instance is stored 
 * as an Integer that reflect the position of the label in the DataSet labels
 * list. Similarly, the attribute values of an instance are stored as Integers
 * that reflect the position of that value in the list attributes[<attribute>].
 * See the Instance class for more details. All ordering of attribute values in
 * an instance follow the ordering of the DataSet attributes list.
 *
 * Do not modify.
 */

public class ArffDataSet {
	
	
    public String relation= null;
    public List<String> labels = null;  // ordered list of class labels
    public List<String> attributes = null;	// ordered list of attributes	
    public Map<String, List<String> > attributeValuesMap = null;  // map to ordered discrete values taken by attributes 
    public List<Instance> instances = null;	 // ordered list of instances
    public List<Map<String,Integer>> mapAttrLoc= null;
    private final String DELIMITER = "\\s*,";  	// delimiter used to split input strings
    private final int classSize= 2;  // number of class labels
	
    /**
     * Adds the relation used by the DataSet
     */
    public void addRelation(String relation) {
        this.relation= relation;
    }
	
    /**
     * Adds the attribute value mapping to corresponding location 
     * which allows faster access and mapping
     */
    public void addMapAttrLoc(List<Map<String,Integer>> mapAttrLoc) {
        this.mapAttrLoc= mapAttrLoc;
    }
	
	
    /**
     * Adds the labels used by the instances.
     * @param line begins with substring "%%"
     */
    public void addLabels(String[] labelValues) {
		
        // prompt error if not enough classes/labels
        if (labelValues.length < classSize) {
            System.err.println("Line doesn't contain enough classes/labels\nRequire at least 2\nexiting...");
            return;
        }
		
        labels = new ArrayList<String>(labelValues.length);
		
        //  each element is a label
        for (int i = 0; i < labelValues.length; i++) {
            labels.add(labelValues[i]);
        }
    }
	
    /**
     * Adds the attributes used by the instances.
     * @param line begins with substring "##"
     */
    public void addAttribute(String attrName, String[] attrLine) {
        if (attributes == null) {
            attributes = new ArrayList<String>();
            attributeValuesMap = new HashMap<String, List<String>>();
        }	
        String[] attrValues= attrLine;
        if (attrValues.length < 2 && !(attrValues[0].equals("real") || attrValues[0].equals("Real") || attrValues[0].equals("REAL") ||
            attrValues[0].equals("numeric") || attrValues[0].equals("Numeric") || attrValues[0].equals("NUMERIC") || 
            attrValues[0].equals("string") || attrValues[0].equals("String") || attrValues[0].equals("STRING") ||
            attrValues[0].equals("integer") || attrValues[0].equals("Integer") || attrValues[0].equals("INTEGER"))) {
            System.err.println("Line doesn't contain enough attributes");
            return; 
        }
        List<String> attrList = new ArrayList<String>();
			
        // ordered list of values for specific attribute
        for (int i = 0; i < attrValues.length; i++) {
            attrList.add(attrValues[i]);
        }
		
        // grab the attribute name
        attributes.add(attrName);
        attributeValuesMap.put(attrName, attrList);	
    }
	
	
    /**
     * Add instance to collection.
     * @param line begins with label
     */
    public void addInstance(String[] line) {
        if (instances == null) {
            instances = new ArrayList<Instance>();
        }
        String[] splitLine = line;
		
        if (splitLine.length < 1 + attributes.size()) { 
            System.err.println("Instance doesn't contain enough attributes");
            return;
        }

        Instance instance = new Instance();
        //  find the label and record its index
        for (int i = 0; i < labels.size(); i++) {
            if (splitLine[splitLine.length-1].equals(labels.get(i)) ) {
                instance.label = labels.get(i);
                break;
            }
        }		

        //  add the values, will be input in same order as attributes
        for (int i = 0; i < splitLine.length-1; i++) {
            List<String> values = attributeValuesMap.get(attributes.get(i) );
            //  find the index of the value
           for (int j = 0; j < values.size(); j++) {
		
                if (splitLine[i].equals(values.get(j))) {
                    instance.addAttribute(values.get(j));
                    break;
                } else if (values.get(j).equals("real") || values.get(j).equals("REAL") || values.get(j).equals("numeric") || values.get(j).equals("NUMERIC")) {
                    instance.addAttribute(splitLine[i]);
                    //System.out.println(instance.attributes);
                    break;
                }
                if (j == values.size() - 1) {
                    System.err.println("Missing attribute : check input files");
                }
            }
        }
        instances.add(instance);
    }
	

    /**
     * Verifies that two DataSets use the same values for labels and attributes 
     * as well as having the same ordering
     * Returns false otherwise.
     */
    public boolean sameMetaValues(ArffDataSet other) {
        // compare labels
        if (other.labels == null || this.labels == null) {
            if (!(this.labels == null && other.labels == null)) {
                return false;
            }
        } else if (other.labels.size() != this.labels.size()) {
            return false;
        } else {
            for (int i = 0; i < other.labels.size(); i++) {
                if (!other.labels.get(i).equals(this.labels.get(i))) {
                    return false;
                }
            }
        }
		
        // compare attributes (and values)
        if (other.attributes == null || this.attributes == null) {
            if (!(this.attributes == null && this.attributes == null)) {
                return false;
            }
        } else if (other.attributes.size() != this.attributes.size() || 
            other.attributes.size() != other.attributeValuesMap.size() ||
            other.attributeValuesMap.size() != this.attributeValuesMap.size()) {
            return false;
        } else {
            for (int i = 0; i < other.attributes.size(); i++) {
                if (!other.attributes.get(i).equals(this.attributes.get(i))) {
                    return false;
                }
                List<String> otherValues = other.attributeValuesMap.get(other.attributes.get(i));
                List<String> thisValues = this.attributeValuesMap.get(other.attributes.get(i));
                for (int j = 0; j < otherValues.size(); j++) {
                    if (!otherValues.get(j).equals(thisValues.get(j))) {
                        return false;
                    }
                }
            }
        }
        return true;
    } 

}
