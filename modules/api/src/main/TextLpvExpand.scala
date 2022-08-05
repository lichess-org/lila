package lila.api

import chess.format.pgn.Pgn
import play.api.Mode
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scalatags.Text.all._

import lila.analyse.AnalysisRepo
import lila.common.config
import lila.game.{ Game, GameRepo }
import lila.memo.CacheApi

final class TextLpvExpand(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    pgnDump: PgnDump,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext) {

  def getPgn(id: Game.ID) = pgnCache get id

  def linkRenderFromText(text: String): Fu[lila.base.RawHtml.LinkRender] =
    gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m =>
        Option(m group 1) filter { id =>
          !notGames(id)
        } map (m.matched -> _)
      }
      .map { case (matched, id) =>
        pgnCache.get(id) map2 { matched -> _ }
      }
      .sequenceFu
      .map(_.flatten.toMap) map { matches => (url: String, text: String) =>
      matches
        .get(url)
        .map { pgn =>
          div(
            cls                      := "lpv--autostart",
            attr("data-pgn")         := pgn.toString,
            attr("data-orientation") := chess.Color.fromWhite(!url.contains("black")).name
          )
        }
    }

  def gamePgnsFromText(text: String): Fu[Map[Game.ID, String]] = {
    val gameIds = gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m => Option(m group 1) filterNot notGames.contains }
      .distinct
    pgnCache getAll gameIds map {
      _.collect { case (gameId, Some(pgn)) => gameId -> pgn }
    }
  }

  private val gameRegex =
    s"""/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b""".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private def lichessPgnViewer(game: Game.WithInitialFen, pgn: Pgn): Frag =
    div(cls := "lpv", attr("data-pgn") := pgn.toString)

  private val pgnFlags =
    lila.game.PgnDump.WithFlags(clocks = true, evals = true, opening = false, literate = true)

  private val pgnCache = cacheApi[Game.ID, Option[String]](512, "textLpvExpand.pgn") {
    _.expireAfterWrite(10 minutes).buildAsyncFuture(id => gameIdToPgn(id).map2(_.render))
  }

  private def gameIdToPgn(id: Game.ID): Fu[Option[Pgn]] =
    gameRepo gameWithInitialFen id flatMap {
      _ ?? { g =>
        analysisRepo.byId(id) flatMap { analysis =>
          pgnDump(g.game, g.fen, analysis, pgnFlags) dmap some
        }
      }
    }
}
