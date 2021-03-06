package ntu.nlp.format;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class InputFormatProcessor {
	public static void randomForestResultMerge(File randomForestResult, List <HotelComment> hcl) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(randomForestResult));
		String line;
		int index = 0;
		while ((line = br.readLine()) != null) {
			line = br.readLine().trim();
			String [] predictions = line.split("\\s+");
			for (String prediction : predictions) {
				hcl.get(index).setLike(Integer.parseInt(prediction));
				index++;
			}
		}
		br.close();
		
		
	}
	public static List <DocumentVector> convertToDocumentVector(Map <String, Integer> wordToDimensionMap ,List <HotelComment> hotelCommentList){
		List <DocumentVector> documentVectorList = new ArrayList <DocumentVector>();
		Map <Integer, Double> dimensionToIdf = new HashMap<Integer, Double>();
		for (int dimension = 1;dimension <= wordToDimensionMap.size();dimension++) {
			dimensionToIdf.put(dimension, 0.0);
		}
		
		double averageDocumentLength = 0.0;
		for (HotelComment hc : hotelCommentList) {
			Map <Integer, Integer> dimensionToTermFrequency = new HashMap <Integer, Integer>();
			
			String [] sentences = hc.getSentences();
			String concatSentences = StringUtils.join(sentences);
			for (String word: wordToDimensionMap.keySet()) {
				if (concatSentences.indexOf(word) == -1)continue;
				dimensionToIdf.put(wordToDimensionMap.get(word), dimensionToIdf.get(wordToDimensionMap.get(word)) + 1  );				
			}
			
			for (int i = 0;i < sentences.length;i++) {
				for (String word : wordToDimensionMap.keySet()) {
					if (sentences[i].indexOf(word) == -1)continue;
					
					if (dimensionToTermFrequency.get(wordToDimensionMap.get(word)) == null) {
						if (isPrefixNegative(sentences[i], word)) {
							dimensionToTermFrequency.put(wordToDimensionMap.get(word), -1);
						} else {
							dimensionToTermFrequency.put(wordToDimensionMap.get(word), 1);
						}
						
					} else {
						if (isPrefixNegative(sentences[i], word)) {
							dimensionToTermFrequency.put(wordToDimensionMap.get(word), dimensionToTermFrequency.get(wordToDimensionMap.get(word)) -1);
						} else {
							dimensionToTermFrequency.put(wordToDimensionMap.get(word), dimensionToTermFrequency.get(wordToDimensionMap.get(word)) +1);
						}
						
					}
					
//					if ( dimensionToTermFrequency.get(wordToDimensionMap.get(word)) == null ) {
//						dimensionToTermFrequency.put(wordToDimensionMap.get(word), StringUtils.countMatches(sentences[i], word));
//					} else {
//						dimensionToTermFrequency.put(wordToDimensionMap.get(word), dimensionToTermFrequency.get(wordToDimensionMap.get(word)) + StringUtils.countMatches(sentences[i], word));
//					}
				}
			}
			
			
			Dimension [] dimensionArray = new Dimension[dimensionToTermFrequency.size()];
			int index = 0;
			for (Integer dimension : dimensionToTermFrequency.keySet()) {
				dimensionArray[index++] = new Dimension(dimension, dimensionToTermFrequency.get(dimension));
			}
			Arrays.sort(dimensionArray);
			documentVectorList.add(new DocumentVector(hc.getId(), hc.getLike(), dimensionArray));
			
			averageDocumentLength +=  StringUtils.join(hc.getSentences(),"").length();
			
		}
		
		int numberOfDocument = hotelCommentList.size();
		averageDocumentLength = averageDocumentLength/ numberOfDocument;
		for (Integer dimension : dimensionToIdf.keySet()) {
			dimensionToIdf.put(dimension, Math.log(numberOfDocument/(dimensionToIdf.get(dimension)+1)) );			
		}
		
		double okapiK = 2.0;
		double okapiB = 0.75;
		for (int i = 0;i < documentVectorList.size();i++) {
			Dimension [] dimensionArray = documentVectorList.get(i).getDimensionArray();
			for (Dimension d : dimensionArray) {
				//d.setValue( (okapiK+1)*d.getValue()/(d.getValue()+okapiK*(1-okapiB+okapiB*StringUtils.join(hotelCommentList.get(i).getSentences(),"").length()/averageDocumentLength ))*dimensionToIdf.get(d.getDimension()));// use okapi
				d.setValue( d.getValue()*dimensionToIdf.get(d.getDimension()));// use okapi
			}
			
		}
		
		return documentVectorList;
		
	}
	
	public static boolean isPrefixNegative(String sentence, String word) {
		int index = sentence.indexOf(word);
		for (int i = index-1;i >= 0 && i >= index - 3 ; i--) {
			if (sentence.charAt(i) == '不')return true;
		}
		return false;
		
	}
	public static List <HotelComment> process(File hotelTraining, Input type){
		List <HotelComment> hotelCommentList = new ArrayList <HotelComment>();
		try {
			//BufferedReader bf = new BufferedReader(new FileReader(hotelTraining));
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(hotelTraining), "UTF8"));
			String line = null;
			while ( (line = bf.readLine()) != null) {
				int like = 0;
				if (type == Input.TRAINING) {
					like = Integer.parseInt(line);
					line = bf.readLine();
				}
				int indexOfDelimiter = line.indexOf('|');
				int id = Integer.parseInt(line.substring(0, indexOfDelimiter));
				String opinion = line.substring(indexOfDelimiter+1);
				hotelCommentList.add(new HotelComment(id, like, opinion));

			}
			if (type == Input.TEST) {
				InputFormatProcessor.randomForestResultMerge(new File("predict_result"), hotelCommentList);
			}

			bf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return hotelCommentList;

	}

}