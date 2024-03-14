package com.ra.common.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * XML文档解析工具栏
 * DOM解析
 */
public class XmlUtils {
    /**
     * 私有方法，把String转换成DOM文档
     * @param xmlStr xml的字符串
     * @return
     */
    private static Document parseXmlString(String xmlStr){

        try{
            InputSource is = new InputSource(new StringReader(xmlStr));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            return doc;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 把XML格式的String转换成Hashmap
     * @param bodyXml xml的字符串
     * @return
     */
    public static Map<String,Object> getXmlBodyContext(String bodyXml){

        Map<String,Object> dataMap = new HashMap<String,Object>();

        Document doc = parseXmlString(bodyXml);
        if(null != doc){
            NodeList rootNode = doc.getElementsByTagName("xml");
            if(rootNode != null){

                Node root = rootNode.item(0);
                NodeList nodes = root.getChildNodes();
                for(int i = 0;i < nodes.getLength(); i++){
                    Node node = nodes.item(i);
                    dataMap.put(node.getNodeName(), node.getTextContent());
                }
            }
        }
        return dataMap;
    }
}
