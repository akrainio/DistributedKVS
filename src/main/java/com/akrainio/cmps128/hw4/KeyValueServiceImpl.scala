package com.akrainio.cmps128.hw4

import java.util.concurrent.ConcurrentHashMap
import javax.ws.rs.core.Response

import com.akrainio.cmps128.hw4.KeyValueService._

import scala.collection._
import scala.collection.convert.decorateAsScala._

//noinspection TypeAnnotation
class KeyValueServiceImpl(val ipport: String) extends KeyValueService{

  val map: concurrent.Map[String, String] = new ConcurrentHashMap[String, String].asScala

  def get(key: String) = map.get(key) match {
    case Some(value) => jsonResp(200)(
      "msg"   -> "success",
      "value" -> value,
      "owner" -> ipport
    )
    case None => jsonResp(404)(
      "msg"   -> "error",
      "error" -> "key does not exist",
      "owner" -> ipport
    )
  }

  def put(key: String, value: String) = map.put(key, value) match {
    case Some(_) => jsonResp(200)(
      "replaced" -> 1,
      "msg" -> "success",
      "owner" -> ipport
    )
    case None => jsonResp(201)(
      "replaced" -> 0,
      "msg" -> "success",
      "owner" -> ipport
    )
  }

  def del(key: String) = map.remove(key) match {
    case Some(_) => jsonResp(200)(
      "msg" -> "success",
      "owner" -> ipport
    )
    case None => jsonResp(404)(
      "msg" -> "error",
      "error" -> "key does not exist",
      "owner" -> ipport
    )
  }

  def rebalance(NodeIndex: Int)(f: Int => Int): List[(Int, (String, String))] =  {
    var evicted: List[(Int, (String, String))] = List()
    for ((key, value) <- map) {
      f(key.hashCode) match {
        case NodeIndex =>
        case x =>
          del(key)
          evicted = evicted :+ (x, (key, value))
      }
    }
    evicted
  }

  def updateView(updateType: String, ipport: String): Response = jsonResp(403)(
    "error" -> "method 'updateView' not supported here"
  )

  def internalUpdate(newView: String): Response = jsonResp(403)(
    "error" -> "method 'internalUpdate' not supported here"
  )

  def rebal(): Response = jsonResp(403)(
    "error" -> "method 'rebal' not supported here"
  )
}
