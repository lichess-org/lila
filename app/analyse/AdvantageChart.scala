package lila
package analyse

import com.codahale.jerkson.Json

final class AdvantageChart(advices: Analysis.InfoAdvices) {

  val max = 15

  def columns = Json generate List(
    "string" :: "Move" :: Nil,
    "number" :: "Advantage" :: Nil)

  def rows = Json generate {

    val scale = floatBox(-max to max) _

    def move(info: Info, advice: Option[Advice]) = info.color.fold(
      "%d. %s", "%d... %s"
    ).format(info.turn, info.move.uci) + advice.fold(" " + _.nag.symbol, "")

    (advices sliding 2 collect {
      case (info, advice) :: (next, _) :: Nil ⇒
        (next.score, next.mate) match {
          case (Some(score), _) ⇒ List(move(info, advice), scale(score.pawns))
          case (_, Some(mate)) ⇒ List(move(info, advice), {
            val mateDelta = math.abs(mate.toFloat / 100)
            val whiteWins = info.color.fold(mate < 0, mate > 0)
            scale(whiteWins.fold(max - mateDelta, mateDelta - max))
          })
          case _ ⇒ List(move(info, none), scale(0))
        }
    }).toList
  }
}
