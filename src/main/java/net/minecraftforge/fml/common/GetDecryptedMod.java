/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class GetDecryptedMod {
    public static String immutableKey;

    public static byte[] getDecryptedBytes(final String key, final File inputFile) {
        try {
            final Key secretKey = new SecretKeySpec(key.getBytes(), "AES");
            final Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            final FileInputStream inputStream = new FileInputStream(inputFile);
            final byte[] inputBytes = new byte[(int)inputFile.length()];
            inputStream.read(inputBytes);
            final byte[] outputBytes = cipher.doFinal(inputBytes);
            inputStream.close();
            return outputBytes;
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | IOException ex3) {
            final Exception ex2 = null;
            final Exception ex = ex2;
            FMLCommonHandler.instance().exitJava(0, true);
            return null;
        }
    }

    public static String getBase64(final String s) {
        return new String(Base64.decodeBase64(s));
    }

    public static String getResponseBody(HttpURLConnection conn) throws IOException {
        Reader streamReader = null;
        streamReader = new InputStreamReader(conn.getInputStream());
        StringBuilder fullResponseBuilder = new StringBuilder();

        BufferedReader in = new BufferedReader(streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        return content.toString();
    }
}
