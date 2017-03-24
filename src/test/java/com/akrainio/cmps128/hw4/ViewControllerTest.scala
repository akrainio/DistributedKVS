//package com.akrainio.cmps128.hw4
//
//import org.junit._
//import org.junit.Assert.assertEquals
//import ViewControllerTest._
//
//class ViewControllerTest {
//
//  @Test
//  def testViewInit(): Unit = {
//    var view = mkView(0)
//    assertEquals(n(0), view.toString)
//
//    view = mkView(1)
//    assertEquals(getN(0, 1), view.toString)
//
//    view = mkView(2)
//    assertEquals(getN(0, 2), view.toString)
//
//    view = mkView(3)
//    assertEquals(getN(0, 2) + "|" + n(3), view.toString)
//
//    view = mkView(7)
//    assertEquals(getN(0,2) + "|" + getN(3, 5) + "|" + getN(6, 7), view.toString)
//  }
//
//  @Test
//  def testAddNode(): Unit = {
//    var view = mkView(0)
//    view.addNode(n(1))
//    assertEquals(getN(0, 1), view.toString)
//
//    view = mkView(1)
//    view.addNode(n(2))
//    assertEquals(getN(0, 2), view.toString)
//
//    view = mkView(2)
//    view.addNode(n(3))
//    assertEquals(getN(0, 2) + "|" + n(3), view.toString)
//  }
//
//  @Test
//  def testDelNode(): Unit = {
//    var view = mkView(0)
//    view.delNode(n(0))
//    assertEquals("localhost:8080", view.toString)
//
//    view = mkView(2)
//    view.delNode(n(2))
//    view.delNode(n(1))
//    assertEquals(n(0), view.toString)
//
//    view = mkView(2)
//    view.delNode(n(0))
//    view.delNode(n(1))
//    assertEquals(n(2), view.toString)
//
//    view = mkView(3)
//    view.delNode(n(3))
//    assertEquals(getN(0, 2), view.toString)
//  }
//}
//
//object ViewControllerTest {
//  val n = Array(
//    "localhost:8080",
//    "localhost:8081",
//    "localhost:8082",
//    "localhost:8083",
//    "localhost:8084",
//    "localhost:8085",
//    "localhost:8086",
//    "localhost:8086",
//    "localhost:8088"
//  )
//  def getN(start: Int, end: Int): String = {
//    val stringBuilder = new StringBuilder
//    for (i <- start until end + 1) {
//      stringBuilder.append(n(i)).append(",")
//    }
//    stringBuilder.deleteCharAt(stringBuilder.length - 1)
//    stringBuilder.toString()
//  }
//
//  def mkView(size: Int): ViewController = {
//    new ViewController(n(0), 3, getN(0, size))
//  }
//}
