package lila.system

import memo._

class MemoBuilderTest extends SystemTest {

  val builder = Builder

  "memo cache" should {
    val f = (s: String) ⇒ s.size
    def makeCache = builder.cache(10, f)
    "f" in {
      f("test") must_== 4
    }
    "compute missing value" in {
      makeCache.get("test") must_== 4
    }
    "return stored value" in {
      val c = builder.cache(10, (s: Any) ⇒ scala.util.Random.nextInt())
      val a = c.get("test")
      c.get("test") must_== a
    }
  }
  "expiry cache" should {
    def makeCache = builder.expiry[String, Int](10)
    "be empty" in {
      makeCache.size must_== 0
    }
    "read missing value" in {
      makeCache.getIfPresent("three") must_== 0
    }
    "store value" in {
      val cache = makeCache
      cache.put("three", 3)
      cache.size must_== 1
    }
    "read stored value" in {
      val cache = makeCache
      cache.put("three", 3)
      cache.getIfPresent("three") must_== 3
    }
  }
}
