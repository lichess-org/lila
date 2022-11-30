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

      single("t" -> cleanText)
        .bind(Map("t" -> "")) must beRight

      single("t" -> cleanText)
        .bind(Map("t" -> "   ")) must beRight
    }
  }

  "garbage chars" >> {
    "be removed before validation" >> {
      single("t" -> cleanText)
        .bind(Map("t" -> " \u200b \u200f a \u202e b \u1160")) must beEqualTo(Right("a  b"))
    }
  }
}
