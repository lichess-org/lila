package lila.api

import chess.format.pgn.Pgn
import scalatags.Text.all.*

import lila.analyse.AnalysisRepo
import lila.game.GameRepo
import lila.memo.CacheApi
import chess.format.pgn.PgnStr

final class TextLpvExpand(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    pgnDump: PgnDump,
    cacheApi: CacheApi,
    net: lila.common.config.NetConfig
)(using Executor):

  def getPgn(id: GameId) = pgnCache get id

  // forum linkRenderFromText builds a LinkRender from relative game urls -> lpv div tags.
  // substitution occurs in common/../RawHtml.scala addLinks
  def linkRenderFromText(text: String): Fu[lila.base.RawHtml.LinkRender] =
    regex.linkRenderRe
      .findAllMatchIn(text)
      .toList
      .flatMap { m =>
        Option(m.group(2)) filter { id =>
          !notGames(id)
        } map (m.group(1) -> _)
      }
      .map { case (matched, id) =>
        pgnCache.get(GameId(id)) map2 { matched -> _ }
      }
      .parallel
      .map(_.flatten.toMap) map { matches => (url, _) =>
      matches
        .get(url)
        .map { pgn =>
          div(
            cls                      := "lpv--autostart is2d",
            attr("data-pgn")         := pgn.toString,
            attr("data-orientation") := chess.Color.fromWhite(!url.contains("black")).name,
            attr("data-ply")         := plyRe.findFirstIn(url).fold("last")(_.substring(1))
          )
        }
    }

  // gamePgnsFromText is used by blogs & ublogs to build game id -> pgn maps but the
  // substitution happens in blog/BlogApi (blogs) and common/MarkdownRender for ublogs
  def gamePgnsFromText(text: String): Fu[Map[GameId, PgnStr]] =
    val gameIds = regex.gamePgnsRe
      .findAllMatchIn(text)
      .toList
      .flatMap { m => Option(m.group(1)) filterNot notGames.contains }
      .distinct
      .map(GameId(_))
    pgnCache getAll gameIds map {
      _.collect { case (gameId, Some(pgn)) => gameId -> pgn }
    }

  private val regex = LpvGameRegex(net.domain.toString)
  private val plyRe = raw"#(\d+)\z".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private val pgnFlags =
    lila.game.PgnDump.WithFlags(clocks = true, evals = true, opening = false, literate = true)

  private val pgnCache = cacheApi[GameId, Option[PgnStr]](512, "textLpvExpand.pgn") {
    _.expireAfterWrite(10 minutes).buildAsyncFuture(id => gameIdToPgn(id).map2(_.render))
  }

  private def gameIdToPgn(id: GameId): Fu[Option[Pgn]] =
    gameRepo gameWithInitialFen id flatMapz { g =>
      analysisRepo.byId(id.value) flatMap { analysis =>
        pgnDump(g.game, g.fen, analysis, pgnFlags) dmap some
      }
    }

final class LpvGameRegex(domain: String):

// linkified forum hrefs are relative but this regex runs pre-linkify, match path & id
  val linkRenderRe =
    raw"(?m)^(?:(?:https?://)?$domain)?(/(\w{8})(?:/(?:white|black)|\w{4}|)(?:#(?:last|\d+))?)\b".r

// for blogs, only allow absolute links and match id
  val gamePgnsRe =
    raw"(?:https?://)?$domain/(\w{8})(?:/(?:white|black)|\w{4}|)(?:#(?:last|\d+))?\b".r
