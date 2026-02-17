package co.com.kura.enterprise.infrastructure;

import java.io.InputStream;

public interface StorageProvider {
    String upload(String key, InputStream data, String contentType);
    String getUrl(String key);
}
