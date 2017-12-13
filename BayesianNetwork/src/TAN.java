/**
 * TAN.java (with BayesianNetwork.java)
 * Clarence Cheung
 * created originally in Spring 2015
 * modified in 2017
 * 
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class TAN {

    private List<String> labels; // ordered list of class labels
    private List<String> attributes; // ordered list of attributes
    private Map<String, List<String>> attrVals; // map to ordered discrete values taken by attributes
    
    private double instanceSize; // stores number of instances
    private int labelSize; // stores number of class labels
    private double[] labelRec; // stores occurrence for each label
    private double[] probLabel; // stores probability for each label
    
    private List<Map<String,Double>> countAttr_ij;  // stores joint occurrence for each attribute pair of each label
    private List<Map<String,Double>> countXi;  // stores occurrence for each attribute of each label

    List<List<Instance>> labelInstances; // stores instances based on class label (for Cross-Validation)
    
    private final double DELTA= 1.0; // add delta constant
    
    private List<Map<String,Integer>> mapAttrLoc; // stores the mapping of attribute value to location
    private Map<String,Integer> mapLabelLoc; // stores the mapping of class label value to location
    
    private double[][] weightMatrix;  // stores the weight matrix to construct MaxST
    private Map<int[],Double> maximalTree;  // stores maximalTree edges and weights
    private int[] parentAttribute;  // stores parent attribute for each attribute
    
    private List<Map<String,Double>> probTable; // stores the conditional probability for each label 
    
    /** constructor */
    TAN() {}
    
    /** train the TAN model with dataset
     * @param dataset
     */
    public void train(ArffDataSet dataset) {
        // initialize the variables 
        this.labels= dataset.labels;
        this.attributes= dataset.attributes;
        this.attrVals= dataset.attributeValuesMap;
        
        instanceSize= dataset.instances.size();
        labelSize= labels.size();
        labelRec= new double[labelSize];
        mapAttrLoc= dataset.mapAttrLoc;
        mapLabelLoc= new HashMap<String,Integer>();

        /** create subsets for each label
         * create ArrayLists that store the number of each attribute value in each label
         */
        labelInstances= new ArrayList<List<Instance>>();
        probTable= new ArrayList<Map<String,Double>>();
        countAttr_ij= new ArrayList<Map<String,Double>>();
        countXi= new ArrayList<Map<String,Double>>();
        for (int l=0;l<labelSize;l++) { 
            List<Instance> instance= new ArrayList<Instance>();
            Map<String,Double> mapAttr_ij= new HashMap<String,Double>();
            Map<String,Double> mapXi= new HashMap<String,Double>();
            Map<String,Double> mapProbT= new HashMap<String,Double>();
            // add empty map
            countAttr_ij.add(mapAttr_ij);
            countXi.add(mapXi);
            probTable.add(mapProbT);
            // add instance
            labelInstances.add(instance);
            mapLabelLoc.put(labels.get(l),l); 
        }

        /** record the total number and the number of entry in each label **/
        for (Instance inst: dataset.instances) {
            int labelNo= mapLabelLoc.get(inst.label);
            labelRec[labelNo]++;
            labelInstances.get(labelNo).add(inst);
            
            for (int ai=0;ai<attributes.size();ai++) {
                int attrValNoXi= mapAttrLoc.get(ai).get(inst.attributes.get(ai)); // get attribute value position
                
                Map<String,Double> mapAttrXi= countXi.get(labelNo);
                String keyXi= new StringBuilder(ai+","+attrValNoXi).toString();
                if (!mapAttrXi.containsKey(keyXi)) { mapAttrXi.put(keyXi,0.0); }
                mapAttrXi.put(keyXi,(mapAttrXi.get(keyXi)+1.0));
                
                for (int aj=0;aj<attributes.size();aj++) {
                    Map<String,Double> mapJointAttr= countAttr_ij.get(labelNo);
                    if (ai!=aj) {
                        int attrValNoXj= mapAttrLoc.get(aj).get(inst.attributes.get(aj));
                        String keyJointAttr= new StringBuilder(ai+","+attrValNoXi+","+aj+","+attrValNoXj).toString();
                        if (!mapJointAttr.containsKey(keyJointAttr)) { mapJointAttr.put(keyJointAttr,0.0); }
                        mapJointAttr.put(keyJointAttr,(mapJointAttr.get(keyJointAttr)+1.0));
                    }
                }  // end of Xj loop
            }  // end of Xi loop
        }  // end of instance loop

        /** get probability for the labels **/
        probLabel= p_l();
        
        /** calculate mutual information matrix to build MST **/
        getMutualInfo();

        /** create Maximal Spanning Tree to find the TAN structure **/
        PrimsMaxST();
        
        /** create Conditional Probability Table **/
        createCPT();

    } // end of training with Dataset

    
    /** classify the test set
     * @param testset
     */
    public void classify(ArffDataSet testset) {
    	
        printAttributeTree();
        // classify each instance and record number of correct answers
        int correctNum= 0;
        for (int inst=0;inst<testset.instances.size();inst++) {
            String[] res= classifyInstance(testset.instances.get(inst));
            String actualLabel= testset.instances.get(inst).label;
            System.out.println(res[0]+" "+actualLabel+" "+res[1]);
            if (res[0].equals(actualLabel)) { correctNum++; }
        }
        System.out.println("\nAccuracy= "+correctNum+"/"+testset.instances.size());
    } // end of classification
    
    
    /** classify each instance to label L
     * @param inst
     * @return String[] of {classified label, probability of maximum likelihood}
     */
    private String[] classifyInstance(Instance inst) {
        
        double[] jointProb= new double[labelSize];
        double allProb= 0.0;
        // for each label
        for (int l=0;l<labelSize; l++) {
            jointProb[l]= probLabel[l];
            // for each attribute in Instance
            for (int attr=0; attr<inst.attributes.size(); attr++) {
                
                int attrValNo= mapAttrLoc.get(attr).get(inst.attributes.get(attr)); // get attribute value position
                
                String keyAttr= new StringBuilder(attr+","+attrValNo+",,").toString();;
                
                // vertex / attribute 0 has no parent
                if (attr>0) {
                
                	String keyParAttrVal= inst.attributes.get(parentAttribute[attr]);
                	int parAttrValNo= mapAttrLoc.get(parentAttribute[attr]).get(keyParAttrVal); // get parent attribute value position
                	keyAttr= new StringBuilder(attr+","+attrValNo+","+parentAttribute[attr]+","+parAttrValNo).toString();
            	
                } // end if else attribute
                
                double prob= probTable.get(l).get(keyAttr);
                jointProb[l]*= prob;
    	        
            } // end of attribute for loop
            allProb+= jointProb[l];
        } // end of label for loop
        
        // find the best label
        double[] classify_prob= new double[labelSize];
        double maxProb= 0.0;
        int classifyClass= 0;
        for (int l=0; l< classify_prob.length; l++) {
			classify_prob[l]= jointProb[l] / allProb;
			if (classify_prob[l] > maxProb) {
				classifyClass= l;
				maxProb= classify_prob[l];
			}
		}
		
		return new String[] {labels.get(classifyClass),String.valueOf(maxProb)};
        
    }
    
    
    /** print attribute Tree structure **/
	public void printAttributeTree() {
	    System.out.println(attributes.get(0)+" class");
        for (int e=1;e<parentAttribute.length;e++) {
            System.out.println(attributes.get(e)+" "+attributes.get(parentAttribute[e])+" class");
        }
        System.out.println();
	}
    
    
    /**
     * calculate mutual information 
     * I(X_i,X_j | Y_k)= sum_{i,j,k} p(X_i,X_j,Y_k)*log( (p(X_i,X_j,Y_k)*p(Y_k)) / (p(X_i,Y_k)*p(X_j,Y_k) )
     */
    private void getMutualInfo() {	
        // initialize the weight Matrix
        weightMatrix= new double[attributes.size()][attributes.size()];
        // loop through all attributes and 
        for (int ai=0;ai<attributes.size();ai++) {
            for (int aj=0;aj<attributes.size();aj++) {
                // default mutual weight (self)
                double mutualWeight= -1.0;
                // retrieve mutual info gain if not the same attribute
                if (ai!=aj) {
                    double mutualInfoGain= 0.0;  // define mutual info gain 
                    int avLenXi= attrVals.get(attributes.get(ai)).size();  // size of attribute X_i values 
                    int avLenXj= attrVals.get(attributes.get(aj)).size();  // size of attribute X_j values
                    // loop through all attribute values 
                    for (int aiVal=0;aiVal<avLenXi;aiVal++) { 
                        for (int ajVal=0;ajVal<avLenXj;ajVal++) {
                            // loop through all class labels
                            for (int l=0;l<labelSize;l++) {
                                // create specific format of key to retrieve its value
                                String keyXi= new StringBuilder(ai+","+aiVal).toString();
                                String keyXj= new StringBuilder(aj+","+ajVal).toString();
                                String keyXiXj= new StringBuilder(ai+","+aiVal+","+aj+","+ajVal).toString();

                                Map<String,Double> mapXi= countXi.get(l), mapXiXj= countAttr_ij.get(l);
                                double numXiY= 0.0, numXjY= 0.0, numXiXjY= 0.0;

                                // get counts for each key
                                if (mapXi.containsKey(keyXi)) { numXiY= mapXi.get(keyXi); }
                                if (mapXi.containsKey(keyXj)) { numXjY= mapXi.get(keyXj); }
                                if (mapXiXj.containsKey(keyXiXj)) { numXiXjY= mapXiXj.get(keyXiXj); }

                                // probability given Y (class label)
                                double probXiXj= (numXiXjY+DELTA)/(instanceSize+(avLenXi*avLenXj*labelSize)*DELTA);
                                double probXi_Y= (numXiY+DELTA)/(labelRec[l]+avLenXi*DELTA);
                                double probXj_Y= (numXjY+DELTA)/(labelRec[l]+avLenXj*DELTA);
                                double probXiXj_Y= (numXiXjY+DELTA)/(labelRec[l]+avLenXi*avLenXj*DELTA);

                                double infoGain= probXiXj*log2(probXiXj_Y/(probXi_Y*probXj_Y));
                                mutualInfoGain+= infoGain;

                            }  // end of label loop	
                         }  // end of Xj value loop
                    }  // end of Xi value loop
                    mutualWeight= mutualInfoGain;

                }  // end of if else 
                weightMatrix[ai][aj]= mutualWeight;
            }  // end of Xj loop
        } // end of Xi loop
    }
    
    
    /**
     * use Prim's algorithm to find MAX Spanning Tree 
     * by treating the original weights as negated values
     * pick the largest instead of the smallest
     */
    private void PrimsMaxST() {
        
        // create HashMap that stores edges as int[] and its weight as double for the MaxST
        // let the first attribute be the root of the tree
        maximalTree= new HashMap<int[],Double>();
        parentAttribute= new int[attributes.size()];
        Set<Integer> vertex= new HashSet<Integer>();
        vertex.add(0);
        parentAttribute[0]= -1;

        // find the MaxST until current tree include all vertices (attributes)
        while (vertex.size()<attributes.size()) {
			
            int[] edges= new int[]{-1,-1};
            double maxVal= 0.0;
			
            // for each element in vertex, loop through all attributes to find the next potential candidate
            for (Integer intg : vertex) {
                for (int p=0;p<weightMatrix[intg].length;p++) {
                    // cannot include existing element to form a cycle
                    if (!vertex.contains(p)) {
                        // update maxVal
                        if (weightMatrix[intg][p]>maxVal) {
                            edges= new int[] {intg,p};
                            maxVal= weightMatrix[intg][p];
                        }
                    }  // end of check cycle if
                }  // end of looping through all attributes
            }  // end of looping through vertex
			
            // update parentAttribute and maximalTree, add new edge to vertex
            parentAttribute[edges[1]]= edges[0];
            maximalTree.put(edges, maxVal);
            vertex.add(edges[1]);
        }
        
    }
	
    /** create Condition Probability Table **/
    private void createCPT() {
    
        // for each label
        for (int l=0; l<labelSize; l++) {
    	
            Map<String,Double> mapAttrXj= countXi.get(l);
 	        Map<String,Double> mapAttrXiXj= countAttr_ij.get(l);
 	    
            // for each parent attribute
            for (int e=0; e<parentAttribute.length; e++) {
    	        
            	String aj= attributes.get(e);
                for (int ajVal=0; ajVal<attrVals.get(aj).size(); ajVal++) {
                    // vertex 0 has no parent
                    if (e==0) {
                        String keyAttrXj= new StringBuilder(e+","+ajVal).toString();
                        String keyProbTableXj= new StringBuilder(e+","+ajVal+",,").toString();
                        //System.out.println(keyAttrXj+": "+mapAttrXj.get(keyAttrXj));
                        double numXj= 0.0;
                        if (mapAttrXj.containsKey(keyAttrXj)) { numXj= mapAttrXj.get(keyAttrXj); }
                        double probXj_givenL= (numXj+DELTA)/(labelRec[l]+attrVals.get(aj).size()*DELTA);
    	        	
                        probTable.get(l).put(keyProbTableXj, probXj_givenL);
    	            
                        //System.out.println(keyProbTableXj+"= "+probTable.get(l).get(keyProbTableXj));
    	            
                    } else {
    	            
                        String ai= attributes.get(parentAttribute[e]);
                        for (int aiVal=0; aiVal<attrVals.get(ai).size(); aiVal++) {
                    
                    	    String keyAttrXi= new StringBuilder(parentAttribute[e]+","+aiVal).toString();
                            String keyAttrXiXj= new StringBuilder(e+","+ajVal+","+parentAttribute[e]+","+aiVal).toString();
                            double numXi= 0.0, numXiXj= 0.0;
                            if (mapAttrXj.containsKey(keyAttrXi)) { numXi= mapAttrXj.get(keyAttrXi); }
                            if (mapAttrXiXj.containsKey(keyAttrXiXj)) { numXiXj= mapAttrXiXj.get(keyAttrXiXj); }
                    
                            double probXj_given_XiL= (numXiXj + DELTA) / (numXi + attrVals.get(aj).size()*DELTA);
    	                
                    	    probTable.get(l).put(keyAttrXiXj, probXj_given_XiL);
    	            	
                        }  // end of parent attribute values for loop
    	        	
                    } // end of if else attribute
    	    	
                } // end of attribute values for loop
            }  // end of looping thru each attribute
        } // end of label for loop
    }
	
    
    /**
     * calculates the probability for each label L 
     */
    private double[] p_l() {
        
    	double[] res= new double[labelSize];
    	double allLabelCounts= 0.0;
    	// count all occurrence
    	for (int l=0;l<labelSize; l++) { allLabelCounts+= labelRec[l]; }
    	// record each probability
    	for (int l=0;l<labelSize; l++) { res[l]= (labelRec[l]+DELTA)/(allLabelCounts+labelSize*DELTA); }
    	return res;
    }
    
    
    /**
     * aux method for converting values to log2
     * @param d1
     * @return double: log(d1)/log(d2)
     */
    private double log2(double d1) { return Math.log(d1)/Math.log(2); }
	
	
}
