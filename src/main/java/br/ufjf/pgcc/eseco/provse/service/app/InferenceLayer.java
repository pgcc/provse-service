/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgcc.eseco.provse.service.app;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Classe responsável por executar as consultas com inferencia na Ontologia
 *
 * @author Lenita
 */
public class InferenceLayer {

    private final OntologyController controller;

    public InferenceLayer() {
        controller = OntologyController.getInstance();
    }

    public List<String> jenaGetIndividualsByClass(String prefix, String className) {

        OntClass oc = controller.getOntModel().getOntClass(getUri(prefix) + className);
        List<String> listIndividuals = new ArrayList<>();
        if (oc != null) {
            for (Iterator i = oc.listInstances(); i.hasNext();) {
                listIndividuals.add(i.next().toString());
            }
        }
        return listIndividuals;
    }

    public List<String> sparqlGetIndividualsByClassInf(String prefix, String entidade) {
        String queryStr = "PREFIX : <" + getUri(prefix) + "> \n"
                + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                + "SELECT DISTINCT ?" + entidade + "\n"
                + "WHERE { \n"
                + " ?" + entidade + " rdf:type :" + entidade + " . \n"
                + " }  \n";

        Query query = QueryFactory.create(queryStr);
        QueryExecution execution = QueryExecutionFactory.create(query, controller.getInfModel());
        ResultSet results = execution.execSelect();
        List<String> list = new ArrayList<>();
        while (results.hasNext()) {
            QuerySolution qs = results.next();
            Resource resource = qs.getResource("?" + entidade + "");
            if (resource.toString() != null && !resource.toString().equals("null")) {
                list.add(resource.toString());
            }
        }
        execution.close();

        return list;
    }

