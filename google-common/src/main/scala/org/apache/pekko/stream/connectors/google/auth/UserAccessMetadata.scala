/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.google.auth

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.HttpMethods.POST
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ FormData, HttpRequest }
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer

import java.time.Clock
import scala.concurrent.Future

@InternalApi
private[auth] object UserAccessMetadata {
  private val tokenUrl = "https://accounts.google.com/o/oauth2/token"
  private val `Metadata-Flavor` = RawHeader("Metadata-Flavor", "Google")

  private def tokenRequest(clientId: String, clientSecret: String, refreshToken: String): HttpRequest = {
    val entity = FormData(
      "client_id" -> clientId,
      "client_secret" -> clientSecret,
      "refresh_token" -> refreshToken,
      "grant_type" -> "refresh_token").toEntity
    HttpRequest(method = POST, uri = tokenUrl, entity = entity).addHeader(`Metadata-Flavor`)
  }

  def getAccessToken(clientId: String, clientSecret: String, refreshToken: String)(
      implicit mat: Materializer,
      clock: Clock): Future[AccessToken] = {
    import SprayJsonSupport._
    import mat.executionContext
    implicit val system = mat.system
    for {
      response <- Http().singleRequest(tokenRequest(clientId, clientSecret, refreshToken))
      token <- Unmarshal(response.entity).to[AccessToken]
    } yield token
  }
}