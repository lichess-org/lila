package lila.fishnet

import org.specs2.mutable._

import lila.analyse.{ Analysis, Info }
import lila.tree.Eval
import lila.tree.Eval._

final class VariationValidationTest extends Specification {

  private val now = org.joda.time.DateTime.now

  "verify USI analysis" should {
    "work :)" in {
      val usiAnalysis = Analysis(
        "ke5ssdgj",
        None,
        Set.empty,
        List(
          Info(1, Eval(Some(Cp(12)), None, None), List("7g7f", "3c3d", "8h2b+")),
          Info(2, Eval(Some(Cp(36)), None, None), List()),
          Info(
            3,
            Eval(Some(Cp(22)), None, None),
            List("7g7f")
          ),
          Info(4, Eval(Some(Cp(-30)), None, None), List()),
          Info(5, Eval(Some(Cp(-48)), None, None), List()),
          Info(
            6,
            Eval(Some(Cp(-80)), None, None),
            List()
          ),
          Info(7, Eval(Some(Cp(-22)), None, None), List()),
          Info(8, Eval(Some(Cp(-64)), None, None), List()),
          Info(9, Eval(Some(Cp(-38)), None, None), List()),
          Info(10, Eval(Some(Cp(-56)), None, None), List()),
          Info(11, Eval(Some(Cp(-48)), None, None), List()),
          Info(12, Eval(Some(Cp(-48)), None, None), List()),
          Info(13, Eval(Some(Cp(-52)), None, None), List()),
          Info(14, Eval(Some(Cp(-98)), None, None), List()),
          Info(15, Eval(Some(Cp(-56)), None, None), List()),
          Info(16, Eval(Some(Cp(-98)), None, None), List()),
          Info(17, Eval(Some(Cp(-54)), None, None), List()),
          Info(18, Eval(Some(Cp(-96)), None, None), List()),
          Info(19, Eval(Some(Cp(-96)), None, None), List()),
          Info(
            20,
            Eval(Some(Cp(-113)), None, None),
            List()
          ),
          Info(
            21,
            Eval(Some(Cp(-42)), None, None),
            List()
          ),
          Info(
            22,
            Eval(Some(Cp(-535)), None, None),
            List()
          ),
          Info(
            23,
            Eval(Some(Cp(-296)), None, None),
            List()
          ),
          Info(24, Eval(None, Some(Mate(3)), None), List()),
          Info(
            25,
            Eval(Some(Cp(-935)), None, None),
            List()
          ),
          Info(26, Eval(Some(Cp(-2165)), None, None), List()),
          Info(
            27,
            Eval(Some(Cp(-2731)), None, None),
            List()
          ),
          Info(28, Eval(None, Some(Mate(2)), None), List()),
          Info(29, Eval(None, Some(Mate(-2)), None), List())
        ),
        0,
        None,
        None,
        now
      )

      val usi =
        "1g1f 1c1d 2g2f 2b1c 4g4f 4c4d 1f1e 4d4e 1e1d 1c2b 4f4e 3a3b 2f2e 3c3d 3i3h 7c7d 1d1c+ 1a1c P*1d 1c1d 1i1d P*1c 2e2d 1c1d 2d2c+ 2b5e 2c3b 4a3b 2h2a+ 5a6b 2a3b P*4b 3b2a 7d7e S*4d"
      val work = Work.Game(
        id = "id",
        initialSfen = None,
        studyId = None,
        variant = shogi.variant.Standard,
        moves = usi
      )
      VariationValidation(work, usiAnalysis) match {
        case (_, errs) => errs must beEmpty
      }
    }
  }
}
