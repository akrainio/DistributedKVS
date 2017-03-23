package com.akrainio.cmps128.hw4

import java.util.TimerTask
import java.util.logging.Logger.getLogger

import scala.Int.int2float

class ViewController(val ThisIpport: String, val k: Int, viewString: String) {

  private val Logger = getLogger(classOf[KeyValueServiceJersey].getName + ThisIpport)

  val kvsImpl = new KeyValueServiceImpl(ThisIpport)

  var view: List[List[KeyValueService]] = List(List(kvsImpl))
  initView(viewString)

  // Called when this node receives a view update (not internal update!)
  def addNode(ipport: String): Unit = {
    val stringBuilder = new StringBuilder
    for ((s, i) <- packToString.split(",").zipWithIndex) {
      if (i == 0) stringBuilder.append(s)
      else stringBuilder.append(",").append(s)
    }
    val newView = stringBuilder.append(",").append(ipport).toString
    initView(newView)
    internalUpdateOthers(None)
    rebalanceAll(None)
  }

  // Called when this node receives a view update (not internal update!)
  def delNode(ipport: String): Unit = {
    val (delRepl, _, _) = findNode(ipport)
    val stringBuilder = new StringBuilder
    for ((s, i) <- packToString.split(",").filterNot(_ == ipport).zipWithIndex) {
      if (i == 0) stringBuilder.append(s)
      else stringBuilder.append(",").append(s)
    }
    val newView = stringBuilder.toString()
    initView(newView)
    internalUpdateOthers(Some(delRepl))
    rebalanceAll(Some(delRepl))
  }

  // Sends updated view to all other nodes
  def internalUpdateOthers(extraNode: Option[KeyValueService]): Unit = {
    val viewString = packToString
    runOnAllOtherNodes(extraNode)(_.internalUpdate(viewString))
  }

  def rebalanceSelf(): Unit = {
    val curPartition = findSelf._2
    val evicted = kvsImpl.rebalance(curPartition)(getPartition)
    for (e: (Int, (String, String)) <- evicted) {
      val (newPart, (key, value)) = e
      view(newPart).foreach((kvs: KeyValueService) => kvs.put("", key, value))
    }
  }

  def rebalanceAll(extraNode: Option[KeyValueService]): Unit = {
    runOnAllOtherNodes(extraNode)(_.rebal())
    rebalanceSelf()
  }

  def getRepls(key: String): List[KeyValueService] = {
    view(getPartition(key))
  }

  def getOtherRepls(key: String): List[KeyValueService] = {
    getRepls(key).filterNot(_.ThisIpport == ThisIpport)
  }

  def keyBelongs(key: String): Boolean = {
    getPartition(key) == findSelf._2
  }

//  def buildView(newView: String): List[List[KeyValueService]] = {
//    val builtView = for {
//      part <- newView.split("\\|").toList
//    } yield {
//      for {
//        repl <- part.split(",").toList
//      } yield {
//        repl match {
//          case ThisIpport => kvsImpl
//          case _ => new KeyValueServiceProxy(repl)
//        }
//      }
//    }
//    builtView
//  }

  def setView(newView: String): Unit = {
    initView(newView)
  }

  def initView(newView: String): Unit = {
    if (newView == "") view = List(List(kvsImpl))
    else {
      val nodes = newView.split(",").toList
      val PartitionCount = math.ceil(nodes.length.toDouble / k).toInt - 1
      view = List.tabulate(PartitionCount + 1) { i =>
        val l: Int = i match {
          case PartitionCount =>
            nodes.length % k match {
              case 0 => k
              case x => x
            }
          case _ => k
        }
        List.tabulate(l) { j =>
          nodes(i * k + j) match {
            case ThisIpport => kvsImpl
            case n => new KeyValueServiceProxy(n)
          }
        }
      }
    }
    val (_, pId, _) = findSelf
    kvsImpl.pId = pId
  }

  private def runOnAllNodes(extraNode: Option[KeyValueService])(f: KeyValueService => Unit): Unit = {
    for {
      part <- view
      repl <- part
    } f(repl)
    for (kvs <- extraNode) f(kvs)
  }

  private def runOnAllOtherNodes(extraNode: Option[KeyValueService])(f: KeyValueService => Unit): Unit = {
    for {
      part <- view
      repl <- part if repl.ThisIpport != ThisIpport
    } f(repl)
    for (kvs <- extraNode) f(kvs)
  }

//  private def buildViewSamePartitions(ipport: String): Unit = {
//    view = for {
//      part <- view
//    } yield {
//      for {
//        repl <- part if repl.ThisIpport != ipport
//      } yield repl
//    }
//  }
//
//  private def buildViewMoveFromLastPartition(ipport: String): Unit = {
//    view = for {
//      part <- view if !(part == view.last && part.length == 1)
//    } yield {
//      if (part == view.last) {
//        // need to remove node
//        if (part.length > 1) part.take(part.length - 1)
//        else part
//      } else {
//        for {
//          repl <- part
//        } yield {
//          if (repl.ThisIpport == ipport) {
//            view.last.last
//          } else {
//            repl
//          }
//        }
//      }
//    }
//  }
//
//  private def buildViewWithoutLastPartition(ipport: String): Unit = {
//    view = for {
//      part <- view if part != view.last
//    } yield part
//  }

  // Returns (kvs, partition index, replica index)
  private def findNode(ipport: String): (KeyValueService, Int, Int) = {
    val self = for {
      (part, i) <- view.zipWithIndex
      (repl, j) <- part.zipWithIndex if repl.ThisIpport == ipport
    } yield {
      (repl,i,j)
    }
    if (self.isEmpty) (null, -1, -1)
    else self.head
  }

  private def findSelf: (KeyValueService, Int, Int) = {
    findNode(ThisIpport)
  }

  def getPartition(hash: String): Int = {
    val ranges = 2^7
    val hashLoc: Float = math.abs(hash.hashCode) % ranges
    val sizeOfSegments = int2float(ranges) / view.length
    math.floor(hashLoc / sizeOfSegments).toInt
  }

  def getPartitionIDs: List[Int] = {
    List.range(0, view.length)
  }

  def getPartitionMembers: List[String] = {
    val (_, pIndex, _) = findSelf
    for (repl <- view(pIndex)) yield repl.ThisIpport
  }

  def getPartitionId: Int = {
    val (_, pIndex, _) = findSelf
    pIndex
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

  def packToString: String = {
    val builder = new StringBuilder()
    for ((partition, i) <- view.zipWithIndex) {
      for ((node, j) <- partition.zipWithIndex) {
        if (i == 0 && j == 0) builder.append(node.ThisIpport)
        else builder.append("," + node.ThisIpport)
      }
    }
    builder.toString
  }
//
//  override def run() = {
//    val repls = getOtherRepls(ThisIpport)
//    for (repl: KeyValueServiceProxy <- repls) {
//
//    }
//  }

}
