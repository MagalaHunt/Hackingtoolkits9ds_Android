import id.packenglish.mobile.NcsdInspector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class NcsdInspectorSmokeTest {
    public static void main(String[] args) throws Exception {
        byte[] image = new byte[0x2000];
        image[0x101] = 'N'; image[0x102] = 'C'; image[0x103] = 'S'; image[0x104] = 'D';
        put32(image, 0x120, 1); // offset in 0x200-byte media units
        put32(image, 0x124, 2); // length in media units
        for (int i = 0x200; i < 0x600; i++) image[i] = (byte) (i & 0xff);

        NcsdInspector.Result result = NcsdInspector.inspect(
                new ByteArrayInputStream(image), image.length, "homebrew.3ds");
        if (!"3DS / NCSD".equals(result.kind)) throw new AssertionError(result.kind);
        if (result.partitions.size() != 1) throw new AssertionError("partition count");
        if (result.partitions.get(0).offset != 0x200) throw new AssertionError("partition offset");
        if (result.partitions.get(0).length != 0x400) throw new AssertionError("partition length");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NcsdInspector.copyRange(new ByteArrayInputStream(image), output, 0x200, 0x400);
        if (output.size() != 0x400) throw new AssertionError("export length");
        if (!Arrays.equals(Arrays.copyOfRange(image, 0x200, 0x600), output.toByteArray())) {
            throw new AssertionError("export bytes differ");
        }
        System.out.println("NcsdInspector smoke test passed");
    }

    private static void put32(byte[] b, int offset, long value) {
        for (int i = 0; i < 4; i++) b[offset + i] = (byte) (value >>> (8 * i));
    }
}