    public List<String> sparqlGetPropertiesByIndividualInf(String entidade) {
        String queryStr = "SELECT DISTINCT * \n"
                + "WHERE {<"
                + OntologyController.URI
                + entidade
                + "> ?predicate ?object} ";
        Query query = QueryFactory.create(queryStr);
        QueryExecution execution = QueryExecutionFactory.create(query, controller.getInfModel());
        ResultSet results = execution.execSelect();
        List<String> list = new ArrayList<>();
        try {
            while (results.hasNext()) {
                QuerySolution qs = results.next();
                Resource predicate = qs.getResource("?predicate");
                String p = predicate.toString();

                String r = "";
                try {
                    Resource resource = qs.getResource("?object");
                    r = resource.toString();
                } catch (ClassCastException e) {
                    Literal literal = qs.getLiteral("?object");
                    r = literal.toString();
                }

                if (p.contains("#")) {
                    try {
                        p = p.split("#")[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        p = "";
                    }
                }

                if (r.contains("#")) {
                    try {
                        r = r.split("#")[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        r = "";
                    }
                }

                if (!r.equals("") && predicate.toString() != null && !predicate.toString().equals("null")) {
                    list.add(p + " => " + r);
                }
            }
        } catch (AbstractMethodError ex) {
            ex.printStackTrace();
        }
        execution.close();

        return list;
    }

    public List<String> sparqlGetResult(String queryStr) {
        if (queryStr == null || queryStr == "") {
            queryStr = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                    + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                    + "SELECT ?subject ?object\n"
                    + "	WHERE { ?subject rdfs:subClassOf ?object }";
        }
        Query query = QueryFactory.create(queryStr);
        QueryExecution execution = QueryExecutionFactory.create(query, controller.getInfModel());
        ResultSet results = execution.execSelect();
        List<String> list = new ArrayList<>();
        try {
            while (results.hasNext()) {
                QuerySolution qs = results.next();
                list.add(qs.toString());
            }
        } catch (AbstractMethodError ex) {
            ex.printStackTrace();
        }
        execution.close();

        return list;
    }

    public List<String> jenaGetPropertiesByClass(String prefix, String classe) {

        OntClass oc = controller.getOntModel().getOntClass(getUri(prefix) + classe);
        List<String> listProperties = new ArrayList<>();
        if (oc != null) {
            for (Iterator i = oc.listDeclaredProperties(); i.hasNext();) {
                String property = i.next().toString();
                listProperties.add(property);
            }
        }
        return listProperties;
    }

    public List<String> jenaGetOPAssertionsByIndividualInf(String individualName, String opPrefix, String objectPropertyName) {
        OntProperty objectProperty;
        objectProperty = controller.getOntModel().getObjectProperty(getUri(opPrefix) + objectPropertyName);
        Resource resource = controller.getInfModel().getResource(OntologyController.URI + individualName);
        List<String> listProperties = new ArrayList<>();
        if (resource != null && objectProperty != null) {
            for (StmtIterator i = resource.listProperties(objectProperty); i.hasNext();) {
                String str = i.next().getResource().toString();
                listProperties.add(str);
            }
        }
        return listProperties;
    }

    public JsonObject jenaGetInferredDataByIndividual(String individualName) {
        DatatypeProperty dpName = controller.getOntModel().getDatatypeProperty(OntologyController.URI + "name");
        individualName = individualName.replace("activity.", "program.");
        JsonObject jsonProperties = new JsonObject();
        Individual individual = controller.getOntModel().getIndividual(OntologyController.URI + individualName);
        if (individual == null) {
            return jsonProperties;
        }

        JsonObject jsonInferred = new JsonObject();
        jsonProperties.add("inferred", jsonInferred);
        JsonObject jsonAsserted = new JsonObject();
        jsonProperties.add("asserted", jsonAsserted);
        JsonObject jsonDataProperties = new JsonObject();
        jsonProperties.add("dataProperties", jsonDataProperties);

        Resource resource = controller.getInfModel().getResource(OntologyController.URI + individualName);
        if (resource != null) {
            jsonProperties.addProperty("resource", resource.getProperty(dpName) != null ? resource.getProperty(dpName).getString() : resource.getLocalName());

            String name = resource.getLocalName();
            name = name.replace("program.", "activity.");
            String type = name.split("\\.")[0];
            jsonDataProperties.addProperty("type", type);

            resource.listProperties().filterDrop(new Filter<Statement>() {
                @Override
                public boolean accept(Statement t) {
                    return t.getPredicate().getLocalName().equals("differentFrom") || controller.getOntModel().contains(t);
                }
            });

            for (Statement s : resource.listProperties().toList()) {
                try {
                    String predicate = s.getPredicate().getLocalName();
                    if (predicate.equals("detail") || predicate.equals("differentFrom") || predicate.equals("type") || predicate.equals("sameAs")) {
                        continue;
                    }
                    predicate = predicate.replaceAll("([a-z])([A-Z]+)", "$1 $2");
                    name = "";
                    if (!s.getObject().isResource()) {
                        name = s.getObject().asLiteral().getValue().toString();
                        predicate = predicate.replace("_", " ");
                        jsonDataProperties.addProperty(predicate, name);
                        continue;
                    }
                    name = s.getResource().getLocalName();
                    name = name.replace("program.", "activity.");
                    type = name.split("\\.")[0];
                    String id = name.split("\\.")[1];
                    if (s.getResource().getProperty(dpName) != null) {
                        name = s.getResource().getProperty(dpName).getString();
                    }

                    JsonObject obj = new JsonObject();
                    obj.addProperty("id", id);
                    obj.addProperty("type", type);
                    obj.addProperty("name", name);

                    if (controller.getOntModel().contains(s)) {
                        JsonArray jsonArray = new JsonArray();
                        if (jsonAsserted.get(predicate) != null) {
                            jsonArray = jsonAsserted.get(predicate).getAsJsonArray();
                        }

                        jsonArray.add(obj);
                        jsonAsserted.add(predicate, jsonArray);
                    } else {
                        JsonArray jsonArray = new JsonArray();
                        if (jsonInferred.get(predicate) != null) {
                            jsonArray = jsonInferred.get(predicate).getAsJsonArray();

                        }
                        jsonArray.add(obj);
                        jsonInferred.add(predicate, jsonArray);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonProperties;
    }

    public ExtendedIterator<Statement> jenaGetInferredData() {
        ExtendedIterator<Statement> stmts = controller.getInfModel().listStatements().filterDrop(new Filter<Statement>() {
            @Override
            public boolean accept(Statement o) {
                return controller.getOntModel().contains(o);
            }
        });
        return stmts;
    }

    public List<String> jenaGetUsedWfmsInf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov-se-o", "usedWfms");
    }

    public List<String> jenaGetExecuteInf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov-se-o", "execute");
    }

    public List<String> jenaGetExecutedInInf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov-se-o", "executedIn");
    }

    public List<String> jenaGetIsSimilarInf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov-se-o", "isSimilar");
    }

    public List<String> jenaGetEvolutionTo(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov", "hadDerivation");
    }

    public List<String> jenaGetEvolutionOf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov", "wasDerivedFrom");
    }

    public List<String> jenaGetWasInfluencedByInf(String individualName) {
        return jenaGetOPAssertionsByIndividualInf(individualName, "prov", "wasInfluencedBy");
    }

    /**
     * Escolhe a URI correta de acordo com o prefixo informado
     *
     * @param prefix
     * @return
     */
    private String getUri(String prefix) {
        switch (prefix) {
            case "prov":
                return OntologyController.PROV_URI;
            case "provone":
                return OntologyController.PROVONE_URI;
            case "prov-se-o":
                return OntologyController.URI;
            default:
                return OntologyController.URI;
        }
    }
}
