/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgcc.eseco.provse.service.app;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Classe responsável pelo tratamento das informações recebidas via serviço ou
 * via banco de dados para incluí-las na ontologia
 *
 * @author Lenita
 */
public class DataLoader {

    private final OntologyController controller;

    private static Map<String, DatatypeProperty> datatypeProperties;
    private static Map<String, ObjectProperty> objectProperties;
    private static Map<String, OntClass> ontClasses;
    private static OntModel ontModel;

    public DataLoader() {
        controller = OntologyController.getInstance();
    }

    /**
     * Classe para a inclusão de Triplas RDF na ontologia. Permite a inclusão de
     * novos indivíduos, propriedades ou relacionamentos.
     *
     * @param subject
     * @param predicate
     * @param object
     * @return
     */
    public boolean addTriple(String subject, String predicate, String object) {
        Resource sbj = controller.getInfModel().getResource(subject);
        if (sbj == null) {
            sbj = controller.getInfModel().createResource(subject);
        }

        Property prop = controller.getInfModel().getProperty(predicate);
        if (prop == null) {
            prop = ResourceFactory.createProperty(predicate);
        }

        Resource obj = controller.getInfModel().getResource(object);
        if (obj != null) {
            sbj.addProperty(prop, obj);
            return true;
        }
        return false;
    }

    private static void prepare() {

        System.out.println("Preparing ontologies.");

        datatypeProperties = new HashMap<>();
        objectProperties = new HashMap<>();
        ontClasses = new HashMap<>();

        ClassLoader loader = DataLoader.class.getClassLoader();
        Model model = ModelFactory.createDefaultModel();

        model.read(loader.getResource(OntologyController.PROV_ONTOLOGY).getPath());
        model.read(loader.getResource(OntologyController.PROVONE_ONTOLOGY).getPath());
        model.read(loader.getResource(OntologyController.ONTOLOGY).getPath());

        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ontModel.prepare();

        ExtendedIterator<DatatypeProperty> listDatatypeProperties = ontModel.listDatatypeProperties();
        for (Iterator iterator = listDatatypeProperties; iterator.hasNext();) {
            DatatypeProperty next = (DatatypeProperty) iterator.next();
            datatypeProperties.put(next.getLocalName().toLowerCase(), next);
        }

        ExtendedIterator<ObjectProperty> listObjectProperties = ontModel.listObjectProperties();
        for (Iterator iterator = listObjectProperties; iterator.hasNext();) {
            ObjectProperty next = (ObjectProperty) iterator.next();
            objectProperties.put(next.getLocalName().toLowerCase(), next);
        }

        ExtendedIterator<OntClass> listClasses = ontModel.listClasses();
        for (Iterator iterator = listClasses; iterator.hasNext();) {
            OntClass next = (OntClass) iterator.next();
            if (next.getLocalName() != null) {
                ontClasses.put(next.getLocalName().toLowerCase(), next);
            }
        }

        model = ModelFactory.createDefaultModel();
        model.read(loader.getResource(OntologyController.ONTOLOGY).getPath());

        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
        ontModel.prepare();
    }

