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
  val id: Int
  val description: String
  val emoticon: String
  override def toString = description
}

object GameAssessment {

  import reactivemongo.bson.{ BSONHandler, BSONInteger }

  implicit val GameAssessmentBSONHandler = new BSONHandler[BSONInteger, GameAssessment] {
    def read(bsonInt: BSONInteger): GameAssessment = bsonInt.value match {
      case 5 => Cheating
      case 4 => LikelyCheating
      case 3 => Unclear
      case 2 => UnlikelyCheating
      case _              => NotCheating
    }
    def write(x: GameAssessment) = BSONInteger(x.id)
  }
  case object Cheating extends GameAssessment {
    val description: String = "Cheating"
    val emoticon: String = ">:("
    val id = 5
  }
  case object LikelyCheating extends GameAssessment {
    val description: String = "Likely cheating"
    val emoticon: String = ":("
    val id = 4
  }
  case object Unclear extends GameAssessment {
    val description: String = "Unclear"
    val emoticon: String = ":|"
    val id = 3
  }
  case object UnlikelyCheating extends GameAssessment {
    val description: String = "Unlikely cheating"
    val emoticon: String = ":)"
    val id = 2
  }
  case object NotCheating extends GameAssessment {
    val description: String = "Not cheating"
    val emoticon: String = ":D"
    val id = 1
  }
}
