
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.StringReader;  
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.nio.file.Path;


public class ParseFiles {
	public static ArrayList<ArrayList<String>> parseFile(String filePath) throws SAXException, IOException, ParserConfigurationException{
		ArrayList<String> contents = new ArrayList<String>();
		ArrayList<String> docids = new ArrayList<String>();
		ArrayList<ArrayList<String>> docs =  new ArrayList<ArrayList<String>>();
//        System.out.println(filePath);
        
      	File file = new File(filePath);
	   	String parent = file.getParent();
		DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		String content = "";
		try
		{
			// read the file content into a string
		    content = new String (Files.readAllBytes(Paths.get(filePath)));
		} 
		catch (IOException e) 
		{
		    e.printStackTrace();
		}
	
		String header = "HEADLINE";
		if(parent.endsWith("fbis")){ // deal with the fbis file
	   		header = "TI";
			content = content.replaceAll("<F P=[0-9]+>", "");
			content = content.replaceAll("<3>", "");
			content = content.replaceAll("</3>", "");
			content = content.replaceAll("</F>", "");
			content = content.replaceAll("<FIG ID=[a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+-[a-zA-Z0-9]+>", "");
			content = content.replaceAll("</FIG>", "");
			content = content.replaceAll("&", "&amp;");
		}

		content = "<root>" + content + "</root>"; // add a root tag so that the file can be parsed as a xml file
		// parse the file 
		Document doc = dBuilder.parse(new InputSource(new StringReader(content)));
	   
	
		NodeList nList = doc.getElementsByTagName("DOC");
		for(int i = 0; i < nList.getLength(); i++) {
			Node nNode = nList.item(i);
			if(nNode.getNodeType() == Node.ELEMENT_NODE) {
				String docid = "";
				String head = "";
			   	String text = "";
				Element eElement = (Element) nNode;
				if(eElement.getElementsByTagName("DOCNO").item(0) != null) {
					docid = eElement.getElementsByTagName("DOCNO").item(0).getTextContent();
					docid= docid.trim();
				}
				if(eElement.getElementsByTagName(header).item(0) != null) {
					head = eElement.getElementsByTagName(header).item(0).getTextContent();
					if(parent.contains("ft")){
						int staretIndex = head.indexOf("/");  // index of the  '/' which separates data and title
						
						head = head.substring(staretIndex+1); // only keep the tile
					}
					
					head = head.replace("\r", " ");
					head = head.replace("\n", " ");
					head = head.trim();

//					System.out.println("title: " + head);
				}
				
				if(eElement.getElementsByTagName("TEXT").item(0) != null) {
					text = eElement.getElementsByTagName("TEXT").item(0).getTextContent();
					text = text.replace("\r", " ");
					text = text.replace("\n", " ");
					text = text.trim();
				}
				contents.add(head+text);
				docids.add(docid);
//				System.out.println("DOCNO:" + eElement.getElementsByTagName("DOCNO").item(0).getTextContent());
//				System.out.println("TEXT:" + eElement.getElementsByTagName("TEXT").item(0).getTextContent());
//				System.out.println("HEADLINE:" + eElement.getElementsByTagName(header).item(0).getTextContent());
	
			}
		}
	    docs.add(docids);
	    docs.add(contents);
		return docs;
		
	}
	public static ArrayList<ArrayList<String>> parseQuery(String filePath) {
		String content = "";
		try
		{
		    content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
		} 
		catch (IOException e) 
		{
		    e.printStackTrace();
		}

		ArrayList<String> topics = new ArrayList<String>();
		ArrayList<String> topicnums = new ArrayList<String>();
		ArrayList<ArrayList<String>> queries = new ArrayList<ArrayList<String>>();

		String [] strArray = content.split("</top>");

//		System.out.println(strArray.length);
		for(int i = 0; i < strArray.length -1; i++) {
			String s = strArray[i];
			String num = "";
			String title = "";
			String desc = "";
			
		    //  locate num
			int startIndex = s.indexOf("<num>");
			int endIndex =  s.indexOf("<title>");

			num = s.substring(startIndex, endIndex).replace("<num>","").replace("Number:","").trim();

			
			//  locate title
			startIndex = endIndex;
			endIndex =  s.indexOf("<desc>");


			title = s.substring(startIndex, endIndex).replace("<title>","").trim();


			// locate description
			startIndex = s.indexOf("Description");
			endIndex =  s.indexOf("<narr>");

			desc = s.substring(startIndex, endIndex).replace("Description:","").trim(); 

			
			String topic = title + ' ' + desc;
//			String topic = title;
			
			// query parser would fail if the query contains some special character
			topic = topic.replaceAll("/", " ");
			topic = topic.replaceAll("\\?", "");
			

			topics.add(topic);
			topicnums.add(num);
			 
		}
		queries.add(topicnums);
		queries.add(topics);
		
		return queries;

	
	}

}
