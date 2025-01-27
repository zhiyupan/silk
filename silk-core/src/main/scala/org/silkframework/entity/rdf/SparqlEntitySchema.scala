/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.entity.rdf

import org.silkframework.config.Prefixes
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.xml.Node

case class SparqlEntitySchema(variable: String = SparqlEntitySchema.variable, restrictions: SparqlRestriction = SparqlRestriction.empty, paths: IndexedSeq[UntypedPath]) {
  require(paths.forall(_.operators.nonEmpty), "Entity description must not contain an empty path")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: UntypedPath): Int = {
    var index = 0
    while(path != paths(index)) {
      index += 1
      if(index >= paths.size) {
        throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${paths.mkString(", ")}.")
      }
    }
    index
  }

  def isEmpty: Boolean = restrictions.isEmpty && paths.isEmpty
}

object SparqlEntitySchema {

  final val variable = "a"

  /**
   * Creates an empty entity description.
   */
  def empty: SparqlEntitySchema = SparqlEntitySchema(variable, SparqlRestriction.empty, IndexedSeq.empty)

  def fromSchema(entitySchema: EntitySchema, entityUris: Seq[Uri]): SparqlEntitySchema = {
    var sparqlRestriction = new SparqlRestrictionBuilder(variable)(Prefixes.empty).apply(entitySchema.filter)
    if(entitySchema.typeUri.uri.nonEmpty) {
      sparqlRestriction = sparqlRestriction merge SparqlRestriction.fromSparql(variable, s"?$variable a <${entitySchema.typeUri}>")
    }
    val subPath = entitySchema.subPath

    def rewriteRestrictionWithParentProperty(subPath: UntypedPath): String = {
      val rootEntity = "?root"
      val sparql = SparqlPathBuilder.path(subPath, rootEntity, "?" + variable, "?st", "?sf")
      sparqlRestriction = SparqlRestriction.fromSparql(variable, sparqlRestriction.toSparql.replace(s"?$variable", rootEntity) + sparql)
      rootEntity
    }

    val rootVariable = if(subPath.operators.nonEmpty) {
      rewriteRestrictionWithParentProperty(subPath)
    } else {
      s"?$variable"
    }
    if(entityUris.nonEmpty) {
      val entityFilter = s"\nFILTER ($rootVariable IN (${entityUris.map(e => s"<$e>").mkString(", ")}))"
      sparqlRestriction = SparqlRestriction.fromSparql(variable, sparqlRestriction.toSparql + entityFilter)
      SparqlEntitySchema(variable, sparqlRestriction, entitySchema.typedPaths.map(_.toUntypedPath))
    }

    SparqlEntitySchema(variable, sparqlRestriction, entitySchema.typedPaths.map(_.toUntypedPath))
  }

  /**
   * XML serialization format.
   */
  implicit object EntityDescriptionFormat extends XmlFormat[SparqlEntitySchema] {
    /**
     * Deserialize an EntityDescription from XML.
     */
    def read(node: Node)(implicit readContext: ReadContext): SparqlEntitySchema = {
      val variable = (node \ "Variable").text.trim
      new SparqlEntitySchema(
        variable = variable,
        restrictions = SparqlRestriction.fromSparql(variable, (node \ "Restrictions").text),
        paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq) yield UntypedPath.parse(pathNode.text.trim)
      )
    }

    /**
     * Serialize an EntityDescription to XML.
     */
    def write(desc: SparqlEntitySchema)(implicit writeContext: WriteContext[Node]): Node =
      <EntityDescription>
        <Variable>{desc.variable}</Variable>
        <Restrictions>{desc.restrictions.toSparql}</Restrictions>
        <Paths> {
          for (path <- desc.paths) yield {
            <Path>{path.serialize()(Prefixes.empty)}</Path>
          }
          }
        </Paths>
      </EntityDescription>
  }

  object specialPaths {
    // Returns the lexical value of the resource/literal this is requested from, e.g. when using a literal for an object mapping.
    final val TEXT = "#text"
    // Special path to request the language tag of a literal when the literal is used as the subject of an entity retrieval request.
    final val LANG = "#lang"

    def isLangSpecialPath(typedPath: Path): Boolean = typedPath.serialize(stripForwardSlash = false).endsWith(s"/$LANG")
    def isTextSpecialPath(typedPath: Path): Boolean = typedPath.serialize(stripForwardSlash = false).endsWith(s"/$TEXT")
  }
}
