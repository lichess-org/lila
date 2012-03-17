package lila.system

import memo._

class MemoBuilderTest extends SystemTest {

  val builder = Builder

  "memo cache" should {
    val f = (s: String) ⇒ s.size
    val cache = builder.cache(10)(f)
    "f" in {
      f("test") must_== 4
    }
    "compute missing value" in {
      cache("test") must_== 4
    }
    "return stored value" in {
      val c = builder.cache(10)((s: Any) ⇒ scala.util.Random.nextInt())
      val a = c("test")
      c("test") must_== a
    }
  }
}
