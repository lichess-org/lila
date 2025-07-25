package lila.common

import play.api.data.*
import play.api.data.Forms.*
import play.api.data.format.*
import play.api.data.format.Formats.*
import play.api.data.validation.*

import lila.common.Form.*

class FormTest extends munit.FunSuite:

  val date = java.time.LocalDateTime.of(2023, 4, 12, 11, 1, 15, 337_000_000)
  test("format iso datetime"):
    val mapping = single("t" -> lila.common.Form.ISODateTime.mapping)
    assertEquals(mapping.unbind(date), Map("t" -> "2023-04-12T11:01:15.337Z"))
  test("format iso date"):
    val mapping = single("t" -> lila.common.Form.ISODate.mapping)
    assertEquals(mapping.unbind(date.date), Map("t" -> "2023-04-12"))
  test("format pretty datetime"):
    val mapping = single("t" -> lila.common.Form.PrettyDateTime.mapping)
    assertEquals(mapping.unbind(date), Map("t" -> "2023-04-12 11:01"))
  test("format timestamp"):
    val mapping = single("t" -> lila.common.Form.Timestamp.mapping)
    assertEquals(mapping.unbind(date.instant), Map("t" -> "1681297275337"))
  test("format iso datetime or timestamp"):
    val mapping = single("t" -> lila.common.Form.ISOInstantOrTimestamp.mapping)
    assertEquals(mapping.unbind(date.instant), Map("t" -> "2023-04-12T11:01:15.337Z"))
  test("format iso date or timestamp"):
    val mapping = single("t" -> lila.common.Form.ISODateOrTimestamp.mapping)
    assertEquals(mapping.unbind(date.date), Map("t" -> "2023-04-12"))

  test("parse iso datetime"):
    val mapping = single("t" -> lila.common.Form.ISODateTime.mapping)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isLeft)
  test("parse iso date"):
    val mapping = single("t" -> lila.common.Form.ISODate.mapping)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isRight)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56")).isLeft)
  test("parse pretty date"):
    val mapping = single("t" -> lila.common.Form.PrettyDateTime.mapping)
    assert(mapping.bind(Map("t" -> "2017-01-01 23:11")).isRight)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isLeft)
  test("parse timestamp"):
    val mapping = single("t" -> lila.common.Form.Timestamp.mapping)
    assert(mapping.bind(Map("t" -> "1483228800000")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01 23:11")).isLeft)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isLeft)
  test("parse iso instant or timestamp"):
    val mapping = single("t" -> lila.common.Form.ISOInstantOrTimestamp.mapping)
    assert(mapping.bind(Map("t" -> "1483228800000")).isRight)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01 23:11")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isLeft)
  test("parse iso date or timestamp"):
    val mapping = single("t" -> lila.common.Form.ISODateOrTimestamp.mapping)
    assert(mapping.bind(Map("t" -> "1483228800000")).isRight)
    assert(mapping.bind(Map("t" -> "2017-01-01")).isRight)
    assert(mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01 23:11")).isLeft)
    assert(mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")).isLeft)

  test("trim before validation"):

    assert(
      FieldMapping("t", List(Constraints.minLength(1)))
        .bind(Map("t" -> " "))
        .isRight
    )

    assert(
      FieldMapping("t", List(Constraints.minLength(1)))
        .as(cleanTextFormatter)
        .bind(Map("t" -> " "))
        .isLeft
    )

    assert(
      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "     "))
        .isLeft
    )

    assert(
      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "aa "))
        .isLeft
    )

    assert(
      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "aaa"))
        .isRight
    )

    assert(
      single("t" -> text)
        .bind(Map("t" -> ""))
        .isRight
    )
    assert(
      single("t" -> cleanText)
        .bind(Map("t" -> ""))
        .isRight
    )
    assert(
      single("t" -> cleanText)
        .bind(Map("t" -> "   "))
        .isRight
    )

  test("invisible chars are removed before validation"):
    val invisibleChars = List('\u200b', '\u200c', '\u200d', '\u200e', '\u200f', '\u202e', '\u1160')
    val invisibleStr = invisibleChars.mkString("")
    assertEquals(single("t" -> cleanText).bind(Map("t" -> invisibleStr)), Right(""))
    assertEquals(single("t" -> cleanText).bind(Map("t" -> s"  $invisibleStr  ")), Right(""))
    assertEquals(single("t" -> cleanTextWithSymbols).bind(Map("t" -> s"  $invisibleStr  ")), Right(""))
    assert(single("t" -> cleanText(minLength = 1)).bind(Map("t" -> invisibleStr)).isLeft)
    assert(single("t" -> cleanText(minLength = 1)).bind(Map("t" -> s"  $invisibleStr  ")).isLeft)
    // braille space
    assert(single("t" -> cleanText(minLength = 1)).bind(Map("t" -> "⠀")).isLeft)
  test("other garbage chars are also removed before validation, unless allowed"):
    val garbageStr = "꧁ ۩۞"
    assertEquals(single("t" -> cleanText).bind(Map("t" -> garbageStr)), Right(""))
    assertEquals(single("t" -> cleanTextWithSymbols).bind(Map("t" -> garbageStr)), Right(garbageStr))
  test("emojis are removed before validation, unless allowed"):
    val emojiStr = "🌈🌚"
    assertEquals(single("t" -> cleanText).bind(Map("t" -> emojiStr)), Right(""))
    assertEquals(single("t" -> cleanTextWithSymbols).bind(Map("t" -> emojiStr)), Right(emojiStr))

  test("special chars"):
    val half = '½'
    assertEquals(single("t" -> cleanTextWithSymbols).bind(Map("t" -> half.toString)), Right(half.toString))
    assertEquals(single("t" -> cleanText).bind(Map("t" -> half.toString)), Right(half.toString))
