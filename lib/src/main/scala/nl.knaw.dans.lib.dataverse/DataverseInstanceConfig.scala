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

import java.net.URI

case class DataverseInstanceConfig(baseUrl: URI,
                                   apiToken: String,
                                   unblockKey: Option[String] = None,
                                   builtinUserKey: Option[String] = None,
                                   connectionTimeout: Int = 5000,
                                   readTimeout: Int = 300000,
                                   apiVersion: String = "1",
                                   lockedRetryTimes: Int = 10,
                                   lockedRetryInterval: Int = 500
                                  )
