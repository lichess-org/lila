package lila.relay

import scala.collection.immutable.SeqMap

import scalalib.Debouncer

import chess.format.pgn.*
import chess.{ FideId, PlayerName }
import chess.Outcome.Points

import lila.core.fide.{ PlayerToken, Tokenize }
import lila.study.ChapterPreview
import lila.study.StudyPlayer
import lila.relay.RelayGroup.ScoreGroup

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

object RelayTeam:
  import chess.{ Color, ByColor }
  case class TeamWithGames(name: TeamName, players: RelayPlayer.RelayPlayers):
    def add(player: RelayPlayer) =
      player.id.fold(this): id =>
        copy(players =
          players.updated(
            id,
            players.get(id).fold(player)(rp => rp.copy(games = rp.games ++ player.games))
          )
        )
    def points = players.values.toList.foldMap(_.score)
    def allGamesFinished: Boolean =
      players.values.forall(_.games.forall(_.points.isDefined))
  case class Pair[A](a: A, b: A):
    def is(p: Pair[A]) = (a == p.a && b == p.b) || (a == p.b && b == p.a)
    def map[B](f: A => B) = Pair(f(a), f(b))
    def bimap[B](f: A => B, g: A => B) = Pair(f(a), g(b))
    def permutations: Pair[PairOf[A]] = Pair((a, b), (b, a))
    def forall(f: A => Boolean) = f(a) && f(b)
    def find(f: A => Boolean): Option[A] = if f(a) then Some(a) else if f(b) then Some(b) else None
    def foldLeft[B](z: B)(f: (B, A) => B) = f(f(z, a), b)
    def reverse = Pair(b, a)

  case class TeamGame(id: StudyChapterId, pov: Color):
    def swap = copy(pov = !pov)

  case class TeamMatch(
      roundId: RelayRoundId,
      teams: Pair[TeamWithGames],
      games: List[TeamGame],
      teamCustomScoring: Option[RelayRound.CustomScoring]
  ):
    def is(teamNames: Pair[TeamName]) = teams.map(_.name).is(teamNames)
    def add(
        chap: ChapterPreview,
        playerAndTeam: Pair[(StudyPlayer, TeamName)],
        game: ByColor[RelayPlayer.Game]
    ): TeamMatch =
      val t0Color = Color.fromWhite(playerAndTeam.a._2 == teams.a.name)
      val wPOV = game(t0Color)
      val bPOV = game(!t0Color)
      val wPlayer = RelayPlayer.empty(bPOV.opponent).copy(score = wPOV.playerScore, games = Vector(wPOV))
      val bPlayer = RelayPlayer.empty(wPOV.opponent).copy(score = bPOV.playerScore, games = Vector(bPOV))
      copy(
        games = TeamGame(chap.id, t0Color) :: games,
        teams = teams.bimap(_.add(wPlayer), _.add(bPlayer))
      )
    def swap = copy(teams = teams.reverse, games = games.map(_.swap))
    def isFinished: Boolean = teams.forall(_.allGamesFinished)
    def pointsPair: Option[Pair[Points]] =
      teams
        .forall(_.allGamesFinished)
        .so:
          (teams.a.points, teams.b.points).mapN: (aPoints, bPoints) =>
            if aPoints == bPoints then Pair(Points.Half, Points.Half)
            else if aPoints > bPoints then Pair(Points.One, Points.Zero)
            else Pair(Points.Zero, Points.One)
    def pointsFor(teamName: TeamName): Option[Points] =
      pointsPair.map(o => if teams.a.name == teamName then o.a else o.b)
    def scoreFor(teamName: TeamName): Option[Float] =
      teamCustomScoring
        .flatMap: cs =>
          pointsFor(teamName).map: pts =>
            pts match
              case Points.One => cs.win
              case Points.Half => cs.draw
              case zero => RelayRound.CustomPoints(zero.value)
        .map(_.value)
        .orElse(pointsFor(teamName).map(_.value))
    def povMatches: Pair[POVMatch] =
      teams.permutations.map: (x, y) =>
        val gp = x.players.values.toList.foldMap(_.games.foldMap(_.playerScore))
        POVMatch(roundId, y.name, y.players, pointsFor(x.name), scoreFor(x.name), gp)
    def povMatch(teamName: TeamName): Option[POVMatch] =
      if teams.a.name == teamName then Some(povMatches.a)
      else if teams.b.name == teamName then Some(povMatches.b)
      else None

  case class POVMatch(
      roundId: RelayRoundId,
      opponentName: TeamName,
      players: RelayPlayer.RelayPlayers,
      points: Option[Points],
      mp: Option[Float],
      gp: Option[Float]
  )
  object POVMatch:
    object json:
      import play.api.libs.json.*
      import lila.common.Json.given
      import RelayPlayer.json.given
      given Writes[POVMatch] = m =>
        Json
          .obj(
            "roundId" -> m.roundId,
            "opponent" -> m.opponentName,
            "players" -> m.players.values.toList
          )
          .add("points" -> m.points)
          .add("mp" -> m.mp)
          .add("gp" -> m.gp)

