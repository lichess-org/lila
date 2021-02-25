package lila.evaluation

import chess.Color

sealed trait GameAssessment {
  val id: Int
  val description: String
  val emoticon: String
  override def toString = description
}

object GameAssessment {

  case object Cheating extends GameAssessment {
    val description: String = "Cheating"
    val emoticon: String    = ">:("
    val id                  = 5
  }
  case object LikelyCheating extends GameAssessment {
    val description: String = "Likely cheating"
    val emoticon: String    = ":("
    val id                  = 4
  }
  case object Unclear extends GameAssessment {
    val description: String = "Unclear"
    val emoticon: String    = ":|"
    val id                  = 3
  }
  case object UnlikelyCheating extends GameAssessment {
    val description: String = "Unlikely cheating"
    val emoticon: String    = ":)"
    val id                  = 2
  }
  case object NotCheating extends GameAssessment {
    val description: String = "Not cheating"
    val emoticon: String    = ":D"
    val id                  = 1
  }
  val all: List[GameAssessment] = List(NotCheating, UnlikelyCheating, Unclear, LikelyCheating, Cheating)
  val byId: Map[Int, GameAssessment] = all.map { a =>
    a.id -> a
  }.toMap
  def orDefault(id: Int) = byId.getOrElse(id, NotCheating)
}
