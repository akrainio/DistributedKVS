package com.akrainio.cmps128.hw4

import java.io.IOException
import java.net.{ConnectException, URI, UnknownHostException}
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import javax.ws.rs.client.ClientBuilder.newClient
import javax.ws.rs.client.Entity.form
import javax.ws.rs.client.{Entity, WebTarget}
import javax.ws.rs.core.{Form, Response}

import com.akrainio.cmps128.hw4.KeyValueService._

//noinspection TypeAnnotation
class KeyValueServiceProxy(val ThisIpport: String) extends KeyValueService {

  private val Logger = getLogger(classOf[KeyValueServiceJersey].getName + ThisIpport)

  override def get(payload: String, key: String) = {
    sendRequest(cl.path(key).queryParam("causal_payload", payload).request.get())
  }

  override def put(payload: String, key: String, value: String) = {
    sendPut(key)(("val", value), ("causal_payload", payload))
  }

  override def putInternal(internal: String, payload: String, key: String, value: String) = {
    sendPut(key)(("val", value), ("causal_payload", payload), ("internal", internal))
  }

  override def updateView(updateType: String, ipport: String) = {
    sendRequest(cl.path("update_view").queryParam("type", updateType).request.put(mkForm(Seq(("ip_port", ipport)))))
  }

  override def internalUpdate(newView: String) = {
    sendPut("internal_update")(("new_view", newView))
  }

  override def gossip(payload: String, kvs: String, sender: String, timeStamp: String) = {
    sendPut("gossip")(("kvs", kvs), ("timeStamp", timeStamp), ("causal_payload", payload), ("sender", sender))
  }

  override def gossipAck(payload: String, kvs: String) = {
    sendPut("gossipAck")(("kvs", kvs), ("causal_payload", payload))
  }

  override def rebal() = sendRequest(cl.path("rebalance").request.post(Entity.text("")))

  private val cl: WebTarget = {
    val c = newClient
    c.target(URI.create(s"http://$ThisIpport/kvs/"))
  }

  private def sendPut(path: String)(pairs: Tuple2[String, String]*): Response = {
    sendRequest(cl.path(path).request.put(mkForm(pairs)))
  }

  private def mkForm(pairs: Seq[Tuple2[String, String]]): Entity[Form] = {
    form(toMultiValuedMap(pairs))
  }

  private def sendRequest(f: => Response): Response = {
    try {
      f
    } catch {
      case e: IOException =>
        Logger.log(Level.SEVERE, e.getMessage, e)
        jsonResp(404)(
          "msg" -> "error",
          "error" -> "service is not available"
        )
      case e: UnknownHostException =>
        Logger.log(Level.SEVERE, e.getMessage, e)
        val c = e
        Logger.log(Level.SEVERE, e.getMessage, e)
        jsonResp(404)(
          "msg" -> "error",
          "error" -> "???"
        )
    }
  }

}