final class RelayTeamTable(
    roundRepo: RelayRoundRepo,
    chapterPreviewApi: lila.study.ChapterPreviewApi,
    cacheApi: lila.memo.CacheApi
)(using Executor):
  import play.api.libs.json.*
  import RelayTeam.*

  def tableJson(roundId: RelayRoundId): Fu[JsObject] = jsonCache.get(roundId.studyId)

  def table(roundId: RelayRoundId): Fu[List[TeamMatch]] =
    cache.get(roundId.studyId)

  private val cache = cacheApi[StudyId, List[TeamMatch]](64, "relay.teamTable"):
    _.expireAfterWrite(3.seconds).buildAsyncFuture(impl.compute)

  private val jsonCache = cacheApi[StudyId, JsObject](8, "relay.teamTable.json"):
    _.expireAfterWrite(3.seconds).buildAsyncFuture(impl.makeJson)

  private object impl:

    def makeJson(studyId: StudyId): Fu[JsObject] =
      import RelayTeamTable.json.given
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
          m0 = table.find(_.is(teams)) | TeamMatch(
            round.id,
            teams.map(TeamWithGames(_, SeqMap.empty)),
            Nil,
            round.teamCustomScoring
          )
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

final class RelayTeamLeaderboard(
    relayGroupApi: RelayGroupApi,
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    teamTable: RelayTeamTable,
    cacheApi: lila.memo.CacheApi
)(using Executor)(using scheduler: Scheduler):
  import play.api.libs.json.*

  type TeamLeaderboard = SeqMap[TeamName, TeamLeaderboardEntry]

  case class TeamLeaderboardEntry(
      name: TeamName,
      matches: List[RelayTeam.TeamMatch]
  ):
    lazy val matchPoints: Float =
      matches
        .filter(_.isFinished)
        .flatMap(_.scoreFor(name))
        .sum
    lazy val gamePoints: Float =
      matches
        .filter(_.isFinished)
        .flatMap(_.teams.find(_.name == name).flatMap(_.points))
        .sum
    lazy val povMatches: List[RelayTeam.POVMatch] = matches.flatMap(_.povMatch(name))
    lazy val players: Iterable[RelayPlayer] = povMatches
      .flatMap(_.players.values)
      .groupBy(_.id)
      .values
      .map:
        _.reduce((r1, r2) => r1.copy(games = r1.games ++ r2.games, score = r1.score |+| r2.score))

  given Ordering[TeamLeaderboardEntry] = Ordering.by(t => (-t.matchPoints, -t.gamePoints, t.name))

  object json:
    import RelayTeam.POVMatch.json.given
    import RelayPlayer.json.given
    given Writes[TeamLeaderboardEntry] = t =>
      Json.obj(
        "name" -> t.name,
        "mp" -> t.matchPoints,
        "gp" -> t.gamePoints,
        "matches" -> t.povMatches,
        "players" -> t.players
      )

  def leaderboardJson(tour: RelayTourId): Fu[JsonStr] =
    relayGroupApi.scoreGroupOf(tour).flatMap(jsonCache.get)

  def leaderboard(tour: RelayTourId): Fu[TeamLeaderboard] =
    relayGroupApi.scoreGroupOf(tour).flatMap(cache.get)

  private val cache = cacheApi[ScoreGroup, TeamLeaderboard](12, "relay.teamLeaderboard"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(aggregate)

  private val jsonCache = cacheApi[ScoreGroup, JsonStr](8, "relay.teamLeaderboard.json"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: tourId =>
      import json.given
      cache
        .get(tourId)
        .map: l =>
          JsonStr(Json.stringify(Json.toJson(l.values.toList)))

  private val invalidateDebouncer = Debouncer[RelayTourId](scheduler.scheduleOnce(3.seconds, _), 32): id =>
    import lila.memo.CacheApi.invalidate
    relayGroupApi
      .scoreGroupOf(id)
      .foreach: key =>
        cache.invalidate(key)
        jsonCache.invalidate(key)

  export invalidateDebouncer.push as invalidate

  private def aggregate(scoreGroup: ScoreGroup): Fu[TeamLeaderboard] =
    tourRepo
      .showTeamScores(scoreGroup.head)
      .flatMapz:
        for
          rounds <- scoreGroup.toList.flatTraverse(roundRepo.idsByTourOrdered)
          matches <- rounds.flatTraverse(teamTable.table)
        yield matches.foldLeft(SeqMap.empty: TeamLeaderboard): (acc, matchup) =>
          matchup.teams
            .foldLeft(acc): (acc, team) =>
              acc.updatedWith(team.name):
                _.fold(TeamLeaderboardEntry(team.name, List(matchup))): team =>
                  team.copy(matches = team.matches :+ matchup)
                .some
            .toList
            .sortBy(_._2)
            .to(SeqMap)

object RelayTeamTable:
  object json:
    import play.api.libs.json.*
    import lila.common.Json.given
    import RelayPlayer.json.given
    import RelayJsonView.given
    import RelayTeam.*
    given [A: Writes]: Writes[Pair[A]] = Writes: p =>
      Json.arr(p.a, p.b)
    given Writes[TeamGame] = Json.writes[TeamGame]
    given Writes[TeamMatch] = Json.writes[TeamMatch]
    given Writes[TeamWithGames] = t =>
      Json
        .obj(
          "name" -> t.name,
          "players" -> Json.toJson(t.players.values.toList)
        )
        .add("points" -> t.points)
