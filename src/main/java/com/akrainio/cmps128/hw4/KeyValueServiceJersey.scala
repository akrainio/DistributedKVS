package com.akrainio.cmps128.hw4

import java.io.IOException
import java.util.logging.Logger.getLogger
import javax.inject.Singleton
import javax.ws.rs.{Consumes, FormParam, _}
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import javax.ws.rs.core.{MediaType, Response}

import com.akrainio.cmps128.hw4.KeyValueService._
import com.akrainio.cmps128.hw4.KeyValueServiceJersey._

//noinspection TypeAnnotation
@Singleton
@Path("/kvs/")
class KeyValueServiceJersey extends KeyValueService {

  val ThisIpport: String = sys.env("IPPORT")

  private val Logger = getLogger(classOf[KeyValueServiceJersey].getName + ThisIpport)

  val k: Int = sys.env("K").toInt

  val view: ViewController = sys.env.get("VIEW") match {
    case Some(v) => new ViewController(ThisIpport, k, v)
    case None => new ViewController(ThisIpport, k, "")
  }

  @GET
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  // Client accessible get, can also be called be called by proxy to redirect client request
  override def get(@QueryParam("causal_payload") payload: String,
                   @PathParam("key") key: String): Response = validateKey(key) {
     key match {
      case "get_partition_id" => jsonResp(200)(
        "msg" -> "success",
        "partition_id" -> view.getPartitionId
      )
      case "get_all_partition_ids" => jsonResp(200)(
        "msg" -> "success",
        "partition_id_list" -> view.getPartitionIDs
      )
      case "testGossip" =>
        view.gossip()
        jsonResp(200)(
          "msg" -> "success"
        )
      case _ =>
        if (view.keyBelongs(key)) {
          return view.kvsImpl.get(payload, key)
        }
        sendRequestToPartition(key)(_.get(payload, key))
    }
  }

  @GET
  @Path("get_partition_members")
  def getPartitionId: Response = {
    jsonResp(200)(
      "msg" -> "success",
      "partition_members" -> view.getPartitionMembers
    )
  }

  @PUT
  @Path("gossip")
  @Produces(Array(APPLICATION_JSON))
  override def gossip(@FormParam("causal_payload") payload: String,
                      @FormParam("kvs") kvs: String,
                      @FormParam("sender") sender: String,
                      @FormParam("timeStamp") timeStamp: String) = {
    view.receiveGossip(payload, kvs, sender, timeStamp)
  }

  @PUT
  @Path("gossipAck")
  @Produces(Array(APPLICATION_JSON))
  override def gossipAck(@FormParam("causal_payload") payload: String,
                         @FormParam("kvs") kvs: String) = {
    view.receiveGossipAck(payload, kvs)
  }

  @PUT
  @Path("update_view")
  @Produces(Array(APPLICATION_JSON))
  // Sent by client to inform the addition or removal of a node. Propagated to all nodes as internal_update
  override def updateView(@QueryParam("type") updateType: String,
                          @FormParam("ip_port") ipport: String) = updateType match {
    case "add" =>
      view.addNode(ipport)
      jsonResp(200)(
        "msg" -> "success",
        "partition_id" -> view.getPartitionId,
        "number_of_partitions" -> view.getPartitionIDs.length
      )

    case "remove" =>
      view.delNode(ipport)
      jsonResp(200)(
        "msg" -> "success",
        "number_of_partitions" -> view.getPartitionIDs.length
      )

    case _ => jsonResp(403)(
      "msg" -> "error",
      "error" -> "update type not specified"
    )
  }

  @PUT
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  // Client accessible put, can also be called be called by proxy to redirect client request.
  // Puts into local KVS and propagates putInternal to replicas, redirects client otherwise.
  override def put(@FormParam("causal_payload") payload: String,
                   @PathParam("key") key: String,
                   @FormParam("val") value: String): Response = validateKey(key) {
    if (value == null) jsonResp(403)(
      "msg" -> "error",
      "error" -> "value cannot be null"
    )
    else if (view.keyBelongs(key)) {
      for (repl <- view.getOtherRepls(key).filterNot(_.ThisIpport == ThisIpport)) {
        repl.putInternal("", payload, key, value)
      }
      view.kvsImpl.put(payload, key, value)
    } else {
      val resp = sendRequestToEntirePartition(key)(_.putInternal("", payload, key, value))
      if (resp != null) {
        resp
      } else {
        jsonResp(403)(
          "msg" -> "error",
          "error" -> "All nodes in partition inaccessible"
        )
      }
    }
  }

  @PUT
  @Path("{key}")
  // Non propagating put request, sent by a replica to other replicas
  override def putInternal(@FormParam("internal") internal: String,
                           @FormParam("causal_payload") payload: String,
                           @PathParam("key") key: String,
                           @FormParam("val") value: String) = {
    view.kvsImpl.put(payload, key, value)
  }

  @PUT
  @Path("internal_update")
  @Produces(Array(APPLICATION_JSON))
  // Sent to all nodes when a node gets an updateView request from client. Locally updates view.
  override def internalUpdate(@FormParam("new_view") newView: String) = {
    view.setView(newView)
    jsonResp(200)("msg" -> "success")
  }

  @POST
  @Path("rebalance")
  @Produces(Array(APPLICATION_JSON))
  // Sent by a node that receives a view update after it sends an internal update to all nodes. Rebalances local kvs.
  override def rebal() = {
    view.rebalanceSelf()
    jsonResp(200)("msg" -> "success")
  }

  @PUT
  @Path("{key}")
  @Produces(Array(APPLICATION_JSON))
  @Consumes(Array(MediaType.TEXT_PLAIN))
  // Additional put method for cases when value is not provided
  def put(@PathParam("key") key: String) = validateKey(key) {
    jsonResp(403)(
      "msg" -> "error",
      "error" -> "value is missing"
    )
  }

  // Sends given request to all responding nodes in another partition
  def sendRequestToEntirePartition(key: String)(f: KeyValueService => Response): Response = {
    var response: Response = null
    for (repl <- view.getRepls(key)) {
      try {
        response = f(repl)
      } catch {
        case _ :IOException =>
      }
    }
    response
  }

  // Sends given request to first responding node in another partition
  def sendRequestToPartition(key: String)(f: KeyValueService => Response): Response = {
    for (repl <- view.getRepls(key)) {
      try {
        return f(repl)
      } catch {
        case _ :IOException =>
      }
    }
    jsonResp(403)(
      "msg" -> "error",
      "error" -> "all nodes in partition down"
    )
  }

}

object KeyValueServiceJersey {

  // Checks to make sure key isn't too long.
  private def validateKey(key: String)(f: => Response): Response = {
    if (key.length > 250) jsonResp(403)(
      "msg"   -> "error",
      "error" -> "key too long"
    ) else {
      f
    }
  }

}
