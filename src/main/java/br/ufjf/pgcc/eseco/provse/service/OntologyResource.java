/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgcc.eseco.provse.service;

import br.ufjf.pgcc.eseco.provse.service.app.DataLoader;
import br.ufjf.pgcc.eseco.provse.service.app.InferenceLayer;
import br.ufjf.pgcc.eseco.provse.service.app.OntologyController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Lenita
 */
@Path("ontology")
public class OntologyResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of OntologyResource
     */
    public OntologyResource() {

    }

    /**
     * Retrieves representation of an instance of
     * br.ufjf.pgcc.eseco.provse.service.OntologyResource
     *
     * @param entity
     * @return an instance of java.lang.String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getXml(@QueryParam("entity") String entity) {
        System.out.println("+++++++++++++++ Getting ");
        JsonObject jObject = new JsonObject();
        jObject.addProperty("teste", Boolean.TRUE);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        InferenceLayer inferenceLayer = new InferenceLayer();
        JsonObject sparqlGetPropertiesByIndividualInf = inferenceLayer.jenaGetInferredDataByIndividual(entity);
        return sparqlGetPropertiesByIndividualInf.toString();
    }

    /**
     * PUT method for updating or creating an instance of OntologyResource
     *
     * @param content representation for the resource
     * @param response
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void putXml(String content, @Context final HttpServletResponse response) {
        System.out.println("+++++++++++++++ Putting: ");
        Gson gson = new GsonBuilder().create();
        JsonElement jsonElement = gson.fromJson(content, JsonElement.class);
        try {
            DataLoader.loadDAO(jsonElement);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        try {
            response.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
