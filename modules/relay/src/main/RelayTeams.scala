package lila.relay

import chess.format.pgn.*
import chess.{ FideId, PlayerName }

import lila.core.fide.{ PlayerToken, Tokenize }
import lila.study.ChapterPreview
import lila.study.StudyPlayer

type TeamName = String

private class RelayTeamsTextarea(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  /* We need this because `PlayerName | FideId` doesn't work
   * the compiler can't differentiate between the two types
   * at runtime using pattern matching. */
  private type PlayerNameStr = String

  lazy val teams: Map[TeamName, List[PlayerNameStr | FideId]] = text.linesIterator
    .take(1000)
    .toList
    .flatMap: line =>
      line.split(';').map(_.trim) match
        case Array(team, player) => Some(team -> (player.toIntOption.fold(player)(FideId(_))))
        case _ => none
    .groupBy(_._1)
    .view
    .mapValues(_.map(_._2))
    .toMap

  private lazy val playerTeams: Map[PlayerNameStr | FideId, TeamName] =
    teams.flatMap: (team, players) =>
      players.map(_ -> team)

  def update(games: RelayGames): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags): Tags =
    Color.all.foldLeft(tags): (tags, color) =>
      val found = tags
        .fideIds(color)
        .flatMap(findMatching)
        .orElse(PlayerName.raw(tags.names(color)).flatMap(findMatching))
      found.fold(tags): team =>
        tags + Tag(_.teams(color), team)

  private def findMatching(player: PlayerNameStr | FideId)(using tokenize: Tokenize): Option[TeamName] =

    val tokenizePlayer: PlayerNameStr | FideId => PlayerToken | FideId =
      case name: PlayerNameStr => tokenize(name)
      // typing with `fideId: FideId` results in a compiler warning. The current code however is ok.
      case fideId => fideId

    lazy val tokenizedPlayerTeams: Map[PlayerToken | FideId, TeamName] =
      playerTeams.mapKeys(tokenizePlayer)

    playerTeams.get(player).orElse(tokenizedPlayerTeams.get(tokenizePlayer(player)))

final class RelayTeamTable(
    chapterPreviewApi: lila.study.ChapterPreviewApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import play.api.libs.json.*

  def tableJson(relay: RelayRound): Fu[JsonStr] = cache.get(relay.studyId)

  private val cache = cacheApi[StudyId, JsonStr](16, "relay.teamTable"):
    _.expireAfterWrite(3.seconds).buildAsyncFuture(impl.makeJson)

  private object impl:

    import chess.{ Color, Outcome }

    def makeJson(studyId: StudyId): Fu[JsonStr] =
      chapterPreviewApi
        .dataList(studyId)
        .map: chapters =>
          import json.given
          val table = makeTable(chapters)
          val ordered = ensureFirstPlayerHasWhite(table)
          JsonStr(Json.stringify(Json.obj("table" -> ordered)))

    case class TeamWithPoints(name: String, points: Float = 0):
      def add(result: Option[Outcome.GamePoints], as: Color) =
        copy(points = points + result.so(_(as).value))
    case class Pair[A](a: A, b: A):
      def is(p: Pair[A]) = (a == p.a && b == p.b) || (a == p.b && b == p.a)
      def map[B](f: A => B) = Pair(f(a), f(b))
      def bimap[B](f: A => B, g: A => B) = Pair(f(a), g(b))
      def reverse = Pair(b, a)

    case class TeamGame(id: StudyChapterId, pov: Color):
      def swap = copy(pov = !pov)

    case class TeamMatch(teams: Pair[TeamWithPoints], games: List[TeamGame]):
      def is(teamNames: Pair[TeamName]) = teams.map(_.name).is(teamNames)
      def add(
          chap: ChapterPreview,
          playerAndTeam: Pair[(StudyPlayer, TeamName)],
          points: Option[Outcome.GamePoints]
      ): TeamMatch =
        val t0Color = Color.fromWhite(playerAndTeam.a._2 == teams.a.name)
        copy(
          games = TeamGame(chap.id, t0Color) :: games,
          teams = teams.bimap(_.add(points, t0Color), _.add(points, !t0Color))
        )
      def swap = copy(teams = teams.reverse, games = games.map(_.swap))

    def makeTable(chapters: List[ChapterPreview]): List[TeamMatch] =
      chapters.reverse.foldLeft(List.empty[TeamMatch]): (table, chap) =>
        (for
          points <- chap.points
          players <- chap.players
          teams <- players.traverse(_.team).map(_.toPair).map(Pair.apply)
          m0 = table.find(_.is(teams)) | TeamMatch(teams.map(TeamWithPoints(_)), Nil)
          m1 = m0.add(chap, Pair(players.white.player -> teams.a, players.black.player -> teams.b), points)
          newTable = m1 :: table.filterNot(_.is(teams))
        yield newTable) | table

    def ensureFirstPlayerHasWhite(table: List[TeamMatch]): List[TeamMatch] =
      table.map: m =>
        if m.games.headOption.forall(_.pov.white) then m
        else m.swap

    object json:
      import lila.common.Json.given
      given [A: Writes]: Writes[Pair[A]] = Writes: p =>
        Json.arr(p.a, p.b)
      given Writes[TeamWithPoints] = Json.writes[TeamWithPoints]
      given Writes[TeamGame] = Json.writes[TeamGame]
      given Writes[TeamMatch] = Json.writes[TeamMatch]
