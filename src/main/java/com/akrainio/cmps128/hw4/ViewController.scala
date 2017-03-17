package com.akrainio.cmps128.hw4

import scala.Int.int2float

class ViewController(val ThisIpport: String, val k: Int, viewString: String) {

  var view: List[List[KeyValueService]] = buildView(viewString)

  private val kvsImpl = new KeyValueServiceImpl(ThisIpport)

  // Called when this node receives a view update (not internal update!)
  def addNode(): Unit = ???

  // Called when this node receives a view update (not internal update!)
  def delNode(): Unit = ???

  // Returns (partition index, node index)
  private def findSelf(): (Int, Int) = ???

  private def buildView(newView: String): List[List[KeyValueService]] = {
    val builtView = for {
      partition <- newView.split("\\|").toList
    } yield {
      for {
        node <- partition.split(",").toList
      } yield {
        node match {
          case ThisIpport => kvsImpl
          case _ => new KeyValueServiceProxy(node)
        }
      }
    }
    builtView
  }

  private def rebuildView(newView: String): Unit = {
    view = buildView(newView)
    findSelf()
  }

  private def getPartition(hash: Int): Int = {
    val ranges = 2^7
    val hashLoc: Float = math.abs(hash) % ranges
    val sizeOfSegments = int2float(ranges) / view.length
    math.floor(hashLoc / sizeOfSegments).toInt
  }

  override def toString: String = {
    val builder = new StringBuilder
    for ((partition, i) <- view.zipWithIndex) {
      if (i != 0) builder.append("|")
      for ((node, j) <- partition.zipWithIndex) {
        if (j != 0) builder.append("," + node.ThisIpport)
        else builder.append(node.ThisIpport)
      }
    }
    builder.toString
  }

}