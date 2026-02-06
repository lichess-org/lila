package lila.relay

import scala.collection.immutable.SeqMap
import play.api.libs.json.*
import scalalib.Debouncer
import chess.{ ByColor, Color, FideId, FideTC, Outcome, PlayerName, IntRating }
import chess.rating.{ Elo, IntRatingDiff }
import chess.tiebreak.{ Tiebreak, TiebreakPoint }

import lila.study.StudyPlayer
import lila.study.StudyPlayer.json.given
import lila.memo.CacheApi
import lila.core.fide.{ PhotosJson, Player as FidePlayer }
import lila.common.Json.given
import lila.relay.RelayGroup.ScoreGroup

// Player in a tournament with current performance rating and list of games
case class RelayPlayer(
    player: StudyPlayer.WithFed,
    ratingsMap: Map[FideTC, IntRating],
    score: Option[Float],
    ratingDiffs: Map[FideTC, IntRatingDiff],
    performances: Map[FideTC, IntRating],
    tiebreaks: Option[Seq[(Tiebreak, TiebreakPoint)]],
    rank: Option[RelayPlayer.Rank],
    games: Vector[RelayPlayer.Game]
):
  export player.player.*
  def withGame(game: RelayPlayer.Game, player: StudyPlayer.WithFed) =
    copy(
      games = games :+ game,
      ratingsMap = player.rating
        .ifFalse(ratingsMap.contains(game.fideTC))
        .fold(ratingsMap)(r => ratingsMap + (game.fideTC -> r))
    )
  def eloGames: Vector[Elo.Game] = games.flatMap(_.eloGame)
  def toTieBreakPlayer: Option[Tiebreak.Player] = player.id.map: id =>
    Tiebreak.Player(id = id.toString, rating = player.rating.map(_.into(Elo)))

given Ordering[List[TiebreakPoint]] = new:
  def compare(a: List[TiebreakPoint], b: List[TiebreakPoint]): Int =
    @scala.annotation.tailrec
    def loop(a: List[TiebreakPoint], b: List[TiebreakPoint]): Int = (a, b) match
      case (Nil, Nil) => 0
      case (Nil, _) => -1 // a is empty, b is not
      case (_, Nil) => 1 // b is empty, a is not
      case (ah :: at, bh :: bt) =>
        val cmp = bh.value.compare(ah.value)
        if cmp != 0 then cmp else loop(at, bt)
    loop(a, b)

given Ordering[Option[List[TiebreakPoint]]] = new Ordering[Option[List[TiebreakPoint]]]:
  def compare(a: Option[List[TiebreakPoint]], b: Option[List[TiebreakPoint]]): Int =
    (a, b) match
      case (Some(ta), Some(tb)) => Ordering[List[TiebreakPoint]].compare(ta, tb)
      case (Some(_), None) => 1 // a is defined, b is not
      case (None, Some(_)) => -1 // b is defined, a is not
      case (None, None) => 0

given Ordering[RelayPlayer] = new Ordering[RelayPlayer]:
  /* Sort players by:
      1. Score (Descending)
      2. Tiebreak points (compare each tiebreak in order, higher is better)
      3. Player rating (Descending)
      4. Player name (Alphabetical, ascending)
   */
  def compare(a: RelayPlayer, b: RelayPlayer): Int =
    val scoreComparison = b.score.compare(a.score)
    lazy val tiebreakComparison = Ordering[Option[List[TiebreakPoint]]]
      .compare(a.tiebreaks.map(_._2F.toList), b.tiebreaks.map(_._2F.toList))
    lazy val ratingComparison = b.rating.map(_.value).compare(a.rating.map(_.value))
    if scoreComparison != 0 then scoreComparison
    else if tiebreakComparison != 0 then tiebreakComparison
    else if ratingComparison != 0 then ratingComparison
    else a.player.name.map(_.value).compare(b.player.name.map(_.value))

