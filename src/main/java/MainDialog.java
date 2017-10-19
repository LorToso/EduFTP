import com.sun.xml.internal.ws.util.CompletedFuture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MainDialog extends JDialog {
    public static String FTP_SERVER = "localhost";
    public static String USER = "test";
    public static String PASSWORD = "test";


    private JPanel contentPane;
    private JButton buttonApply;
    private JButton buttonClose;
    private JButton durchsuchenButton;
    private JTextField selectedFileField;
    private JProgressBar progressBar;

    private SimpleFTP ftp = new SimpleFTP();
    private File selectedFile;

    public MainDialog() {
        setResizable(false);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonApply);

        progressBar.setStringPainted(true);

        buttonApply.addActionListener(e -> onApply());

        buttonClose.addActionListener(e -> onClose());
        durchsuchenButton.addActionListener(e -> onBrowse());

        // call onClose() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        // call onClose() on ESCAPE
        contentPane.registerKeyboardAction(e -> onClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onBrowse() {
        FileDialog fileDialog = new FileDialog(this);
        fileDialog.setVisible(true);


        String dir = fileDialog.getDirectory();
        String filename = fileDialog.getFile();
        if (filename == null)
            return;
        selectedFile = new File(dir + filename);
        selectedFileField.setText(selectedFile.getAbsolutePath());
        resetStatusField();
    }

    private void resetStatusField() {
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);
        progressBar.setString("");
    }

    private void onApply() {
        if(selectedFile == null)
            return;
        CompletableFuture.runAsync(() -> {
            try {
                startUpload();
                ftp.connect(FTP_SERVER, USER, PASSWORD);
                ftp.store(selectedFile);
                finishUpload(true);
            } catch (IOException e) {
                finishUpload(false);
                System.err.print(e);
            }
        });
    }

    private void finishUpload(boolean successful) {
        progressBar.setIndeterminate(false);
        if(successful){
            progressBar.setString("Erfolgreich!");
            UIManager.put("ProgressBar.foreground", Color.GREEN);
            UIManager.put("ProgressBar.selectionForeground", Color.GREEN);
        }
        else {
            progressBar.setString("Fehler!");
            UIManager.put("ProgressBar.background", Color.ORANGE);
            UIManager.put("ProgressBar.selectionBackground", Color.RED);
        }
        progressBar.setValue(100);

    }

    private void startUpload() {
        progressBar.setValue(0);
        progressBar.setString("Datei wird hochgeladen...");
        progressBar.setIndeterminate(true);
    }

    private void onClose() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainDialog dialog = new MainDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}