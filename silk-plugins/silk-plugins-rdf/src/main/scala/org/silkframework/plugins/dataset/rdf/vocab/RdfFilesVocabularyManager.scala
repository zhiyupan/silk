package org.silkframework.plugins.dataset.rdf.vocab

import java.util.logging.Logger

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint
import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.resource.{FileResource, ResourceNotFoundException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory

@Plugin(
  id = "rdfFiles",
  label = "RDF Files",
  description = "Loads vocabularies from RDF files, which are part of the project resources."
)
case class RdfFilesVocabularyManager() extends VocabularyManager {

  private val prefix = "urn:"
  private val log: Logger = Logger.getLogger(classOf[RdfFilesVocabularyManager].getName)

  override def get(uri: String, project: Option[Identifier])
                  (implicit userContext: UserContext): Option[Vocabulary] = {
    // Get resource
    try {
      val vocabularyResource = project.map(p => WorkspaceFactory().workspace.project(p).resources.get(uri, mustExist = true))
      if(vocabularyResource.nonEmpty && vocabularyResource.get.size.nonEmpty &&
          !vocabularyResource.get.size.contains(0L)) { // only consider files
        val resource = vocabularyResource.get
        // Load into Jena model
        val model = ModelFactory.createDefaultModel()
        val inputStream = resource.inputStream
        RDFDataMgr.read(model, inputStream, RDFLanguages.filenameToLang(resource.name))
        inputStream.close()

        // Create vocabulary loader
        val dataset = DatasetFactory.createTxnMem()
        dataset.addNamedModel(prefix + uri, model)
        val endpoint = new JenaDatasetEndpoint(dataset)
        val loader = new VocabularyLoader(endpoint)

        // Load vocabulary
        loader.retrieveVocabulary(prefix + uri)
      } else {
        None
      }
    } catch {
      case _: ResourceNotFoundException =>
        log.warning(s"Non-existing vocabulary file $uri. Not loading any vocabulary.")
        None
    }
  }

  override def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]] = {
    // FIXME: Not clear how to automatically decide which RDF files are global vocabularies without registering them.
    None
  }
}
