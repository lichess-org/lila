package lila.analyse

import play.api.libs.json.Json

final class AdvantageChart(
    advices: InfoAdvices,
    pgnMoves: IndexedSeq[String]) {

  val max = Score.CEILING 

  def columns = Json.arr(
    Json.arr("string", "Move"),
    Json.arr("number", "Advantage")
  )

  def rows = Json toJson {

    val scale = intBox(-max to max) _

    def move(info: Info, advice: Option[Advice]) = "%s%s %s%s".format(
      info.turn,
      info.color.fold(".", "..."),
      pgnMoves lift (info.ply - 1) getOrElse "",
      advice.??(" " + _.makeComment(withEval = true, withBestMove = false))
    )

    def point(name: String, y: Int) = Json.obj(
      "name" -> name,
      "y" -> scale(y))

    advices map {
      case (info, advice) ⇒
        (info.score, info.mate) match {
          case (Some(score), _) ⇒ point(move(info, advice), score.centipawns)
          case (_, Some(mate)) ⇒ point(move(info, advice), {
            val mateDelta = math.abs(mate)
            val whiteWins = mate > 0
            whiteWins.fold(max - mateDelta, mateDelta - max)
          })
          case _ ⇒ point(move(info, none), 0)
        }
    }
  }
}

object AdvantageChart {

  def apply(advices: InfoAdvices, pgnStr: String): AdvantageChart =
    new AdvantageChart(advices, pgnStr.split(' ').toIndexedSeq)
}
