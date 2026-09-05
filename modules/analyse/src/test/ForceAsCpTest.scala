package lila.analyse

import chess.eval.{ Score, WinPercent }
import chess.eval.Eval.Mate

import lila.tree.Eval

/* `Mate(0)` means the side to move has been checkmated — scalachess spells that
 * `Score.isCheckmate = mate.exists(_.value == 0)`, and `Mate.signum` reports -1 for it.
 *
 * `Eval.forceAsCp` used to branch on `Mate.negative` (`value < 0`), which is false for
 * `Mate(0)`, so a checkmated player was scored as `Cp(Int.MaxValue)` — a 97.5% win
 * instead of a 2.5% one. These tests pin the sign down. */
class ForceAsCpTest extends munit.FunSuite:

  private def forced(mate: Int) = Eval(None, Some(Mate(mate)), None).forceAsCp

  test("being checkmated does not score as winning"):
    assert(forced(0).exists(_.value < 0), s"expected a losing score for Mate(0), got ${forced(0)}")

  test("forceAsCp agrees with Mate.signum"):
    List(-3, -1, 0, 1, 3).foreach: m =>
      val cp = forced(m).get.value
      assertEquals(math.signum(cp.toDouble).toInt, Mate(m).signum, s"sign mismatch for Mate($m)")

  test("no sign flip between being mated in 1 and being mated"):
    assert(forced(-1).get.value < 0)
    assert(forced(0).get.value < 0)

  test("forceAsCp and WinPercent.fromScore agree about a checkmated player"):
    val viaScore = WinPercent.raw(WinPercent.fromScore(Score.Mate(Mate(0))))
    val viaCp = WinPercent.raw(WinPercent.fromCentiPawns(forced(0).get))
    assert(
      math.abs(viaScore - viaCp) < 1d,
      s"the two paths disagree: score says $viaScore%, forceAsCp says $viaCp%"
    )

  test("mating the opponent still scores as winning"):
    assert(forced(1).exists(_.value > 0))
    assert(forced(5).exists(_.value > 0))
