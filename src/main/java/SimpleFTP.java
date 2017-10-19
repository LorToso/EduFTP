import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SimpleFTP implements AutoCloseable{
    private FTPClient client;

    public SimpleFTP() {
        this.client = new FTPClient();
    }
    public void connect(String server, String user, String password) throws IOException {
        client.connect(server);
        client.user(user);
        client.pass(password);

        int replyCode = client.getReplyCode();
        if(!FTPReply.isPositiveCompletion(replyCode)){
            throw new IOException("Negative reply code");
        }
    }
    public void store(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        client.storeFile(file.getName(), fileInputStream);
    }

    @Override
    public void close() throws Exception {
        if(client.isConnected()) {
            try {
                client.disconnect();
            } catch (IOException ioe) {
                // do nothing
            }
        }
    }
}
