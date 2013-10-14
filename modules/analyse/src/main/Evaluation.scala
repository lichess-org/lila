package lila.analyse

case class Evaluation(
    score: Option[Score],
    mate: Option[Int],
    line: List[String]) {

  def checkMate = mate == Some(0)

  override def toString = s"Evaluation ${score.fold("?")(_.showPawns)} ${mate | 0} ${line.mkString(" ")}"
}

object Evaluation {

  lazy val start = Evaluation(Score(20).some, none, Nil)

  def toInfos(evals: List[Evaluation], moves: List[String]): List[Info] =
    (evals filterNot (_.checkMate) sliding 2).toList.zip(moves).zipWithIndex map {
      case ((List(before, after), move), index) ⇒ {
        Info(
          ply = index + 1,
          score = after.score,
          mate = after.mate,
          variation = before.line match {
            case first :: rest if first != move ⇒ first :: rest
            case _                              ⇒ Nil
          }) |> { info ⇒
            if (info.ply % 2 == 1) info.reverse else info
          }
      }
    }
}

