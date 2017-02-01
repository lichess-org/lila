package lila.tree

import chess.format.Uci

case class Eval(
    cp: Option[Eval.Cp],
    mate: Option[Eval.Mate],
    best: Option[Uci.Move]) {

  def isEmpty = cp.isEmpty && mate.isEmpty

  def dropBest = copy(best = None)

  def invert = copy(cp = cp.map(_.invert), mate = mate.map(_.invert))
}

object Eval {

  case class Score(value: Either[Cp, Mate]) extends AnyVal {

    def cp: Option[Cp] = value.left.toOption
    def mate: Option[Mate] = value.right.toOption

    def isCheckmate = value == Right(0)
    def mateFound = value.isRight

    def invert = copy(value = value.left.map(_.invert).right.map(_.invert))
    def invertIf(cond: Boolean) = if (cond) invert else this
  }

  object Score {

    def cp(x: Cp): Score = Score(Left(x))
    def mate(y: Mate): Score = Score(Right(y))
  }

  case class Cp(value: Int) extends AnyVal {

    def centipawns = value

    def pawns: Float = value / 100f
    def showPawns: String = "%.2f" format pawns

    def ceiled =
      if (value > Cp.CEILING) Cp(Cp.CEILING)
      else if (value < -Cp.CEILING) Cp(-Cp.CEILING)
      else this

    def invert = Cp(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def <(i: Int): Boolean = value < i
    def >(i: Int): Boolean = value > i
    def <=(i: Int): Boolean = value <= i
    def >=(i: Int): Boolean = value >= i
    def ==(i: Int): Boolean = value == i

    def signum: Int = Math.signum(value).toInt
  }

  object Cp {

    val CEILING = 1000

    val initial = Cp(15)
  }

  case class Mate(value: Int) extends AnyVal {

    def moves = value

    def invert = Mate(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def <(i: Int): Boolean = value < i
    def >(i: Int): Boolean = value > i
    def <=(i: Int): Boolean = value <= i
    def >=(i: Int): Boolean = value >= i
    def ==(i: Int): Boolean = value == i

    def signum: Int = Math.signum(value).toInt
  }

  val initial = Eval(Some(Cp.initial), None, None)

  val empty = Eval(None, None, None)

  object JsonHandlers {
    import play.api.libs.json._

    private implicit val uciWrites: Writes[Uci.Move] = Writes { uci =>
      JsString(uci.uci)
    }
    implicit val cpFormat: Format[Cp] = Format[Cp](
      Reads.of[Int] map Cp.apply,
      Writes { cp => JsNumber(cp.value) })

    implicit val mateFormat: Format[Mate] = Format[Mate](
      Reads.of[Int] map Mate.apply,
      Writes { mate => JsNumber(mate.value) })

    implicit val evalWrites = Json.writes[Eval]
  }
}
