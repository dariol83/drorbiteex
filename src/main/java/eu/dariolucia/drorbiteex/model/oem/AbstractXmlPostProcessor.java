/*
 * Copyright (c) 2023 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.drorbiteex.model.oem;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public abstract class AbstractXmlPostProcessor implements IOemPostProcessor {

    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Override
    public void postProcess(String filePath, OemGenerationRequest request, Date generationDate) {
        try {
            Document doc = load(filePath);
            doPostProcessing(doc, filePath, request, generationDate);
            writeXml(doc, filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected abstract void doPostProcessing(Document doc, String filePath, OemGenerationRequest request, Date generationDate);

    private Document load(String filePath) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new File(filePath));
    }

    private void writeXml(Document doc, String file) throws TransformerException, FileNotFoundException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new FileOutputStream(file));
        transformer.transform(source, result);
    }

    protected NodeList find(Document doc, String expression) throws XPathExpressionException {
        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
    }

}
