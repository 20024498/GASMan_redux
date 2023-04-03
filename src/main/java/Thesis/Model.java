package Thesis;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import smile.*;

public class Model {

    private String filePath;
    private Network net;
    private StringBuilder code;
    private boolean hmm;
    private String hmmError;
    private boolean noisyOr;

    public Model() {
        this.filePath = "";
        this.net = new Network();
        this.code = new StringBuilder();
        this.hmm=true;
        this.hmmError="";
        this.noisyOr = false;

    }

    public void netCreation(){


        //CARICAMENTO RETE
        net.readFile(filePath);

        //INIZIALIZZAZIONE CODICE
        code.append("clear \n\n");

        //VARIABILI NASCOSTE
        ArrayList<Integer> hStates = hiddenVariables();

        //VARIABILI OSSERVABILI (indicate con un colore diverso da quello di default)
        ArrayList<Integer> obs = observableVariables();

        //TEST HIDDEN MARKOV MODEL
        hmm=true;
        try {
            hmmTest(hStates, obs);
        }
        catch(NotHMMException e) {
            hmm=false;
            hmmError = e.getMessage();
        }

        //INSIEME DEI NOMI
        code.append("%Array containing each node name \n");
        code.append("names=[h_states, obs];\n\n");

        //NUMERO DI NODI
        code.append("%Number of nodes \n");
        code.append("n=length(names);\n\n");

        //ARCHI INTRASLICE
        intrasliceArcs();

        //MATRICE DI ADIACENZA ARCHI INTRASLICE
        code.append("%Making intraslice adjiacent matrix\n");
        code.append("[intra, names] = mk_adj_mat(intrac, names, 1);\n\n");

        //ARCHI INTERSLICE (SOLO ORDINE 1)
        intersliceArcs();

        //MATRICE DI ADIACENZA ARCHI INTERSLICE
        code.append("%Making interslice adjiacent matrix\n");
        code.append("inter = mk_adj_mat(interc, names, 0);\n\n");

        //NUMERO DI STATI DI CIASCUN NODO
        numberOfStates();

        //CREAZIONE RETE BAYESIANA
        code.append("% Creating the DBN\n");
        code.append("bnet = mk_dbn(intra, inter, ns, 'names', names);\n\n");

        //CREAZIONE LISTE NODI DI CUI CALCOLARE CPD
        code.append("% Creating the CPDs\n\n");
        code.append("%%%%%%%%% ------- slice 1 -------\n\n");

        ArrayList<Integer> tempNodes = new ArrayList<Integer>();
        ArrayList<Integer> cpdNodes = new ArrayList<Integer>();
        cpdNodesCalc(hStates, obs, cpdNodes, tempNodes);

        //CICLO CALCOLO CPD
        int tresh = cpdNodes.size() - tempNodes.size();
        for(int i =0; i<cpdNodes.size();i++) {

            if(i==tresh)
                code.append("%%%%%%%%% ------- slice 2 --------\n\n");

            int h = cpdNodes.get(i);

            // NODO PROBABILISTICO
            if(net.getNodeType(h) == Network.NodeType.CPT) {

                // NO ARCHI TEMPORALI ENTRANTI
                if(i<tresh)
                    printTabularCpd(h);

                    // ARCHI TEMPORALI ENTRANTI
                else
                    printTempTabularCpd(h);
            }

            // NODO DETERMINISTICO
            if(net.getNodeType(h) == Network.NodeType.TRUTH_TABLE) {

                // NO ARCHI TEMPORALI ENTRANTI
                if(i<tresh) {

                    // NODO DI TIPO OR
                    if(checkOR(h)) {
                        printBooleanCpdOR(h);
                    }
                    // NODO DI TIPO AND
                    else if(checkAND(h)) {
                        printBooleanCpdAND(h);
                    }
                    //NODI DETERMINISTICI GENERICI
                    else {
                        printTabularCpd(h);
                    }
                }

                //ARCHI TEMPORALI ENTRANTI
                else {

                    // NODO DETERMINISTICO GENERICO
                    printTempTabularCpd(h);
                }
            }

            // NODO RUMOROSO
            if(net.getNodeType(h) == Network.NodeType.NOISY_MAX) {

                //NO ARCHI TEMPORALI ENTRANTI
                if(i<tresh) {

                    //NODO DI TIPO NOISY-OR
                    if(checkNoisyOr(h))
                        printNoisyOr(h);

                        //NODO DI TIPO NOISY MAX
                    else
                        printNoisyMax(h);
                }

                //ARCHI TEMPORALI ENTRANTI
                else {

                    //NODO DI TIPO NOISY MAX
                    printTempNoisyMax(h);
                }
            }
        }

    }



