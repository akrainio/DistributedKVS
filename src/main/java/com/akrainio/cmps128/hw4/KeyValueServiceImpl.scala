package com.akrainio.cmps128.hw4

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger.getLogger
import javax.ws.rs.core.Response

import com.akrainio.cmps128.hw4.KeyValueService._

import scala.collection._
import scala.collection.convert.decorateAsScala._

//noinspection TypeAnnotation
class KeyValueServiceImpl(val ThisIpport: String) extends KeyValueService{

  private val Logger = getLogger(classOf[KeyValueServiceJersey].getName + ThisIpport)

  var clock = new Clock(ThisIpport)

  var pId = 0

  val map: concurrent.Map[String, String] = new ConcurrentHashMap[String, String].asScala

  override def get(payload: String, key: String) = map.get(key) match {
    case Some(value) =>
      if (payload != "") {
        clock.combine(new Clock(payload))
      }
      clock.increment(ThisIpport)
      jsonResp(200)(
      "msg"   -> "success",
      "value" -> value,
      "partition_id" -> pId,
      "causal_payload" -> clock.pack,
      "timestamp" -> System.currentTimeMillis()
    )
    case None => jsonResp(404)(
      "msg"   -> "error",
      "error" -> "key does not exist"
    )
  }

  override def put(payload: String, key: String, value: String) = {
    map.put(key, value)
    if (payload != "") {
      clock.combine(new Clock(payload))
    }
    clock.increment(ThisIpport)
    jsonResp(200)(
      "msg" -> "success",
      "partition_id" -> pId,
      "causal_payload" -> clock.pack,
      "timestamp" -> System.currentTimeMillis()
    )
  }

  // Only gets called by rebalance()
  def del(key: String): Response = map.remove(key) match {
    case Some(_) => jsonResp(200)(
      "msg" -> "success",
      "owner" -> ThisIpport
    )
    case None => jsonResp(404)(
      "msg" -> "error",
      "error" -> "key does not exist"
    )
  }

  // Deletes unwanted pairs from map, returns them as a list
  def rebalance(PartIndex: Int)(f: String => Int): List[(Int, (String, String))] =  {
    var evicted: List[(Int, (String, String))] = List()
    for ((key, value) <- map) {
      val updatedPartitionIndex = f(key)
      updatedPartitionIndex match {
        case PartIndex =>
        case x =>
          del(key)
          evicted = evicted :+ (x, (key, value))
      }
    }
    evicted
  }

  def getAllKeys: (String, String) = {
    (clock.pack, pack)
  }

  def pack: String = {
    val stringBuilder = new StringBuilder
    for ((k, v) <- map) {
      stringBuilder.append(s"!$k,$v")
    }
    stringBuilder.deleteCharAt(0)
    stringBuilder.toString
  }

  override def updateView(updateType: String, ipport: String) = {
    throw new IOException(s"method 'updateView' not supported in KVSImpl. Params: [updateType = $updateType, ipport = $ipport]")
  }

  override def internalUpdate(newView: String) = {
    throw new IOException(s"method 'internalUpdate' not supported in KVSImpl. Params: [newView = $newView]")
  }

  override def rebal() = {
    throw new IOException(s"method 'rebal' not supported in KVSImpl.")
  }

  override def putInternal(internal: String, key: String, value: String) = {
    throw new IOException(s"method 'putInternal' not supported in KVSImpl. Params: [key = $key, value = $value]")
  }

}
