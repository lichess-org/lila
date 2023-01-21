package lila.common

import org.specs2.mutable.*
import play.api.data._
import play.api.data.format._
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.data.validation._
import scalatags.Text.all._

import lila.common.Form._

class FormTest extends Specification {

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
