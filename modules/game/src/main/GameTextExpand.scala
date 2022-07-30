package lila.game

import chess.format.pgn.Pgn
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scalatags.Text.all._

import lila.common.config
import lila.memo.{ CacheApi, Syncache }
import play.api.Mode

final class GameTextExpand(
    gameRepo: GameRepo,
    netDomain: config.NetDomain,
    pgnDump: PgnDump,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext, mode: Mode) {

  def getPgn(id: Game.ID)     = pgnCache async id
  def getPgnSync(id: Game.ID) = pgnCache.sync(id)

  def fromText(text: String): Fu[lila.base.RawHtml.LinkRender] =
    gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m =>
        Option(m group 1) filter { id =>
          !notGames(id)
        } map (m.matched -> _)
      }
      .map { case (matched, id) =>
        pgnCache.async(id) map2 { removeScheme(matched) -> _ }
      }
      .sequenceFu
      .map(_.flatten.toMap) map { matches => (url: String, text: String) =>
      matches
        .get(url)
        .orElse(matches.get(removeScheme(url)))
        .fold[Frag](raw(url)) { pgn =>
          div(cls := "lpv--autostart", attr("data-pgn") := pgn.toString)
        }
    }

  def preloadGamesFromText(text: String): Funit = pgnCache preloadMany {
    gameRegex
      .findAllMatchIn(text)
      .toList
      .flatMap { m => Option(m group 1) filterNot notGames.contains }
  }

  private val gameRegex =
    s"""$netDomain/(?:embed/)?(?:game/)?(\\w{8})(?:(?:/(white|black))|\\w{4}|)(#\\d+)?\\b""".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private object removeScheme {
    private val regex      = "^(?:https?://)?(.+)$".r
    def apply(url: String) = regex.replaceAllIn(url, m => Option(m group 1) | m.matched)
  }

  private def lichessPgnViewer(game: Game.WithInitialFen, pgn: Pgn): Frag =
    div(cls := "lpv", attr("data-pgn") := pgn.toString)

  private val pgnFlags = PgnDump.WithFlags(clocks = false, evals = false, opening = false)

  private val pgnCache = cacheApi.sync[Game.ID, Option[String]](
    "gameTextExpand.pgnSync",
    512,
    compute = id => gameIdToPgn(id).map2(_.render),
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(500 millis, if (mode == Mode.Prod) 25 else 0),
    expireAfter = Syncache.ExpireAfterWrite(10 minutes)
  )

  private def gameIdToPgn(id: Game.ID): Fu[Option[Pgn]] =
    gameRepo gameWithInitialFen id flatMap {
      _ ?? { g => pgnDump(g.game, g.fen, pgnFlags) dmap some }
    }
}
