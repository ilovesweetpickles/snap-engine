package org.esa.beam.dataio.ceos.avnir2.records;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecord;
import org.esa.beam.dataio.ceos.records.BaseLeaderFileDescriptorRecordTest;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.Arrays;

/*
 * $Id: Avnir2LeaderFDRTest.java,v 1.1 2006/09/13 09:12:35 marcop Exp $
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

/**
 * Created by marco.
 *
 * @author marco
 * @version $Revision: 1.1 $ $Date: 2006/09/13 09:12:35 $
 */
public class Avnir2LeaderFDRTest extends BaseLeaderFileDescriptorRecordTest {

    protected BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader, final int startPos) throws
                                                                                                              IOException,
                                                                                                              IllegalCeosFormatException {
        return new Avnir2LeaderFDR(reader, startPos);
    }

    protected BaseLeaderFileDescriptorRecord createLeaderFDR(final CeosFileReader reader) throws IOException,
                                                                                                 IllegalCeosFormatException {
        return new Avnir2LeaderFDR(reader);
    }

    protected void writeFields17To21(final ImageOutputStream ios) throws IOException {
        // Field 17
        ios.writeBytes("                "); // 16 blanks
        // Field 18
        ios.writeBytes("     3");
        ios.writeBytes("   541");
        ios.writeBytes(" 32");
        ios.writeBytes("N");
        // Field 19
        ios.writeBytes("                "); // 16 blanks
        // Field 20
        ios.writeBytes("                "); // 16 blanks

        // Field 21
        final char[] blanks = new char[4256];
        Arrays.fill(blanks, ' ');
        ios.writeBytes(new String(blanks));
    }

    protected void assertRecords17To21(final BaseLeaderFileDescriptorRecord record) {
        final Avnir2LeaderFDR avnir2Record = (Avnir2LeaderFDR) record;
        assertEquals(3, avnir2Record.getPixelSizeLocator());
        assertEquals(541, avnir2Record.getPixelSizeLocatorDataStart());
        assertEquals(32, avnir2Record.getPixelSizeLocatorNumBytes());
        assertEquals("N", avnir2Record.getPixelSizeLocatorDataType());
    }
}
