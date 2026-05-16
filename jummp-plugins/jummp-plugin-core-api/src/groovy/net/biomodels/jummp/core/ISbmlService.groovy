/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
**/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC

/**
 * @short Interface describing the service to access an SBML Model.
 *
 * The implementation of this interface is in the SBML plugin. Although this
 * interface is defined in core-api there is no guarantee that core provides
 * support for models in SBML. It is as well possible to support different
 * model formats in which case another service has to be defined to support
 * the interaction with the service for the file format.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
interface ISbmlService {
    /**
     * Retrieves the metaId on the Model level. Does not retrieve the metaId of child elements
     * of model such as compartment.
     * @param revision
     * @return MetaId on Model level
     */
    public String getMetaId(RevisionTC revision)
    /**
     *
     * @param revision
     * @return Version of the SBML file
     */
    public long getVersion(RevisionTC revision)
    /**
     *
     * @param revision
     * @return Level of the SBML file
     */
    public long getLevel(RevisionTC revision)
    /**
     * Retrieves the notes element as an xml String on model level.
     * It cannot be used to retrieve notes on another level. If there
     * are no notes an empty string is returned.
     * @param revision
     * @return The notes of the model.
     */
    public String getNotes(RevisionTC revision)

    /**
     * Retrieves the MIRIAM annotations for the model element.
     * The returned list contains one map for each annotation with the following keys/values:
     * @li qualifier: name for the MIRIAM qualifier as listed in http://sbml.org/Special/Software/JSBML/build/apidocs/org/sbml/jsbml/CVTerm.Qualifier.html
     * @li biologicalQualifier: @c true if the qualifier is a biological qualifier, @c false otherwise
     * @li modelQualifier: @c true if the qualifier is a model qualifier, @c false otherwise
     * @li resources: list of all uris for this annotation fully resolved as a Map each with the following items:
     * @li urn: The unique MIRIAM urn
     * @li dataTypeLocation: URL to the Website of this data type (optional)
     * @li dataTypeName: Human readable name for the data type (optional)
     * @li url: direct URL to the website describing the resource (optional)
     * @li name: Human readable name of the resource (optional)
     * @param revision
     * @return List of all MIRIAM annotations of the model element
     */
    public List<Map> getAnnotations(RevisionTC revision)

    /**
     * Retrieves the global parameters of the SBML Model.
     * The returned list contains one map for each parameter with the following keys/values:
     * @li id: the id element
     * @li metaId: The metaId element
     * @li name: The name element
     * @li constants: @c true if parameter is a constant, @c false otherwise
     * @li value: The value element if set, otherwise @c null
     * @li sbo: Map describing the SBO term, like resources in @link getAnnotations
     * @li unit: The unit element
     * @param revision
     * @return List of all parameters in the Model
     */
    public List<Map> getParameters(RevisionTC revision)
    /**
     * Retrieves the parameter with given @p id from the SBML Model.
     * The returned Map contains all the elements as explained in @link getParameters
     * with additionally the annotation and notes added to the map. The annotation
     * element follows the description of @link getAnnotations.
     * @param revision
     * @param id The unique id in the SBML Model.
     * @return Map of all parameter data
     */
    public Map getParameter(RevisionTC revision, String id)
    /**
     * Retrieves all parameters local to the reactions in the SBML Model.
     * The parameters are sorted to the reactions. The returned list is actually a list
     * of minimum information about the reaction plus the parameters:
     * @li id: The id of the reaction
     * @li name: The name of the reaction
     * @li parameters: List of parameters as in @link getParameters with the following addition:
     * constant is always true and an additional map to identify the reaction is added.
     * @param revision
     * @return List of all reactions with their parameters
     */
    public List<Map> getLocalParameters(RevisionTC revision)
    /**
     * Retrieves all reactions in the SBML Model.
     * The returned list contains one map for each reaction with the following keys/values:
     * @li id: the id element
     * @li metaId: The metaId element
     * @li name: The name element
     * @li reversible: boolean indicating whether the reaction is reversible
     * @li sbo: Map describing the SBO term, like resources in @link getAnnotations
     * @li reactants: list of all Reactants with a Map for each Reactant, description see below
     * @li products: list of all Products with a Map for each Product, description see below
     * @li modifiers: list of all Modifiers with a Map for each Modifier, description see below
     *
     * The three contained lists reactants, products and modifiers represent a list of species each.
     * Each of the lists can be empty or contain one to many maps with the following keys/values:
     * @li species: the id of the referenced species
     * @li speciesName: The name of the referenced species
     * @li constant: boolean indicating whether the species has a constant value (only for products and reactants)
     * @li stoichiometry: The shoichiometry value (only for products and reactants
     * @param revision
     * @return  List of all reactions
     */
    public List<Map> getReactions(RevisionTC revision)
    /**
     * Retrieves the Reaction with the given @p id from the SBML Model.
     * The returned Map contains all the elements as explained in @link getReactions
     * with additionally the annotation, notes and math added to the map. The annotation
     * element follows teh description of @link getAnnotations. The math value is the
     * contained MathML of the kinetic law as a String or an empty string in case there
     * is no kinetic law
     * @param revision
     * @param id The id of the Reaction
     * @return Map describing the reaction
     */
    public Map getReaction(RevisionTC revision, String id)
    /**
     * Retrieves all events in the SBML Model.
     * The returned list contains one map for each event with the following keys/values:
     * @li id: The id element
     * @li metaId: The metaId element
     * @li name: The name element
     * @li assignments: List of all Assignments for this Event. It contains a map for each Assignment
     * with the following key/value pairs:
     * @li metaId: The metaId element associated with the event assignment
     * @li math: The Content MathML of the assignment as a String
     * @li variableId: The Id of the referenced variable
     * @li variableName: The resolved name of the referenced variable
     * @li variableType: The element name of the referenced variable
     * @param revision
     * @return List of all events
     */
    public List<Map> getEvents(RevisionTC revision)
    /**
     * Retrieves the Event with the given @p id from the SBML Model.
     * The returned map contains all the elements as explained in @link getEvents
     * with additionally the annotation, notes, sbo, trigger and delay. The
     * annotation element follows the description of @link getAnnotations. Trigger
     * and delay contain both either a Content MathML String or an empty String.
     * @param revision
     * @param id The Id of the Event
     * @return Map describing the event
     */
    public Map getEvent(RevisionTC revision, String id)
    /**
     * Retrieves all rules in the SBML Model.
     * The returned list contains one map for each rule with the following keys/values:
     * @li metaId: The metaId element
     * @li math: The Content MathML of this rule
     * @li type: Either rate, assignment or algebraic. Describes the type of the rule.
     * @li variableId: Id element of referenced variable if present, otherwise @c null
     * @li variableId: Name element of referenced variable if present, otherwise @c null
     * @li variableType: Element name of referenced variable if present, otherwise @c null
     * @param revision
     * @return List of all rules
     */
    public List<Map> getRules(RevisionTC revision)
    /**
     * Retrieves the Rule for the given referenced @p variable in the SBML Model.
     * The returned map contains all the elements as explained in @link getRules with
     * additionally the annotation and notes string. The annotation element follows
     * the description of @link getAnnotations.
     * @param revision
     * @param variable The id of the referenced Variable
     * @return Map describing the Rule
     */
    public Map getRule(RevisionTC revision, String variable)
    /**
     * Retrieves all function definitions in the SBML Model.
     * The returned list contains one map for each function definition with the following keys/values:
     * @li id The id element
     * @li name The name element
     * @li metaId The metaId element
     * @li math The content MathML String
     * @param revision
     * @return List of all Function Definitions
     */
    public List<Map> getFunctionDefinitions(RevisionTC revision)
    /**
     * Retrieves the Function Definition with the given @p id in the SBML Model.
     * The returned map contains all the elements as explained in @link getFunctionDefinitions with
     * additionally the annotation, notes and sbo. The annotation element follows the description
     * of @link getAnnotations.
     * @param revision
     * @param id
     * @return Map describing the Function Definition
     */
    public Map getFunctionDefinition(RevisionTC revision, String id)
    /**
     * Retrieves all compartments in the SBML Model.
     * The returned list contains one map for each compartment with the following keys/values:
     * spatialDimensions: compartment.spatialDimensions
     * @li metaId: The meta id element
     * @li id: The id element
     * @li name: The name element
     * @li size: The size of the object
     * @li spatialDimensions: The spatial dimensions element
     * @li units: The unit element
     * @li notes:  The notes element
     * @li sbo: Map describing the SBO term, like resources in @link getAnnotations
     * @li allSpecies: The needed attributes of all related species
     * @param revision
     * @return List of all Compartments
     */
    public List<Map> getCompartments(RevisionTC revision)
    /**
     * Retrieves the Compartment with the given @p id in the SBML Model.
     * The returned map contains all the elements as explained in @link getCompartments with
     * additionally the annotation and notes. The annotation element follows the description
     * of @link getAnnotations.
     * @param revision
     * @param id
     * @return Map describing the Compartment
     */
    public Map getCompartment(RevisionTC revision, String id)
    /**
     * Retrieves all species in the SBML Model.
     * The returned list contains one map for each compartment with the following keys/values:
     * metaid: species.metaId
     * @li metaId: The metaId element
     * @li id: The id element
     * @li initialAmount: The initial amount element
     * @li initialConcentration: The initial concentration element
     * @li substanceUnits: The substance units  element
     * @param modelId
     * @param revisionNumber
     * @return List of all species
     */
    public List<Map> getAllSpecies(RevisionTC revision)
    /**
     * Retrieves the Species for the given referenced @p variable in the SBML Model.
     * The returned map contains all the elements as explained in @link getAllSpecies with
     * additionally the annotation and notes string. The annotation element follows
     * the description of @link getAnnotations.
     * @param revision
     * @param id
     * @return Map describing the Species
     */
     public Map getSpecies(RevisionTC revision, String id)
    /**
     * Triggers the generation of a sub model taking selected parts of an existing model.
     * The returned String contains the generated SBML model.
     * @param revision
     * @param subModelId: The sub model id which can be given by a user
     * @param metaId: The given meta id
     * @param compartmentIds: The selected compartment ids
     * @param speciesIds: The selected species ids
     * @param reactionIds: The selected reaction ids
     * @param ruleIds: The selected rule ids
     * @param eventIds: The selected event ids
     * @return String containing the generated SBML model
     */
    public String triggerSubmodelGeneration(RevisionTC revision, String subModelId, String metaId, List<String> compartmentIds, List<String> speciesIds, List<String> reactionIds, List<String> ruleIds, List<String> eventIds)
    /**
     * Generates an SVG for the given SBML model.
     * @param revision The Revision identifying an SBML model
     * @return Content of generated SVG
     */
    public byte[] generateSvg(RevisionTC revision)
    /**
     * Generates Octave output for the given SBML model.
     * @param revision The Revision identifying an SBML model
     * @return Content of generated Octave file
     */
    public String generateOctave(RevisionTC revision)
    /**
     * Generates BioPAX output for the given SBML model.
     * @param revision The Revision identifying an SBML model
     * @return Content of generated BioPAX file
     */
    public String generateBioPax(RevisionTC revision)

    /**
     * Adds the identifiers to the model-level annotations of a {@link RevisionTC}.
     *
     * @param identifiers the identifier URIs that should be added to the model's annotations
     *           (e.g. http://identifiers.org/biomodels.db/BIOMD0000000001).
     * @param revision the revision command to which we should add the annotation
     * @return true if the annotation
     * @throws ModelException in case of I/O or parsing issues when updating the model file.
     */
    boolean addModelIdentifiersAsAnnotation(RevisionTC revision, String... identifiers)
            throws ModelException

    boolean addModellingApproachAsAnnotation(RevisionTC revision, ModellingApproach approach) throws
        ModelException

    /**
     * Adds publication identifier (DOI or PubMed) as an annotation
     * @param revision  A RevisionTC instance to be added the publication annotation
     * @param publication A PublicationTransportCommand instance indicating the publication record to be added to the revision
     * @return true or false indicating the operation succeeds or fails
     * @throws ModelException
     */
    boolean addPublicationAsAnnotation(RevisionTC revision, PubTC publication) throws ModelException
}
