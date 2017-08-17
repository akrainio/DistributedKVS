package com.akrainio.cmps128.hw4

import java.net.URI
import javax.ws.rs.client.ClientBuilder.newClient
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Entity.form
import javax.ws.rs.client.Invocation.Builder
import javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE

import com.akrainio.cmps128.hw4.KeyValueService.toMultiValuedMap
import com.akrainio.cmps128.hw4.KeyValueServiceTest._
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer
import org.glassfish.jersey.server.ResourceConfig
import org.junit.Assert.assertEquals
import org.junit.{AfterClass, BeforeClass, Test}

class KeyValueServiceTest {

 @Ignore @Test
 def testPutGet(): Unit = {
   val key = uniqueKey();
   {
     val resp = request(key).put(form(toMultiValuedMap("val", "b")))
     assertEquals(201, resp.getStatus)
     assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
     assertEquals(
       """{
         |  "replaced" : 0,
         |  "msg" : "success",
         |  "owner" : null
         |}""".stripMargin,
       resp.readEntity(classOf[String])
     )
   }
   {
     val resp = request(key).get()
     assertEquals(200, resp.getStatus)
     assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
     assertEquals(
       """{
         |  "msg" : "success",
         |  "value" : "b",
         |  "owner" : null
         |}""".stripMargin,
       resp.readEntity(classOf[String])
     )
   }
 }

 // Not sure how to send put request with no payload
 @Ignore @Test
 def testPutWithoutVal(): Unit = {
   val key = uniqueKey()
   val resp = request(key).put(Entity.text(""))
   assertEquals(403, resp.getStatus)
   assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
   assertEquals(
     """{
       |  "msg" : "error",
       |  "error" : "value is missing"
       |}""".stripMargin,
     resp.readEntity(classOf[String])
   )
 }

 @Ignore @Test
 def testPutWithNullVal(): Unit = {
   val key = uniqueKey()
   val resp = request(key).put(form(toMultiValuedMap("bogus", "bogus value")))
   assertEquals(403, resp.getStatus)
   assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
   assertEquals(
     """{
       |  "msg" : "error",
       |  "error" : "value cannot be null"
       |}""".stripMargin,
     resp.readEntity(classOf[String])
   )
 }

 @Ignore @Test
 def testGetNonExistentKey(): Unit = {
   val key = uniqueKey()
   val resp = request(key).get()
   assertEquals(404, resp.getStatus)
   assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
   assertEquals(
     """{
     |  "msg" : "error",
     |  "error" : "key does not exist",
     |  "owner" : null
     |}""".stripMargin,
   resp.readEntity(classOf[String])
   )
 }


 @Ignore @Test
 def testPutReplace(): Unit = {
   val key = uniqueKey();
   {
     request(key).put(form(toMultiValuedMap("val", "b")))
     val resp = request(key).put(form(toMultiValuedMap("val", "c")))
     assertEquals(200, resp.getStatus)
     assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
     assertEquals(
       """{
       |  "replaced" : 1,
       |  "msg" : "success",
       |  "owner" : null
       |}""".stripMargin,
     resp
       .readEntity(classOf[String])
     )
   }
   {
     val resp = request(key).get()
     assertEquals(200, resp.getStatus)
     assertEquals(APPLICATION_JSON_TYPE, resp.getMediaType)
     assertEquals(
       """{
         |  "msg" : "success",
         |  "value" : "c",
         |  "owner" : null
         |}""".stripMargin,
       resp.readEntity(classOf[String])
     )
   }
 }

}

object KeyValueServiceTest {
 var counter = 0

 @BeforeClass
 def setUpClass(): Unit = {
   server.start()
 }

 @AfterClass
 def tearDownClass(): Unit = {
   server.shutdownNow()
 }

 private val baseUri = URI.create("http://localhost:8080/")

 private val server = {
   val config = new ResourceConfig().packages(classOf[KeyValueServiceJersey].getPackage.getName)
   createHttpServer(baseUri, config, false)
 }

 private val client = {
   val c = newClient
   c.target(baseUri)
 }

 private def request(key: String): Builder = {
   client.path(s"kvs/$key").request
 }

 private def uniqueKey(): String = {
   counter += 1
   s"key-$counter"
 }
}
