package views.html

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.user.User

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

object insights {

  def apply(user: User, path: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} - ${trans.insights.insights.txt()}",
      moreJs = frag(
        jsModule("insights"),
        jsAt("vendor/multiple-select/multiple-select.js"),
        embedJsUnsafe(s"""$$(function() {
LishogiInsights(document.getElementById('insights-app'), ${safeJsonValue(
            Json.obj(
              "username" -> user.username,
              "usernameHash" -> MessageDigest
                .getInstance("MD5")
                .digest(
                  (insightsSecret + user.id).getBytes(UTF_8)
                )
                .map("%02x".format(_))
                .mkString,
              "isBot"    -> user.isBot,
              "path"     -> path,
              "endpoint" -> insightsEndpoint,
              "i18n"     -> i18nJsObject(i18nKeys)
            )
          )})})""")
      ),
      moreCss = cssTag("insights"),
      robots = false
    ) {
      main(id := "insights-app")
    }

  def privated(user: User)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} - ${trans.insights.insights.txt()}",
      moreCss = cssTag("insights"),
      robots = false
    ) {
      main(id   := "insights-app")(
        div(cls := "page-menu__menu"),
        div(cls := "page-menu__content")(
          h1(cls := "text")(trans.isPrivate.txt())
        )
      )
    }

  private val i18nKeys: List[lila.i18n.MessageKey] =
    List(
      trans.black,
      trans.white,
      trans.sente,
      trans.gote,
      trans.custom,
      trans.nbGames,
      trans.games,
      trans.nbMoves,
      trans.winRate,
      trans.opponent,
      trans.insights.resign,
      trans.insights.noStart,
      trans.checkmate,
      trans.stalemate,
      trans.impasse,
      trans.repetition,
      trans.perpetualCheck,
      trans.royalsLost,
      trans.bareKing,
      trans.timeOut,
      trans.xDidntMove,
      trans.xLeftTheGame,
      trans.unknown,
      trans.yes,
      trans.no,
      trans.computer,
      trans.insights.insights,
      trans.insights.moves,
      trans.insights.drops,
      trans.insights.total,
      trans.insights.count,
      trans.insights.outcomes,
      trans.insights.color,
      trans.insights.termination,
      trans.insights.speed,
      trans.insights.weekday,
      trans.insights.timeOfDay,
      trans.insights.earlyBishopExchange,
      trans.insights.game,
      trans.insights.opponents,
      trans.insights.average,
      trans.insights.averageOpponentRating,
      trans.insights.averageOpponentRatingDiff,
      trans.insights.mostPlayedOpponents,
      trans.insights.averageAccuracy,
      trans.insights.wins,
      trans.insights.draws,
      trans.insights.losses,
      trans.insights.accuracyByMoveNumber,
      trans.insights.accuracyByPiece,
      trans.insights.totalTimeSpentThinking,
      trans.insights.averageTimePerGame,
      trans.insights.averageTimePerMoveOrDrop,
      trans.insights.timeSpentThinkingByPiece,
      trans.insights.movesAndDropsByPiece,
      trans.insights.capturesByPiece,
      trans.insights.promotionsByPiece,
      trans.insights.mostPlayedOpenings,
      trans.insights.openingMoves,
      trans.insights.insightsUpdate,
      trans.insights.accuracy,
      trans.insights.nbOfMovesAndDrops,
      trans.insights.nbOfMovesAndDropsPerGame,
      trans.insights.nbOfCaptures,
      trans.insights.nbOfPromotions,
      trans.insights.opponentRating,
      trans.insights.opponentRatingDiff,
      trans.insights.totalTimeOfMovesAndDrops,
      trans.insights.movesAndDrops,
      trans.insights.nbOfGames,
      // pieces
      trans.pieces.pawn,
      trans.pieces.lance,
      trans.pieces.knight,
      trans.pieces.silver,
      trans.pieces.gold,
      trans.pieces.bishop,
      trans.pieces.rook,
      trans.pieces.king,
      trans.pieces.tokin,
      trans.pieces.promotedlance,
      trans.pieces.promotedknight,
      trans.pieces.promotedsilver,
      trans.pieces.horse,
      trans.pieces.dragon,
      trans.pieces.promotedpawn,
      trans.pieces.leopard,
      trans.pieces.copper,
      trans.pieces.elephant,
      trans.pieces.chariot,
      trans.pieces.tiger,
      trans.pieces.kirin,
      trans.pieces.phoenix,
      trans.pieces.sidemover,
      trans.pieces.verticalmover,
      trans.pieces.lion,
      trans.pieces.promotedpawn,
      trans.pieces.queen,
      trans.pieces.gobetween,
      trans.pieces.whitehorse,
      trans.pieces.lionpromoted,
      trans.pieces.queenpromoted,
      trans.pieces.bishoppromoted,
      trans.pieces.sidemoverpromoted,
      trans.pieces.verticalmoverpromoted,
      trans.pieces.rookpromoted,
      trans.pieces.prince,
      trans.pieces.whale,
      trans.pieces.horsepromoted,
      trans.pieces.elephantpromoted,
      trans.pieces.stag,
      trans.pieces.boar,
      trans.pieces.ox,
      trans.pieces.falcon,
      trans.pieces.eagle,
      trans.pieces.dragonpromoted,
      // filter
      trans.filterGames,
      trans.variant,
      trans.standard,
      trans.minishogi,
      trans.chushogi,
      trans.annanshogi,
      trans.kyotoshogi,
      trans.checkshogi,
      trans.ultrabullet,
      trans.bullet,
      trans.blitz,
      trans.rapid,
      trans.classical,
      trans.correspondence,
      trans.mode,
      trans.casual,
      trans.rated,
      trans.nbDays
    ).map(_.key)
}
