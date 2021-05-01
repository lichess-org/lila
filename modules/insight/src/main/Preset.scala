package lila.insight

import lila.rating.PerfType
import lila.i18n.{ I18nKeys => trans }

case class Preset(name: String, question: Question[_])

object Preset {

  import lila.insight.{ Dimension => D, Metric => M }

  private val filterBlitzPlus = List(
    Filter(D.Perf, List(PerfType.Blitz, PerfType.Rapid, PerfType.Classical))
  )

  val forMod = List(
    Preset(
      trans.acplByDate.txt(),
      Question(D.Date, M.MeanCpl, filterBlitzPlus)
    ),
    Preset(
      trans.blursByDate.txt(),
      Question(D.Date, M.Blurs, filterBlitzPlus)
    ),
    Preset(
      trans.acplByBlur.txt(),
      Question(D.Blur, M.MeanCpl, filterBlitzPlus)
    ),
    Preset(
      trans.blursByResult.txt(),
      Question(D.Result, M.Blurs, filterBlitzPlus)
    ),
    Preset(
      trans.acplByTimeVariance.txt(),
      Question(D.TimeVariance, M.MeanCpl, Nil)
    ),
    Preset(
      trans.blurByTimeVariance.txt(),
      Question(D.TimeVariance, M.Blurs, filterBlitzPlus)
    ),
    Preset(
      trans.timeVarianceByDate.txt(),
      Question(D.Date, M.TimeVariance, Nil)
    )
  )

  val base = List(
    Preset(
      trans.doIGainMoreRatingPointsAgainstWeakerOrStrongerOpponents.txt(),
      Question(D.OpponentStrength, M.RatingDiff, Nil)
    ),
    Preset(
      trans.howQuicklyDoIMoveEachPieceInBulletAndBlitzGames.txt(),
      Question(
        D.PieceRole,
        M.Movetime,
        List(
          Filter(D.Perf, List(PerfType.Bullet, PerfType.Blitz))
        )
      )
    ),
    Preset(
      trans.whatIsTheWinRateOfMyFavouriteOpeningsAsWhite.txt(),
      Question(
        D.Opening,
        M.Result,
        List(
          Filter(
            D.Perf,
            List(PerfType.Bullet, PerfType.Blitz, PerfType.Rapid, PerfType.Classical, PerfType.Correspondence)
          ),
          Filter(D.Color, List(chess.White))
        )
      )
    ),
    Preset(
      trans.howOftenDoIPunishBlundersMadeByMyOpponentDuringEachGamePhase.txt(),
      Question(D.Phase, M.Opportunism, Nil)
    ),
    Preset(
      trans.doIGainRatingWhenIDontCastleKingside.txt(),
      Question(
        D.Perf,
        M.RatingDiff,
        List(
          Filter(D.MyCastling, List(Castling.Queenside, Castling.None))
        )
      )
    ),
    Preset(
      trans.whenITradeQueensHowDoGamesEnd.txt(),
      Question(
        D.Perf,
        M.Result,
        List(
          Filter(D.QueenTrade, List(QueenTrade.Yes))
        )
      )
    ),
    Preset(
      trans.whatIsTheAverageRatingOfMyOpponentsAcrossEachVariant.txt(),
      Question(D.Perf, M.OpponentRating, Nil)
    ),
    Preset(
      trans.howWellDoIMoveEachPieceInTheOpening.txt(),
      Question(
        D.PieceRole,
        M.MeanCpl,
        List(
          Filter(D.Phase, List(Phase.Opening))
        )
      )
    )
  )
}
