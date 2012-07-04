package lila
package analyse

import com.codahale.jerkson.Json

final class AdvantageChart(advices: Analysis.InfoAdvices) {

  val max = 10

  def columns = AdvantageChart.columns

  def rows = Json generate chartValues

  private lazy val values: List[(String, Float)] =
    (advices sliding 2 collect {
      case (info, advice) :: (next, _) :: Nil ⇒
        (next.score, next.mate) match {
          case (Some(score), _) ⇒ move(info, advice) -> box(score.pawns)
          case (_, Some(mate))  ⇒ move(info, advice) -> box(info.color.fold(-mate, mate) * max)
          case _                ⇒ move(info, none) -> box(0)
        }
    }).toList

  private def chartValues: List[List[Any]] = values collect {
    case (move, score) if score > 0 ⇒ List(move, score, none)
    case (move, score) if score < 0 ⇒ List(move, none, score)
    case (move, _)                  ⇒ List(move, 0, 0)
  }

  private def box(v: Float) = math.min(max, math.max(-max, v))

  private def move(info: Info, advice: Option[Advice]) = info.color.fold(
    "%d. %s", "%d... %s"
  ).format(info.turn, info.move.uci) + advice.fold(" " + _.nag.symbol, "")

}

object AdvantageChart {

  val columns = Json generate List(
    "string" :: "Move" :: Nil,
    "number" :: "White" :: Nil,
    "number" :: "Black" :: Nil)
}
