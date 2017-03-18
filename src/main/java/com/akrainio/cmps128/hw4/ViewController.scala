package com.akrainio.cmps128.hw4

import scala.Int.int2float

class ViewController(val ThisIpport: String, val k: Int, viewString: String) {

  var view: List[List[KeyValueService]] = buildView(viewString)

  private val kvsImpl = new KeyValueServiceImpl(ThisIpport)

  // Called when this node receives a view update (not internal update!)
  def addNode(ipport: String): Unit = {
    if (view.tail.length < k) {
      view = for {
        part <- view
        //repl <- part
      } yield {
        if (part == view.tail) part :+ new KeyValueServiceProxy(ipport)
        else part
      }
    } else {
      view = view :+ List(new KeyValueServiceProxy(ipport))
    }
  }

  // Called when this node receives a view update (not internal update!)
  def delNode(ipport: String): Unit = {
    if (ipport == ThisIpport) {
      ???
    } else {
      val (pIndex, rIndex) = findNode(ipport)
      if (pIndex + 1 == view.length) {
        view = for {
          part <- view
        } yield
          for {
            repl <- part if repl.ThisIpport != ipport
          } yield {
            repl
        }
      } else {
        ???
      }
    }
  }

  // Returns (partition index, replica index)
  private def findNode(ipport: String): (Int, Int) = {
    val self = for {
      (part, i) <- view.zipWithIndex
      (repl, j) <- part.zipWithIndex if repl.ThisIpport == ipport
    } yield (i,j)
    self.head
  }

  private def findSelf(): (Int, Int) = {
    findNode(ThisIpport)
  }

  private def buildView(newView: String): List[List[KeyValueService]] = {
    val builtView = for {
      part <- newView.split("\\|").toList
    } yield {
      for {
        repl <- part.split(",").toList
      } yield {
        repl match {
          case ThisIpport => kvsImpl
          case _ => new KeyValueServiceProxy(repl)
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