import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * UserManager
 * - 서버 측 계정 정보 로딩, 저장, 인증을 담당한다.
 * - 단순 과제용으로 파일에 username:hashedPassword 형식으로 저장한다.
 */
public class UserManager {
    private final Map<String, String> credentials = new HashMap<>();
    private final File storageFile;

    public UserManager(String filePath) {
        this.storageFile = new File(filePath);
        load();
    }

    /**
     * 신규 사용자를 등록한다. 이미 존재하면 false를 반환한다.
     */
    public synchronized boolean register(String username, String password) {
        if (credentials.containsKey(username)) {
            return false;
        }
        credentials.put(username, hash(password));
        save();
        return true;
    }

    /**
     * 사용자 인증을 수행한다.
     */
    public synchronized boolean authenticate(String username, String password) {
        String stored = credentials.get(username);
        if (stored == null) return false;
        return stored.equals(hash(password));
    }

    private void load() {
        if (!storageFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(storageFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException ignored) {}
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(storageFile))) {
            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException ignored) {}
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}

