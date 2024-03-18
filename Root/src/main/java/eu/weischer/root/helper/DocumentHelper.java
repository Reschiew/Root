package eu.weischer.root.helper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import eu.weischer.root.application.Logger;

public abstract class DocumentHelper {
    public static Element getXmlElement (Node parentNode, String elementName) {
        try {
            NodeList childList = parentNode.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node child = childList.item(i);
                if (child.getNodeName().equals(elementName))
                    return (Element) child;
            }
        } catch (Exception ex) {
            log.e(ex, "Exception during getXmlElement");
        }
        return null;
    }
    public static List<Element> getXmlListChilds(Node parentNode, String listName, String elementName) {
        try {
            Element list = getXmlElement(parentNode, listName);
            if (list != null)
                return getXmlChilds(list, elementName);
        } catch (Exception ex) {
            log.e(ex,"Exception during getXmlListChilds");
        }
        return new LinkedList<Element>();
    }
    public static List<Element> getXmlChilds (Node parentNode, String elementName) {
        List<Element> result = new LinkedList<>();
        try {
            NodeList childList = parentNode.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node child = childList.item(i);
                if (child.getNodeName().equals(elementName))
                    result.add((Element) child);
            }
        } catch (Exception ex) {
            log.e(ex, "Exception during getXmlChilds");
            result.clear();
        }
        return result;
    }
    public static String getXmlTextContent(Node parentNode, String elementName) {
        Element element = getXmlElement (parentNode, elementName);
        return element==null ? "" : element.getTextContent();
    }
    public static void logDocument(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            String lines [] = writer.getBuffer().toString().split("\n|\r");
            for (int i=0; i<lines.length; i++)
                log.v(lines[i]);
        } catch (Exception ex) {
            log.e(ex,"Exception during logDocument");
        }
    }
    public static Element getLocalXmlElement (Node parentNode, String elementName) {
        try {
            NodeList childList = parentNode.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node child = childList.item(i);
                String name = child.getLocalName() == null ? child.getNodeName() : child.getLocalName();
                if (name.equals(elementName))
                    return (Element) child;
            }
        } catch (Exception ex) {
            log.e(ex, "Exception during getXmlElement");
        }
        return null;
    }
    private static final Logger.LogAdapter log = Logger.getLogAdapter("DocumentHelper");
}
