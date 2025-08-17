package lila.common.autoconfig

import com.typesafe.config.*
import play.api.{ ConfigLoader, Configuration }

import scala.concurrent.duration.*

class AutoConfigTest extends munit.FunSuite:

  private def parse(c: String) = Configuration(ConfigFactory.parseString(c.stripMargin))

  test("simple"):
    case class Foo(str: String, int: Int)
    given ConfigLoader[Foo] = AutoConfig.loader

    val config = Configuration(ConfigFactory.parseString("""
      |foo = {
      |  str = string
      |  int = 7
      |}
    """.stripMargin))

    assertEquals(config.get[Foo]("foo"), Foo("string", 7))

  test("option"):
    case class Foo(str: String, int: Option[Int])
    given ConfigLoader[Foo] = AutoConfig.loader

    assertEquals(
      parse("""
    |foo = {
    |  str = string
    |}
  """).get[Foo]("foo"),
      Foo("string", None)
    )
    assertEquals(
      parse("""
    |foo = {
    |  str = string
    |  int = 43
    |}
  """).get[Foo]("foo"),
      Foo("string", Some(43))
    )

  test("named keys"):

    final class BarApiConfig(
        @ConfigName("api-key") val apiKey: String,
        @ConfigName("request-timeout") val requestTimeout: Duration
    )

    object BarApiConfig:
      given ConfigLoader[BarApiConfig] = AutoConfig.loader
      def fromConfiguration(conf: Configuration) = conf.get[BarApiConfig]("api.foo")

    val conf = parse("""
    |api.foo {
    |  api-key = "abcdef"
    |  api-password = "secret"
    |  request-timeout = 1 minute
    |}
  """)

    assertEquals(BarApiConfig.fromConfiguration(conf).apiKey, "abcdef")
    assertEquals(BarApiConfig.fromConfiguration(conf).requestTimeout, 1.minute)

  test("nested config"):

    case class FooNestedConfig(
        @ConfigName("nested.str") str: String,
        @ConfigName("nested.deep.int") int: Int
    )
    given ConfigLoader[FooNestedConfig] = AutoConfig.loader

    val config = parse("""
      |foo = {
      |  nested.str = string
      |  nested.deep.int = 7
      |}
    """)

    assertEquals(config.get[FooNestedConfig]("foo"), FooNestedConfig("string", 7))

  test("curried constructor"):
    case class Bar(a: String, b: String)(c: Double):
      assert(c >= 0)

    given ConfigLoader[Bar] = AutoConfig.loader
    val config = parse("""
          |bar = {
          |  a = hello
          |  b = goodbye
          |  c = 4.2
          |}
        """)

    assertEquals(config.get[Bar]("bar"), Bar("hello", "goodbye")(4.2))
