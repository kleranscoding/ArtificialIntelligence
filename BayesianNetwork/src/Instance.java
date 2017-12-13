/**
 * Instance.java
 * Clarence Cheung
 * created originally in Spring 2015
 * modified in 2017
 *
 * Disclaimer: Some files and codes are adapted and modified from CS540 Fall 2014 (Instance.java)
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Holds data for particular instance.
 * Integer values refer to offsets in meta-data
 * arrays of a surrounding ArffDataSet. 
 */

public class Instance {

    public String label;
    public List<String> attributes = null;

    /**
     * Add attribute values in the order of
     * attributes as specified by the dataset
     */
    public void addAttribute(String str) {
        if (attributes == null) { attributes = new ArrayList<String>(); }
            attributes.add(str);
	}

}
