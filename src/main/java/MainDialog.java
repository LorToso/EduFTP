import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainDialog extends JDialog {


    private static final Logger LOGGER = Logger.getLogger(MainDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonApply;
    private JButton buttonClose;
    private JButton durchsuchenButton;
    private JTextField selectedFileField;
    private JProgressBar progressBar;
    private JTextField titleField;
    private JRadioButton unternehmensprofilRadioButton;
    private JRadioButton käuferprofilRadioButton;
    private JRadioButton führungskraftgesuchRadioButton;
    private JRadioButton führungskraftangebotRadioButton;
    private JRadioButton veröffentlichungRadioButton;
    private JRadioButton veranstaltungRadioButton;

    private ButtonGroup group = new ButtonGroup();
    private SimpleFTP ftp = new SimpleFTP();
    private File selectedFile;
    private Type contentType = Type.UNTERNEHMENS_PROFIL;

    public MainDialog() throws IOException {
        LOGGER.addHandler(new FileHandler("Upload.log"));
        setTitle("Datei hochladen");
        setResizable(false);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonApply);
        group.add(unternehmensprofilRadioButton);
        group.add(käuferprofilRadioButton);
        group.add(führungskraftgesuchRadioButton);
        group.add(führungskraftangebotRadioButton);
        group.add(veröffentlichungRadioButton);
        group.add(veranstaltungRadioButton);
        unternehmensprofilRadioButton.setSelected(true);

        progressBar.setStringPainted(true);
        resetStatusField();

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

        unternehmensprofilRadioButton.addActionListener((e) -> contentType = Type.UNTERNEHMENS_PROFIL);
        käuferprofilRadioButton.addActionListener((e) -> contentType = Type.KAEUFER_PROFIL);
        führungskraftgesuchRadioButton.addActionListener((e) -> contentType = Type.FUEHRUNSKRAFT_GESUCH);
        führungskraftangebotRadioButton.addActionListener((e) -> contentType = Type.FUEHRUNSKRAFT_ANGEBOT);
        veröffentlichungRadioButton.addActionListener((e) -> contentType = Type.VEROEFFENTLICHUNG);
        veranstaltungRadioButton.addActionListener((e) -> contentType = Type.VERANSTALTUNG);
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
        if(selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Es wurde keine Datei ausgewählt", "Keine Datei", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(titleField.getText() == null || titleField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Es wurde kein Titel angegeben", "Kein Titel.", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String normalizedFilename =
                Normalizer
                        .normalize(selectedFile.getName(), Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "")
                        .replaceAll("\\s","");


        CompletableFuture.runAsync(this::startConnection)
        .thenAccept((n) -> checkIfFileAlreadyExists(normalizedFilename))
        .thenAccept((n) -> uploadFile(normalizedFilename))
        .thenAccept((n) -> editHTML(normalizedFilename))
        .thenAccept((n) -> finalizeUpload())
        .exceptionally(ex -> {
            ex.printStackTrace();
            finishUpload(false);
            LOGGER.log(Level.SEVERE,ex, ex::toString);
            return null;
        })
        .whenComplete((a,b) -> LOGGER.info("File " + normalizedFilename + " uploaded successfully"));
    }

    private void startConnection() {
        try {
            startUpload();
            ftp.connect(Credentials.FTP_SERVER, Credentials.USER, Credentials.PASSWORD);
            ftp.cd("/new/web/uploads/pdf");
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void editHTML(String normalizedFileName) {
        try{
            ftp.cd_up();
            ftp.cd_up();
            File tempFile1 = generateTempFile();
            File tempFile2 = generateTempFile();

            String typeFileName;
            String typeOldLine;

            switch (contentType){
                case UNTERNEHMENS_PROFIL:
                    typeFileName = "verkaufsangebote-kaufsangebote.html";
                    typeOldLine = "<ul class=\"Unternehmensprofile\">";
                    break;
                case KAEUFER_PROFIL:
                    typeFileName = "verkaufsangebote-kaufsangebote.html";
                    typeOldLine = "<ul class=\"Käuferprofile\">";
                    break;
                case FUEHRUNSKRAFT_GESUCH:
                    typeFileName = "fuehrungskraefte-vermittlung.html";
                    typeOldLine = "<ul class=\"Gesuche\">";
                    break;
                case FUEHRUNSKRAFT_ANGEBOT:
                    typeFileName = "fuehrungskraefte-vermittlung.html";
                    typeOldLine = "<ul class=\"Angebote\">";
                    break;
                case VEROEFFENTLICHUNG:
                    typeFileName = "veroeffentlichungen.html";
                    typeOldLine="<ul class=\"Veröffentlichungen\">";
                    break;
                case VERANSTALTUNG:
                    typeFileName = "veranstaltungen.html";
                    typeOldLine = "<ul class=\"Veranstaltungen\">";
                    break;
                default:
                    throw new IOException("Invalid type!");
            }

            File file = ftp.downloadFile(typeFileName, tempFile1);
            List<String> lines = Files.readAllLines(file.toPath());
            String newLine = "<li><a href=\"http://unu-nachfolge.de/uploads/pdf/" + normalizedFileName + "\" target=\"_blank\">" + titleField.getText() + "</a></li>";
            insertAfter(lines, typeOldLine, newLine);
            Files.write(tempFile2.toPath(), lines);
            ftp.store(tempFile2, typeFileName);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void insertAfter(List<String> lines, String oldLine, String newLine) {

        for(int i = 0; i < lines.size(); i++){
            if(!lines.get(i).trim().equalsIgnoreCase(oldLine.trim()))
                continue;
            lines.add(i+1, newLine);
            return;
        }
    }

    private void finalizeUpload() {
        try {
            ftp.close();
            finishUpload(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void uploadFile(String normalizedFilename) {
        try {
            ftp.store(selectedFile, normalizedFilename);
            finishUpload(true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkIfFileAlreadyExists(String normalizedFilename) {
        try{
            startUpload();
            if(ftp.fileExists(normalizedFilename)){
                JOptionPane.showMessageDialog(this, "Normalisierter Dateiname " + normalizedFilename + " existiert bereits!", "Datei existiert bereits", JOptionPane.ERROR_MESSAGE);
                throw new IOException("Filename already exists.");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private String  findTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
    @NotNull
    private File generateTempFile() {
        String tempDir = findTempDir();
        return new File(tempDir + UUID.randomUUID());
    }

    private void finishUpload(boolean successful) {
        progressBar.setIndeterminate(false);
        if(successful){
            progressBar.setString("Erfolgreich!");
            //progressBar.setForeground(Color.GREEN);
            //progressBar.setBackground(Color.BLACK);
        }
        else {
            progressBar.setString("Fehler!");
            //progressBar.setForeground(Color.RED);
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

    public static void main(String[] args) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, IOException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        MainDialog dialog = new MainDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }

    enum Type{
        UNTERNEHMENS_PROFIL,
        KAEUFER_PROFIL,
        FUEHRUNSKRAFT_GESUCH,
        FUEHRUNSKRAFT_ANGEBOT,
        VEROEFFENTLICHUNG,
        VERANSTALTUNG
    }
}
