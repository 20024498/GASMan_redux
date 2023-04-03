package Thesis;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import javax.swing.JTextField;

public class View {

    private String version  = "0.1";
    private JFrame frameProgramma;
    private JTextField pathTextField;
    private JPanel panelPrincipale;
    private JPanel pannello1;
    private JPanel pannello2;
    private JPanel pannello3;
    private JPanel pannello4;
    private JPanel pannello5;
    private JPanel pannello6;
    private JComboBox<String> comboBox2;
    private JComboBox<Integer> comboBox3;
    private JComboBox<String> comboBox4;
    private JComboBox<String> comboBox5;
    private JComboBox<String> comboBox6;
    private JButton btnNetSelect;
    private JButton btnExecute;
    private String[] infEng = {"JT","BK"};
    private Integer[] tStep = {1,2,3,4,5,6,7,8,9,10};
    private String[] ff = {"No","Yes"};
    private String[] infType = {"Smoothing","Filtering"};


    /**
     * Create the application.
     */
    public View() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frameProgramma = new JFrame();
        frameProgramma.setResizable(false);
        frameProgramma.setTitle("GASMan Ver." + version);
        frameProgramma.setBounds(100, 100, 450, 620);
        frameProgramma.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameProgramma.getContentPane().setLayout(null);

        panelPrincipale = new JPanel();
        panelPrincipale.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        panelPrincipale.setBounds(0, 0, 444, 585);
        frameProgramma.getContentPane().add(panelPrincipale);
        panelPrincipale.setLayout(null);

        pannello1 = new JPanel();
        pannello1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello1.setBounds(12, 13, 420, 62);
        panelPrincipale.add(pannello1);
        pannello1.setLayout(null);

        btnNetSelect = new JButton("Seleziona Rete");
        btnNetSelect.setBounds(12, 20, 150, 22);
        pannello1.add(btnNetSelect);

        pathTextField = new JTextField();
        pathTextField.setEditable(false);
        pathTextField.setBounds(174, 20, 232, 22);
        pannello1.add(pathTextField);
        pathTextField.setColumns(10);

        pannello2 = new JPanel();
        pannello2.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello2.setBounds(12, 88, 420, 75);
        panelPrincipale.add(pannello2);
        pannello2.setLayout(null);

        JLabel label2 = new JLabel("Seleziona motore inferenziale:");
        label2.setBounds(12, 13, 220, 16);
        pannello2.add(label2);

        comboBox2 = new JComboBox<String>(infEng);
        comboBox2.setBounds(12, 42, 140, 22);
        pannello2.add(comboBox2);

        pannello3 = new JPanel();
        pannello3.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello3.setLayout(null);
        pannello3.setBounds(12, 176, 420, 75);
        panelPrincipale.add(pannello3);

        JLabel label3 = new JLabel("Incremento dei timestep:");
        label3.setBounds(12, 13, 200, 16);
        pannello3.add(label3);

        comboBox3 = new JComboBox<Integer>(tStep);
        comboBox3.setBounds(12, 42, 140, 22);
        pannello3.add(comboBox3);

        pannello4 = new JPanel();
        pannello4.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello4.setLayout(null);
        pannello4.setBounds(12, 264, 420, 75);
        panelPrincipale.add(pannello4);

        JLabel label4 = new JLabel("Fully Factorized:");
        label4.setBounds(12, 13, 200, 16);
        pannello4.add(label4);

        comboBox4 = new JComboBox<String>(ff);
        comboBox4.setBounds(12, 42, 140, 22);
        pannello4.add(comboBox4);

        pannello5 = new JPanel();
        pannello5.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello5.setLayout(null);
        pannello5.setBounds(12, 352, 420, 75);
        panelPrincipale.add(pannello5);

        JLabel label5 = new JLabel("Tipo di inferenza:");
        label5.setBounds(12, 13, 200, 16);
        pannello5.add(label5);

        comboBox5 = new JComboBox<String>(infType);
        comboBox5.setBounds(12, 42, 140, 22);
        pannello5.add(comboBox5);

        pannello6 = new JPanel();
        pannello6.setLayout(null);
        pannello6.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        pannello6.setBounds(12, 440, 420, 75);
        panelPrincipale.add(pannello6);

        JLabel label6 = new JLabel("Seleziona caso di studio:");
        label6.setBounds(12, 13, 200, 16);
        pannello6.add(label6);

        comboBox6 = new JComboBox<String>();
        comboBox6.setBounds(12, 42, 140, 22);
        pannello6.add(comboBox6);

        btnExecute = new JButton("ESEGUI");
        btnExecute.setBounds(170, 528, 120, 44);
        panelPrincipale.add(btnExecute);

        frameProgramma.setVisible(true);
    }



    public JFrame getFrameProgramma() {
        return frameProgramma;
    }

    public JTextField getPathTextField() {
        return pathTextField;
    }

    public JPanel getPanelPrincipale() {
        return panelPrincipale;
    }

    public JPanel getPannello1() {
        return pannello1;
    }

    public JPanel getPannello2() {
        return pannello2;
    }

    public JPanel getPannello3() {
        return pannello3;
    }

    public JPanel getPannello4() {
        return pannello4;
    }

    public JPanel getPannello5() {
        return pannello5;
    }

    public JPanel getPannello6() {
        return pannello6;
    }

    public JComboBox<String> getComboBox2() {
        return comboBox2;
    }

    public JComboBox<Integer> getComboBox3() {
        return comboBox3;
    }

    public JComboBox<String> getComboBox4() {
        return comboBox4;
    }

    public JComboBox<String> getComboBox5() {
        return comboBox5;
    }

    public JComboBox<String> getComboBox6() {
        return comboBox6;
    }

    public JButton getBtnNetSelect() {
        return btnNetSelect;
    }

    public JButton getBtnExecute() {
        return btnExecute;
    }

    public void setFrameProgramma(JFrame frameProgramma) {
        this.frameProgramma = frameProgramma;
    }

    public void setPathTextField(JTextField pathTextField) {
        this.pathTextField = pathTextField;
    }

    public void setPanelPrincipale(JPanel panelPrincipale) {
        this.panelPrincipale = panelPrincipale;
    }

    public void setPannello1(JPanel pannello1) {
        this.pannello1 = pannello1;
    }

    public void setPannello2(JPanel pannello2) {
        this.pannello2 = pannello2;
    }

    public void setPannello3(JPanel pannello3) {
        this.pannello3 = pannello3;
    }

    public void setPannello4(JPanel pannello4) {
        this.pannello4 = pannello4;
    }

    public void setPannello5(JPanel pannello5) {
        this.pannello5 = pannello5;
    }

    public void setPannello6(JPanel pannello6) {
        this.pannello6 = pannello6;
    }

    public void setComboBox2(JComboBox<String> comboBox2) {
        this.comboBox2 = comboBox2;
    }

    public void setComboBox3(JComboBox<Integer> comboBox3) {
        this.comboBox3 = comboBox3;
    }

    public void setComboBox4(JComboBox<String> comboBox4) {
        this.comboBox4 = comboBox4;
    }

    public void setComboBox5(JComboBox<String> comboBox5) {
        this.comboBox5 = comboBox5;
    }

    public void setComboBox6(JComboBox<String> comboBox6) {
        this.comboBox6 = comboBox6;
    }

    public void setBtnNetSelect(JButton btnNetSelect) {
        this.btnNetSelect = btnNetSelect;
    }

    public void setBtnExecute(JButton btnExecute) {
        this.btnExecute = btnExecute;
    }



}
