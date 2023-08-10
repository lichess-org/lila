package lila.study

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

import cats.syntax.all.*
import lila.tree.NewRoot
import chess.Situation
import StudyArbitraries.*

class StudyTreeCheck extends ScalaCheckSuite:

  test("GameTree"):
    forAll(genRoot(Situation(chess.variant.Standard))): root =>
      true
