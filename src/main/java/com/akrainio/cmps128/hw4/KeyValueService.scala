package com.akrainio.cmps128.hw4

import javax.ws.rs.core.{MultivaluedMap, Response}

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap
import play.api.libs.json.Json

import scala.collection._

trait KeyValueService {

  val ThisIpport: String

  def get(payload: String, key: String): Response

  def put(payload: String, key: String, value: String): Response

  def putInternal(internal: String, payload: String, key: String, value: String): Response

  def updateView(updateType: String, ipport: String): Response

  def internalUpdate(newView: String): Unit

  def gossip(payload: String, kvs: String, sender: String, timeStamp: String): Unit

  def gossipAck(payload: String, kvs: String): Unit

  def rebal(): Response

}

object KeyValueService {

  def toJson(keyVals: Seq[(String, Json.JsValueWrapper)]): String = {
    Json.prettyPrint(Json.obj(keyVals: _*))
  }

  def jsonResp(status: Int)(keyVals: (String, Json.JsValueWrapper)*): Response = {
    Response.status(status).entity(toJson(keyVals)).build()
  }

  def toMultiValuedMap(pairs: Seq[(String, String)]): MultivaluedMap[String, String] = {
    val stringMap = new MultivaluedStringMap()
    for ((key, value) <- pairs) {
      stringMap.add(key, value)
    }
    stringMap
  }

}