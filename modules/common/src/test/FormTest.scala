package lila.common

import org.specs2.mutable.Specification
import play.api.data._
import play.api.data.format._
import play.api.data.format.Formats._
import play.api.data.Forms._
import play.api.data.validation._
import scalatags.Text.all._

import lila.common.Form._

class FormTest extends Specification {

  "trim" should {
    "apply before validation" in {

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

  "garbage chars" should {
    "be removed before validation" in {

      val chars = List('\u200b', '\u200c', '\u200d', '\u200e', '\u200f', '\u202e', '\u1160')

      val str = chars mkString ""

      single("t" -> cleanText(minLength = 1))
        .bind(Map("t" -> str)) must beLeft

      single("t" -> cleanText(minLength = 1))
        .bind(Map("t" -> s"  $str  ")) must beLeft

      single("t" -> cleanText)
        .bind(Map("t" -> s"  $str  ")) must beRight
    }
  }

}
