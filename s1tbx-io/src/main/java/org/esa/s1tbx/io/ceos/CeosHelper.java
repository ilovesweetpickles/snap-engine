/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.io.ceos;

import org.esa.s1tbx.io.binary.BinaryFileReader;
import org.esa.s1tbx.io.binary.BinaryRecord;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.util.StringUtils;
import org.jdom2.Document;

import java.io.File;
import java.io.IOException;

public class CeosHelper {

    public static FilePointerRecord[] readFilePointers(final BinaryRecord vdr, final Document filePointerXML,
                                                       final String recName) throws IOException {
        final int numFilePointers = vdr.getAttributeInt("Number of filepointer records");
        final BinaryFileReader reader = vdr.getReader();
        reader.seek(vdr.getRecordLength());
        final FilePointerRecord[] filePointers = new FilePointerRecord[numFilePointers];
        for (int i = 0; i < numFilePointers; i++) {
            filePointers[i] = new FilePointerRecord(reader, filePointerXML, recName);
        }
        return filePointers;
    }

    private static String[] ExcludedExt = new String[] {".TXT",".JPG",".TIF",".PNG","KML"};

    public static File getCEOSFile(final File baseDir, final String[] prefixList) throws IOException {
        final File[] fileList = baseDir.listFiles((file, fileName) -> {
            final String name = fileName.toUpperCase();
            for (String ext : ExcludedExt) {
                if(name.endsWith(ext)) {
                    return false;
                }
            }
            for (String prefix : prefixList) {
                if (name.startsWith(prefix) || name.endsWith('.' + prefix))
                    return true;
            }
            return false;
        });
        if(fileList != null) {
            if (fileList.length > 1) {
                throw new IOException("Multiple descriptor files found in directory:\n" + baseDir.getPath());
            }
            return fileList[0];
        }
        return null;
    }

    public static String getProductName(final BinaryRecord textRecord) {
        if (textRecord == null) return "unknown";
        final String name = textRecord.getAttributeString("Product type specifier").trim().replace("PRODUCT:", "")
                + '-' + textRecord.getAttributeString("Scene identification").trim();
        return StringUtils.createValidName(name.trim(), new char[]{'_', '-'}, '_');
    }

    public static String getProductType(final BinaryRecord textRecord) {
        if (textRecord == null) return "unknown";
        String type = textRecord.getAttributeString("Product type specifier").trim();
        type = type.replace("PRODUCT:", "");
        type = type.replace("JERS-1", "JERS1");
        type = type.replace("JERS_1", "JERS1");
        type = type.replace("ERS-1", "ERS1");
        type = type.replace("ERS_1", "ERS1");
        type = type.replace("ERS-2", "ERS2");
        type = type.replace("ERS_2", "ERS2");
        return type.trim();
    }

    public static void addMetadata(MetadataElement sphElem, BinaryRecord rec, String name) {
        if (rec != null) {
            final MetadataElement metadata = new MetadataElement(name);
            rec.assignMetadataTo(metadata);
            sphElem.addElement(metadata);
        }
    }
}