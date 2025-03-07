package lila.api

import chess.format.pgn.PgnStr
import scalatags.Text.all.*

import lila.analyse.{ Analysis, AnalysisRepo }
import lila.core.config.NetDomain
import lila.core.i18n.{ Translate, Translator }
import lila.core.misc.lpv.*
import lila.memo.CacheApi

final class TextLpvExpand(
    gameRepo: lila.core.game.GameRepo,
    analysisRepo: AnalysisRepo,
    studyApi: lila.study.StudyApi,
    pgnDump: PgnDump,
    studyPgnDump: lila.study.PgnDump,
    cacheApi: CacheApi,
    net: lila.core.config.NetConfig
)(using Executor, Translator):

  def getPgn(id: GameId) = if notGames.contains(id.value) then fuccess(none) else gamePgnCache.get(id)
  def getChapterPgn(id: StudyChapterId) = chapterPgnCache.get(id)
  def getStudyPgn(id: StudyId)          = studyPgnCache.get(id)

  // forum linkRenderFromText builds a LinkRender from relative game|chapter urls -> lpv div tags.
  // substitution occurs in common/../RawHtml.scala addLinks
  def linkRenderFromText(text: String): Fu[LinkRender] =
    regex.forumPgnCandidatesRe
      .findAllMatchIn(text)
      .map(_.group(1))
      .map:
        case regex.gamePgnRe(url, id)    => getPgn(GameId(id)).map(url -> _)
        case regex.chapterPgnRe(url, id) => getChapterPgn(StudyChapterId(id)).map(url -> _)
        case regex.studyPgnRe(url, id)   => getStudyPgn(StudyId(id)).map(url -> _)
        case link                        => fuccess(link -> link)
      .parallel
      .map:
        _.collect { case (url, Some(LpvEmbed.PublicPgn(pgn))) => url -> pgn }.toMap
      .map: pgns =>
        (url, _) =>
          pgns
            .get(url)
            .map: pgn =>
              div(
                cls              := "lpv--autostart is2d",
                attr("data-pgn") := pgn.value,
                plyRe.findFirstIn(url).map(_.substring(1)).map(ply => attr("data-ply") := ply),
                (url.contains("/black")).option(attr("data-orientation") := "black")
              )

  // used by blogs & ublogs to build game|chapter id -> pgn maps
  // the substitution happens later in blog/BlogApi or common/MarkdownRender
  def allPgnsFromText(text: String, max: Max): Fu[Map[String, LpvEmbed]] =
    regex.blogPgnCandidatesRe
      .findAllMatchIn(text)
      .map(_.group(1))
      .toList
      .foldLeft(max.value -> List.empty[Fu[(String, Option[LpvEmbed])]]):
        case ((0, replacements), _) => 0 -> replacements
        case ((counter, replacements), candidate) =>
          val (cost, replacement) = candidate match
            case regex.gamePgnRe(url, id)    => 1 -> getPgn(GameId(id)).map(id -> _)
            case regex.chapterPgnRe(url, id) => 1 -> getChapterPgn(StudyChapterId(id)).map(id -> _)
            case regex.studyPgnRe(url, id)   => 1 -> getStudyPgn(StudyId(id)).map(id -> _)
            case link                        => 0 -> fuccess(link -> none)
          (counter - cost) -> (replacement :: replacements)
      ._2
      .parallel
      .map:
        _.collect:
          case (id, Some(embed)) => id -> embed
        .toMap

  private val regex = LpvGameRegex(net.domain)
  private val plyRe = raw"#(\d+)\z".r

  private val notGames =
    Set("training", "analysis", "insights", "practice", "features", "password", "streamer", "timeline")

  private val pgnFlags =
    lila.game.PgnDump.WithFlags(clocks = true, evals = true, opening = false, literate = true)

  private val gamePgnCache = cacheApi[GameId, Option[LpvEmbed]](256, "textLpvExpand.pgn.game"):
    _.expireAfterWrite(10.minutes).buildAsyncFuture(gameIdToPgn)

  private val chapterPgnCache = cacheApi[StudyChapterId, Option[LpvEmbed]](256, "textLpvExpand.pgn.chapter"):
    _.expireAfterWrite(10.minutes).buildAsyncFuture(studyChapterIdToPgn)

  private val studyPgnCache = cacheApi[StudyId, Option[LpvEmbed]](64, "textLpvExpand.pgn.firstChapter"):
    _.expireAfterWrite(10.minutes).buildAsyncFuture(studyIdToPgn)

  private def gameIdToPgn(id: GameId): Fu[Option[LpvEmbed]] =
    given Translate = summon[Translator].toDefault
    gameRepo
      .gameWithInitialFen(id)
      .flatMapz: g =>
        analysisRepo
          .byId(Analysis.Id(id))
          .flatMap: analysis =>
            pgnDump(g.game, g.fen, analysis, pgnFlags).map: pgn =>
              LpvEmbed.PublicPgn(pgn.render).some

  private def studyChapterIdToPgn(id: StudyChapterId): Fu[Option[LpvEmbed]] =
    val flags = lila.study.PgnDump.fullFlags
    studyApi
      .byChapterId(id)
      .flatMapz: sc =>
        if sc.study.isPrivate then fuccess(LpvEmbed.PrivateStudy.some)
        else studyPgnDump.ofChapter(sc.study, flags)(sc.chapter).map(LpvEmbed.PublicPgn.apply).map(_.some)

  private def studyIdToPgn(id: StudyId): Fu[Option[LpvEmbed]] =
    val flags = lila.study.PgnDump.fullFlags
    studyApi
      .byId(id)
      .flatMapz: s =>
        if s.isPrivate then fuccess(LpvEmbed.PrivateStudy.some)
        else studyPgnDump.ofFirstChapter(s, flags).map2(LpvEmbed.PublicPgn.apply)

final class LpvGameRegex(domain: NetDomain):

  private val quotedDomain = java.util.regex.Pattern.quote(domain.value)

  val pgnCandidates = raw"""(?:https?://)?(?:lichess\.org|$quotedDomain)(/[/\w#]{8,})\b"""

  val blogPgnCandidatesRe  = pgnCandidates.r
  val forumPgnCandidatesRe = raw"(?m)^$pgnCandidates".r

  val params = raw"""(?:#(?:last|\d{1,4}))?"""

  val gamePgnRe    = raw"^(/(\w{8})(?:\w{4}|/(?:white|black))?$params)$$".r
  val chapterPgnRe = raw"^(/study/(?:embed/)?(?:\w{8})/(\w{8})$params)$$".r
  val studyPgnRe   = raw"^(/study/(?:embed/)?(\w{8})$params)$$".r