    private ArrayList<Integer> hiddenVariables(){
        code.append("%Hidden variables\n");
        ArrayList<Integer> hStates = new ArrayList<Integer>();
        String init = "h_states = {";
        StringBuilder str = new StringBuilder (init);

        for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h)) {
            if(net.getNodeBgColor(h).equals(new Color(229,246,247))) {
                hStates.add(h);
                str.append("'"+net.getNodeId(h)+"'");
                str.append(", ");
            }
        }

        if(str.length()>init.length())
            truncList(str, 2);

        code.append(str);
        code.append("};\n");

        return hStates;
    }

    private ArrayList<Integer> observableVariables(){
        code.append("%Observable variables\n");
        ArrayList<Integer> obs = new ArrayList<Integer>();
        String init = "obs = {";
        StringBuilder str = new StringBuilder (init);

        for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h)) {

            if(!net.getNodeBgColor(h).equals(new Color(229,246,247))) {
                obs.add(h);
                str.append("'"+net.getNodeId(h)+"'");
                str.append(", ");
            }
        }
        if(str.length()>init.length())
            truncList(str, 2);

        code.append(str);
        code.append("};\n");

        return obs;
    }


    private void hmmTest(ArrayList<Integer> hStates, ArrayList<Integer> obs) throws NotHMMException {

        if(hStates.isEmpty())
            throw new NotHMMException("Nessuno stato nascosto trovato");
        if(obs.isEmpty())
            throw new NotHMMException("Nessuno stato osservabile trovato");
        for(Integer i : hStates)
            if(net.getMaxNodeTemporalOrder(i)>1)
                throw new NotHMMException("Archi temporali di ordine maggiore di 1 rilevati tra gli stati nascosti");
        for(Integer i : obs)
            if(net.getMaxNodeTemporalOrder(i)>0)
                throw new NotHMMException("Archi temporali rilevati tra gli osservabili");
        for(Integer h : hStates)
            for(Integer p : net.getParents(h))
                if(net.temporalArcExists(p, h, 0))
                    throw new NotHMMException("Archi temporali rilevati tra stati nascosti e osservabili");

    }

    private void intrasliceArcs() {

        code.append("%Intraslice edges\n");
        String init = "intrac = {";
        StringBuilder str = new StringBuilder (init);

        for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h)) {
            int[] children;
            if((children = net.getChildren(h)).length>0)
                for(Integer i : children) {
                    str.append("'"+net.getNodeId(h)+"'");
                    str.append(", ");
                    str.append("'"+net.getNodeId(i)+"'");
                    str.append(";\n");
                }
        }
        if(str.length()>init.length())
            truncList(str, 2);

        code.append(str);
        code.append("};\n\n");
    }

    private void intersliceArcs() {

        code.append("%Interslice edges\n");
        String init = "interc = {";
        int initlen = init.length();
        StringBuilder str = new StringBuilder (init);

        for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h)) {
            TemporalInfo[] tParents = {};
            try {tParents = net.getTemporalParents(h, 1);
            }catch(Exception e) {}

            for(TemporalInfo t : tParents) {
                str.append("'"+net.getNodeId(t.handle)+"'");
                str.append(", ");
                str.append("'"+net.getNodeId(h)+"'");
                str.append(";\n");
            }

        }


        if(init.length()>initlen)
            truncList(str, 2);

        code.append(str);
        code.append("};\n\n");

    }

    private void numberOfStates () {

        code.append("% Number of states (ns(i)=x means variable i has x states)\n");
        String init = "ns = [";
        StringBuilder str = new StringBuilder (init);

        for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h))
            str.append(net.getOutcomeCount(h)+" ");
        if(str.length()>init.length())
            truncList(str, 1);

        code.append(str);
        code.append("];\n\n");
    }

    private void cpdNodesCalc(ArrayList<Integer> hStates, ArrayList<Integer> obs, ArrayList<Integer> cpdNodes, ArrayList<Integer> tempNodes ) {

        for(Integer i : hStates)
            cpdNodes.add(i);

        for(Integer i : obs)
            cpdNodes.add(i);

        for(Integer i : hStates)
            for(int parent :net.getAllNodes())
                if(net.temporalArcExists(parent, i, 1)&& !tempNodes.contains(i)){
                    cpdNodes.add(i);
                    tempNodes.add(i);
                }

        for(Integer i : obs)
            for(int parent :net.getAllNodes())
                if(net.temporalArcExists(parent, i, 1) && !tempNodes.contains(i)){
                    cpdNodes.add(i);
                    tempNodes.add(i);
                }
    }

    private void printTabularCpd(int nodeHandle) {

        code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 1 \n");
        printParentOrder(nodeHandle);
        myCpdPrint(nodeHandle);
        int nParents = net.getParents(nodeHandle).length;
        boolean moreParents = nParents>1;
        if(moreParents)
            cpt1Print(nodeHandle);
        code.append("bnet.CPD{bnet.names('"+net.getNodeId(nodeHandle)+"')}=tabular_CPD(bnet,bnet.names('"+net.getNodeId(nodeHandle)+"'),'CPT',"+(moreParents?"cpt1":"cpt")+");\n");
        code.append("clear cpt;");
        if(moreParents)
            code.append("clear cpt1;");
        code.append("\n\n");
    }

    private void printTempTabularCpd(int nodeHandle) {

        code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 2 \n");
        printTempParentOrder(nodeHandle);
        myTempCpdPrint(nodeHandle);
        int nParents = net.getTemporalParents(nodeHandle, 1).length + net.getParents(nodeHandle).length;
        boolean moreParents = nParents>1;
        if(moreParents)
            tempCpt1Print(nodeHandle);
        code.append("bnet.CPD{bnet.eclass2(bnet.names('"+net.getNodeId(nodeHandle)+"'))}=tabular_CPD(bnet,n+bnet.names('"+net.getNodeId(nodeHandle)+"'),'CPT',"+(moreParents?"cpt1":"cpt")+");\n");
        code.append("clear cpt; ");
        if(moreParents)
            code.append("clear cpt1;");
        code.append("\n\n");
    }



    private boolean checkAND(int h) {
        if(net.getOutcomeCount(h)>2)
            return false;
        double[] defs = net.getNodeDefinition(h);
        int len = defs.length;
        if(defs[len-2]==1.0 && defs[len-1]==0.0) {
            for(int i=0; i<len-2;i++) {
                if(i%2==0) {
                    if(defs[i]!=0.0)
                        return false;
                }
                else
                if(defs[i]!=1.0)
                    return false;
            }
        }
        else
            return false;

        return true;
    }



    private boolean checkOR(int h) {
        if(net.getOutcomeCount(h)>2)
            return false;
        double[] defs = net.getNodeDefinition(h);
        if(defs[0]==0.0 && defs[1]==1.0) {
            for(int i=2; i<defs.length;i++) {
                if(i%2==0) {
                    if(defs[i]!=1.0)
                        return false;
                }
                else
                if(defs[i]!=0.0)
                    return false;
            }
        }
        else
            return false;

        return true;


    }

    private void printBooleanCpdAND (int nodeHandle) {
        code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 1 \n");
        printParentOrder(nodeHandle);
        code.append("bnet.CPD{bnet.names('"+net.getNodeId(nodeHandle)+"')}=boolean_CPD(bnet,bnet.names('"+net.getNodeId(nodeHandle)+"'),'named',");
        code.append("'all');\n");
        code.append("clear cpt;\n\n");
    }

    private void printBooleanCpdOR (int nodeHandle) {
        code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 1 \n");
        printParentOrder(nodeHandle);
        code.append("bnet.CPD{bnet.names('"+net.getNodeId(nodeHandle)+"')}=boolean_CPD(bnet,bnet.names('"+net.getNodeId(nodeHandle)+"'),'named',");
        code.append("'any');\n");
        code.append("clear cpt;\n\n");
    }


    private boolean checkNoisyOr(int nodeHandle) {
        if(net.getNodeType(nodeHandle) != Network.NodeType.NOISY_MAX)
            return false;
        int parents[] = net.getParents(nodeHandle);
        for(int p : parents)
            if(net.getOutcomeCount(p)>2)
                return false;
        if(net.getOutcomeCount(nodeHandle)>2)
            return false;

        return true;
    }

    private void printNoisyOr(int nodeHandle) {
		/*
		code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 1 \n");
		printParentOrder(nodeHandle);
		code.append("leak=");
		double[] defs = net.getNodeDefinition(nodeHandle);
		code.append(defs[defs.length-2]+";\n");
		code.append("parents_dn={");
		for(int p : net.getParents(nodeHandle))
			code.append("'"+net.getNodeId(p)+"'"+", ");
		truncList(code, 2);
		code.append("};\n");
		code.append("inh_prob=[");
		for(int d =1; d<defs.length-2;d+=4)
			code.append((defs[d])+", ");
		truncList(code, 2);
		code.append("];\n");
		code.append("inh_prob1=mk_named_noisyor(bnet.names('"+net.getNodeId(nodeHandle)+"'),parents_dn,names,bnet.dag,inh_prob);\n");
		code.append("bnet.CPD{bnet.names('"+net.getNodeId(nodeHandle)+"')}=noisyor_CPD(bnet, bnet.names('"+net.getNodeId(nodeHandle)+"'),leak, inh_prob1);\n");
		code.append("clear inh_prob inh_prob1 leak;\n\n");
		*/
        noisyOr = true;
        printNoisyMax(nodeHandle);
    }

    private void printNoisyMax(int nodeHandle) {

        code.append("%node "+net.getNodeName(nodeHandle)+"(id="+ net.getNodeId(nodeHandle)+")"+" slice 1 \n");
        printParentOrder(nodeHandle);
        double[] cpt = net.getNoisyExpandedDefinition(nodeHandle);
        int[] parents = net.getParents(nodeHandle);
        int[] pIndex = new int[parents.length];
        int[] coords = new int[parents.length];

        int totCptColumn = cpt.length/net.getOutcomeCount(nodeHandle);
        for(int i=0; i<totCptColumn;i++) {
            code.append("cpt(");
            if(parents.length==0)
                code.append(":,");
            int prod = 1;
            for(int j=parents.length-1;j>=0;j--) {
                coords[j]=(((pIndex[j]++/prod)%net.getOutcomeCount(parents[j])));
                prod*=net.getOutcomeCount(parents[j]);

            }
            for(int k =0; k<parents.length;k++)
                code.append(coords[k]+1 +",");

            code.append(":)=[");
            for(int w =0; w<net.getOutcomeCount(nodeHandle);w++)
                code.append(cpt[i*net.getOutcomeCount(nodeHandle)+w]+", ");
            truncList(code, 2);
            code.append("];\n");
        }

        code.append("bnet.CPD{bnet.names('"+net.getNodeId(nodeHandle)+"')}=tabular_CPD(bnet,bnet.names('"+net.getNodeId(nodeHandle)+"'),'CPT',cpt);\n");
        code.append("clear cpt;\n\n");

    }



    private void printTempNoisyMax(int nodeHandle) {

        net.setNodeType(nodeHandle, Network.NodeType.CPT);
        printTempTabularCpd(nodeHandle);
        net.setNodeType(nodeHandle, Network.NodeType.NOISY_MAX);

    }

    public void saveFile(String fileName) throws IOException {
        StringBuilder scriptName = new StringBuilder();
        scriptName.append(fileName);
        scriptName.append(".m");
        BufferedWriter writer;
        writer = new BufferedWriter(new FileWriter(new File("scripts",scriptName.toString())));
        writer.write(code.toString());
        writer.close();

    }

    private void setEvidence(String fileName, String caseName) throws ParserConfigurationException, SAXException, IOException {

        File inputFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        NodeList caseList = doc.getElementsByTagName("case");

        for (int i = 0; i < caseList.getLength(); i++) {
            Node caseNode = caseList.item(i);

            if (caseNode.getNodeType() == Node.ELEMENT_NODE) {
                Element caseElement = (Element) caseNode;

                if(caseElement.getAttribute("name").equals(caseName)) {

                    NodeList evidenceList = caseElement.getChildNodes();
                    for(int j = 0; j < evidenceList.getLength(); j++) {
                        Node evidenceNode = evidenceList.item(j);
                        if (evidenceNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element evidenceElement = (Element) evidenceNode;
                            if(evidenceNode.getNodeName().contentEquals("evidence"))
                                net.setTemporalEvidence(evidenceElement.getAttribute("node"), Integer.valueOf(evidenceElement.getAttribute("slice")), evidenceElement.getAttribute("state"));
                        }

                    }

                }

            }

        }

    }

    public static String[] retrieveCases (String fileName) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<String> casesArrList = new ArrayList<String>();
        File inputFile = new File(fileName);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        NodeList caseList = doc.getElementsByTagName("case");

        for (int i = 0; i < caseList.getLength(); i++) {
            Node caseNode = caseList.item(i);

            if (caseNode.getNodeType() == Node.ELEMENT_NODE) {
                Element caseElement = (Element) caseNode;
                casesArrList.add(caseElement.getAttribute("name"));

            }

        }
        String[] cases = new String[casesArrList.size()];
        return casesArrList.toArray(cases);
    }

    private void printEvidence(){

        ArrayList<TemporalEvidence> evidences = new ArrayList<TemporalEvidence>();
        for(int t=0;t<net.getSliceCount();t++) {
            for (int h = net.getFirstNode(); h >= 0; h = net.getNextNode(h)) {
                if(net.hasTemporalEvidence(h)) {
                    if(net.isTemporalEvidence(h, t))
                        evidences.add(new TemporalEvidence(t, net.getNodeId(h), net.getTemporalEvidence(h, t)+1));
                }
            }
        }

        boolean slicePrinted = false;
        int slice=0;
        for(TemporalEvidence e : evidences) {
            if(e.timeSlice>slice)
                slicePrinted=false;
            if(!slicePrinted) {
                slice = e.timeSlice;
                slicePrinted = true;
                code.append("t="+slice+";\n");
            }
            code.append("evidence{bnet.names('"+e.name+"'),t+1}="+e.state+"; \n");
        }
    }

    public void printInference(String fileName, String caseName, String inferenceEngine, boolean fullyFactorized, int timeSpan, int timeStep, boolean filtering) throws ParserConfigurationException, SAXException, IOException {

        code.append("% choose the inference engine\n" +
                "ec='"+ inferenceEngine +"';\n" +
                "\n" +
                "% ff=0 --> no fully factorized  OR ff=1 --> fully factorized\n" +
                "ff="+((fullyFactorized==true)?"1":"0")+";\n" +
                "\n" +
                "% list of clusters\n" +
                "if (ec=='JT')\n" +
                "	engine=bk_inf_engine(bnet, 'clusters', 'exact'); %exact inference\n" +
                "else\n" +
                "	if (ff==1)\n" +
                "		engine=bk_inf_engine(bnet, 'clusters', 'ff'); % fully factorized\n" +
                "	else\n" +
                "		clusters={[]};\n" +
                "		engine=bk_inf_engine(bnet, 'clusters', clusters);\n" +
                "	end\n" +
                "end\n" +
                "\n" +
                "% IMPORTANT: GeNIe start slices from 0,\n" +
                "T="+timeSpan+"; %max time span thus from 0 to T-1\n" +
                "tStep="+timeStep+"; %Time Step\n" +
                "evidence=cell(n,T); % create the evidence cell array\n" +
                "\n" +
                "% Evidence\n" +
                "% first cells of evidence are for time 0\n");

        setEvidence(fileName, caseName);

        printEvidence();

        code.append("% Inference algorithm (filtering / smoothing)\n" +
                "filtering="+((filtering==true)?"1":"0")+";\n" +
                "% filtering=0 --> smoothing (is the default - enter_evidence(engine,evidence))\n" +
                "% filtering=1 --> filtering\n" +
                "if ~filtering\n" +
                "	fprintf('\\n*****  SMOOTHING *****\\n\\n');\n" +
                "else\n" +
                "	fprintf('\\n*****  FILTERING *****\\n\\n');\n" +
                "end\n" +
                "\n" +
                "[engine, loglik] = enter_evidence(engine, evidence, 'filter', filtering);\n" +
                "\n" +
                "% analysis time is t for anterior nodes and t+1 for ulterior nodes\n" +
                "for t=1:tStep:T-1\n" +
                "%t = analysis time\n" +
                "\n" +
                "% create the vector of marginals\n" +
                "% marg(i).T is the posterior distribution of node T\n" +
                "% with marg(i).T(false) and marg(i).T(true)\n" +
                "\n" +
                "% NB. if filtering then ulterior nodes cannot be marginalized at time t=1\n" +
                "\n" +
                "if ~filtering\n" +
                "	for i=1:(n*2)\n" +
                "		marg(i)=marginal_nodes(engine, i , t);\n" +
                "	end\n" +
                "else\n" +
                "	if t==1\n" +
                "		for i=1:n\n" +
                "			marg(i)=marginal_nodes(engine, i, t);\n" +
                "		end\n" +
                "	else\n" +
                "		for i=1:(n*2)\n" +
                "			marg(i)=marginal_nodes(engine, i, t);\n" +
                "		end\n" +
                "	end\n" +
                "end\n" +
                "\n" +
                "% Printing results\n" +
                "% IMPORTANT: To be consistent with GeNIe we start counting/printing time slices from 0\n" +
                "\n" +
                "\n" +
                "% Anterior nodes are printed from t=1 to T-1\n" +
                "fprintf('\\n\\n**** Time %i *****\\n****\\n\\n',t-1);\n" +
                "%fprintf('*** Anterior nodes \\n');\n" +
                "for i=1:n\n" +
                "	if isempty(evidence{i,t})\n" +
                "		for k=1:ns(i)\n" +
                "			fprintf('Posterior of node %i:%s value %i : %d\\n',i, names{i}, k, marg(i).T(k));\n" +
                "		end\n" +
                "			fprintf('**\\n');\n" +
                "		else\n" +
                "			fprintf('Node %i:%s observed at value: %i\\n**\\n',i,names{i}, evidence{i,t});\n" +
                "		end\n" +
                "	end\n" +
                "end\n" +
                "\n" +
                "% Ulterior nodes are printed at last time slice\n" +
                "fprintf('\\n\\n**** Time %i *****\\n****\\n\\n',T-1);\n" +
                "%fprintf('*** Ulterior nodes \\n');\n" +
                "for i=(n+1):(n*2)\n" +
                "	if isempty(evidence{i-n,T})\n" +
                "		for k=1:ns(i-n)\n" +
                "			fprintf('Posterior of node %i:%s value %i : %d\\n',i, names{i-n}, k, marg(i).T(k));\n" +
                "		end\n" +
                "		fprintf('**\\n');\n" +
                "	else\n" +
                "		fprintf('Node %i:%s observed at value: %i\\n**\\n',i,names{i-n}, evidence{i-n,T});\n" +
                "	end\n" +
                "end");
    }

    private void myCpdPrint(int nodeHandle) {
        double[] cpt = net.getNodeDefinition(nodeHandle);
        int[] parents = net.getParents(nodeHandle);
        int[] pIndex = new int[parents.length];
        int[] coords = new int[parents.length];

        int totCptColumn = cpt.length/net.getOutcomeCount(nodeHandle);
        for(int i=0; i<totCptColumn;i++) {
            code.append("cpt(");
            if(parents.length==0)
                code.append(":,");
            int prod = 1;
            for(int j=parents.length-1;j>=0;j--) {
                coords[j]=(((pIndex[j]++/prod)%net.getOutcomeCount(parents[j])));
                prod*=net.getOutcomeCount(parents[j]);

            }
            for(int k =0; k<parents.length;k++)
                code.append(coords[k]+1 +",");

            code.append(":)=[");
            for(int w =0; w<net.getOutcomeCount(nodeHandle);w++)
                code.append(cpt[i*net.getOutcomeCount(nodeHandle)+w]+", ");
            truncList(code, 2);
            code.append("];\n");
        }

    }
    private void myTempCpdPrint(int nodeHandle) {
        double[] cpt = net.getNodeTemporalDefinition(nodeHandle, 1);
        TemporalInfo[] tParents = net.getTemporalParents(nodeHandle, 1);


        int[] nParents = net.getParents(nodeHandle);
        int[] parents = new int[nParents.length+tParents.length];
        int p = 0;
        for(int z=0;z<nParents.length;z++,p++)
            parents[p]=nParents[z];
        for(int z=0;z<tParents.length;z++,p++)
            parents[p]=tParents[z].handle;


        int[] pIndex = new int[parents.length];
        int[] coords = new int[parents.length];

        int totCptColumn = cpt.length/net.getOutcomeCount(nodeHandle);
        for(int i=0; i<totCptColumn;i++) {
            code.append("cpt(");
            if(parents.length==0)
                code.append(":,");
            int prod = 1;
            for(int j=parents.length-1;j>=0;j--) {
                coords[j]=(((pIndex[j]++/prod)%net.getOutcomeCount(parents[j])));
                prod*=net.getOutcomeCount(parents[j]);

            }
            for(int k =0; k<parents.length;k++)
                code.append(coords[k]+1 +",");

            code.append(":)=[");
            for(int w =0; w<net.getOutcomeCount(nodeHandle);w++)
                code.append(cpt[i*net.getOutcomeCount(nodeHandle)+w]+", ");
            truncList(code, 2);
            code.append("];\n");
        }

    }

    private void cpt1Print(int nodeHandle){

        int[] parents = net.getParents(nodeHandle);
        code.append("cpt1=mk_named_CPT({");
        for(int p : parents)
            code.append("'"+net.getNodeId(p)+"', ");

        code.append("'"+net.getNodeId(nodeHandle)+"'");
        code.append("},names, bnet.dag, cpt);\n");
    }

    private void tempCpt1Print(int nodeHandle){
        int[] parents = net.getParents(nodeHandle);
        TemporalInfo[] tParents = net.getTemporalParents(nodeHandle, 1);
        ArrayList<Integer> parentsList = new ArrayList<Integer>();
        ArrayList<Integer> tparentsList = new ArrayList<Integer>();
        ArrayList<Integer> family = new ArrayList<Integer>();

        code.append("cpt1=mk_named_CPT_inter({");

        for(int p : parents) {
            code.append("'"+net.getNodeId(p)+"', ");
            parentsList.add(p);
            family.add(p);
        }

        for(TemporalInfo t : tParents) {
            code.append("'"+net.getNodeId(t.handle)+"', ");
            tparentsList.add(t.handle);
            family.add(t.handle);
        }

        code.append("'"+net.getNodeId(nodeHandle)+"'");
        family.add(nodeHandle);

        StringBuilder idx = new StringBuilder();
        code.append("},names, bnet.dag, cpt,[");

        for(Integer p:parentsList)
            idx.append(family.lastIndexOf(p)+1+",");

        if(idx.length()>0)
            truncList(idx, 1);

        code.append(idx.toString());
        code.append("]);\n");

    }

    private void printParentOrder(int nodeHandle) {

        code.append("%parent order:{");

        int[] parents = net.getParents(nodeHandle);
        for(int p: parents) {
            code.append(net.getNodeId(p));
            code.append(", ");
        }

        if(parents.length!=0) {
            truncList(code, 2);
        }

        code.append("}\n");
    }

    private void printTempParentOrder(int nodeHandle) {
        code.append("%parent order:{");
        boolean noParents = true;

        int[] parents = net.getParents(nodeHandle);
        for(int p: parents) {
            code.append(net.getNodeId(p));
            code.append(", ");
            noParents = false;
        }

        for (int p : net.getAllNodes()) {
            if(net.temporalArcExists(p, nodeHandle, 1)) {
                code.append(net.getNodeId(p));
                code.append(", ");
                noParents = false;
            }
        }

        if(noParents==false) {
            truncList(code, 2);
        }

        code.append("}\n");
    }


    private void truncList (StringBuilder str, int delChar) {
        str.setLength(str.length()-delChar);
    }


    private static class NotHMMException extends Exception{
        private static final long serialVersionUID = 1L;
        public NotHMMException(String message) {
            super(message);
        }
    }

    private static class TemporalEvidence {

        private int timeSlice;
        private String name;
        private int state;

        public TemporalEvidence(int timeSlice,String name,int state) {
            this.timeSlice=timeSlice;
            this.name=name;
            this.state=state;
        }
    }

    public static void pathInit() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        System.setProperty("java.library.path", "./libs" );
        Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
        fieldSysPath.setAccessible( true );
        fieldSysPath.set( null, null );
    }


    public static License licenseInit() throws IOException {
        InputStream is = new FileInputStream("license/License.java");
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        int skip = 0;
        while(line != null){
            if(skip++>2)
                sb.append(line).append("\n");
            line = buf.readLine();
        }
        buf.close();
        String fileAsString = sb.toString();

        String[] splitStr = fileAsString.split(",\\n\\tnew byte\\[\\] ");
        splitStr[0] = splitStr[0].replaceAll("\t", "");
        splitStr[0] = splitStr[0].replaceAll("\" \\+", "");
        splitStr[0] = splitStr[0].replaceAll("\"", "");
        splitStr[0] = splitStr[0].replaceAll("\n", "");
        splitStr[0] = StringEscapeUtils.unescapeJava(splitStr[0]);

        splitStr[1] = splitStr[1].replaceAll("\\);","");
        splitStr[1] = splitStr[1].replaceAll("\t","");
        splitStr[1] = splitStr[1].replaceAll("\n","");
        splitStr[1] = splitStr[1].substring(1, splitStr[1].length()-1);
        String[] bytesStr = splitStr[1].split(",");
        byte[] bytes = new byte[bytesStr.length];
        for(int i=0;i<bytesStr.length;i++)
            bytes[i] = Byte.valueOf(bytesStr[i]);

        return new License(splitStr[0],bytes);
    }



    public String extractFileName (String filePath) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] dirs;
        if(os.indexOf("win") >= 0)
            dirs = filePath.split("\\\\");
        else
            dirs = filePath.split("/");
        StringBuilder fileName = new StringBuilder(dirs[dirs.length-1]);
        fileName.setLength(fileName.length()-5);
        return fileName.toString();
    }

    public static void checkExtension(String filePath) throws Exception {
        if(filePath.length()<=5)
            throw new Exception("Formato file non supportato");
        if(!filePath.substring(filePath.length()-5, filePath.length()).equals(".xdsl"))
            throw new Exception("Formato file non supportato");
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Network getNet() {
        return net;
    }

    public StringBuilder getCode() {
        return code;
    }

    public boolean isHmm() {
        return hmm;
    }

    public String getHmmError() {
        return hmmError;
    }

    public boolean isNoisyOr() {
        return noisyOr;
    }

}
