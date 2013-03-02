/*
 * Copyright (C) 2012 McEvoy Software Ltd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.milton.cloud.server.web.templating;

import io.milton.cloud.server.web.RenderFileResource;
import io.milton.cloud.util.JDomUtils;
import io.milton.common.Path;
import java.io.*;
import java.util.Arrays;
import javax.xml.stream.XMLStreamException;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Source;
import org.apache.commons.io.IOUtils;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 *
 * @author brad
 */
public class HtmlTemplateParser {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HtmlTemplateParser.class);
    public static String CDATA_START = "<![CDATA[";
    public static String CDATA_END = "]]>";
    private static long time;
    private HtmlFormatter htmlFormatter = new HtmlFormatter();

    /**
     * Parse the file associated with the meta, extracting webresources, body
     * class attributes and the template, and setting that information on the
     * meta object
     *
     * @param meta
     */
    public void parse(HtmlPage meta, Path webPath) throws IOException, XMLStreamException {
        log.info("parse: " + meta.getSource() + " - " + meta.getClass() + " accumulated time=" + time + "ms");
        long tm = System.currentTimeMillis();


        try (InputStream fin = meta.getInputStream()) {
            if (fin != null) {
                BufferedInputStream bufIn = new BufferedInputStream(fin);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copy(bufIn, bout);
                String sourceXml = bout.toString("UTF-8");
                Source source = new Source(sourceXml);
                net.htmlparser.jericho.Element elHead = source.getFirstElement("head");
                parseWebResourcesFromHtml(elHead, meta, webPath);

                net.htmlparser.jericho.Element elBody = source.getFirstElement("body");
                if (elBody != null) {
                    String sBodyClasses = elBody.getAttributeValue("class");
                    if (sBodyClasses != null) {
                        meta.getBodyClasses().clear();
                        meta.getBodyClasses().addAll(Arrays.asList(sBodyClasses.split(" ")));
                    }
                }
                String body = getContent(elBody);
                meta.setBody(body);
            }
        }
        tm = System.currentTimeMillis() - tm;
        time += tm;
    }

    /**
     * Does the opposite of parse, formats the structured fields into HTML
     *
     * @param aThis
     * @param bout
     */
    public void update(RenderFileResource r, ByteArrayOutputStream bout) {
        htmlFormatter.update(r, bout);
    }

    private void parseWebResourcesFromHtml(net.htmlparser.jericho.Element elHead, HtmlPage meta, Path webPath) {
        //System.out.println("parseWebResourcesFromHtml");
        meta.getWebResources().clear();
        for (net.htmlparser.jericho.Element wrTag : elHead.getChildElements()) {
            //System.out.println("tag: " + wrTag.getName());
            if (wrTag.getName().equals("title")) {
                String s = wrTag.getRenderer().toString();
                meta.setTitle(s);
            } else {
                if (!wrTag.getName().startsWith("!")) {
                    WebResource wr = new WebResource(webPath);
                    meta.getWebResources().add(wr);
                    wr.setTag(wrTag.getName());
                    String body = getContent(wrTag);
                    wr.setBody(body);
                    Attributes atts = wrTag.getAttributes();
                    if (atts != null) {
                        for (net.htmlparser.jericho.Attribute att : atts) {
                            wr.getAtts().put(att.getName(), att.getValue());
                        }
                    }
                }
            }
        }
    }

    public static String getContent(net.htmlparser.jericho.Element el) {
        String s = el.getContent().toString().trim();
        s = stripCDATA(s);
        return s;
    }

    public static String stripCDATA(String s) {
        if (s.startsWith(CDATA_START)) {
            s = s.substring(CDATA_START.length());
        }
        if (s.endsWith(CDATA_END)) {
            s = s.substring(0, s.length() - CDATA_END.length());
        }
        return s.trim();
    }
}
