package lila.analyse

import org.specs2.mutable.Specification

import lila.common.Strings
import lila.game.Game.SideAndStart
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

class AccuracyPercentTest extends Specification {

  val defaultSide = SideAndStart(chess.White, chess.White, 0)

  def fromWinPercents(before: Int, after: Int): Double =
    AccuracyPercent.fromWinPercents(WinPercent(before), WinPercent(after)).value

  "AccuracyPercent" should {
    "fromWinPercents" in {
      List(
        (100, 100),
        (50, 45),
        (0, 0),
        (100, 90),
        (100, 80),
        (100, 60),
        (100, 50),
        (100, 30)
      ) foreach { case (b, a) =>
        println(s"$b -> $a = ${fromWinPercents(b, a)}")
      }
      fromWinPercents(100, 100) must_== 100
      fromWinPercents(50, 50) must_== 100
      fromWinPercents(0, 0) must_== 100
      fromWinPercents(100, 90) must_== 80
      // fromWinPercents(90, 80) must_== 90
      fromWinPercents(50, 40) must_== 60
      fromWinPercents(50, 0) must_== 0
      fromWinPercents(50, 10) must_== 0
    }
    // "fromEvalsAndPov" in {
    //   AccuracyPercent.fromEvalsAndPov(defaultSide, Nil) must_== Nil
    //   val percents: List[Double] = AccuracyPercent
    //     .fromEvalsAndPov(
    //       defaultSide,
    //       List(
    //         Cp.initial.value,
    //         0,
    //         -100,
    //         500,
    //         -500,
    //         -500,
    //         -490
    //       ).map(cp => Eval(cp = Some(Cp(cp)), None, None))
    //     )
    //     .map(_.value)
    //   percents(0) must_== 100
    //   percents(1) must beCloseTo(68d, 5)
    //   percents(2) must beCloseTo(12d, 5)
    //   percents(3) must_== 100
    // }
  }
}
