/*
 * $Id: XmlWriter.java,v 1.1.1.1 2006/09/11 08:16:47 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.util;

import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

//@todo 1 he/nf - class documentation

public class XmlWriter {

    public final static String XML_HEADER_LINE = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";

    private final static String[] _indents = initIndents();
    private final PrintWriter _pWriter;
    private static XMLOutputter _xmlOutputter;

    public XmlWriter(File file) throws IOException {
        this(new FileWriter(file), true);
    }

    public XmlWriter(Writer writer, boolean initHeadline) {
        Guardian.assertNotNull("writer", writer);
        if (writer instanceof PrintWriter) {
            _pWriter = (PrintWriter) writer;
        } else {
            _pWriter = new PrintWriter(writer);
        }

        _xmlOutputter = new XMLOutputter();
        final Format format = _xmlOutputter.getFormat();
        format.setIndent("    ");   // four spaces
        _xmlOutputter.setFormat(format);
        if (initHeadline) {
            init();
        }
    }

    private void init() {
        _pWriter.println(XML_HEADER_LINE);
    }

    public void println(String str) {
        _pWriter.println(str);
    }

    public void print(String str) {
        _pWriter.print(str);
    }

    public void printElement(int indent, Element element) {
        if (element != null) {
            try {
                final StringWriter sw = new StringWriter();
                _xmlOutputter.output(element, sw);
                final StringBuffer buffer = sw.getBuffer();
                final BufferedReader br = new BufferedReader(new StringReader(buffer.toString()));
                String line;
                while ((line = br.readLine()) != null) {
                    _pWriter.write(_indents[indent]);
                    _pWriter.write(line);
                    _pWriter.println();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        _pWriter.close();
    }

    public static String[] createTags(int indent, String name) {
        final String[] tags = new String[2];
        tags[0] = _indents[indent] + "<" + name + ">";
        tags[1] = _indents[indent] + "</" + name + ">";
        return tags;
    }

    public static String[] createTags(int indent, String name, String[][] attributes) {
        Debug.assertNotNullOrEmpty(name);
        Debug.assertNotNull(attributes);
        final StringBuffer tag = new StringBuffer();
        tag.append(_indents[indent]);
        tag.append("<");
        tag.append(name);
        for (int i = 0; i < attributes.length; i++) {
            final String[] att_val = attributes[i];
            if (att_val.length > 1) {
                final String attribute = att_val[0];
                final String value = att_val[1];
                if (attribute != null && attribute.length() > 0) {
                    tag.append(" " + attribute + "=\"");
                    if (value != null) {
                        tag.append(encode(value));
                    }
                    tag.append("\"");
                }
            }
        }
        tag.append(">");
        final String[] tags = new String[2];
        tags[0] = tag.toString();
        tags[1] = _indents[indent] + "</" + name + ">";
        return tags;
    }

    public void printLine(int indent, String tagName, boolean b) {
        printLine(indent, tagName, String.valueOf(b));
    }

    public void printLine(int indent, String tagName, int i) {
        printLine(indent, tagName, String.valueOf(i));
    }

    public void printLine(int indent, String tagName, float f) {
        printLine(indent, tagName, String.valueOf(f));
    }

    public void printLine(int indent, String tagName, double d) {
        printLine(indent, tagName, String.valueOf(d));
    }

    public void printLine(int indent, String tagName, String text) {
        final String[] tags = createTags(indent, tagName);
        printLine(tags, text);
    }

    public void printLine(int indent, String tagName, String[][] attributes, String text) {
        final String[] tags = createTags(indent, tagName, attributes);
        printLine(tags, text);
    }

    public void printLine(String[] tags, String text) {
        if (text != null && text.trim().length() > 0) {
            _pWriter.print(tags[0]);
            _pWriter.print(encode(text));
            _pWriter.println(tags[1].trim());
        } else {
            _pWriter.println(tags[0].substring(0, tags[0].length() - 1).concat(" />"));
        }
    }

    private static String[] initIndents() {
        final String[] indents = new String[20];
        indents[0] = "";
        indents[1] = "    "; // four spaces per indent level
        for (int j = 2; j < indents.length; j++) {
            indents[j] = indents[j - 1] + indents[1];
        }
        return indents;
    }

    private static String encode(String text) {
        if (text != null) {
            text = _xmlOutputter.outputString(new Text(text.trim()));
        }
        return text;
    }

}
