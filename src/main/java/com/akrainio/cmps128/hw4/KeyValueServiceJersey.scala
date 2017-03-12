package com.akrainio.cmps128.hw4

import javax.inject.Singleton
import javax.ws.rs.{FormParam, _}
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.{MediaType, Response}

import com.akrainio.cmps128.hw4.KeyValueService._
import com.akrainio.cmps128.hw4.KeyValueServiceJersey._

import scala.Int.int2float


//noinspection TypeAnnotation
@Singleton
@Path("/kvs/")
class KeyValueServiceJersey extends KeyValueService {

  val ThisIpport = System.getenv("IPPORT")

  val kvsImpl = new KeyValueServiceImpl(ThisIpport)

  var partionIndex = 0

  var NodeIndex = 0

  var k = sys.env.get("K") match {
    case Some(x) => x
    case None => 0
  }

  var view: Vector[Vector[(KeyValueService, String)]] = sys.env.get("VIEW") match {
    case Some(x) => makeView(x)
    case None => Vector(Vector[(KeyValueService, String)](kvsImpl, ThisIpport))
  }

//  var view: Vector[Vector[(KeyValueService, String)]] = sys.env.get("VIEW") match {
//    case Some(x) => for (p <- x.split(",").toVector.zipWithIndex; i = 0) {
//
//    }
//  }

  @GET
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  override def get(@PathParam("key") key: String) = validateKey(key) {
    getDelegate(key).get(key)
  }

  @DELETE
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  override def del(@PathParam("key") key: String) = validateKey(key) {
    getDelegate(key).del(key)
  }

  @PUT
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  override def put(@PathParam("key") key: String, @FormParam("val") value: String) = validateKey(key) {
    if (value == null) {
      jsonResp(403)(
        "msg" -> "error",
        "error" -> "value cannot be null"
      )
    } else {
      getDelegate(key).put(key, value)
    }
  }

  @PUT
  @Path("view_update")
  @Produces(Array(APPLICATION_JSON))
  override def updateView(@QueryParam("type") updateType: String, @FormParam("ip_port") ipport: String) = updateType match {
    case "add" =>
      addNode(ipport)
      jsonResp(200)("msg" -> "success")

    case "remove" =>
      delNode(ipport)
      jsonResp(200)("msg" -> "success")

    case _ => jsonResp(403)(
      "msg" -> "error",
      "error" -> "update type not specified"
    )
  }

  @PUT
  @Path("internal_update")
  @Produces(Array(APPLICATION_JSON))
  override def internalUpdate(@FormParam("new_view") newView: String) = {
    view = makeView(newView)
    jsonResp(200)("msg" -> "success")
  }

  @POST
  @Path("rebalance")
  @Produces(Array(APPLICATION_JSON))
  override def rebal() = {
    rebalance()
    jsonResp(200)("msg" -> "success")
  }

  /**
    * Additional put method for cases when value is not provided
    *
    * @param key doesn't matter
    * @return Always returns error response
    */
  @PUT
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  @Consumes(Array(MediaType.TEXT_PLAIN))
  def put(@PathParam("key") key: String) = validateKey(key) {
    jsonResp(403)(
      "msg" -> "error",
      "error" -> "value is missing"
    )
  }

  private def addNode(ipport: String): Unit = {
    view = view :+ (new KeyValueServiceProxy(ipport), ipport)
    for (n <- view) n._1.internalUpdate(viewToString(view))
    for (n <- view) n._1.rebal()
    rebalance()
  }

  private def delNode(ipport: String): Unit = {
    val oldView = view
    view = view.filterNot((p: (KeyValueService, String)) => p._2 == ipport)
    for (n <- oldView) n._1.internalUpdate(viewToString(view))
    fixNodeIndex()
    for (n <- oldView) n._1.rebal()
    rebalance()
  }

  private def rebalance(): Unit = {
    fixNodeIndex()
    val evicted = kvsImpl.rebalance(NodeIndex)(getNode)
    for (e: (Int, (String, String)) <- evicted) {
      view(e._1)._1.put(e._2._1, e._2._2)
    }
  }

  private def makeView(newView: String): Vector[Vector[(KeyValueService, String)]] = {
    val view: Vector[Vector[(KeyValueService, String)]] = newView.split("\\|").map { (s: String) =>
      s.split(",").toVector.zipWithIndex.map {
        case (x, i) => x match {
          case ThisIpport => (kvsImpl, ThisIpport)
          case _ => (new KeyValueServiceProxy(s), s)
        }
      }
    }.toVector
    fixNodeIndex()
    view
  }

  private val getNode = (hash: Int) => {
    val ranges = 100
    val hashLoc: Float = math.abs(hash) % ranges
    val sizeOfSegments = int2float(ranges) / view.length
    math.floor(hashLoc / sizeOfSegments).toInt
  }

  private def getDelegate(key: String): KeyValueService = view(getNode(key.hashCode))._1

  private def fixNodeIndex(): Unit = {
    NodeIndex = -1
    for (n <- view.zipWithIndex) if (n._1._2 == ThisIpport) NodeIndex = n._2
  }

}

object KeyValueServiceJersey {

  private def viewToString(view: Vector[(KeyValueService, String)]): String = {
    val builder = new StringBuilder
    for(v <- view) builder.append("," + v._2)
    builder.deleteCharAt(0)
    builder.toString
  }

  private def validateKey(key: String)(f: => Response): Response = {
    if (key.length > 250) jsonResp(403)(
      "msg"   -> "error",
      "error" -> "key too long"
    ) else {
      f
    }
  }

}