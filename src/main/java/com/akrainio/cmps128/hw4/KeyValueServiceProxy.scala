package com.akrainio.cmps128.hw4

import java.io.IOException
import java.net.{ConnectException, URI}
import java.util.logging.Level
import java.util.logging.Logger.getLogger
import javax.ws.rs.client.ClientBuilder.newClient
import javax.ws.rs.client.Entity.form
import javax.ws.rs.client.{Entity, WebTarget}
import javax.ws.rs.core.Response

import com.akrainio.cmps128.hw4.KeyValueService._

//noinspection TypeAnnotation
class KeyValueServiceProxy(val ThisIpport: String) extends KeyValueService {

  private val Logger = getLogger(classOf[KeyValueServiceJersey].getName + ThisIpport)

  override def get(payload: String, key: String) = sendRequest(cl.path(key).queryParam("causal_payload", payload).request.get())

  override def put(payload: String, key: String, value: String) = {
    sendRequest(cl.path(key).queryParam("causal_payload", payload).request.put(form(toMultiValuedMap("val", value))))
  }

  override def putInternal(internal: String, key: String, value: String) = {
    sendRequest(cl.path(key).queryParam("internal", internal).request.put(form(toMultiValuedMap("val", value))))
  }

  override def updateView(updateType: String, ipport: String) = {
    sendRequest(cl.path("view_update").queryParam("type", updateType).request.put(form(toMultiValuedMap("ip_port", ipport))))
  }

  override def internalUpdate(newView: String) = {
    sendRequest(cl.path("internal_update").request.put(form(toMultiValuedMap("new_view", newView))))
  }

//  override def gossip(payload: String, kvs: String) = sendRequest(cl.path("gossip").queryParam("causal_payload", payload).request.put(form(toMultiValuedMap("kvs", kvs))))

  override def rebal() = sendRequest(cl.path("rebalance").request.post(Entity.text("")))

  private val cl: WebTarget = {
    val c = newClient
    c.target(URI.create(s"http://$ThisIpport/kvs/"))
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
    }
  }

}