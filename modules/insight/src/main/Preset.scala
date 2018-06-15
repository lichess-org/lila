package lila.insight

import lila.rating.PerfType

case class Preset(name: String, question: Question[_])

object Preset {

  import lila.insight.{ Dimension => D, Metric => M }

  val all = List(

    Preset(
      "Do I gain more rating points against weaker or stronger opponents?",
      Question(D.OpponentStrength, M.RatingDiff, Nil)
    ),

    Preset(
      "How quickly do I move each piece in bullet and blitz games?",
      Question(D.PieceRole, M.Movetime, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz))
      ))
    ),

    Preset(
      "What is the Win-Rate of my favourite openings as white?",
      Question(D.Opening, M.Result, List(
        Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)),
        Filter(D.Color, List(chess.White))
      ))
    ),

    Preset(
      "How often do I punish blunders made by my opponent during each game phase?",
      Question(D.Phase, M.Opportunism, Nil)
    ),

    Preset(
      "Do I gain rating when I don't castle kingside?",
      Question(D.Perf, M.RatingDiff, List(
        Filter(D.MyCastling, List(Castling.Queenside, Castling.None))
      ))
    ),

    Preset(
      "When I trade queens, how do games end?",
      Question(D.Perf, M.Result, List(
        Filter(D.QueenTrade, List(QueenTrade.Yes))
      ))
    ),

    Preset(
      "What is the average rating of my opponents across each variant?",
      Question(D.Perf, M.OpponentRating, Nil)
    ),

    Preset(
      "How well do I move each piece in the opening?",
      Question(D.PieceRole, M.MeanCpl, List(
        Filter(D.Phase, List(Phase.Opening))
      ))
    )
  )
}
