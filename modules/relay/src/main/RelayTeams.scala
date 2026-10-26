package lila.relay

import chess.Outcome.Points
import chess.format.pgn.*
import chess.{ ByColor, FideId, IntRating, PlayerName }
import scalalib.Debouncer

import scala.collection.immutable.SeqMap

import lila.core.fide.{ PlayerToken, Tokenize }
import lila.core.fide.Federation
import lila.relay.RelayGroup.ScoreGroup
import lila.relay.RelayTeam.TeamMatch
import lila.study.{ ChapterPreview, StudyPlayer }

type TeamName = String

private class RelayTeamsTextarea(val text: String):

  def sortedText = text.linesIterator.toList.sorted.mkString("\n")

  /* We need this because `PlayerName | FideId` doesn't work
   * the compiler can't differentiate between the two types
   * at runtime using pattern matching. */
  private type PlayerNameStr = String

  lazy val teams: Map[TeamName, List[PlayerNameStr | FideId]] = text.linesIterator
    .take(1500)
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

  def update(games: RelayGames)(using Tokenize): RelayGames = games.map: game =>
    game.copy(tags = update(game.tags))

  private def update(tags: Tags)(using Tokenize): Tags =
    Color.all.foldLeft(tags): (tags, color) =>
      val found = tags
        .fideIds(color)
        .flatMap(findMatching)
        .orElse(PlayerName.raw(tags.names(color)).flatMap(findMatching))
      found.fold(tags): team =>
        tags + Tag(_.teams(color), team)

  private def findMatching(player: PlayerNameStr | FideId)(using Tokenize): Option[TeamName] =
    def tokenizedPlayerTeams: Map[PlayerToken | FideId, TeamName] = playerTeams.mapKeys(tokenizePlayer)
    playerTeams.get(player).orElse(tokenizedPlayerTeams.get(tokenizePlayer(player)))

  private def tokenizePlayer(using tokenize: Tokenize): PlayerNameStr | FideId => PlayerToken | FideId =
    case name: PlayerNameStr => tokenize.exec(name)
    // typing with `fideId: FideId` results in a compiler warning. The current code however is ok.
    case fideId => fideId

object RelayTeam:
  import chess.{ Color, ByColor }
  extension (players: Iterable[RelayPlayer])
    def allGamesFinished: Boolean = players.forall(_.games.forall(_.ongoing.not))
  case class TeamWithGames(name: TeamName, players: RelayPlayer.RelayPlayers):
    def add(player: RelayPlayer) =
      player.id.fold(this): id =>
        copy(players =
          players.updated(
            id,
            players
              .get(id)
              .fold(player): rp =>
                rp.copy(games = rp.games ++ player.games, score = rp.score |+| player.score)
          )
        )
    def points = players.values.toList.foldMap(_.score)

  case class TeamGame(id: StudyChapterId, pov: Color):
    def swap = copy(pov = !pov)

  case class TeamMatch(
      roundId: RelayRoundId,
      teams: ByColor[TeamWithGames],
      games: List[TeamGame],
      teamCustomScoring: Option[RelayRound.CustomScoring]
  ):
    def isSameMatchup(teamNames: ByColor[TeamName]): Boolean =
      val t = teams.map(_.name)
      List(t, t.swap).exists(teamNames.eqv)
    def add(
        chapterId: StudyChapterId,
        playerAndTeam: ByColor[(StudyPlayer, TeamName)],
        game: ByColor[RelayPlayer.Game]
    ): TeamMatch =
      val pov = Color.fromWhite(playerAndTeam.white._2 == teams.white.name)
      val players = pov
        .fold(game, game.swap)
        .map: g =>
          RelayPlayer.empty(g.opponent).copy(games = Vector(g), score = g.playerScore)
      copy(
        games = TeamGame(chapterId, pov) :: games,
        teams = teams.zip(players).map(_.add(_))
      )
    def swap = copy(teams = teams.swap, games = games.map(_.swap))
    private def pointsPair: Option[ByColor[Points]] =
      teams
        .forall(_.players.values.allGamesFinished)
        .so:
          teams.traverseReduce(_.points): (aPoints, bPoints) =>
            if aPoints == bPoints then ByColor(Points.Half, Points.Half)
            else if aPoints > bPoints then ByColor(Points.One, Points.Zero)
            else ByColor(Points.Zero, Points.One)
    private def scorePair: Option[ByColor[Float]] =
      pointsPair.map: p =>
        teamCustomScoring
          .fold(p.map(_.value)): cs =>
            p.map:
              case Points.One => cs.win
              case Points.Half => cs.draw
              case zero => RelayRound.CustomPoints(zero.value)
            .map(_.value)
    def povMatches: ByColor[POVMatch] =
      teams
        .zip(teams.swap)
        .mapWithColor:
          case (c, (team, opp)) =>
            val gp = team.players.values.toList.foldMap(_.games.foldMap(_.playerScore))
            POVMatch(roundId, opp.name, team.players, pointsPair.map(_(c)), scorePair.map(_(c)), gp)

  case class POVMatch(
      roundId: RelayRoundId,
      opponentName: TeamName,
      players: RelayPlayer.RelayPlayers,
      points: Option[Points],
      mp: Option[Float],
      gp: Option[Float]
  ):
    def isFinished: Boolean = points.isDefined
  object POVMatch:
    object json:
      import play.api.libs.json.*
      import lila.common.Json.given
      import RelayPlayer.json.given
      given Writes[POVMatch] = m =>
        Json
          .obj(
            "roundId" -> m.roundId,
            "opponent" -> m.opponentName
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
          teams <- players.traverse(_.team)
          game = players.swap
            .mapWithColor: (c, opp) =>
              RelayPlayer.Game(
                round.id,
                chap.id,
                opp,
                c,
                points,
                round.rated,
                chess.FideTC.standard,
                round.customScoring,
                unplayed = false,
                ongoing = false
              )
          m0 = table.find(_.isSameMatchup(teams)) | TeamMatch(
            round.id,
            teams.map(TeamWithGames(_, SeqMap.empty)),
            Nil,
            round.teamCustomScoring
          )
          m1 = m0.add(chap.id, players.map(_.player).zip(teams, (_, _)), game)
          newTable = m1 :: table.filterNot(_.isSameMatchup(teams))
        yield newTable) | table

    def ensureFirstPlayerHasWhite(table: List[TeamMatch]): List[TeamMatch] =
      table.map: m =>
        if m.games.headOption.forall(_.pov.white) then m
        else m.swap

