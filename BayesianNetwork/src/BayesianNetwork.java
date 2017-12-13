/**
 * BayesianNetwork.java (main drive program)
 * Clarence Cheung
 * created originally in Spring 2015
 * modified in 2017
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class BayesianNetwork {

    public static void main(String[] args) {
        // read in files		
        if (args.length != 3) {
            System.out.println("usage: bayes <trainFilename> <testFilename> <n|t>");
            System.exit(-1);
        }

        // define training and testing files and options
        String trainFile = args[0], testFile = args[1];
        String options= args[2];
				
        // check if correct options		
        if (! (options.equals("n") || options.equals("t") ) ) {
            System.out.println("Please enter n or t\nyour input: "+options);
            System.exit(-1);
        }
		
        // define and create new DataSets
        ArffParser ap=new ArffParser();
        ArffDataSet trainDataset= ap.createSets(trainFile);
        ArffDataSet testDataset= ap.createSets(testFile);
		
        // check if same metavalues
        if (!trainDataset.sameMetaValues(testDataset)) {
            System.out.println("Error: metavalues are not the same...");
            System.exit(-1);
        }
	
        // main program	
        if (options.equals("n")) {
            NaiveBayes nb= new NaiveBayes();
            nb.train(trainDataset);
            nb.classify(testDataset);
        } else if (options.equals("t")) {
            TAN tan= new TAN();
            tan.train(trainDataset);
            tan.classify(testDataset);
        }	
    }
		
	
    /** aux method to print out number DataSets info **/
    private static void printDataSetInfo(ArffDataSet arffdataset) {
		
        // relation
        System.out.println("Relation: "+arffdataset.relation);
		
        // labels
        System.out.print("Class: ");
        for (String str: arffdataset.labels) { System.out.print(str+","); }
        System.out.println();
        
        // attributes and values
        int count= 0;
        Map<String,List<String>> map= arffdataset.attributeValuesMap;
        for (String s: map.keySet()) { 
            count++;
            System.out.print(s+": ");
            for (String str: map.get(s)) { System.out.print(str+","); }
                System.out.println();
        } 
        System.out.println(count);
    	
        // instances
        int countI= 0;
        for (Instance i: arffdataset.instances) {
            countI++;
            for (String str: i.attributes) System.out.print(str+","); 
            System.out.println();
        }
        System.out.println(countI);	
    }
    
}
