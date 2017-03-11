package com.akrainio.cmps128.hw4

import java.net.{ConnectException, URI}
import javax.ws.rs.client.ClientBuilder.newClient
import javax.ws.rs.client.Entity.form
import javax.ws.rs.client.{Entity, WebTarget}
import javax.ws.rs.core.Response

import com.akrainio.cmps128.hw4.KeyValueService._

//noinspection TypeAnnotation
class KeyValueServiceProxy(forwardingAddr: String) extends KeyValueService {

  def get(key: String) = sendRequest(cl.path(key).request.get())

  def put(key: String, value: String) = {
    sendRequest(cl.path(key).request.put(form(toMultiValuedMap("val", value))))
  }

  def del(key: String) = sendRequest(cl.path(key).request.delete())

  def updateView(updateType: String, ipport: String) = sendRequest(cl.path("view_update").queryParam("type", updateType).request.put(form(toMultiValuedMap("ip_port", ipport))))

  def internalUpdate(newView: String) = sendRequest(cl.path("internal_update").request.put(form(toMultiValuedMap("new_view", newView))))

  def rebal() = sendRequest(cl.path("rebalance").request.post(Entity.text("")))

  private val cl: WebTarget = {
    val c = newClient
    c.target(URI.create(s"http://$forwardingAddr/kvs/"))
  }

  private def sendRequest(f: => Response): Response = {
    try {
      f
    } catch {
      case e: ConnectException => jsonResp(404)(
        "msg" -> "error",
        "error" -> "service is not available"
      )
    }
  }

}