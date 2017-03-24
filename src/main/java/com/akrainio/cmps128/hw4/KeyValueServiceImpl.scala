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

  var timeStamp = System.currentTimeMillis()

  var pId = 0

  val map: concurrent.Map[String, String] = new ConcurrentHashMap[String, String].asScala

  override def get(payload: String, key: String) = map.get(key) match {
    case Some(value) =>
      noteEvent(payload)
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
    noteEvent(payload)
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

  def getClock: String = {
    clock.pack
  }

  def pack: String = {
    if (map.isEmpty) ""
    else {
      val stringBuilder = new StringBuilder
      for ((k, v) <- map) {
        stringBuilder.append(s"!$k,$v")
      }
      stringBuilder.deleteCharAt(0)
      stringBuilder.toString
    }
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

  def setKvs(payload: String, kvs: String): Unit = {
    kvs match {
      case "youWon" =>
      case "" =>
        noteEventQuiet(payload)
        map.clear()
      case _ =>
        noteEventQuiet(payload)
        map.clear()
        for (p <- kvs.split("!")) {
          val k = p.split(",")(0)
          val v = p.split(",")(1)
          map += (k -> v)
        }
    }
  }

  override def putInternal(internal: String, payload: String, key: String, value: String) = {
    throw new IOException(s"method 'putInternal' not supported in KVSImpl. Params: [key = $key, value = $value]")
  }

  override def gossip(payload: String, kvs: String, sender: String, timeStamp: String) = ???
  override def gossipAck(payload: String, kvs: String) = ???

  def noteEvent(payload: String): Unit = {
    if (payload != null) {
      clock = clock.combine(Clock.unPack(payload))
    }
    clock.increment(ThisIpport)
    timeStamp = System.currentTimeMillis()
  }

  def noteEventQuiet(payload: String): Unit = {
    if (payload != null) {
      clock = clock.combine(Clock.unPack(payload))
    }
    timeStamp = System.currentTimeMillis()
  }

}