final class RelayTeamLeaderboard(
    relayGroupApi: RelayGroupApi,
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    chapterRepo: lila.study.ChapterRepo,
    federationsOf: lila.core.fide.Federation.FedsOf,
    cacheApi: lila.memo.CacheApi
)(using Federation.Guess, Executor)(using scheduler: Scheduler):
  import play.api.libs.json.*

  type TeamLeaderboard = SeqMap[TeamName, TeamLeaderboardEntry]

  case class TeamLeaderboardEntry(
      name: TeamName,
      povMatches: List[RelayTeam.POVMatch]
  ):
    lazy val matchPoints: Float = povMatches.filter(_.isFinished).flatMap(_.mp).sum
    lazy val gamePoints: Float = povMatches.filter(_.isFinished).flatMap(_.gp).sum
    lazy val averageRating: Option[IntRating] = IntRating.from:
      scalalib.Maths.mean(IntRating.raw(players.flatMap(_.rating))).map(x => Math.round(x).toInt)
    lazy val players: Iterable[RelayPlayer] = povMatches
      .flatMap(_.players.values)
      .groupBy(_.id)
      .values
      .map:
        _.reduce((acc, p) => acc.copy(games = acc.games ++ p.games, score = acc.score |+| p.score))

  given Ordering[TeamLeaderboardEntry] = Ordering.by(t => (-t.matchPoints, -t.gamePoints, t.name))

  object json:
    import lila.common.Json.given
    import RelayTeam.POVMatch.json.given
    import RelayPlayer.json.given
    given Writes[TeamLeaderboardEntry] = t =>
      Json
        .obj(
          "name" -> t.name,
          "mp" -> t.matchPoints,
          "gp" -> t.gamePoints,
          "matches" -> t.povMatches,
          "players" -> t.players
        )
        .add("averageRating" -> t.averageRating)

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
          JsonStr(Json.toJson(l.values.toList))

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
          tourIds = scoreGroup.toList
          tours <- tourRepo.byIds(tourIds)
          toursById = tours.mapBy(_.id)
          rounds <- roundRepo.roundsOfScoreGroup(tours)
          roundsById = rounds.mapBy(_.id)
          chapters <- chapterRepo.tagsByStudyIds(rounds.map(_.studyId))
          uniqueFideIds = chapters.view.values
            .flatMap(_.view.values.flatMap(_.fideIds.flatten))
            .toList
            .distinct
          fedsById <- federationsOf(uniqueFideIds)
          matchesByRound = chapters
            .map: (studyId, chaps) =>
              val roundId = studyId.into(RelayRoundId)
              val teamMatches = chaps
                .foldLeft(List.empty[TeamMatch]):
                  case (roundMatches, (chapterId, tags)) =>
                    val withNewTeamGame = for
                      players <- StudyPlayer.fromTags(tags)
                      withFed <- players.traverse: p =>
                        p.id.map(_ -> p.copy(fed = p.fed.orElse(p.fideId.flatMap(fedsById.get))))
                      roundId = studyId.into(RelayRoundId)
                      round <- roundsById.get(roundId)
                      tourTC = toursById.get(round.tourId).map(_.info.fideTCOrGuess)
                      game = withFed.mapWithColor: (c, p) =>
                        RelayPlayer.Game.make(round, chapterId, p._2, c, tags, tourTC)
                      teams <- players.traverse(_.team)
                      teamMatch = roundMatches.find(_.isSameMatchup(teams)) | TeamMatch(
                        roundId,
                        teams.map(RelayTeam.TeamWithGames(_, SeqMap.empty)),
                        Nil,
                        round.teamCustomScoring
                      )
                    yield roundMatches.filterNot(_.isSameMatchup(teams)) :+
                      teamMatch.add(chapterId, players.map(_.player).zip(teams, (_, _)), game)
                    withNewTeamGame | roundMatches
              roundId -> teamMatches
        yield matchesByRound
          .flatMap(_._2)
          .foldLeft(SeqMap.empty: TeamLeaderboard): (acc, matchup) =>
            matchup.teams
              .zip(matchup.povMatches)
              .fold(acc): (acc, combo) =>
                acc.updatedWith(combo._1.name): entry =>
                  entry.fold(TeamLeaderboardEntry(combo._1.name, List(combo._2)).some): team =>
                    team.copy(povMatches = team.povMatches :+ combo._2).some
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
    given Writes[TeamGame] = Json.writes[TeamGame]
    given Writes[TeamMatch] = Json.writes[TeamMatch]
    given Writes[TeamWithGames] = t =>
      Json
        .obj(
          "name" -> t.name,
          "players" -> Json.toJson(t.players.values.toList),
          "points" -> Json.toJson(t.points.orZero)
        )
    type Outs = TeamWithGames | TeamMatch | TeamGame
    given [A <: Outs: Writes]: Writes[ByColor[A]] = Writes: p =>
      Json.arr(p.white, p.black)
