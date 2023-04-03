package Thesis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class Controller {

    private Model model;
    private View view;
    private boolean netSelected;
    private String filePath;
    private boolean initError;

    public Controller(){
        netSelected = false;
        filePath = "";
        initError = false;
    }

    public void start() {

        //INIZIALIZZAZIONE PATH DELLA NATIVE LIBRARY
        try {
            view = new View();
            Model.pathInit();
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            initError=true;
            JOptionPane.showMessageDialog(view.getFrameProgramma(),
                    "Impossibile settare il path della native library di Smile: "+ e.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
        }

        //LICENZA
        try {
            Model.licenseInit();
        } catch (FileNotFoundException e1) {
            initError=true;
            JOptionPane.showMessageDialog(view.getFrameProgramma(),
                    "Impossibile recuperare il file di licenza: "+ e1.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e2) {
            initError=true;
            JOptionPane.showMessageDialog(view.getFrameProgramma(),
                    "Impossibile completare l'operazione di lettura della licenza: "+ e2.getMessage(),
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
        }


        if(initError==false) {
            model = new Model();
            listenersInit();
        }

    }


    private void listenersInit() {

        ActionListener netSelectListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("net");
                int returnVal = fileChooser.showOpenDialog(view.getFrameProgramma());
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    netSelected = true;
                    filePath = fileChooser.getSelectedFile().getPath();
                    view.getPathTextField().setText(filePath);

                    //CONTROLLO ESTENSIONE E RECUPERO CASI
                    try {
                        Model.checkExtension(filePath);
                        String [] cases = Model.retrieveCases(filePath);
                        view.getComboBox6().setModel(new DefaultComboBoxModel<String>(cases));
                    } catch (ParserConfigurationException | SAXException | IOException e1) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Impossibile Parsificare i casi di studio: "+ e1.getMessage(),
                                "Errore",
                                JOptionPane.ERROR_MESSAGE);
                        netSelected =false;
                    } catch (Exception e2) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                e2.getMessage(),
                                "Errore",
                                JOptionPane.ERROR_MESSAGE);
                        netSelected =false;
                    }
                }
            }
        };

        ActionListener executeListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if(netSelected) {

                    String infEng = (String) view.getComboBox2().getSelectedItem();
                    Integer tStep = (Integer)view.getComboBox3().getSelectedItem();
                    boolean fullyfact = (((String) view.getComboBox4().getSelectedItem()).equals("Yes"))?true:false;
                    boolean filtering = (((String) view.getComboBox5().getSelectedItem()).equals("Filtering"))?true:false;
                    String caseName = (String) view.getComboBox6().getSelectedItem();

                    //ASSEGNAMENTO FILEPATH
                    model.setFilePath(filePath);

                    //CREAZIONE RETE
                    model.netCreation();

                    //INFERENZA
                    try {
                        model.printInference(filePath,caseName,infEng,fullyfact,model.getNet().getSliceCount(),tStep,filtering);
                    } catch (ParserConfigurationException | SAXException | IOException e1) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Impossibile Parsificare i casi di studio: "+ e1.getMessage(),
                                "Errore",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    //FILE DI OUTPUT
                    try {
                        model.saveFile("MatlabScript_"+model.extractFileName(filePath));
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Impossibile Completare il salvataggio dello script: "+ e1.getMessage(),
                                "Errore",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    System.out.println(model.getCode().toString());

                    if(model.isNoisyOr()) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Attenzione la rete inserita contiene un noisy-OR, durante la traduzione è il nodo è stato convertito in un nodo semplice",
                                "Attenzione",
                                JOptionPane.WARNING_MESSAGE);
                    }

                    if(model.isHmm()) {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Lo script '"+ "MatlabScript_"+model.extractFileName(filePath)+".m' è stato generato correttamente nella cartella /scripts !");
                    }
                    else {
                        JOptionPane.showMessageDialog(view.getFrameProgramma(),
                                "Attenzione la rete inserita non rappresenta un Hidden Markov Model per il seguente motivo:\n"+model.getHmmError()+"\n\nLo script'"+ "MatlabScript_"+model.extractFileName(filePath) +".m' generato potrebbe non essere corretto.",
                                "Attenzione",
                                JOptionPane.WARNING_MESSAGE);
                    }

                }

                else {
                    JOptionPane.showMessageDialog(view.getFrameProgramma(),
                            "Selezionare una rete valida per procedere",
                            "Attenzione",
                            JOptionPane.WARNING_MESSAGE);
                }

            }
        };

        view.getBtnNetSelect().addActionListener(netSelectListener);
        view.getBtnExecute().addActionListener(executeListener);
    }

}
