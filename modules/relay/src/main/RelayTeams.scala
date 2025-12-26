package lila.relay

import chess.format.pgn.*
import chess.{ FideId, PlayerName }
import chess.Outcome.Points

import lila.core.fide.{ PlayerToken, Tokenize }
import lila.study.ChapterPreview
import lila.study.StudyPlayer
import scala.collection.immutable.SeqMap

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
    .mapValues(_._2F)
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
    roundRepo: RelayRoundRepo,
    chapterPreviewApi: lila.study.ChapterPreviewApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import chess.{ Color, ByColor }
  import play.api.libs.json.*

  def tableJson(relay: RelayRound): Fu[JsObject] = jsonCache.get(relay.studyId)

  def table(relay: RelayRound): Fu[List[TeamMatch]] =
    cache.get(relay.studyId)

  private val cache = cacheApi[StudyId, List[TeamMatch]](64, "relay.teamTable"):
    _.expireAfterWrite(3.seconds).buildAsyncFuture(impl.compute)

  private val jsonCache = cacheApi[StudyId, JsObject](8, "relay.teamTable.json"):
    _.expireAfterWrite(3.seconds).buildAsyncFuture(impl.makeJson)

  case class TeamWithGames(name: TeamName, games: List[RelayPlayer.Game]):
    def add(game: RelayPlayer.Game) =
      copy(games = games :+ game)
    def points = games.foldMap(_.playerScore)
  case class Pair[A](a: A, b: A):
    def is(p: Pair[A]) = (a == p.a && b == p.b) || (a == p.b && b == p.a)
    def map[B](f: A => B) = Pair(f(a), f(b))
    def bimap[B](f: A => B, g: A => B) = Pair(f(a), g(b))
    def forall(f: A => Boolean) = f(a) && f(b)
    def foreach(f: A => Unit): Unit =
      f(a)
      f(b)
    def reverse = Pair(b, a)

  case class TeamGame(id: StudyChapterId, pov: Color):
    def swap = copy(pov = !pov)

  case class TeamMatch(teams: Pair[TeamWithGames], games: List[TeamGame]):
    def is(teamNames: Pair[TeamName]) = teams.map(_.name).is(teamNames)
    def add(
        chap: ChapterPreview,
        playerAndTeam: Pair[(StudyPlayer, TeamName)],
        game: ByColor[RelayPlayer.Game]
    ): TeamMatch =
      val t0Color = Color.fromWhite(playerAndTeam.a._2 == teams.a.name)
      copy(
        games = TeamGame(chap.id, t0Color) :: games,
        teams = teams.bimap(_.add(game(t0Color)), _.add(game(!t0Color)))
      )
    def swap = copy(teams = teams.reverse, games = games.map(_.swap))
    def outcome: Option[Pair[Points]] =
      teams
        .forall(t => t.games.nonEmpty)
        .so:
          (teams.a.points, teams.b.points).mapN: (aPoints, bPoints) =>
            if aPoints == bPoints then Pair(Points.Half, Points.Half)
            else if aPoints > bPoints then Pair(Points.One, Points.Zero)
            else Pair(Points.Zero, Points.One)
  private object impl:

    def makeJson(studyId: StudyId): Fu[JsObject] =
      import json.given
      for matches <- cache.get(studyId)
      yield Json.obj("table" -> matches)

    def compute(studyId: StudyId): Fu[List[TeamMatch]] =
      for
        round <- roundRepo.byId(studyId.into(RelayRoundId)).orFail(s"Missing relay round $studyId")
        chapters <- chapterPreviewApi.dataList(studyId)
        table = makeTable(chapters, round)
        ordered = ensureFirstPlayerHasWhite(table)
      yield ordered

    def makeTable(chapters: List[ChapterPreview], round: RelayRound): List[TeamMatch] =
      chapters.reverse.foldLeft(List.empty[TeamMatch]): (table, chap) =>
        (for
          points <- chap.points
          players <- chap.players.map(_.map(_.studyPlayer))
          teams <- players.traverse(_.team).map(_.toPair).map(Pair.apply)
          game = players.mapWithColor: (c, p) =>
            RelayPlayer.Game(round.id, chap.id, p, c, points, round.rated, round.customScoring, false)
          m0 = table.find(_.is(teams)) | TeamMatch(teams.map(TeamWithGames(_, Nil)), Nil)
          m1 = m0.add(
            chap,
            Pair(players.white.player -> teams.a, players.black.player -> teams.b),
            game
          )
          newTable = m1 :: table.filterNot(_.is(teams))
        yield newTable) | table

    def ensureFirstPlayerHasWhite(table: List[TeamMatch]): List[TeamMatch] =
      table.map: m =>
        if m.games.headOption.forall(_.pov.white) then m
        else m.swap

    object json:
      import lila.common.Json.given
      import RelayPlayer.json.given
      given [A: Writes]: Writes[Pair[A]] = Writes: p =>
        Json.arr(p.a, p.b)
      given Writes[TeamGame] = Json.writes[TeamGame]
      given Writes[TeamMatch] = Json.writes[TeamMatch]
      given Writes[TeamWithGames] = t =>
        Json
          .obj(
            "name" -> t.name,
            "games" -> t.games
          )
          .add("points" -> t.points)

final class TeamLeaderboard(
    tourRepo: RelayTourRepo,
    relayGroupApi: RelayGroupApi,
    roundRepo: RelayRoundRepo,
    teamTable: RelayTeamTable,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import play.api.libs.json.*

  type TeamLeaderboard = SeqMap[TeamName, List[RelayPlayer.Game]]

  def leaderboardJson(tour: RelayTourId): Fu[TeamLeaderboard] = cache.get(tour)

  private val cache = cacheApi[RelayTourId, TeamLeaderboard](12, "relay.teamLeaderboard"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(aggregate)

  private def aggregate(tourId: RelayTourId) =
    for
      scoreGroup <- relayGroupApi.scoreGroupOf(tourId)
      tours <- scoreGroup.traverse(tourRepo.byId(_))
      rounds <- tours.map(_.traverse(t => roundRepo.byTourOrdered(t.id)))
      table <- rounds.flatTraverse(teamTable.table)
    yield table.foldLeft(SeqMap.empty: TeamLeaderboard): (acc, teamMatch) =>
      teamMatch.teams.foreach(team =>
        acc.updated(team.name, acc.get(team.name).fold(team.games)(_ :+ team.games))
      )
      acc
