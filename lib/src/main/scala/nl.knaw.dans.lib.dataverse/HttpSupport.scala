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
import scalaj.http.{ Http, HttpRequest, HttpResponse, MultiPart, MultiPartConnectFunc }

import scala.collection.mutable
import scala.util.Try

/**
 * Functions supporting HTTP interaction with Dataverse.
 */
private[dataverse] trait HttpSupport extends DebugEnhancedLogging {
  private val HEADER_CONTENT_TYPE = "Content-Type"
  private val HEADER_X_DATAVERSE_KEY = "X-Dataverse-key"

  private val PARAM_UNBLOCK_KEY = "unblock-key"

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
  protected val builtinUserKey: Option[String]
  protected val apiPrefix: String
  protected val apiVersion: Option[String]
  protected val lockedRetryTimes: Int
  protected val lockedRetryInterval: Int

  /**
   * Posts a multi-part message with an optional file and optional JSON metadata part. Probably at least one is required, but since this
   * is an internal function, this is not validated.
   *
   * @param subPath         subpath to post to
   * @param optFile         the optional file
   * @param optJsonMetadata the optional metadata
   * @param headers         extra headers
   * @param params          extra query parameters
   * @tparam D the payload type for the DataverseResponse
   * @return a DataverseResponse
   */
  protected def postFile[D: Manifest](subPath: String,
                                      optFile: Option[File],
                                      optJsonMetadata: Option[String] = Option.empty,
                                      headers: Map[String, String] = Map.empty,
                                      params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    trace(subPath, optFile, optJsonMetadata, headers, params)

    /*
     * Create the parts for file and metadata respectively
     */
    val partsBuffer = mutable.ListBuffer[MultiPart]()
    optFile.foreach(f => partsBuffer.append(MultiPart(name = "file", filename = f.name, mime = MEDIA_TYPE_OCTET_STREAM, new FileInputStream(f.pathAsString), f.size, lenWritten => {})))
    optJsonMetadata.foreach(md => partsBuffer.append(MultiPart(data = md.getBytes(StandardCharsets.UTF_8), name = "jsonData", filename = "jsonData", mime = MEDIA_TYPE_JSON)))

    for {
      uri <- createUri(Option(subPath))
      response <- postMulti[D](uri, partsBuffer.toList, headers = headers, params = params)
    } yield response
  }

  protected def get[D: Manifest](subPath: String = null,
                                 headers: Map[String, String] = Map.empty,
                                 params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    trace(subPath)
    for {
      uri <- createUri(Option(subPath))
      response <- bodylessRequest[D](METHOD_GET, uri, headers, params)
    } yield response
  }

  protected def postJson[D: Manifest](subPath: String = null,
                                      body: String = null,
                                      headers: Map[String, String] = Map.empty,
                                      params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    trace(subPath, body)
    for {
      uri <- createUri(Option(subPath))
      response <- postString[D](uri, body, headers ++ Map(HEADER_CONTENT_TYPE -> MEDIA_TYPE_JSON), params)
    } yield response
  }

  protected def postText[D: Manifest](subPath: String = null,
                                      body: String = null,
                                      headers: Map[String, String] = Map.empty,
                                      params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- postString[D](uri, body, headers ++ Map(HEADER_CONTENT_TYPE -> MEDIA_TYPE_TEXT), params)
    } yield response
  }

  protected def put[D: Manifest](subPath: String = null,
                                 body: String = null,
                                 headers: Map[String, String] = Map.empty,
                                 params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- putString[D](uri, body, headers, params)
    } yield response
  }

  protected def deletePath[D: Manifest](subPath: String = null, headers: Map[String, String] = Map.empty, params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    for {
      uri <- createUri(Option(subPath))
      response <- bodylessRequest[D](METHOD_DELETE, uri, headers, params)
    } yield response
  }

  /*
   * Private helper functions
   */

  private def createUri(subPath: Option[String]): Try[URI] = Try {
    baseUrl resolve new URI(s"${ apiPrefix }/${ apiVersion.map(version => s"v$version/").getOrElse("") }${ subPath.getOrElse("") }")
  }

  // Spiritual request
  private def bodylessRequest[D: Manifest](method: String,
                                           uri: URI,
                                           headers: Map[String, String] = Map.empty,
                                           params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    dispatchHttp(Http(uri.toASCIIString).method(method), headers, params)
  }

  private def putString[D: Manifest](uri: URI,
                                     body: String = null,
                                     headers: Map[String, String] = Map.empty,
                                     params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    dispatchHttp(Http(uri.toASCIIString).put(body), headers, params)
  }

  private def postString[D: Manifest](uri: URI,
                                      body: String = null,
                                      headers: Map[String, String] = Map.empty,
                                      params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    dispatchHttp(Http(uri.toASCIIString).postData(body), headers, params)
  }

  private def postMulti[D: Manifest](uri: URI,
                                     parts: List[MultiPart],
                                     headers: Map[String, String] = Map.empty,
                                     params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = {
    // Not using postMulti because it installs a UrlBuilder that ignore the query params, which
    // causes persistentId not to come through. See source of Http.
    dispatchHttp(Http(uri.toASCIIString).copy(
      method = METHOD_POST,
      connectFunc = MultiPartConnectFunc(parts)),
      headers,
      params)
  }

  private def dispatchHttp[D: Manifest](baseRequest: HttpRequest,
                                        headers: Map[String, String] = Map.empty,
                                        params: Map[String, String] = Map.empty): Try[DataverseResponse[D]] = Try {
    trace(headers, params)
    val optBasicAuthCredentials = maybeBasicAuthCredentials()
    val headersPlusMaybeApiKey = maybeIncludeApiKey(headers)
    val paramsPlusMaybeUnblockKey = maybeIncludeUnblockKey(params)

    val request = baseRequest
      .headers(headersPlusMaybeApiKey)
      .params(paramsPlusMaybeUnblockKey)
      .timeout(connTimeoutMs = connectionTimeout, readTimeoutMs = readTimeout)
    val completeRequest = optBasicAuthCredentials
      .map { case (u, p) => request.auth(u, p) }
      .getOrElse(request)
    val response = getResponse(completeRequest)
    if (response.code >= 200 && response.code < 300) DataverseResponse(response)
    else throw DataverseException(response.code, new String(response.body, StandardCharsets.UTF_8), response)
  }

  private def getResponse(request: HttpRequest): HttpResponse[Array[Byte]] = {
    var retries = 0
    var response = request.asBytes
    while (retries < lockedRetryTimes && mustRetry(response)) {
      Thread.sleep(lockedRetryInterval)
      response = request.asBytes
      retries += 1
    }
    response
  }

  private def mustRetry(response: HttpResponse[Array[Byte]]): Boolean = {
    val messageBody = new String(response.body, StandardCharsets.UTF_8)
    (response.code == 403 &&
      (messageBody.contains("This dataset is locked") || messageBody.contains("Dataset cannot be edited due to dataset lock"))
      || (response.code == 400 &&
      messageBody.contains("Failed to add file to dataset")))
  }
  /**
   * Normally the API-key is sent in a header
   *
   * @param headers headers to be augmented
   * @return
   */
  private def maybeIncludeApiKey(headers: Map[String, String]): Map[String, String] = {
    if (!sendApiTokenViaBasicAuth) headers + (HEADER_X_DATAVERSE_KEY -> apiToken)
    else headers
  }

  /**
   * SWORD requires the API-key to be sent through the user field of basic auth
   *
   * @return
   */
  private def maybeBasicAuthCredentials(): Option[(String, String)] = {
    if (sendApiTokenViaBasicAuth) Option(apiToken, "")
    else Option.empty
  }

  /**
   * To use the Admin API from non-localhost you need an unlock-key
   *
   * @see [[https://guides.dataverse.org/en/latest/installation/config.html#blockedapikey]]
   * @param params params to be augmented
   * @return
   */
  private def maybeIncludeUnblockKey(params: Map[String, String]): Map[String, String] = {
    (params.toList ::: unblockKey.map(k => List((PARAM_UNBLOCK_KEY -> k))).getOrElse(Nil)).toMap
  }
}