/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgcc.eseco.provse.service.app;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import java.io.File;
import java.nio.file.Paths;
import org.mindswap.pellet.jena.PelletReasonerFactory;

/**
 * Classe responsável por controlar o acesso à Ontologia
 *
 * @author Lenita
 */
public class OntologyController {

    //variavel para acesso à ontologia com inferencia
    private final InfModel infModel;

    //variavel para acesso à ontologia sem inferencia
    private final OntModel ontModel;

    //uri da ontologia
    public static final String URI = "http://www.semanticweb.org/lenita/ontologies/2016/10/prov-se-o#";

    //uri das ontologias importadas
    public static final String PROV_URI = "http://www.w3.org/ns/prov#";
    public static final String PROVONE_URI = "http://purl.dataone.org/provone/2015/01/15/ontology#";

    //caminho para a ontologia
    public static final String ONTOLOGY = "ontologies/prov-se-o-clean.owl";

    //caminho para a ontologia carregada
    public static final String ONTOLOGY_LOAD = System.getProperty("java.io.tmpdir") + File.separator + "prov-se-o-load.owl";

    //caminho para as ontologias importadas
    public static final String PROV_ONTOLOGY = "ontologies/prov-o.owl";
    public static final String PROVONE_ONTOLOGY = "ontologies/provone.owl";

    private static OntologyController instance;

    public static OntologyController getInstance() {
        if (instance == null) {
            instance = new OntologyController();
        }
        return instance;
    }

    public static void refresh() {
        instance = new OntologyController();
    }

    private OntologyController() {

        Model model = ModelFactory.createDefaultModel();

        model.read(PROV_ONTOLOGY);
        model.read(PROVONE_ONTOLOGY);
        model.read(Paths.get(ONTOLOGY_LOAD).toUri().toString());

        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ontModel.loadImports();
        ontModel.prepare();

        Reasoner reasoner = PelletReasonerFactory.theInstance().create();
        infModel = ModelFactory.createInfModel(reasoner, ontModel);
        infModel.prepare();
    }

    public InfModel getInfModel() {
        return infModel;
    }

    public OntModel getOntModel() {
        return ontModel;
    }

//    public static void main(String[] args) {
//        //colocar public static final String ONTOLOGY = "/files/ontologies/prov-se-o.owl";
//        // e Paths.get(OntologyController.ONTOLOGY).toUri().toString() no read
//        InferenceLayer inferenceLayer = new InferenceLayer();
//        List<String> jenaGetIndividualsByClass = inferenceLayer.jenaGetIndividualsByClass(URI, "Researcher");
//
//        for (String string : jenaGetIndividualsByClass) {
//            System.out.println(string);
//        }
//        System.out.println("OK \\o/ \\o/");
//    }
}
