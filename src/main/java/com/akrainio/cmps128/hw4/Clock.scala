package com.akrainio.cmps128.hw4

import scala.collection.{Seq, mutable}

class Clock(val ThisIpport: String) {

  var clock: mutable.Map[String, Int] = mutable.Map(ThisIpport -> 0)

  def increment(ipport: String): Unit = {
    clock(ipport) = clock.getOrElse(ipport, 0) + 1
  }

  def getCausality(other: Clock): Int = {
    val diff = (clock.keySet -- other.clock.keySet) ++ (other.clock.keySet -- clock.keySet)
    if (diff.isEmpty) {
      var lessThanFlag = false
      var greaterThanFlag = false
      for ((k, v) <- clock) {
        if (v > other.clock(k)) {
          if (lessThanFlag) return 0
          else greaterThanFlag = true
        }
        else {
          if (greaterThanFlag) return 0
          else lessThanFlag = true
        }
      }
      if (greaterThanFlag) 1
      else -1
    }
    else 0
  }

  def combine(other: Clock): Clock = {
    var map: mutable.Map[String, Int] = mutable.Map.empty[String, Int]
    val inThis = clock.keySet -- other.clock.keySet
    val inOther = other.clock.keySet -- clock.keySet
    val intersect = clock.keySet ++ other.clock.keySet -- inThis -- inOther
    for (key <- inThis) map += (key -> clock(key))
    for (key <- inOther) map += (key -> other.clock(key))
    for (key <- intersect) {
      val greaterVal = math.max(clock(key), other.clock(key))
      map += (key -> greaterVal)
    }
    val result = new Clock(ThisIpport)
    result.clock = map
    result
  }

  def pack: String = {
    val stringBuilder = new StringBuilder
    for ((k, v) <- clock) {
      stringBuilder.append(s"!$k,$v")
    }
    stringBuilder.deleteCharAt(0)
    stringBuilder.insert(0, ThisIpport + ">")
    stringBuilder.toString
  }

}

object Clock {

  def unPack(s: String): Clock = {
    var map: mutable.Map[String, Int] = mutable.Map.empty[String, Int]
    val pair = s.split(">")
    val ipport = pair(0)
    val counters = pair(1)
    for (p <- counters.split("!")) {
      val k = p.split(",")(0)
      val v = p.split(",")(1).toInt
      map += (k -> v)
    }
    val result = new Clock(ipport)
    result.clock = map
    result
  }

}