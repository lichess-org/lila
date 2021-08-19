package lila.fishnet

import shogi.Replay
import shogi.format.pgn.Reader
import org.specs2.mutable._

import lila.analyse.{ Analysis, Info }
import lila.tree.Eval
import lila.tree.Eval._

final class UciToPgnTest extends Specification {

  private val now = org.joda.time.DateTime.now

  private def evenIncomplete(result: Reader.Result): Replay =
    result match {
      case Reader.Result.Complete(replay)      => replay
      case Reader.Result.Incomplete(replay, _) => replay
    }

  "convert UCI analysis to PGN" should {
    "work :)" in {
      val uciAnalysis = Analysis(
        "ke5ssdgj",
        None,
        List(
          Info(1, Eval(Some(Cp(12)), None, None), List()),
          Info(2, Eval(Some(Cp(36)), None, None), List()),
          Info(
            3,
            Eval(Some(Cp(22)), None, None),
            List()
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

      val pgn =
        "Pc4 Re8 Rd2 Pe6 Gd1e2 Kd8 Kf2 Kc8 Kg2 Kb8 Kh2 Sc8 Sg2 Pg6 Pd4 Sf8 Pg4 Pe5 Ng3 Pf6 Sc2 Pe4 Gf1f2 Pxe3+ Ge2xe3 Pa6 Pa4 Ph6 Ph4"
      val rep = Replay(pgn.split(' ').toList, None, shogi.variant.Standard).map(evenIncomplete).toOption.get
      UciToPgn(rep, uciAnalysis) match {
        case (_, errs) => errs must beEmpty
      }
    }
  }
}
