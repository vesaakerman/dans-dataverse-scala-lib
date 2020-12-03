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
package nl.knaw.dans.lib.dataverse.model

/**
 * A JSON response message sent by Dataverse. The message may contain payload object (for example dataset information when this was requested).
 * This can be retrieved from the `data` field. It may also contain an informational message. That can be retrieved from the `message` field.
 * Sometimes the informational message is packaged in the `data` field. In these cases the payload type will be [[DataMessage]].
 *
 * @param status  current status, usually OK or ERROR
 * @param data    payload of the response; in case of an ERROR: the error message
 * @param message a message from Dataverse, maybe informational or a warning
 * @tparam D the expected type of payload data
 * */
case class DataverseMessage[D](status: String, data: Option[D], message: Option[String])

