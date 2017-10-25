package de.lorenzotoso;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.util.Arrays;

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
    public void store(File file, String newFileName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        client.storeFile(newFileName, fileInputStream);
    }

    @Override
    public void close() throws Exception {
        if(client.isConnected()) {
            client.disconnect();
        }
    }

    public boolean fileExists(String filename) throws IOException {
        return client.listFiles(filename).length > 0;
    }

    public void cd(String path) throws IOException {
        String[] split = path.split("/");
        for (String part : split) {
            if(part.isEmpty())
                continue;
            client.changeWorkingDirectory(part);
            System.out.println(part );
            System.out.println(Arrays.toString(client.listFiles()));
        }
    }

    public void cd_up() throws IOException {
        client.changeToParentDirectory();
    }

    public File downloadFile(String filename, File resultFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(resultFile);
        client.retrieveFile(filename, outputStream);
        outputStream.close();
        return resultFile;
    }
}
