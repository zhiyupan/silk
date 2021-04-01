package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{InMemoryResourceManager, ResourceTooLargeException, WritableResource}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.time.Instant

class RdfFileDatasetTest extends FlatSpec with MustMatchers {
  behavior of "RDF file dataset"

  val resourceManager = InMemoryResourceManager()

  implicit val userContext: UserContext = UserContext.Empty

  it should "not load files larger than 'max. read size'" in {
    val largeResource = new WritableResource {
      override def name: String = "largeResource"
      override def path: String = "path"
      override def size: Option[Long] = Some(1000000000L)

      override def createOutputStream(append: Boolean): OutputStream = new ByteArrayOutputStream()
      override def delete(): Unit = {}
      override def exists: Boolean = true
      override def modificationTime: Option[Instant] = None
      override def inputStream: InputStream = new ByteArrayInputStream(Array.emptyByteArray)
    }

    val rdfDataset = RdfFileDataset(largeResource, "Turtle")
    intercept[ResourceTooLargeException] {
      rdfDataset.source.retrieveTypes()
    }
  }
}
