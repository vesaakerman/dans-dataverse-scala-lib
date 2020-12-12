/**
 * Copyright (C) 2020 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib

import java.nio.charset.StandardCharsets

import better.files.File
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataFieldSerializer
import org.apache.commons.io.FileUtils
import org.json4s.native.Serialization
import org.json4s.{ DefaultFormats, Formats }

import scala.util.Try

package object dataverse {
  def tryReadFileToString(file: File): Try[String] = Try {
    FileUtils.readFileToString(file.toJava, StandardCharsets.UTF_8)
  }

  def serializeAsJson(o: Any, pretty: Boolean = false): Try[String] = Try {
    implicit val jsonFormats: Formats = DefaultFormats + MetadataFieldSerializer
    if (pretty) Serialization.writePretty(o)
    else Serialization.write(o)
  }

  case class LockException(msg: String, cause: Throwable = null)
    extends Exception(s"$msg", cause)

}
