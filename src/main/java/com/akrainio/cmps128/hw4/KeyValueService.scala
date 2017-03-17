package com.akrainio.cmps128.hw4

import javax.ws.rs.core.{MultivaluedMap, Response}

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap
import play.api.libs.json.Json

import scala.collection._

trait KeyValueService {

  val ThisIpport: String

  def get(key: String): Response

  def put(key: String, value: String): Response

  def del(key: String): Response

  def updateView(updateType: String, ipport: String): Response

  def internalUpdate(newView: String): Response

  def rebal(): Response

}

object KeyValueService {

  def toJson(keyVals: Seq[(String, Json.JsValueWrapper)]): String = {
    Json.prettyPrint(Json.obj(keyVals: _*))
  }

  def jsonResp(status: Int)(keyVals: (String, Json.JsValueWrapper)*): Response = {
    Response.status(status).entity(toJson(keyVals)).build()
  }

  def toMultiValuedMap(key: String, value: String): MultivaluedMap[String, String] = {
    val stringMap = new MultivaluedStringMap()
    stringMap.add(key, value)
    stringMap
  }

}