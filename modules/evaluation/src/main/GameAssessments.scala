package lila.evaluation

import chess.Color

case class PlayerAssessments(
  white: Option[PlayerAssessment],
  black: Option[PlayerAssessment]) {
  def color(c: Color) = c match {
    case Color.White => white
    case _ => black
  }
}

sealed trait GameAssessment {
  val description: String
  val emoticon: String
  val colorClass: Int
  override def toString = description
}

object GameAssessment {

	import reactivemongo.bson.{ BSONHandler, BSONInteger }

	implicit val GameAssessmentBSONHandler = new BSONHandler[BSONInteger, GameAssessment] {
		def read(bsonInt: BSONInteger): GameAssessment = bsonInt match {
			case BSONInteger(5) => Cheating
			case BSONInteger(4) => LikelyCheating
			case BSONInteger(3) => Unclear
			case BSONInteger(2) => UnlikelyCheating
			case _ 							=> NotCheating
		}
		def write(x: GameAssessment) = BSONInteger(x.colorClass)
	}
  case object Cheating extends GameAssessment {
    val description: String = "Cheating"
    val emoticon: String = ">:("
    val colorClass = 5
  }
  case object LikelyCheating extends GameAssessment {
    val description: String = "Likely cheating"
    val emoticon: String = ":("
    val colorClass = 4
  }
  case object Unclear extends GameAssessment {
    val description: String = "Unclear"
    val emoticon: String = ":|"
    val colorClass = 3
  }
  case object UnlikelyCheating extends GameAssessment {
    val description: String = "Unlikely cheating"
    val emoticon: String = ":)"
    val colorClass = 2
  }
  case object NotCheating extends GameAssessment {
    val description: String = "Not cheating"
    val emoticon: String = ":D"
    val colorClass = 1
  }
}
