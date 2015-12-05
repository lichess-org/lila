package lila.insight

import lila.rating.PerfType

case class Preset(name: String, question: Question[_])

object Preset {

  import lila.insight.{ Dimension => D, Metric => M }

  val all = List(

    Preset("Do I gain more rating points against weaker or stronger opponents?",
      Question(D.OpponentStrength, M.RatingDiff, Nil)),

    Preset("How fast do I move each piece in bullet and blitz games?",
      Question(D.PieceRole, M.Movetime, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz))))),

    Preset("What are my results for my favourite openings as white, in standard chess?",
      Question(D.Opening, M.Result, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz, PerfType.Classical, PerfType.Correspondence)),
        Filter(D.Color, List(chess.White))
      ))),

    Preset("Am I more accurate when I trade queens?",
      Question(D.PieceRole, M.MeanCpl, Nil))
  )
}