    private static void saveNewOntology() {

        System.out.println("Validating the loadOntology. ");

        //Gerar o novo arquivo com os dados do banco na nova ontologia
        Writer fstream = null;
        BufferedWriter out = null;

        try {
            //caminho para o novo arquivo de ontologia
            File f = new File(OntologyController.ONTOLOGY_LOAD);
            System.out.println("Exist = " + f.exists() + " diretory = " + !f.isDirectory());
            if (f.exists() && !f.isDirectory()) {
                f.delete();
            }
            //se não existir arquivo, o mesmo será criado, se não, será reescrito
            fstream = new OutputStreamWriter(new FileOutputStream(OntologyController.ONTOLOGY_LOAD), StandardCharsets.UTF_8);

            //determinando que o fluxo de saida vai para o arquivo e não para a tela            
            out = new BufferedWriter(fstream);

            //ontologia carregada
            // ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, ontModel);
            //utilizar RDF/XML-ABBREV, so RDF/XML da erro no protege!        
            ontModel.write(out);
            fstream.flush();
            out.flush();
            fstream.close();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static void loadDAO(JsonElement data) {

        prepare();

        System.out.println("Loading data from database.");

        JsonObject asJsonObject = data.getAsJsonObject();

        JsonObject agentAsJsonObject = asJsonObject.getAsJsonObject("agent");

        JsonArray researchGroupAsJsonObject = agentAsJsonObject.getAsJsonArray("researchGroup");
        loadInstancesAndDataProperties(researchGroupAsJsonObject, "ResearchGroup", "ResearchGroup");

        JsonArray institutionAsJsonObject = agentAsJsonObject.getAsJsonArray("institution");
        loadInstancesAndDataProperties(institutionAsJsonObject, "Institution", "Institution");

        JsonArray researcherAsJsonObject = agentAsJsonObject.getAsJsonArray("researcher");
        loadInstancesAndDataProperties(researcherAsJsonObject, "Researcher", "Researcher");

        JsonArray developerAsJsonObject = agentAsJsonObject.getAsJsonArray("developer");
        loadInstancesAndDataProperties(developerAsJsonObject, "Developer", "Developer");

        JsonArray serviceAsJsonObject = agentAsJsonObject.getAsJsonArray("service");
        loadInstancesAndDataProperties(serviceAsJsonObject, "Service", "Service");

        JsonArray wfmsAsJsonObject = agentAsJsonObject.getAsJsonArray("wfms");
        loadInstancesAndDataProperties(wfmsAsJsonObject, "Wfms", "Wfms");

        asJsonObject.remove("agent");

        JsonObject entityAsJsonObject = asJsonObject.getAsJsonObject("entity");

        JsonArray experimentAsJsonObject = entityAsJsonObject.getAsJsonArray("experiment");
        loadInstancesAndDataProperties(experimentAsJsonObject, "Experiment", "Experiment");

        JsonArray workflowAsJsonObject = entityAsJsonObject.getAsJsonArray("workflow");
        loadInstancesAndDataProperties(workflowAsJsonObject, "Workflow", "Workflow");

        JsonArray programAsJsonObject = entityAsJsonObject.getAsJsonArray("program");
        loadInstancesAndDataProperties(programAsJsonObject, "Program", "Program");

        JsonArray dataAsJsonObject = entityAsJsonObject.getAsJsonArray("data");
        loadInstancesAndDataProperties(dataAsJsonObject, "Data", "Data");

        JsonArray documentAsJsonObject = entityAsJsonObject.getAsJsonArray("document");
        loadInstancesAndDataProperties(documentAsJsonObject, "Document", "Document");

        JsonArray portAsJsonObject = entityAsJsonObject.getAsJsonArray("port");
        loadInstancesAndDataProperties(portAsJsonObject, "Port", "Port");

        asJsonObject.remove("entity");

        JsonObject activityAsJsonObject = asJsonObject.getAsJsonObject("activity");

        JsonArray workflowExecutionAsJsonObject = activityAsJsonObject.getAsJsonArray("workflowExecution");
        loadInstancesAndDataProperties(workflowExecutionAsJsonObject, "Execution", "WorkflowExecution");

        JsonArray activityExecutionAsJsonObject = activityAsJsonObject.getAsJsonArray("activityExecution");
        loadInstancesAndDataProperties(activityExecutionAsJsonObject, "Execution", "ActivityExecution");

        asJsonObject.remove("activity");

        JsonObject locationAsJsonObject = asJsonObject.getAsJsonObject("location");

        JsonArray cityExecutionAsJsonObject = locationAsJsonObject.getAsJsonArray("city");
        loadInstancesAndDataProperties(cityExecutionAsJsonObject, "City", "City");

        asJsonObject.remove("location");

        for (Map.Entry<String, JsonElement> entry : asJsonObject.entrySet()) {
            loadObjectProperties(entry.getValue().getAsJsonArray(), entry.getKey());
        }
        saveNewOntology();
    }

    private static void loadInstancesAndDataProperties(JsonArray object, String className, String objectName) {
        if (object == null) {
            return;
        }
        for (Iterator iterator = object.iterator(); iterator.hasNext();) {
            JsonObject next = (JsonObject) iterator.next();
            if (next != null) {
                int idValue = next.get("id").getAsInt();
                Individual individual = ontModel.createIndividual(OntologyController.URI + objectName.toLowerCase() + "." + idValue, ontClasses.get(className.toLowerCase()));
                for (Map.Entry<String, JsonElement> en1 : next.entrySet()) {
                    String key = en1.getKey();
                    JsonElement value = en1.getValue();
                    if (value == null || value.isJsonNull()) {
                        continue;
                    }
                    Literal l;

                    if (value.isJsonPrimitive()) {
                        JsonPrimitive jsonPrimitive = value.getAsJsonPrimitive();
                        if (jsonPrimitive.isNumber()) {

                            int i = jsonPrimitive.getAsInt();
                            l = ontModel.createTypedLiteral(i);

                        } else if (jsonPrimitive.isBoolean()) {

                            boolean b = jsonPrimitive.getAsBoolean();
                            l = ontModel.createTypedLiteral(b);

                        } else if (jsonPrimitive.isString()) {

                            String s = jsonPrimitive.getAsString();
                            try {
                                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                                Date date = inputFormat.parse(s);
                                Calendar c = Calendar.getInstance();
                                c.setTime(date);
                                l = ontModel.createTypedLiteral(new XSDDateTime(c));

                            } catch (Exception ex) {
                                try {
                                    DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                                    Date date = inputFormat.parse(s);
                                    Calendar c = Calendar.getInstance();
                                    c.setTime(date);
                                    l = ontModel.createTypedLiteral(new XSDDateTime(c));
                                } catch (Exception e) {
                                    l = ontModel.createTypedLiteral(value.getAsString());
                                }
                            }

                        } else {
                            continue;
                        }
                    } else if (value.isJsonArray()) {
                        JsonArray asJsonArray = value.getAsJsonArray();
                        for (JsonElement jsonElement : asJsonArray) {
                            l = ontModel.createTypedLiteral(jsonElement.getAsString());
                            individual.addProperty(datatypeProperties.get(key.toLowerCase()), l);
                        }
                        continue;
                    } else {
                        continue;
                    }
                    individual.addProperty(datatypeProperties.get(key.toLowerCase()), l);

                }
            }
        }
    }

    private static void loadObjectProperties(JsonArray object, String propertyName) {
        if (object == null) {
            return;
        }
        for (Iterator iterator = object.iterator(); iterator.hasNext();) {
            JsonObject next = (JsonObject) iterator.next();
            Set<Map.Entry<String, JsonElement>> entrySet = next.entrySet();
            Iterator<Map.Entry<String, JsonElement>> it = entrySet.iterator();
            Map.Entry<String, JsonElement> individual1 = it.next();
            Map.Entry<String, JsonElement> individual2 = it.next();

            ObjectProperty property = objectProperties.get(propertyName.toLowerCase());
            if (property == null) {
                if (propertyName.toLowerCase().endsWith("relation")) {
                    property = objectProperties.get(propertyName.toLowerCase().replace("relation", ""));
                }
            }

            Individual i1 = ontModel.getIndividual(OntologyController.URI + individual1.getKey().toLowerCase() + "." + individual1.getValue().getAsString());
            Individual i2 = ontModel.getIndividual(OntologyController.URI + individual2.getKey().toLowerCase() + "." + individual2.getValue().getAsString());
            if (i1 == null) {
                i1 = ontModel.createIndividual(OntologyController.URI + individual1.getKey().toLowerCase() + "." + individual1.getValue().getAsString().toLowerCase(), ontClasses.get(individual1.getKey().toLowerCase()));
            }
            if (i2 == null) {
                i2 = ontModel.createIndividual(OntologyController.URI + individual2.getKey().toLowerCase() + "." + individual2.getValue().getAsString().toLowerCase(), ontClasses.get(individual2.getKey().toLowerCase()));
            }
            if (i1 == null || i2 == null || property == null) {
                continue;
            }

            i1.addProperty(property, i2);
        }
    }
}
