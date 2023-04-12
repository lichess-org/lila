package lila.common

import org.specs2.mutable.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import play.api.data._
import play.api.data.format._
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.data.validation._

import lila.common.Form._

class FormTest extends Specification {

  "format dates" should {
    // val date = java.time.LocalDateTime.of(2023, 4, 12, 10, 0, 15, 337)
    val date = new DateTime(2023, 4, 12, 11, 1, 15, 337, DateTimeZone.UTC)
    "iso datetime" in {
      val mapping = single("t" -> lila.common.Form.ISODateTime.isoDateTime)
      mapping.unbind(date) === Map("t" -> "2023-04-12T11:01:15.337+0000")
    }
    "iso date" in {
      val mapping = single("t" -> lila.common.Form.ISODate.isoDate)
      mapping.unbind(date) === Map("t" -> "2023-04-12")
    }
    "pretty date" in {
      val mapping = single("t" -> lila.common.Form.PrettyDate.prettyDate)
      mapping.unbind(date) === Map("t" -> "2023-04-12 11:01")
    }
    "timestamp" in {
      val mapping = single("t" -> lila.common.Form.Timestamp.timestamp)
      mapping.unbind(date) === Map("t" -> "1681297275337")
    }
    "iso datetime or timestamp" in {
      val mapping = single("t" -> lila.common.Form.ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)
      mapping.unbind(date) === Map("t" -> "2023-04-12T11:01:15.337+0000")
    }
    "iso date or timestamp" in {
      val mapping = single("t" -> lila.common.Form.ISODateOrTimestamp.isoDateOrTimestamp)
      mapping.unbind(date) === Map("t" -> "2023-04-12")
    }
  }

  "parse dates" should {
    "iso datetime" in {
      val mapping = single("t" -> lila.common.Form.ISODateTime.mapping)
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beRight
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beRight
      mapping.bind(Map("t" -> "2017-01-01T12:34:56")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01")) must beLeft
    }
    "iso date" in {
      val mapping = single("t" -> lila.common.Form.ISODate.mapping)
      mapping.bind(Map("t" -> "2017-01-01")) must beRight
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56")) must beLeft
    }
    "pretty date" in {
      val mapping = single("t" -> lila.common.Form.PrettyDateTime.mapping)
      mapping.bind(Map("t" -> "2017-01-01 23:11")) must beRight
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01")) must beLeft
    }
    "timestamp" in {
      val mapping = single("t" -> lila.common.Form.Timestamp.mapping)
      mapping.bind(Map("t" -> "1483228800000")) must beRight
      mapping.bind(Map("t" -> "2017-01-01 23:11")) must beLeft
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01")) must beLeft
    }
    "iso instant or timestamp" in {
      val mapping = single("t" -> lila.common.Form.ISOInstantOrTimestamp.mapping)
      mapping.bind(Map("t" -> "1483228800000")) must beRight
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beRight
      mapping.bind(Map("t" -> "2017-01-01 23:11")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beRight
      mapping.bind(Map("t" -> "2017-01-01")) must beLeft
    }
    "iso date or timestamp" in {
      val mapping = single("t" -> lila.common.Form.ISODateOrTimestamp.mapping)
      mapping.bind(Map("t" -> "1483228800000")) must beRight
      mapping.bind(Map("t" -> "2017-01-01")) must beRight
      mapping.bind(Map("t" -> "2023-04-25T10:00:00.000Z")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01 23:11")) must beLeft
      mapping.bind(Map("t" -> "2017-01-01T12:34:56Z")) must beLeft
    }
  }

  "trim" >> {
    "apply before validation" >> {

      FieldMapping("t", List(Constraints.minLength(1)))
        .bind(Map("t" -> " "))
        .must(beRight)

      FieldMapping("t", List(Constraints.minLength(1)))
        .as(cleanTextFormatter)
        .bind(Map("t" -> " "))
        .must(beLeft)

      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "     ")) must beLeft

      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "aa ")) must beLeft

      single("t" -> cleanText(minLength = 3))
        .bind(Map("t" -> "aaa")) must beRight

      single("t" -> text)
        .bind(Map("t" -> "")) must beRight
      single("t" -> cleanText)
        .bind(Map("t" -> "")) must beRight

      single("t" -> cleanText)
        .bind(Map("t" -> "   ")) must beRight
    }
  }

  "garbage chars" >> {

    "invisible chars are removed before validation" >> {
      val invisibleChars = List('\u200b', '\u200c', '\u200d', '\u200e', '\u200f', '\u202e', '\u1160')
      val invisibleStr   = invisibleChars mkString ""
      single("t" -> cleanText).bind(Map("t" -> invisibleStr)) === Right("")
      single("t" -> cleanText).bind(Map("t" -> s"  $invisibleStr  ")) === Right("")
      single("t" -> cleanTextWithSymbols).bind(Map("t" -> s"  $invisibleStr  ")) === Right("")
      single("t" -> cleanText(minLength = 1)).bind(Map("t" -> invisibleStr)) must beLeft
      single("t" -> cleanText(minLength = 1)).bind(Map("t" -> s"  $invisibleStr  ")) must beLeft
    }
    "other garbage chars are also removed before validation, unless allowed" >> {
      val garbageStr = "꧁ ۩۞"
      single("t" -> cleanText).bind(Map("t" -> garbageStr)) === Right("")
      single("t" -> cleanTextWithSymbols).bind(Map("t" -> garbageStr)) === Right(garbageStr)
    }
    "emojis are removed before validation, unless allowed" >> {
      val emojiStr = "🌈🌚"
      single("t" -> cleanText).bind(Map("t" -> emojiStr)) === Right("")
      single("t" -> cleanTextWithSymbols).bind(Map("t" -> emojiStr)) === Right(emojiStr)
    }
  }

  "special chars" >> {
    val half = '½'
    single("t" -> cleanTextWithSymbols).bind(Map("t" -> half.toString)) === Right(half.toString)
    single("t" -> cleanText).bind(Map("t" -> half.toString)) === Right(half.toString)
  }

}
