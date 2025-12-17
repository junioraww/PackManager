package me.junioraww.packmanager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.UUID;

public class ResourcePackUtil {
  public static byte[] sha1(File file) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-1");

    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] buffer = new byte[8192];
      int read;
      while ((read = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }
    return digest.digest();
  }

  public static UUID uuidFromHash(byte[] hash) {
    ByteBuffer bb = ByteBuffer.wrap(hash);
    long most = bb.getLong();
    long least = bb.getLong();
    return new UUID(most, least);
  }

  public static String hex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static void download(String url, File target) throws Exception {
    target.getParentFile().mkdirs();

    try (var in = new java.net.URL(url).openStream();
         var out = new java.io.FileOutputStream(target)) {

      byte[] buffer = new byte[8192];
      int len;
      while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
      }
    }
  }
}
