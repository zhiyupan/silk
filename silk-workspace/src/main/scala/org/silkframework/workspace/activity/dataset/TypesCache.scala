package org.silkframework.workspace.activity.dataset

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.CachedActivity

/**
  * Holds the most frequent types.
  */
class TypesCache(dataset: ProjectTask[GenericDatasetSpec]) extends CachedActivity[Types] {

  override def name: String = s"Types cache ${dataset.id}"

  override def initialValue: Option[Types] = Some(Types.empty)

  override def loadCache(context: ActivityContext[Types], fullReload: Boolean)
                        (implicit userContext: UserContext): Unit = {
    implicit val prefixes: Prefixes = dataset.project.config.prefixes
    val dataSource = dataset.source
    val types = Types(dataSource.retrieveTypes().toSeq)
    context.value() = types
  }

  override def resource: WritableResource = dataset.project.cacheResources.child("dataset").get(s"${dataset.id}_cache.xml")

  override protected val wrappedXmlFormat = WrappedXmlFormat()
}