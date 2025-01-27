package org.silkframework.workspace.activity.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.VocabularyManager
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.{CachedActivity, PathsCacheTrait}

/**
 * Holds the target vocabularies.
 */
class VocabularyCache(task: ProjectTask[TransformSpec]) extends CachedActivity[VocabularyCacheValue] with PathsCacheTrait {

  override def name: String = s"Vocabulary cache ${task.id}"

  override def initialValue: Option[VocabularyCacheValue] = Some(new VocabularyCacheValue(Seq.empty, None))

  override protected val persistent = false

  override def loadCache(context: ActivityContext[VocabularyCacheValue], fullReload: Boolean)
                        (implicit userContext: UserContext): Unit = {
    val transform = task.data
    if(transform.targetVocabularies.explicitVocabularies.nonEmpty) {
      val vocabManager = VocabularyManager()
      val vocabularies = for (vocab <- transform.targetVocabularies.explicitVocabularies.distinct) yield vocabManager.get(vocab, Some(task.project.name))
      context.value() = new VocabularyCacheValue(vocabularies.flatten.sortBy(_.info.uri), Some(System.currentTimeMillis()))
    }
  }

  override def resource: WritableResource = task.project.cacheResources.child("transform").child(task.id).get(s"vocabularyCache.xml")

  val wrappedXmlFormat: WrappedXmlFormat = WrappedXmlFormat()
}
