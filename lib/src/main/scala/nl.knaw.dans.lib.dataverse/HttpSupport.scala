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
import scalaj.http.{ Http, MultiPart }

import scala.collection.mutable
import scala.util.Try

/**
 * Functions supporting HTTP interaction with Dataverse.
 */
private[dataverse] trait HttpSupport extends DebugEnhancedLogging {
  private val HEADER_CONTENT_TYPE = "Content-Type"
  private val HEADER_X_DATAVERSE_KEY = "X-Dataverse-key"

  private val MEDIA_TYPE_JSON = "application/json"
  private val MEDIA_TYPE_OCTET_STREAM = "application/octet-stream"
  private val MEDIA_TYPE_TEXT = "text/plain"

  private val METHOD_GET = "GET"
  private val METHOD_POST = "POST"
  private val METHOD_PUT = "PUT"
  private val METHOD_DELETE = "DELETE"

  protected val connectionTimeout: Int
  protected val readTimeout: Int
  protected val baseUrl: URI
  protected val apiToken: String
  // If false, it is sent through the X-Dataverse-key header
  protected val sendApiTokenViaBasicAuth: Boolean
  protected val unblockKey: Option[String]
  protected val apiPrefix: String
  protected val apiVersion: Option[String]

  protected def postFile[D: Manifest](subPath: String, optFile: Option[File], optJsonMetadata: Option[String] = None): Try[DataverseResponse[D]] = {
    trace(subPath, optFile, optJsonMetadata)
    for {
      uri <- createUri(Option(subPath))
      response <- httpPostMulti[D](uri, optFile, optJsonMetadata)
    } yield response
  }

  private def httpPostMulti[D: Manifest](uri: URI, optFile: Option[File], optJsonMetadata: Option[String] = None, headers: Map[String, String] = Map()): Try[DataverseResponse[D]] = Try {
    trace(uri, optFile, optJsonMetadata, headers)

    /*
     * SWORD sends the API key through the user name of basic auth. The other APIs use the X-Dataverse-key.
     */
    val hs = if (!sendApiTokenViaBasicAuth) headers + (HEADER_X_DATAVERSE_KEY -> apiToken)
             else headers
    val credentials = if (sendApiTokenViaBasicAuth) Option(apiToken, "")
                      else Option.empty

    val partsBuffer = mutable.ListBuffer[MultiPart]()
    optFile.foreach(f => partsBuffer.append(MultiPart(name = "file", filename = f.name, mime = MEDIA_TYPE_OCTET_STREAM, new FileInputStream(f.pathAsString), f.size, lenWritten => {}) ))
    optJsonMetadata.foreach(md => partsBuffer.append(MultiPart(data = md.getBytes(StandardCharsets.UTF_8), name = "jsonData", filename = "jsonData", mime = MEDIA_TYPE_JSON)))

    val request = Http(uri.toASCIIString).postMulti(partsBuffer.toList: _*)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
      .headers(hs)
    val response = credentials
      .map { case (u, p) => request.auth(u, p) }
      .getOrElse(request).asBytes

    if (response.code >= 200 && response.code < 300) DataverseResponse(response)
    else throw DataverseException(response.code, new String(response.body, StandardCharsets.UTF_8), response)
  }

  protected def get[D: Manifest](subPath: String = null, params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    trace(subPath)
    for {
      uri <- createUri(Option(subPath))
      response <- http[D](METHOD_GET, uri, body = null, Map.empty, params)
    } yield response
  }

  protected def postJson[D: Manifest](subPath: String = null)(body: String = null): Try[DataverseResponse[D]] = {
    trace(subPath, body)
    for {
      uri <- createUri(Option(subPath))
      response <- http[D](METHOD_POST, uri, body, Map(HEADER_CONTENT_TYPE -> MEDIA_TYPE_JSON))
    } yield response
  }

  protected def postText[D: Manifest](subPath: String = null)(body: String = null): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- http[D](METHOD_POST, uri, body, Map(HEADER_CONTENT_TYPE -> MEDIA_TYPE_TEXT))
    } yield response
  }

  protected def put[D: Manifest](subPath: String = null)(body: String = null): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- http[D](METHOD_PUT, uri, body)
    } yield response
  }

  protected def deletePath[D: Manifest](subPath: String = null): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- http[D](METHOD_DELETE, uri, null)
    } yield response
  }

  private def createUri(subPath: Option[String]): Try[URI] = Try {
    baseUrl resolve new URI(s"${ apiPrefix }/${ apiVersion.map(version => s"v$version/").getOrElse("") }${ subPath.getOrElse("") }")
  }

  private def http[D: Manifest](method: String, uri: URI,
                                body: String = null,
                                headers: Map[String, String] = Map.empty,
                                params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = Try {
    trace(method, uri, body, headers, params)
    debug(s"Request URL = $uri, query params = $params")

    /*
     * SWORD sends the API key through the user name of basic auth. The other APIs use the X-Dataverse-key.
     */
    val hs = if (!sendApiTokenViaBasicAuth) headers + (HEADER_X_DATAVERSE_KEY -> apiToken)
             else headers
    val credentials = if (sendApiTokenViaBasicAuth) Option(apiToken, "")
                      else Option.empty

    // TODO: Refactor request 1,2,3 stuff
    val request = {
      if (body == null) Http(uri.toASCIIString)
      else Http(uri.toASCIIString).postData(body)
    }.method(method)
      .headers(hs)
      .params(params)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
    val request2 = credentials
      .map { case (u, p) => request.auth(u, p) }
      .getOrElse(request)
    val request3 = unblockKey
      .map {  k => request2.param("unblock-key", k)}
      .getOrElse(request2)
    val response = request3.asBytes
    if (response.code >= 200 && response.code < 300) DataverseResponse(response)
    else throw DataverseException(response.code, new String(response.body, StandardCharsets.UTF_8), response)
  }
}