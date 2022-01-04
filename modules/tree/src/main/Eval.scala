package lila.tree

import shogi.format.usi.Usi

case class Eval(
    cp: Option[Eval.Cp],
    mate: Option[Eval.Mate],
    best: Option[Usi]
) {

  def isEmpty = cp.isEmpty && mate.isEmpty

  def dropBest = copy(best = None)

  def invert = copy(cp = cp.map(_.invert), mate = mate.map(_.invert))

  def score: Option[Eval.Score] = cp.map(Eval.Score.cp) orElse mate.map(Eval.Score.mate)
}

object Eval {

  case class Score(value: Either[Cp, Mate]) extends AnyVal {

    def cp: Option[Cp]     = value.left.toOption
    def mate: Option[Mate] = value.toOption

    def isCheckmate = value == Score.checkmate
    def mateFound   = value.isRight

    def invert                  = copy(value = value.left.map(_.invert).map(_.invert))
    def invertIf(cond: Boolean) = if (cond) invert else this

    def eval = Eval(cp, mate, None)
  }

  object Score {

    def cp(x: Cp): Score     = Score(Left(x))
    def mate(y: Mate): Score = Score(Right(y))

    val checkmate: Either[Cp, Mate] = Right(Mate(0))
  }

  case class Cp(value: Int) extends AnyVal with Ordered[Cp] {

    def centipawns = value

    def pawns: Float      = value / 100f
    def showPawns: String = "%.2f" format pawns

    def ceiled =
      if (value > Cp.CEILING) Cp(Cp.CEILING)
      else if (value < -Cp.CEILING) Cp(-Cp.CEILING)
      else this

    def invert                  = Cp(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def compare(other: Cp) = Integer.compare(value, other.value)

    def signum: Int = Math.signum(value.toFloat).toInt
  }

  object Cp {

    val CEILING = 5500

    val initial = Cp(50)
  }

  case class Mate(value: Int) extends AnyVal with Ordered[Mate] {

    def moves = value

    def invert                  = Mate(value = -value)
    def invertIf(cond: Boolean) = if (cond) invert else this

    def compare(other: Mate) = Integer.compare(value, other.value)

    def signum: Int = Math.signum(value.toFloat).toInt

    def positive = value > 0
    def negative = value < 0
  }

  val initial = Eval(Some(Cp.initial), None, None)

  val empty = Eval(None, None, None)

  object JsonHandlers {
    import play.api.libs.json._

    implicit private val usiWrites: Writes[Usi] = Writes { usi =>
      JsString(usi.usi)
    }
    implicit val cpFormat: Format[Cp] = Format[Cp](
      Reads.of[Int] map Cp.apply,
      Writes { cp =>
        JsNumber(cp.value)
      }
    )

    implicit val mateFormat: Format[Mate] = Format[Mate](
      Reads.of[Int] map Mate.apply,
      Writes { mate =>
        JsNumber(mate.value)
      }
    )

    implicit val evalWrites = Json.writes[Eval]
  }
}
