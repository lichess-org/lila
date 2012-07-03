package lila
package analyse

import com.codahale.jerkson.Json

final class AdvantageChart(infos: List[Info]) {

  val max = 10

  def columns = AdvantageChart.columns

  def rows = Json generate values.map(_.map(x ⇒ List(x._1, x._2)))

  private lazy val values: List[Option[(String, Float)]] = (infos sliding 2 map {
    case info :: next :: Nil ⇒ (next.score, next.mate) match {
      case (Some(score), _) ⇒ Some(move(info, next) -> box(score.pawns))
      case (_, Some(mate))  ⇒ Some(move(info, next) -> box(info.color.fold(-mate, mate) * max))
      case _                ⇒ None
    }
    case _ ⇒ None
  }).toList.dropWhile(_.isEmpty).reverse.dropWhile(_.isEmpty).reverse

  private def box(v: Float) = math.min(max, math.max(-max, v))

  private def move(info: Info, next: Info) = info.color.fold(
    "%d. %s", "%d... %s"
  ).format(info.turn, info.move.uci) + Advice(info, next).fold(" " + _.nag.symbol, "")

}

object AdvantageChart {

  val columns = Json generate List(
    "string" :: "Move" :: Nil,
    "number" :: "Advantage" :: Nil)
}
