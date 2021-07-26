package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.riot.{Lang, RDFLanguages}
import org.silkframework.plugins.dataset.rdf.datasets.RdfLangAutocompletionProvider.supportedLanguages
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider}
import org.silkframework.util.StringUtils
import org.silkframework.workspace.WorkspaceReadTrait

import scala.collection.JavaConverters._

/**
  * Autocompletion provider that completes all RDF languages that we support when reading/writing RDF files.
  */
case class RdfLangAutocompletionProvider() extends PluginParameterAutoCompletionProvider {

  override def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    supportedLanguages
      .filter(_.matches(multiSearchWords))
      .map(_.toResult)
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String],
                            workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    supportedLanguages.find(_.value == value).map(_.label)
  }
}

object RdfLangAutocompletionProvider {

  val supportedLanguages = Seq(
    RdfLang(RDFLanguages.NTRIPLES, writable = true),
    RdfLang(RDFLanguages.NQUADS, writable = false),
    RdfLang(RDFLanguages.TURTLE, writable = false),
    RdfLang(RDFLanguages.RDFXML, writable = false),
    RdfLang(RDFLanguages.RDFJSON, writable = false)
  )

  case class RdfLang(lang: Lang, writable: Boolean) {

    def value: String = {
      lang.getName
    }

    def label: String = {
      if(writable) {
        s"${lang.getName} (read/write)"
      } else {
        s"${lang.getName} (read)"
      }
    }

    def matches(lowerCaseSearchTerms: Seq[String]): Boolean = {
      val names = Set(lang.getName) ++ lang.getAltNames.asScala
      names.exists { name =>
        StringUtils.matchesSearchTerm(lowerCaseSearchTerms, name.toLowerCase)
      }
    }

    def toResult: AutoCompletionResult = {
      AutoCompletionResult(lang.getName, Some(label))
    }
  }

}