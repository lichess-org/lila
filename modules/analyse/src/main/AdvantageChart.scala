package lila.analyse

import play.api.libs.json.Json

final class AdvantageChart(
    advices: InfoAdvices,
    pgnMoves: IndexedSeq[String]) {

  val max = Score.CEILING / 100

  def columns = Json.arr(
    Json.arr("string", "Move"),
    Json.arr("number", "Advantage")
  )

  def rows = Json toJson {

    val scale = floatBox(-max to max) _

    def move(info: Info, advice: Option[Advice]) = "%s%s %s%s".format(
      info.turn,
      info.color.fold(".", "..."),
      pgnMoves lift (info.ply - 1) getOrElse "",
      advice.??(" " + _.makeComment(false))
    )

    advices map {
      case (info, advice) ⇒
        (info.score, info.mate) match {
          case (Some(score), _) ⇒ Json.arr(move(info, advice), scale(score.pawns).toString)
          case (_, Some(mate)) ⇒ Json.arr(move(info, advice), {
            val mateDelta = math.abs(mate.toFloat / 100)
            val whiteWins = mate > 0
            scale(whiteWins.fold(max - mateDelta, mateDelta - max)).toString
          })
          case _ ⇒ Json.arr(move(info, none), scale(0))
        }
    }
  }
}

object AdvantageChart {

  def apply(advices: InfoAdvices, pgnStr: String): AdvantageChart =
    new AdvantageChart(advices, pgnStr.split(' ').toIndexedSeq)
}
