package lila.api

import chess.format.pgn.Pgn
import scalatags.Text.all.*

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.game.GameRepo
import lila.memo.CacheApi
import chess.format.pgn.PgnStr
import lila.common.config.NetDomain

final class TextLpvExpand(
    gameRepo: GameRepo,
    analysisRepo: AnalysisRepo,
    studyApi: lila.study.StudyApi,
    pgnDump: PgnDump,
    studyPgnDump: lila.study.PgnDump,
    cacheApi: CacheApi,
    net: lila.common.config.NetConfig
)(using Executor):

  export gamePgnCache.{ get as getPgn }

  // forum linkRenderFromText builds a LinkRender from relative game/chapter urls -> lpv div tags.
  // substitution occurs in common/../RawHtml.scala addLinks
  def linkRenderFromText(text: String): Fu[lila.base.RawHtml.LinkRender] = for
    games <- regex.forumGamePgnsRe
      .findAllMatchIn(text)
      .toList
      .flatMap: m =>
        Option(m.group(2)).filterNot(notGames).map(m.group(1) -> _)
      .map: (matched, id) =>
        gamePgnCache.get(GameId(id)) map2 { matched -> _ }
      .parallel
      .map(_.flatten.toMap)
    chapters <- regex.forumChapterPgnsRe
      .findAllMatchIn(text)
      .toList
      .flatMap: m =>
        Option(m.group(1)).map(m.group(1) -> _)
      .map: (matched, id) =>
        chapterPgnCache.get(StudyChapterId(id)) map2 { matched -> _ }
      .parallel
      .map(_.flatten.toMap)
    matches = games ++ chapters
  yield (url, _) =>
    matches
      .get(url)
      .map: pgn =>
        div(
          cls                      := "lpv--autostart is2d",
          attr("data-pgn")         := pgn.toString,
          attr("data-orientation") := chess.Color.fromWhite(!url.contains("black")).name,
          attr("data-ply")         := plyRe.findFirstIn(url).fold("last")(_.substring(1))
        )

  // gamePgnsFromText is used by blogs & ublogs to build game id -> pgn maps but the
  // substitution happens in blog/BlogApi (blogs) and common/MarkdownRender for ublogs
  def gamePgnsFromText(text: String): Fu[Map[GameId, PgnStr]] =
    val gameIds = GameId from
      regex.gamePgnsRe
        .findAllMatchIn(text)
        .toList
        .flatMap { m => Option(m.group(1)) filterNot notGames.contains }
        .distinct
    gamePgnCache getAll gameIds map {
      _.collect { case (gameId, Some(pgn)) => gameId -> pgn }
    }

  // studyPgnsFromText is used by blogs & ublogs to build chapter id -> pgn maps but the
  // substitution happens in blog/BlogApi (blogs) and common/MarkdownRender for ublogs
  def chapterPgnsFromText(text: String): Fu[Map[StudyChapterId, PgnStr]] =
    val chapterIds = StudyChapterId from
      regex.chapterPgnsRe
        .findAllMatchIn(text)
        .toList
        .flatMap { m => Option(m.group(1)) }
        .distinct
    chapterPgnCache getAll chapterIds map {
      _.collect { case (chapterId, Some(pgn)) => chapterId -> pgn }
    }

  // studyPgnsFromText is used by blogs & ublogs to build chapter id -> pgn maps but the
  // substitution happens in blog/BlogApi (blogs) and common/MarkdownRender for ublogs
  def allPgnsFromText(text: String): Fu[Map[String, PgnStr]] =
    gamePgnsFromText(text) zip chapterPgnsFromText(text) map { (g, c) =>
      g.mapKeys(_.value) ++ c.mapKeys(_.value)
    }

  private val regex = LpvGameRegex(net.domain)
  private val plyRe = raw"#(\d+)\z".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private val pgnFlags =
    lila.game.PgnDump.WithFlags(clocks = true, evals = true, opening = false, literate = true)

  private val gamePgnCache = cacheApi[GameId, Option[PgnStr]](512, "textLpvExpand.pgn.game"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture(id => gameIdToPgn(id).map2(_.render))

  private val chapterPgnCache = cacheApi[StudyChapterId, Option[PgnStr]](512, "textLpvExpand.pgn.chapter"):
    _.expireAfterWrite(10 minutes).buildAsyncFuture(chapterIdToPgn)

  private def gameIdToPgn(id: GameId): Fu[Option[Pgn]] =
    gameRepo gameWithInitialFen id flatMapz { g =>
      analysisRepo.byId(id into Analysis.Id) flatMap { analysis =>
        pgnDump(g.game, g.fen, analysis, pgnFlags) dmap some
      }
    }

  private def chapterIdToPgn(id: StudyChapterId): Fu[Option[PgnStr]] =
    val flags = lila.study.PgnDump.fullFlags
    studyApi.byChapterId(id) flatMapz { sc =>
      studyPgnDump.ofChapter(sc.study, flags)(sc.chapter) dmap some
    }

final private class LpvGameRegex(domain: NetDomain):

// linkified forum hrefs are relative but these regexes run pre-linkify, match path & id
  val forumGamePgnsRe =
    raw"(?m)^(?:(?:https?://)?$domain)?(/(\w{8})(?:/(?:white|black)|\w{4}|)(?:#(?:last|\d+))?)\b".r
  val forumChapterPgnsRe =
    raw"(?m)^(?:(?:https?://)?$domain)?/study/(?:embed/)?(?:\w{8})/(\w{8})(?:(#|\b))".r

// for blogs, only allow absolute links and match id
  val gamePgnsRe =
    raw"(?:https?://)?$domain/(\w{8})(?:/(?:white|black)|\w{4}|)(?:#(?:last|\d+))?\b".r

// for blogs, only allow absolute links and match id
  val chapterPgnsRe =
    raw"(?:https?://)?$domain/study/(?:embed/)?(?:\w{8})/(\w{8})(?:(#|\b))".r
