/**
 * NaiveBayes.java (with BayesianNetwork.java)
 * Clarence Cheung
 * created originally in Spring 2015
 * modified in 2017
 * 
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public class NaiveBayes {

	private List<String> labels; // ordered list of class labels
	private List<String> attributes; // ordered list of attributes
	
	private double instanceSize; // stores number of instances
	private int labelSize; // stores number of class labels
	private double[] labelRec; // stores occurrence for each label
	List<List<List<Double>>> occurRec; // stores occurrence for each attribute value of each class label
	List<List<Instance>> labelInstances; // stores instances based on class label (for Cross-Validation)
	
	private final double DELTA= 1.0; // add delta constant
	
	private List<Map<String,Integer>> mapAttrLoc; // stores the mapping of attribute value to location
	private Map<String,Integer> mapLabelLoc; // stores the mapping of class label value to location
	
	private double[] probLabel; // stores smoothed probability for each class label
	
	/** trivial constructor */
	NaiveBayes() {} 
	
	/** train the NB model with the dataset
	 * @param dataset
	 */
	public void train(ArffDataSet dataset) {
		
		// initialize the variables
		this.labels= dataset.labels;
		this.attributes= dataset.attributes;
		
		mapAttrLoc= dataset.mapAttrLoc;
		mapLabelLoc= new HashMap<String,Integer>();
		
		labelSize= labels.size();
		labelRec= new double[labelSize];
		probLabel= new double[labelSize];
		
        // create subsets of each label
		// create ArrayLists that store the number of each attribute value in each label
		// record the total number and the number of entry in each label
		labelInstances= new ArrayList<List<Instance>>();
		occurRec = new ArrayList<List<List<Double>>>();
		
		// initialize occurRec and labelInstances
		// for each class label
		for (int l=0;l<labelSize;l++) {
			List<Instance> instList= new ArrayList<Instance>();
			labelInstances.add(instList);
			mapLabelLoc.put(labels.get(l),l);
			List<List<Double>> occurAttr = new ArrayList<List<Double>>();
			// for each attribute
			for (int a=0;a<attributes.size();a++) {
				List<Double> occurAttrVal= new ArrayList<Double>();
				// for each attribute value
				for (int av=0; av<dataset.attributeValuesMap.get(attributes.get(a)).size();av++) {
					occurAttrVal.add(0.0);
				}
				occurAttr.add(occurAttrVal);
			}
			occurRec.add(occurAttr);
		}
		
		// check occurrence for each Instance
		instanceSize= dataset.instances.size();
		for (Instance inst: dataset.instances) {
			
			int labelNo= mapLabelLoc.get(inst.label);
			
			labelRec[labelNo]++;
			labelInstances.get(labelNo).add(inst);
			for (int a=0;a<inst.attributes.size();a++) {
				String attr= inst.attributes.get(a);
				int attrValNum= mapAttrLoc.get(a).get(attr);
				double occurNum= occurRec.get(labelNo).get(a).get(attrValNum)+1.0;
				occurRec.get(labelNo).get(a).set(attrValNum, occurNum);
			}
		}
		
		// sum up occurrence for each attribute value
		for (int ocr=0;ocr<occurRec.size();ocr++) {
			for (int ocn=0; ocn<occurRec.get(ocr).size();ocn++) {
				double subSumAttr= 0.0;
				for (int oca=0; oca<occurRec.get(ocr).get(ocn).size(); oca++) {
					subSumAttr+= occurRec.get(ocr).get(ocn).get(oca);
				}
				occurRec.get(ocr).get(ocn).add(subSumAttr);
			}
		}
		
		// record the smoothed probability for each class label P(Y=y_i)
		for (int l=0;l<labelRec.length;l++) {
			probLabel[l]= (labelRec[l]+DELTA)/(instanceSize + labelRec.length*DELTA);
		}
		
	}
	
	
	/** classify the dataset
	 * @param testset
	 */
	public void classify(ArffDataSet testset) {
		
		int correctNum= 0;
		for (int inst=0;inst<testset.instances.size();inst++) {
			String[] res= classifyInstance(testset.instances.get(inst));
			String actualLabel= testset.instances.get(inst).label;
			System.out.println(res[0]+" "+actualLabel+" "+res[1]);
			if (res[0].equals(actualLabel)) { correctNum++; }
		}
		System.out.println("\nAccuracy= "+correctNum+"/"+testset.instances.size());
		
	}
	
	/** classify each instance of the dataset
	 * @param Instance inst
	 * @return String[] {classified class, probability of likelihood} 
	 */
	private String[] classifyInstance(Instance inst) {
		
		// joint probability
		double[] jointProb= probLabel.clone();
		for (int a=0;a<inst.attributes.size();a++) {
			for (int l=0;l<labelSize;l++) {
				jointProb[l]*= p_v_given_l(l,a,inst.attributes.get(a));
			}
		}
		
		// total probability
		double totalProb= 0.0, maxProb= 0.0;
		for (int l=0;l<labelSize;l++) { totalProb+= jointProb[l]; }
		
		// classification
		int classifyClass= 0;
		for (int l=0;l<labelSize;l++) {
			// update maximum likelihood if probability is greater
			double classifyProb= jointProb[l]/totalProb;
			if (classifyProb> maxProb) {
				maxProb= classifyProb;
				classifyClass= l;
			}
		}
		return new String[] {labels.get(classifyClass),String.valueOf(maxProb)};
	}
	
	/**
	 * Returns the smoothed conditional probability of an attribute value given the label
	 * P(X=x_i | Y=y_j) 
	 */
	private double p_v_given_l(int label,int attrNo,String attrStrVal) {
		
		int attrValNo= mapAttrLoc.get(attrNo).get(attrStrVal);
		int attrLastIdx= occurRec.get(label).get(attrNo).size()-1;
		double count= occurRec.get(label).get(attrNo).get(attrValNo);
		double sum= occurRec.get(label).get(attrNo).get(attrLastIdx);
		
		//System.out.println(count+"/"+sum);
		return (count+DELTA)/(sum+attrLastIdx*DELTA);
	}
	
	// aux method to print all occurrence of attribute value
	private void printOccurRec() {
		for (int i=0;i<occurRec.size();i++) { for (int j=0;j<occurRec.get(i).size();j++) { for (int k=0;k<occurRec.get(i).get(j).size();k++) {  System.out.printf("%d,%d,%d= %.2f\n",i,j,k,occurRec.get(i).get(j).get(k)); } } }
	}
	
}
