/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.f1x.tools;

import org.f1x.api.message.types.ByteEnum;
import org.f1x.api.message.types.IntEnum;
import org.f1x.api.message.types.StringEnum;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** Generates enum types from QuickFIX Dictionary. Arguments: C:\projects\toys\6pmfix\resources\quickfix\FIX44.xml C:\projects\toys\6pmfix\src */
public class DictionaryGenerator {

    private final StringBuilder textBuffer = new StringBuilder (128);
    private final File outputDir;

    public DictionaryGenerator(String output) throws IOException {
        outputDir = new File (output);
        if ( ! outputDir.exists())
            throw new IOException("Destination directory doesn't exist: '"+ outputDir.getName() + '\'');
    }

    private void process (File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);

        Writer constantsFile = generateJavaSource("class", "org.f1x.api.message.fields", "FixTags");
        process (doc, constantsFile);
        closeJavaSource(constantsFile);
    }

    private void process(Document doc, Writer constantsFile) throws IOException {
        NodeList fieldsList = doc.getElementsByTagName("fields");
        assert fieldsList.getLength() == 1;
        Node fieldsNode = fieldsList.item(0);
        NodeList fieldList = fieldsNode.getChildNodes();
        final int cnt = fieldList.getLength();
        for (int i = 0; i < cnt; i++) {
            Node fieldNode = fieldList.item(i);
            if (fieldNode.getNodeType() == Node.ELEMENT_NODE) {
                if (fieldNode.getNodeName().equals("field"))
                    process ((Element)fieldNode, constantsFile);
                else
                    System.err.println("Unexpected element: " + fieldNode.getNodeName());
            }
        }
    }

    private void process(Element fieldNode, Writer constantsFile) throws IOException {
        String name = fieldNode.getAttribute("name");
        String type = fieldNode.getAttribute("type");
        int number = Integer.parseInt(fieldNode.getAttribute("number"));

        appendFieldDefinition (name, number, constantsFile);

        switch (type) {
            case "CHAR": generateNumericEnum(name, fieldNode); break;
            case "INT": generateNumericEnum(name, fieldNode); break;
            case "STRING": generateStringEnum(name, fieldNode); break;
//            case "QTY": break;
//            case "CURRENCY": break;
//            case "AMT": break;
//            case "PRICE": break;
//            case "EXCHANGE": break;
//            case "MULTIPLEVALUESTRING": break;
        }

    }

    private void generateStringEnum(String name, Element fieldNode) throws IOException {
        NodeList fieldList = fieldNode.getElementsByTagName("value");
        if (fieldList.getLength() > 0) {
            generateEnum(name, fieldList, StringEnum.class, "String", "\"", "\"");
        }
    }


    private void generateNumericEnum(String name, Element fieldNode) throws IOException {
        NodeList fieldList = fieldNode.getElementsByTagName("value");
        if (fieldList.getLength() > 0) {
            if (isAllValuesFitInByte(fieldList))
                generateEnum(name, fieldList, ByteEnum.class, "byte", "(byte)'", "'");
            else
                generateEnum(name, fieldList, IntEnum.class, "int", null, null);
        }
    }

    private static boolean isAllValuesFitInByte(NodeList fieldList) {
        final int cnt = fieldList.getLength();
        try {
            for (int i = 0; i < cnt; i++) {
                Element valueElem = (Element) fieldList.item(i);
                String enumValue = valueElem.getAttribute("enum");
                if (enumValue.length() > 1) {
                    Integer.parseInt(enumValue);
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return true; //TODO: what if this is a string enum?
        }
    }

    private void generateEnum(String name, NodeList fieldList, Class enumBaseClass, String enumCodeType, String literalValuePrefix, String literalValueSuffix) throws IOException {
        Writer enumWriter = generateJavaSource("enum", "org.f1x.api.message.fields", name, enumBaseClass.getName());

        generateValuesList(fieldList, literalValuePrefix, literalValueSuffix, enumWriter);

        generateConstructorAndTypeCodeField(name, enumCodeType, enumWriter);

        generateValueOf (name, fieldList, literalValuePrefix, literalValueSuffix, enumWriter);

        closeJavaSource(enumWriter);
    }

    private void generateValuesList(NodeList fieldList, String literalValuePrefix, String literalValueSuffix, Writer enumWriter) throws IOException {
        final int cnt = fieldList.getLength();
        for (int i = 0; i < cnt;) {
            Element valueElem = (Element) fieldList.item(i);
            String desc = valueElem.getAttribute("description");
            String enumValue = valueElem.getAttribute("enum");
            textBuffer.setLength(0);
            textBuffer.append('\t');
            textBuffer.append(desc);
            textBuffer.append('(');
            if (literalValuePrefix != null)
                textBuffer.append(literalValuePrefix);
            textBuffer.append(enumValue);
            if (literalValueSuffix != null)
                textBuffer.append(literalValueSuffix);
            textBuffer.append(')');
            if (++i < cnt)
                textBuffer.append(',');
            else
                textBuffer.append(';');

            textBuffer.append('\n');

            enumWriter.write(textBuffer.toString());
        }
    }

    private void generateValueOf(String className, NodeList fieldList, String literalValuePrefix, String literalValueSuffix, Writer enumWriter) throws IOException {
        final int cnt = fieldList.getLength();
        textBuffer.setLength(0);
        textBuffer.append("\n\tpublic static ");
        textBuffer.append(className);
        textBuffer.append(" parse(String s) {\n");
        textBuffer.append("\t\tswitch(s) {\n");
        enumWriter.write(textBuffer.toString());
        for (int i = 0; i < cnt; i++) {
            Element valueElem = (Element) fieldList.item(i);
            String desc = valueElem.getAttribute("description");
            String enumValue = valueElem.getAttribute("enum");
            textBuffer.setLength(0);
            textBuffer.append("\t\t\tcase ");
            textBuffer.append('"');
            textBuffer.append(enumValue);
            textBuffer.append("\" : return ");
            textBuffer.append(desc);
            textBuffer.append(";\n");

            enumWriter.write(textBuffer.toString());
        }
        textBuffer.setLength(0);
        textBuffer.append("\t\t}\n\t\treturn null;\n");
        textBuffer.append("\t}\n");
        enumWriter.write(textBuffer.toString());

    }

    private void generateConstructorAndTypeCodeField(String name, String enumCodeType, Writer enumWriter) throws IOException {
        textBuffer.setLength(0);
        textBuffer.append("\n\tprivate final ");
        textBuffer.append(enumCodeType);
        textBuffer.append(" code;\n\n");

        textBuffer.append('\t');
        textBuffer.append(name);
        textBuffer.append(" (");
        textBuffer.append(enumCodeType);
        textBuffer.append(" code) {\n");
        textBuffer.append ("\t\tthis.code  = code;\n");
        if (enumCodeType.equals("String")) {
            textBuffer.append("\t\tbytes = code.getBytes();\n");
        }
        textBuffer.append ("\t}\n\n");

        textBuffer.append ("\tpublic ");
        textBuffer.append (enumCodeType);
        textBuffer.append (" getCode() { return code; }\n");

        if (enumCodeType.equals("String")) {
            textBuffer.append("\n\tprivate final byte[] bytes;");
            textBuffer.append("\n\tpublic byte[] getBytes() { return bytes; }\n\n");
        }


        enumWriter.write(textBuffer.toString());
    }


    private void appendFieldDefinition(String name, int number, Writer constantsFile) throws IOException {
        textBuffer.setLength(0);
        textBuffer.append("\tpublic static final int ");
        textBuffer.append(name);
        textBuffer.append(" = ");
        textBuffer.append(number);
        textBuffer.append(";\n");
        constantsFile.write(textBuffer.toString());
    }

    private Writer generateJavaSource (String typeName, String packageName, String simpleClassName) throws IOException {
        return generateJavaSource (typeName, packageName, simpleClassName, null);
    }

    private Writer generateJavaSource (String typeName, String packageName, String simpleClassName, String implementsInterface) throws IOException {
        File dir = new File (outputDir, packageName.replace('.', File.separatorChar));
        if ( ! dir.exists())
            if ( ! dir.mkdirs())
                throw new IOException("Can't create destination directory \'" + dir.getAbsolutePath() + '\'');

        File sourceFile = new File (dir, simpleClassName + ".java");
        FileWriter writer = new FileWriter (sourceFile, false);
        textBuffer.setLength(0);

        textBuffer.append("package ");
        textBuffer.append(packageName);
        textBuffer.append(";\n\n");

        textBuffer.append("// Generated by ");
        textBuffer.append(this.getClass().getName());
        textBuffer.append(" from QuickFIX dictionary\n");

        textBuffer.append("public ");
        textBuffer.append(typeName);
        textBuffer.append(' ');

        textBuffer.append(simpleClassName);
        if (implementsInterface != null) {
            textBuffer.append(" implements ");
            textBuffer.append(implementsInterface);
        }
        textBuffer.append(" {\n");

        writer.write(textBuffer.toString());
        return writer;
    }

    private void closeJavaSource(Writer writer) throws IOException {
        textBuffer.setLength(0);
        textBuffer.append("\n}");

        writer.write(textBuffer.toString());
        writer.close();
    }


    public static void main (String [] args) throws Exception {
        // syntax: dictionary.xml output-dir
        String dictionary = args[0];
        String outputDir = args[1];

        DictionaryGenerator gen = new DictionaryGenerator(outputDir);
        gen.process(new File (dictionary));
    }
}
