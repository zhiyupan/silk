package de.fuberlin.wiwiss.silk.dataset

import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Represents an abstraction over a data sink.
 *
 * Implementing classes of this trait must override the write methods.
 */
trait DataSink extends EntitySink with LinkSink {
  /**
   * Initializes this writer.
   *
   * @param properties The list of properties of the entities to be written.
   */
  override def open(properties: Seq[String] = Seq.empty) { init() }

  /**
   * Closes this writer.
   */
  override def close() {}
}