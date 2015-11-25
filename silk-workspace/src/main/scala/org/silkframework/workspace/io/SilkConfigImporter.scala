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

package org.silkframework.workspace.io

import org.silkframework.workspace.Project
import org.silkframework.config.LinkingConfig

/**
 * Imports a Silk SilkConfig into a project.
 */
object SilkConfigImporter {
  def apply(config: LinkingConfig, project: Project) {
    //Add all prefixes
    project.config = project.config.copy(prefixes = project.config.prefixes ++ config.prefixes)

    //Add all sources
    for(source <- config.sources) {
      project.addTask(source.id, source)
    }

    //Add all outputs
    for(output <- config.outputs) {
      project.addTask(output.id, output)
    }

    //Add all linking tasks
    for(linkSpec <- config.linkSpecs) {
      project.addTask(linkSpec.id, linkSpec)
    }
  }
}