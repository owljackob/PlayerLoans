package cz.sovajakub.economy.util;

import com.google.common.io.ByteStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class ConversionUtil {

    public static @NotNull InputStream convertUniqueId(@NotNull UUID uuid) {
        byte[] bytes = new byte[16];
        ByteBuffer.wrap(bytes)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return new ByteArrayInputStream(bytes);
    }

    public static @Nullable UUID convertBinaryStream(InputStream stream) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        try {
            buffer.put(ByteStreams.toByteArray(stream)).flip();
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