object RelayPlayer:

  opaque type Rank = Int
  object Rank extends OpaqueInt[Rank]

  type RelayPlayers = SeqMap[StudyPlayer.Id, RelayPlayer]

  def empty(player: StudyPlayer.WithFed) =
    RelayPlayer(player, Map.empty, None, Map.empty, Map.empty, None, None, Vector.empty)

  case class Game(
      round: RelayRoundId,
      id: StudyChapterId,
      opponent: StudyPlayer.WithFed,
      color: Color,
      points: Option[Outcome.GamePoints],
      rated: chess.Rated,
      fideTC: FideTC,
      customScoring: Option[ByColor[RelayRound.CustomScoring]] = None,
      unplayed: Boolean
  ):
    def playerPoints = points.map(_(color))
    def customPlayerPoints: Option[RelayRound.CustomPoints] = customScoring.flatMap: cs =>
      playerPoints.map:
        case Outcome.Points.One => cs(color).win
        case Outcome.Points.Half => cs(color).draw
        case zero => RelayRound.CustomPoints(zero.value)

    def playerScore: Option[Float] =
      customPlayerPoints
        .map(_.value)
        .orElse(playerPoints.map(_.value))

    def toTiebreakGame: Option[Tiebreak.Game] =
      (opponent.id, playerPoints).mapN: (opponentId, points) =>
        Tiebreak.Game(
          color = color,
          opponent = Tiebreak.Player(opponentId.toString, opponent.rating.map(_.into(Elo))),
          points = points,
          roundId = round.value.some
        )

    // only rate draws and victories, not exotic results
    def isRated = rated.yes && !unplayed && points.exists(_.mapReduce(_.value)(_ + _) == 1)
    def eloGame = for
      pp <- playerPoints
      if isRated
      opRating <- opponent.rating
    yield Elo.Game(pp, opRating.into(Elo))

  object json:
    import scalalib.Json.writeAs
    import RelayJsonView.given
    given Writes[Outcome] = Json.writes
    given Writes[Outcome.Points] = writeAs(_.show)
    given Writes[Outcome.GamePoints] = writeAs(points => Outcome.showPoints(points.some))
    given Writes[FideTC] = writeAs(_.toString)
    given Writes[RelayPlayer.Game] = Json.writes
    given Writes[Seq[(Tiebreak, TiebreakPoint)]] = Writes: tbs =>
      Json.toJson:
        tbs.map: (tb, tbv) =>
          Json.obj(
            "extendedCode" -> tb.extendedCode,
            "description" -> tb.description,
            "points" -> tbv.value
          )
    given KeyWrites[FideTC] = _.toString // required by ratingDiffs & performances

    given OWrites[RelayPlayer] = OWrites: p =>
      Json.toJsObject(p.player) ++ Json
        .obj("played" -> p.games.count(_.points.isDefined))
        .add("score" -> p.score)
        .add("ratingDiff" -> p.ratingDiffs.headOption._2F) // API BC grace
        .add("ratingsMap" -> p.ratingsMap.nonEmptyOption)
        .add("ratingDiffs" -> p.ratingDiffs.nonEmptyOption)
        .add("performance" -> p.performances.headOption._2F) // API BC grace
        .add("performances" -> p.performances.nonEmptyOption)
        .add("tiebreaks" -> p.tiebreaks)
        .add("rank" -> p.rank)
    def full(
        tour: RelayTour
    )(p: RelayPlayer, fidePlayer: Option[FidePlayer], user: Option[User], follow: Option[Boolean]): JsObject =
      val tc = tour.info.fideTcOrGuess
      lazy val eloPlayer = p.rating
        .map(_.into(Elo))
        .orElse(fidePlayer.flatMap(_.ratingOf(tc)))
        .map:
          Elo.Player(_, fidePlayer.fold(chess.rating.KFactor.default)(_.kFactorOf(tc)))
      val gamesJson = p.games.map: g =>
        val rd = tour.showRatingDiffs.so:
          (eloPlayer, g.eloGame).tupled.map: (ep, eg) =>
            Elo.computeRatingDiff(tc)(ep, List(eg))
        Json
          .obj(
            "round" -> g.round,
            "id" -> g.id,
            "opponent" -> g.opponent,
            "color" -> g.color,
            "fideTC" -> g.fideTC
          )
          .add("points" -> g.playerPoints)
          .add("customPoints" -> g.customPlayerPoints)
          .add("ratingDiff" -> rd)
      Json
        .toJsObject(p)
        .add("user", user.map(_.light))
        .add("fide", fidePlayer.map(Json.toJsObject).map(_.add("follow", follow))) ++
        Json.obj("games" -> gamesJson)
    given OWrites[FidePlayer] = OWrites: p =>
      Json.obj("ratings" -> p.ratingsMap, "year" -> p.year)

