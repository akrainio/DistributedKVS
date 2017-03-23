package com.akrainio.cmps128.hw4

import org.junit.Test
import org.junit.Assert.assertEquals

import scala.collection.mutable

class ClockTest {

  @Test
  def testIncrement(): Unit = {
    var c1 = new Clock("a")
    c1.increment("a")
    assertEquals("a,1", c1.pack)

    c1.increment("b")
    assertEquals("b,1!a,1", c1.pack)

    c1.increment("a")
    assertEquals("b,1!a,2", c1.pack)
  }

  @Test
  def testGetCausality(): Unit = {
    var c1 = Clock.unPack("a,0!b,1!c,2")
    var c2 = Clock.unPack("a,1!b,2!c,3")
    var c3 = Clock.unPack("a,0!b,3!c,1")
    assertEquals(-1, c1.getCausality(c2))
    assertEquals(1, c2.getCausality(c1))
    assertEquals(0, c3.getCausality(c2))

  }

  @Test
  def testCombine(): Unit = {
    var c1 = Clock.unPack("a,0!b,1!c,2")
    var c2 = Clock.unPack("a,1!b,2!c,3")
    println(c1.combine(c2).pack)

    c1 = Clock.unPack("a,1!b,2!c,3")
    c2 = Clock.unPack("a,0!b,1!c,2")
    println(c1.combine(c2).pack)

    c1 = Clock.unPack("a,0!b,1!c,2")
    c2 = Clock.unPack("a,0!b,1!c,2")
    println(c1.combine(c2).pack)

    c1 = Clock.unPack("a,0!b,2!c,2")
    c2 = Clock.unPack("a,1!b,1!c,3")
    println(c1.combine(c2).pack)
  }

  @Test
  def testPacking(): Unit = {
    var c = Clock.unPack("a,0")
    assertEquals("a,0", c.pack)

    c = Clock.unPack("a,0!b,1!c,2")
    assertEquals(mutable.Map("a" -> 0, "b" -> 1, "c" -> 2), c.clock)
  }
}
