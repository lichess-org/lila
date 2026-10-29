package lila.evaluation

enum GameAssessment(val id: Int, val description: String, val emoticon: String):

  case NotCheating extends GameAssessment(1, "Not cheating", ":D")
  case UnlikelyCheating extends GameAssessment(2, "Unlikely cheating", ":)")
  case Unclear extends GameAssessment(3, "Unclear", ":|")
  case LikelyCheating extends GameAssessment(4, "Likely cheating", ":(")
  case Cheating extends GameAssessment(5, "Cheating", ">:(")

  override def toString = description

object GameAssessment:
  val byId: Map[Int, GameAssessment] = values.mapBy(_.id)
  def orDefault(id: Int) = byId.getOrElse(id, NotCheating)
