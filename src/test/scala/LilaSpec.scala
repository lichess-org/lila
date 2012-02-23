package lila

import ornicar.scalalib.test.OrnicarValidationMatchers
import org.specs2.mutable.Specification
import org.specs2.matcher.{ Matcher }

import model._

trait LilaSpec
    extends Specification
    with OrnicarValidationMatchers {

  def bePoss(poss: Pos*): Matcher[Valid[Set[Pos]]] = beSuccess.like {
    case p ⇒ p.toSet must_== poss
  }
}
