package lila.common

import play.api.libs.json.*
import scalalib.StringUtils.safeJsonString

class StringSafeJsonTest extends munit.FunSuite:

  import lila.common.String.html.safeJsonValue

  // Borrowed from:
  // https://github.com/playframework/play-json/blob/160f66a84a9c5461c52b50ac5e222534f9e05442/play-json/js/src/main/scala/StaticBinding.scala#L65
  // Reference implementation: the original recursive serializer, used to assert the
  // iterative implementation produces byte-for-byte identical output.
  def reference(jsValue: JsValue): String = jsValue match
    case JsNull => "null"
    case JsString(s) => safeJsonString(s)
    case JsNumber(n) => n.toString
    case JsFalse => "false"
    case JsTrue => "true"
    case JsArray(items) => items.map(reference).mkString("[", ",", "]")
    case JsObject(fields) =>
      fields
        .map: (k, v) =>
          s"${safeJsonString(k)}:${reference(v)}"
        .mkString("{", ",", "}")

  def check(js: JsValue)(using munit.Location): Unit =
    assertEquals(safeJsonValue(js).value, reference(js))

  test("null"):
    check(JsNull)

  test("booleans"):
    check(JsTrue)
    check(JsFalse)

  test("numbers"):
    check(JsNumber(0))
    check(JsNumber(-42))
    check(JsNumber(BigDecimal("3.14159")))
    check(JsNumber(BigDecimal("1e100")))

  test("plain strings"):
    check(JsString(""))
    check(JsString("hello world"))

  test("strings needing escaping"):
    check(JsString("quote \" backslash \\ slash /"))
    check(JsString("tab\tnewline\ncarriage\r"))
    // control chars (NUL, unit separator) built from code points so this source file
    // stays plain ASCII text rather than becoming a binary blob.
    check(JsString(s"control ${0.toChar}${31.toChar} chars"))
    check(JsString("unicode ♞ é 日本語"))

  test("empty array"):
    assertEquals(safeJsonValue(JsArray.empty).value, "[]")
    check(JsArray.empty)

  test("empty object"):
    assertEquals(safeJsonValue(Json.obj()).value, "{}")
    check(Json.obj())

  test("flat array"):
    check(Json.arr(1, 2, 3))
    check(Json.arr("a", "b", JsNull, true))

  test("flat object preserves key order"):
    val js = Json.obj("z" -> 1, "a" -> 2, "m" -> 3)
    assertEquals(safeJsonValue(js).value, """{"z":1,"a":2,"m":3}""")
    check(js)

  test("object keys needing escaping"):
    check(Json.obj("has \"quote\"" -> 1, "back\\slash" -> 2))

  test("mixed nested array/object"):
    val js = Json.obj(
      "name" -> "test",
      "tags" -> Json.arr("a", "b"),
      "nested" -> Json.obj(
        "deep" -> Json.arr(Json.obj("x" -> 1), JsNull, Json.arr(2, 3))
      ),
      "flag" -> true,
      "n" -> JsNull
    )
    check(js)

  test("deep nesting via arrays does not stack overflow"):
    val depth = 100_000
    var js: JsValue = JsNumber(1)
    for _ <- 0 until depth do js = JsArray(Seq(js))
    val out = safeJsonValue(js).value
    assertEquals(out.take(depth), "[" * depth)
    assertEquals(out, "[" * depth + "1" + "]" * depth)

  test("deep nesting via single-field objects does not stack overflow"):
    val depth = 100_000
    var js: JsValue = JsNumber(1)
    for _ <- 0 until depth do js = JsObject(Seq("k" -> js))
    val out = safeJsonValue(js).value
    assert(out.startsWith("""{"k":"""))
    assert(out.endsWith("}"))
