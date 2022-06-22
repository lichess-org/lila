package lila.analyse

import org.specs2.mutable.Specification

import lila.common.Strings
import lila.game.Game.SideAndStart
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

class AccuracyPercentTest extends Specification {

  val defaultSide = SideAndStart(chess.White, chess.White, 0)

  "AccuracyPercent" should {
    "fromEvalsAndPov" in {
      AccuracyPercent.fromEvalsAndPov(defaultSide, Nil) must_== Nil
      val percents: List[Double] = AccuracyPercent
        .fromEvalsAndPov(
          defaultSide,
          List(
            Cp.initial.value,
            0,
            -100,
            500,
            -500,
            -500,
            -490
          ).map(cp => Eval(cp = Some(Cp(cp)), None, None))
        )
        .map(_.value)
      percents(0) must_== 100
      percents(1) must beCloseTo(68d, 5)
      percents(2) must beCloseTo(12d, 5)
      percents(3) must_== 100
    }
  }
}
