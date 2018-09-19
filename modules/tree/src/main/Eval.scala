package lidraughts.tree

import draughts.format.Uci

case class Eval(
    cp: Option[Eval.Cp],
    win: Option[Eval.Win],
    best: Option[Uci]
) {

  def isEmpty = cp.isEmpty && win.isEmpty

  def dropBest = copy(best = None)

  def invert = copy(cp = cp.map(_.invert), win = win.map(_.invert))

  def score: Option[Eval.Score] = cp.map(Eval.Score.cp) orElse win.map(Eval.Score.win)
}

object Eval {

  case class Score(value: Either[Cp, Win]) extends AnyVal {

    def cp: Option[Cp] = value.left.toOption
    def win: Option[Win] = value.right.toOption

    def isWin = value == Score.iswin
    def winFound = value.isRight

    def invert = copy(value = value.left.map(_.invert).right.map(_.invert))
    def invertIf(cond: Boolean) = if (cond) invert else this

    def eval = Eval(cp, win, None)
  }

  object Score {

    def cp(x: Cp): Score = Score(Left(x))
    def win(y: Win): Score = Score(Right(y))

    val iswin: Either[Cp, Win] = Right(Win(0))
  }

  case class Cp(value: Int) extends AnyVal with Ordered[Cp] {

    def centipieces = value

    def pieces: Float = value / 100f
    def showPieces: String = "%.2f" format pieces

    def ceiled =
      if (value > Cp.CEILING) Cp(Cp.CEILING)
      else if (value < -Cp.CEILING) Cp(-Cp.CEILING)
      else this

    def invert = Cp(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def compare(other: Cp) = value compare other.value

    def signum: Int = Math.signum(value).toInt
  }

  object Cp {

    val CEILING = 1000

    val initial = Cp(15)
  }

  case class Win(value: Int) extends AnyVal with Ordered[Win] {

    def moves = value

    def invert = Win(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def compare(other: Win) = value compare other.value

    def signum: Int = Math.signum(value).toInt

    def positive = value > 0
    def negative = value < 0
  }

  val initial = Eval(Some(Cp.initial), None, None)

  val empty = Eval(None, None, None)

  object JsonHandlers {
    import play.api.libs.json._

    private implicit val uciWrites: Writes[Uci] = Writes { uci =>
      JsString(uci.uci)
    }
    implicit val cpFormat: Format[Cp] = Format[Cp](
      Reads.of[Int] map Cp.apply,
      Writes { cp => JsNumber(cp.value) }
    )

    implicit val winFormat: Format[Win] = Format[Win](
      Reads.of[Int] map Win.apply,
      Writes { win => JsNumber(win.value) }
    )

    implicit val evalWrites = Json.writes[Eval]
  }
}
