package lila.api

import chess.format.pgn.Pgn
import play.api.Mode
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scalatags.Text.all.*

import lila.analyse.AnalysisRepo
import lila.common.config
import lila.game.{ Game, GameRepo }
import lila.memo.CacheApi

final class TextLpvExpand(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    pgnDump: PgnDump,
    cacheApi: CacheApi
)(using ec: ExecutionContext):

  def getPgn(id: GameId) = pgnCache get id

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
        pgnCache.get(GameId(id)) map2 { matched -> _ }
      }
      .sequenceFu
      .map(_.flatten.toMap) map { matches => (url: String, text: String) =>
      matches
        .get(url)
        .map { pgn =>
          div(
            cls                      := "lpv--autostart is2d",
            attr("data-pgn")         := pgn.toString,
            attr("data-orientation") := chess.Color.fromWhite(!url.contains("black")).name,
            attr("data-ply")         := plyRegex.findFirstIn(url).fold("last")(_.substring(1))
          )
        }
    }

  def gamePgnsFromText(text: String): Fu[Map[GameId, String]] =
    val gameIds = gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m => Option(m group 1) filterNot notGames.contains }
      .distinct
      .map(GameId(_))
    pgnCache getAll gameIds map {
      _.collect { case (gameId, Some(pgn)) => gameId -> pgn }
    }

  private val gameRegex =
    s"""/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b""".r

  private val plyRegex = raw"#(\d+)\z".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private def lichessPgnViewer(game: Game.WithInitialFen, pgn: Pgn): Frag =
    div(cls := "lpv", attr("data-pgn") := pgn.toString)

  private val pgnFlags =
    lila.game.PgnDump.WithFlags(clocks = true, evals = true, opening = false, literate = true)

  private val pgnCache = cacheApi[GameId, Option[String]](512, "textLpvExpand.pgn") {
    _.expireAfterWrite(10 minutes).buildAsyncFuture(id => gameIdToPgn(id).map2(_.render))
  }

  private def gameIdToPgn(id: GameId): Fu[Option[Pgn]] =
    gameRepo gameWithInitialFen id flatMap {
      _ ?? { g =>
        analysisRepo.byId(id.value) flatMap { analysis =>
          pgnDump(g.game, g.fen, analysis, pgnFlags) dmap some
        }
      }
    }
