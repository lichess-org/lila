package lila.analyse

import play.api.libs.json.Json

object AdvantageChart {

  val max = Score.CEILING
  private val scale = intBox(-max to max) _

  def apply(advices: InfoAdvices, moves: List[String]): String = {

    val pgnMoves = moves.toIndexedSeq

    def move(info: Info, advice: Option[Advice]) = "%s%s %s%s".format(
      info.turn,
      info.color.fold(".", "..."),
      pgnMoves lift (info.ply - 1) getOrElse "",
      advice.??(" " + _.makeComment(withEval = true, withBestMove = false))
    )

    def point(name: String, y: Int) = Json.obj(
      "name" -> name,
      "y" -> scale(y))

    Json stringify {
      Json toJson {
        advices map {
          case (info, advice) =>
            (info.score, info.mate) match {
              case (Some(score), _) => point(move(info, advice), score.centipawns)
              case (_, Some(mate)) => point(move(info, advice), {
                val mateDelta = math.abs(mate)
                val whiteWins = mate > 0
                whiteWins.fold(max - mateDelta, mateDelta - max)
              })
              case _ => point(move(info, none), 0)
            }
        }
      }
    }
  }
}
