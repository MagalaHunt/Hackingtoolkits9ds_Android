package id.packenglish.mobile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only structural inspection for NCSD/NCCH/CIA containers. */
public final class NcsdInspector {
    private static final int HEADER_BYTES = 0x200;
    private static final long MEDIA_UNIT = 0x200L;

    private NcsdInspector() { }

    public static final class Partition {
        public final int index;
        public final long offset;
        public final long length;

        Partition(int index, long offset, long length) {
            this.index = index;
            this.offset = offset;
            this.length = length;
        }
    }

    public static final class Result {
        public final String kind;
        public final String description;
        public final List<Partition> partitions;

        Result(String kind, String description, List<Partition> partitions) {
            this.kind = kind;
            this.description = description;
            this.partitions = partitions;
        }
    }

    public static Result inspect(InputStream input, long fileLength, String filename) throws IOException {
        byte[] header = readAtMost(input, HEADER_BYTES);
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        List<Partition> partitions = new ArrayList<>();

        if (hasMagic(header, 0x101, "NCSD") || hasMagic(header, 0x100, "NCSD")) {
            if (header.length < 0x160) {
                return new Result("NCSD", "Incomplete NCSD header.", partitions);
            }
            int tableOffset = 0x120;
            for (int i = 0; i < 8; i++) {
                int entry = tableOffset + (i * 8);
                if (entry + 8 > header.length) break;
                long startUnits = u32le(header, entry);
                long sizeUnits = u32le(header, entry + 4);
                if (startUnits == 0 || sizeUnits == 0) continue;
                if (startUnits > Long.MAX_VALUE / MEDIA_UNIT || sizeUnits > Long.MAX_VALUE / MEDIA_UNIT) continue;
                long offset = startUnits * MEDIA_UNIT;
                long length = sizeUnits * MEDIA_UNIT;
                if (offset < 0 || length < 0) continue;
                if (fileLength >= 0 && (offset > fileLength || length > fileLength - offset)) continue;
                partitions.add(new Partition(i, offset, length));
            }
            String desc = "NCSD container detected. Valid partition entries: " + partitions.size()
                    + ". Partition data is copied as-is; the app does not decrypt it.";
            return new Result("3DS / NCSD", desc, partitions);
        }

        if (hasMagic(header, 0x101, "NCCH") || hasMagic(header, 0x100, "NCCH")
                || lowerName.endsWith(".cxi")) {
            String desc;
            if (hasMagic(header, 0x101, "NCCH") || hasMagic(header, 0x100, "NCCH")) {
                desc = "NCCH header detected. The file can be read as a CXI/NCCH container.";
            } else {
                desc = "CXI extension detected, but no NCCH signature was found at the usual header offsets.";
            }
            return new Result("CXI / NCCH", desc, partitions);
        }

        if (lowerName.endsWith(".cia")) {
            if (header.length < 0x20) {
                return new Result("CIA", "CIA header is too short to inspect.", partitions);
            }
            long headerSize = u32le(header, 0x00);
            long certSize = u32le(header, 0x08);
            long ticketSize = u32le(header, 0x0c);
            long tmdSize = u32le(header, 0x10);
            long metaSize = u32le(header, 0x14);
            long contentSize = u64leSafe(header, 0x18);
            String contentText = contentSize < 0 ? "unreadable" : humanBytes(contentSize);
            String desc = String.format(Locale.ROOT,
                    "CIA header parsed. Header: 0x%X bytes; certificate: %s; ticket: %s; TMD: %s; metadata: %s; content area: %s.\nThis check only reads size fields; it does not decrypt or rebuild content.",
                    headerSize, humanBytes(certSize), humanBytes(ticketSize), humanBytes(tmdSize),
                    humanBytes(metaSize), contentText);
            return new Result("CIA", desc, partitions);
        }

        return new Result("Unknown", "No NCSD/NCCH signature was found, and the file extension is not CIA.", partitions);
    }

    public static void copyRange(InputStream input, java.io.OutputStream output,
                                 long offset, long length) throws IOException {
        if (offset < 0 || length < 0) throw new IOException("Invalid file range.");
        skipFully(input, offset);
        byte[] buffer = new byte[64 * 1024];
        long remaining = length;
        while (remaining > 0) {
            int wanted = (int) Math.min(buffer.length, remaining);
            int read = input.read(buffer, 0, wanted);
            if (read < 0) throw new IOException("The source file ended before the requested range was copied.");
            if (read == 0) continue;
            output.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private static void skipFully(InputStream input, long count) throws IOException {
        long remaining = count;
        byte[] discard = new byte[8192];
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int read = input.read(discard, 0, (int) Math.min(discard.length, remaining));
            if (read < 0) throw new IOException("Unable to reach the partition offset.");
            remaining -= read;
        }
    }

    private static byte[] readAtMost(InputStream input, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(max);
        byte[] buffer = new byte[4096];
        while (out.size() < max) {
            int read = input.read(buffer, 0, Math.min(buffer.length, max - out.size()));
            if (read < 0) break;
            if (read == 0) continue;
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static boolean hasMagic(byte[] bytes, int offset, String magic) {
        if (offset < 0 || offset + magic.length() > bytes.length) return false;
        for (int i = 0; i < magic.length(); i++) {
            if (bytes[offset + i] != (byte) magic.charAt(i)) return false;
        }
        return true;
    }

    private static long u32le(byte[] b, int offset) {
        if (offset < 0 || offset + 4 > b.length) return 0;
        return (b[offset] & 0xffL)
                | ((b[offset + 1] & 0xffL) << 8)
                | ((b[offset + 2] & 0xffL) << 16)
                | ((b[offset + 3] & 0xffL) << 24);
    }

    /** Returns -1 when the unsigned value cannot fit in a positive Java long. */
    private static long u64leSafe(byte[] b, int offset) {
        if (offset < 0 || offset + 8 > b.length) return -1;
        long value = 0;
        for (int i = 7; i >= 0; i--) value = (value << 8) | (b[offset + i] & 0xffL);
        return value < 0 ? -1 : value;
    }

    public static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[unit]);
    }
}
