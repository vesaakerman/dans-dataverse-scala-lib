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
package nl.knaw.dans.lib.dataverse

import java.io.FileInputStream
import java.net.URI
import java.nio.charset.StandardCharsets

import better.files.File
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils
import scalaj.http.{ Http, HttpResponse, MultiPart }

import scala.util.Try

private[dataverse] trait HttpSupport extends DebugEnhancedLogging {
  type Response = HttpResponse[Array[Byte]]

  protected val connectionTimeout: Int
  protected val readTimeout: Int
  protected val baseUrl: URI
  protected val apiToken: String
  protected val apiVersion: String

  private def http(method: String, uri: URI, body: String = null, headers: Map[String, String] = Map.empty[String, String]): Try[HttpResponse[Array[Byte]]] = Try {
    {
      if (body == null) Http(uri.toASCIIString)
      else Http(uri.toASCIIString).postData(body)
    }.method(method)
      .headers(headers)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
      .asBytes
  }

  private def http2[P: Manifest](method: String, uri: URI, body: String = null, headers: Map[String, String] = Map.empty[String, String]): Try[DataverseResponse[P]] = Try {
    val response = {
      if (body == null) Http(uri.toASCIIString)
      else Http(uri.toASCIIString).postData(body)
    }.method(method)
      .headers(headers)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
      .asBytes
    if (response.code >= 200 && response.code < 300) new DataverseResponse(response)
    else throw DataverseException(response.code, new String(response.body, StandardCharsets.UTF_8), response)
  }

  protected def httpPostMulti(uri: URI, file: File, optJsonMetadata: Option[String] = None, headers: Map[String, String] = Map()): Try[HttpResponse[Array[Byte]]] = Try {
    trace(())
    val parts = MultiPart(name = "file", filename = file.name, mime = "application/octet-stream", new FileInputStream(file.pathAsString), file.size, lenWritten => {}) +:
      optJsonMetadata.map {
        json => List(MultiPart(data = json.getBytes(StandardCharsets.UTF_8), name = "jsonData", filename = "jsonData", mime = "application/json"))
      }.getOrElse(Nil)

    Http(uri.toASCIIString).postMulti(parts: _*)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
      .headers(headers)
      .asBytes
  }

  protected def postFile(subPath: String, file: File, optJsonMetadata: Option[String] = None)(expectedStatus: Int, formatResponseAsJson: Boolean = false): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- httpPostMulti(uri, file, optJsonMetadata, Map("X-Dataverse-key" -> apiToken))
      _ = debug(s"response: ${ response.statusLine }, ${ new String(response.body, StandardCharsets.UTF_8) }")
    } yield response
  }

  protected def get(subPath: String = null): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http("GET", uri, body = null, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }


  protected def get2[P: Manifest](subPath: String = null): Try[DataverseResponse[P]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http2[P]("GET", uri, body = null, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }

  protected def postJson(subPath: String = null)(body: String = null): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http("POST", uri, body, Map("Content-Type" -> "application/json", "X-Dataverse-key" -> apiToken))
    } yield response
  }

  protected def postJson2[P: Manifest](subPath: String = null)(body: String = null): Try[DataverseResponse[P]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http2[P]("POST", uri, body, Map("Content-Type" -> "application/json", "X-Dataverse-key" -> apiToken))
    } yield response
  }


  protected def postText(subPath: String = null)(body: String = null): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http("POST", uri, body, Map("Content-Type" -> "text/plain", "X-Dataverse-key" -> apiToken))
    } yield response
  }

  protected def put(subPath: String = null)(body: String = null): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http("PUT", uri, body, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }

  protected def put2[P: Manifest](subPath: String = null)(body: String = null): Try[DataverseResponse[P]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http2[P]("PUT", uri, body, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }


  protected def deletePath(subPath: String = null): Try[HttpResponse[Array[Byte]]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http("DELETE", uri, null, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }

  protected def deletePath2[P: Manifest](subPath: String = null): Try[DataverseResponse[P]] = {
    for {
      uri <- uri(s"api/v${ apiVersion }/${ Option(subPath).getOrElse("") }")
      _ = debug(s"Request URL = $uri")
      response <- http2[P]("DELETE", uri, null, Map("X-Dataverse-key" -> apiToken))
    } yield response
  }


  //  protected def prettyPrintJson(json: String): Try[String] = Try {
  //    trace(())
  //    Serialization.writePretty(JsonParser.parse(json))
  //  }
  //
  def uri(s: String): Try[URI] = Try {
    baseUrl resolve s
  }
}