private final class RelayPlayerApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    relayGroupApi: RelayGroupApi,
    chapterRepo: lila.study.ChapterRepo,
    cacheApi: CacheApi,
    fidePlayerGet: lila.core.fide.GetPlayer,
    photosJson: PhotosJson.Get
)(using Executor)(using scheduler: Scheduler):
  import RelayPlayer.*

  private val cache = cacheApi[ScoreGroup, RelayPlayers](128, "relay.players.data"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(computeScoreGroup)

  private val jsonCache = cacheApi[ScoreGroup, JsonStr](32, "relay.players.json"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: sg =>
      import RelayPlayer.json.given
      for players <- cache.get(sg)
      yield JsonStr(Json.stringify(Json.toJson(players.values.toList)))

  def get(tourId: RelayTourId): Fu[RelayPlayers] =
    relayGroupApi.scoreGroupOf(tourId).flatMap(cache.get)

  def jsonList(tourId: RelayTourId): Fu[JsonStr] =
    relayGroupApi.scoreGroupOf(tourId).flatMap(jsonCache.get)

  private val photosJsonCache = cacheApi[RelayTourId, PhotosJson](64, "relay.players.photos.json"):
    _.expireAfterWrite(15.seconds).buildAsyncFuture: tourId =>
      for
        sg <- relayGroupApi.scoreGroupOf(tourId)
        studyIds <- sg.toList.flatTraverse(roundRepo.studyIdsOf)
        fideIds <- chapterRepo.fideIdsOf(studyIds)
        photos <- photosJson(fideIds)
      yield photos

  def photosJson(tourId: RelayTourId): Fu[PhotosJson] = photosJsonCache.get(tourId)

  def player(tour: RelayTour, str: String): Fu[Option[RelayPlayer]] =
    val id = FideId.from(str.toIntOption) | PlayerName(str)
    for
      players <- get(tour.id)
      player = players.get(id)
    yield player

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val invalidateDebouncer = Debouncer[RelayTourId](scheduler.scheduleOnce(3.seconds, _), 32): id =>
    import lila.memo.CacheApi.invalidate
    relayGroupApi
      .scoreGroupOf(id)
      .foreach: key =>
        cache.invalidate(key)
        jsonCache.invalidate(key)

  private def computeScoreGroup(sg: ScoreGroup): Fu[RelayPlayers] =
    // Use the first tour to retrieve display settings (scores, rating diffs, tiebreaks)
    tourRepo
      .byId(sg.head)
      .flatMapz: tour =>
        for
          players <- readGamesAndPlayers(sg.toList)
          withScore = if tour.showScores then computeScores(players) else players
          withRatingDiff <-
            if tour.showRatingDiffs then computeRatingDiffs(withScore)
            else fuccess(withScore)
          withTiebreaks <- tour.tiebreaks.fold(fuccess(withRatingDiff)): tiebreaks =>
            roundRepo
              .idsByTourOrdered(sg.last)
              .map(_.lastOption)
              .map(computeTiebreaks(withRatingDiff, tiebreaks, _))
        yield withTiebreaks

  private def sgIsParallel(tours: List[RelayTour]): Boolean =
    tours.headOption
      .flatMap(_.dates.map(_.start))
      .exists: firstStart =>
        tours.tailOption.exists(_.forall(_.dates.map(_.start).exists(_.isBefore(firstStart.plusMinutes(20)))))

  private def readGamesAndPlayers(tourIds: List[RelayTourId]): Fu[RelayPlayers] =
    for
      tours <- tourRepo.byIds(tourIds)
      toursById = tours.mapBy(_.id)
      rounds <-
        if sgIsParallel(tours) then roundRepo.byToursOrdered(tourIds)
        else tourIds.flatTraverse(roundRepo.byTourOrdered)
      roundsById = rounds.mapBy(_.id)
      chapters <- chapterRepo.tagsByStudyIds(rounds.map(_.studyId))
      allFideIds = chapters.flatMap(_._2.flatMap((_, tags) => tags.fideIds.flatten)).toList.distinct
      fedsById <- allFideIds
        .traverse(id => fidePlayerGet(id).map(id -> _))
        .map(_.flatMap((id, playerOpt) => playerOpt.flatMap(_.fed).map(id -> _)).toMap)
    yield chapters.foldLeft(SeqMap.empty: RelayPlayers):
      case (playersAcc, (studyId, chaps)) =>
        roundsById
          .get(studyId.into(RelayRoundId))
          .fold(playersAcc): round =>
            chaps.foldLeft(playersAcc):
              case (playersAcc, (chapterId, tags)) =>
                StudyPlayer
                  .fromTags(tags)
                  .flatMap:
                    _.traverse: p =>
                      p.id.map(_ -> StudyPlayer.WithFed(p, p.fideId.flatMap(fedsById.get)))
                  .fold(playersAcc): gamePlayers =>
                    gamePlayers.zipColor.foldLeft(playersAcc):
                      case (playersAcc, (color, (playerId, player))) =>
                        val (_, opponent) = gamePlayers(!color)
                        val game = RelayPlayer.Game(
                          round.id,
                          chapterId,
                          opponent,
                          color,
                          tags.points,
                          round.rated,
                          toursById.get(round.tourId).flatMap(_.info.fideTc).getOrElse(FideTC.standard),
                          round.customScoring,
                          unplayed = tags.value.contains(RelayGame.unplayedTag)
                        )
                        playersAcc.updated(
                          playerId,
                          playersAcc
                            .getOrElse(playerId, RelayPlayer.empty(player))
                            .withGame(game, player)
                        )

  private def computeScores(players: RelayPlayers): RelayPlayers =
    players.view
      .mapValues: p =>
        p.copy(
          score = p.games.foldMap(_.playerScore),
          performances = p.games
            .groupBy(_.fideTC)
            .foldLeft(Map.empty):
              case (acc, (gameTC, tcGames)) =>
                val performanceRating = Elo.computePerformanceRating(tcGames.flatMap(_.eloGame))
                performanceRating.fold(acc)(r => acc + (gameTC -> r.into(IntRating)))
        )
      .to(SeqMap)

  private def computeRatingDiffs(players: RelayPlayers): Fu[RelayPlayers] =
    players.toList
      .traverse: (id, player) =>
        val eloGames = player.eloGames
        if eloGames.isEmpty then fuccess(id -> player)
        else
          player.fideId
            .so(fidePlayerGet)
            .map: fidePlayerOpt =>
              val newPlayer = fidePlayerOpt.fold(player): fidePlayer =>
                val newRatingDiffs = player.games
                  .groupBy(_.fideTC)
                  .foldLeft(Map.empty[FideTC, IntRatingDiff]):
                    case (diffs, (gameTC, tcGames)) =>
                      player.ratingsMap
                        .get(gameTC)
                        .map(_.into(Elo))
                        .orElse(fidePlayer.ratingOf(gameTC))
                        .fold(diffs): rating =>
                          val p = Elo.Player(rating, fidePlayer.kFactorOf(gameTC))
                          val newDiff = Elo.computeRatingDiff(gameTC)(p, tcGames.flatMap(_.eloGame))
                          diffs + (gameTC -> newDiff)
                player.copy(ratingDiffs = newRatingDiffs)
              id -> newPlayer
      .map(_.to(SeqMap))

  private def computeTiebreaks(
      players: RelayPlayers,
      tiebreaks: Seq[Tiebreak],
      lastRoundId: Option[RelayRoundId]
  ): RelayPlayers =
    val tbGames: Map[String, Tiebreak.PlayerWithGames] =
      players.view.values
        .flatMap: p =>
          p.toTieBreakPlayer.map: tbPlayer =>
            tbPlayer.id -> Tiebreak.PlayerWithGames(tbPlayer, p.games.flatMap(_.toTiebreakGame))
        .toMap
    val result = Tiebreak.compute(tbGames, tiebreaks.toList, lastRoundId = lastRoundId.map(_.value))
    players
      .map: (id, rp) =>
        val found = result.find(p => p.player.id == id.toString)
        id -> rp.copy(
          tiebreaks = found.map(t => tiebreaks.zip(t.tiebreakPoints).to(Seq))
        )
      .toList
      .sortBy(_._2)
      .mapWithIndex:
        case ((id, rp), index) =>
          id -> rp.copy(rank = Rank.from((index + 1).some))
      .to(SeqMap)
