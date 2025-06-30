package lila.relay

import scala.collection.immutable.SeqMap
import play.api.libs.json.*
import scalalib.Json.writeAs
import scalalib.Debouncer
import chess.{ ByColor, Color, FideId, Outcome, PlayerName, IntRating }
import chess.rating.{ Elo, IntRatingDiff }

import lila.study.{ ChapterPreviewApi, StudyPlayer }
import lila.study.StudyPlayer.json.given
import lila.memo.CacheApi
import lila.core.fide.Player as FidePlayer
import lila.common.Json.given
import lila.core.fide.FideTC
import chess.tiebreaker.Tiebreaker.{ PlayerGames, POVGame, tb, Player }
import chess.tiebreaker.{ Tiebreaker, TieBreakPoints }
import cats.syntax.all.*
import cats.data.NonEmptySeq

// Player in a tournament with current performance rating and list of games
case class RelayPlayer(
    player: StudyPlayer.WithFed,
    score: Option[Float],
    ratingDiff: Option[IntRatingDiff],
    performance: Option[IntRating],
    tiebreaks: Option[SeqMap[Tiebreaker, TieBreakPoints]],
    rank: Option[Int],
    games: Vector[RelayPlayer.Game]
):
  export player.player.*
  def withGame(game: RelayPlayer.Game) = copy(games = games :+ game)
  def eloGames: Vector[Elo.Game]       = games.flatMap(_.eloGame)
  def toTieBreakPlayer: Option[Player] = player.id.map: id =>
    Player(name = id.toString, rating = player.rating.map(_.into(Elo)))

object RelayPlayer:
  case class Game(
      round: RelayRoundId,
      id: StudyChapterId,
      opponent: StudyPlayer.WithFed,
      color: Color,
      points: Option[Outcome.GamePoints],
      rated: chess.Rated,
      customScoring: Option[ByColor[RelayRound.CustomScoring]] = None
  ):
    def playerPoints                                        = points.map(_(color))
    def customPlayerPoints: Option[RelayRound.CustomPoints] = customScoring.flatMap: cs =>
      playerPoints.map:
        case Outcome.Points.One  => cs(color).win
        case Outcome.Points.Half => cs(color).draw
        case zero                => RelayRound.CustomPoints(zero.value)

    def playerScore = customPlayerPoints
      .map(_.value)
      .orElse(playerPoints.map(_.value))

    def toTiebreakerGame: Option[POVGame] =
      opponent.id.map: (opponentId) =>
        POVGame(
          color = color,
          opponent = Player(opponentId.toString, opponent.rating.map(_.into(Elo))),
          points = playerPoints
        )

    // only rate draws and victories, not exotic results
    def isRated = rated.yes && points.exists(_.mapReduce(_.value)(_ + _) == 1)
    def eloGame = for
      pp <- playerPoints
      if isRated
      opRating <- opponent.rating
    yield Elo.Game(pp, opRating.into(Elo))

  object json:
    import JsonView.given
    given Writes[Outcome]                            = Json.writes
    given Writes[Outcome.Points]                     = writeAs(_.show)
    given Writes[Outcome.GamePoints]                 = writeAs(points => Outcome.showPoints(points.some))
    given Writes[RelayPlayer.Game]                   = Json.writes
    given Writes[SeqMap[Tiebreaker, TieBreakPoints]] = Writes { tbs =>
      JsObject(
        tbs.map { case (tb, tbv) =>
          tb.code -> Json.toJson(tbv)
        }
      )
    }
    given OWrites[RelayPlayer] = OWrites: p =>
      Json.toJsObject(p.player) ++ Json
        .obj("played" -> p.games.count(_.points.isDefined))
        .add("score" -> p.score)
        .add("ratingDiff" -> p.ratingDiff)
        .add("performance" -> p.performance)
        .add("tiebreaks" -> p.tiebreaks)
        .add("rank" -> p.rank)
    def full(tour: RelayTour)(p: RelayPlayer, fidePlayer: Option[FidePlayer]): JsObject =
      val tc             = tour.info.fideTcOrGuess
      lazy val eloPlayer = p.rating
        .map(_.into(Elo))
        .orElse(fidePlayer.flatMap(_.ratingOf(tc)))
        .map:
          Elo.Player(_, fidePlayer.fold(chess.rating.KFactor.default)(_.kFactorOf(tc)))
      val gamesJson = p.games.map: g =>
        val rd = tour.showRatingDiffs.so:
          (eloPlayer, g.eloGame).tupled.map: (ep, eg) =>
            Elo.computeRatingDiff(ep, List(eg))
        Json
          .obj(
            "round"    -> g.round,
            "id"       -> g.id,
            "opponent" -> g.opponent,
            "color"    -> g.color
          )
          .add("points" -> g.playerPoints)
          .add("customPoints" -> g.customPlayerPoints)
          .add("ratingDiff" -> rd)
      Json.toJsObject(p).add("fide", fidePlayer) ++ Json.obj("games" -> gamesJson)
    given OWrites[FidePlayer] = OWrites: p =>
      Json.obj("ratings" -> p.ratingsMap.mapKeys(_.toString), "year" -> p.year)

private final class RelayPlayerApi(
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    chapterRepo: lila.study.ChapterRepo,
    chapterPreviewApi: ChapterPreviewApi,
    cacheApi: CacheApi,
    fidePlayerGet: lila.core.fide.GetPlayer
)(using Executor)(using scheduler: Scheduler):
  import RelayPlayer.*

  type RelayPlayers = SeqMap[StudyPlayer.Id, RelayPlayer]

  private val cache = cacheApi[RelayTourId, RelayPlayers](32, "relay.players.data"):
    _.expireAfterWrite(1.minute).buildAsyncFuture(compute)

  private val jsonCache = cacheApi[RelayTourId, JsonStr](32, "relay.players.json"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: tourId =>
      import RelayPlayer.json.given
      cache
        .get(tourId)
        .map: players =>
          JsonStr(Json.stringify(Json.toJson(players.values.toList)))

  export cache.get
  export jsonCache.get as jsonList

  def player(tour: RelayTour, str: String): Fu[Option[JsObject]] =
    val id = FideId.from(str.toIntOption) | PlayerName(str)
    for
      players <- cache.get(tour.id)
      player = players.get(id)
      fidePlayer <- player.flatMap(_.fideId).so(fidePlayerGet)
    yield player.map(json.full(tour)(_, fidePlayer))

  def invalidate(id: RelayTourId) = invalidateDebouncer.push(id)

  private val invalidateDebouncer = Debouncer[RelayTourId](scheduler.scheduleOnce(3.seconds, _), 32): id =>
    import lila.memo.CacheApi.invalidate
    cache.invalidate(id)
    jsonCache.invalidate(id)

  private def compute(tourId: RelayTourId): Fu[RelayPlayers] =
    tourRepo
      .byId(tourId)
      .flatMapz: tour =>
        for
          rounds <- roundRepo.byTourOrdered(tourId)
          roundsById = rounds.mapBy(_.id)
          studyPlayers <- fetchStudyPlayers(rounds.map(_.id))
          chapters     <- chapterRepo.tagsByStudyIds(rounds.map(_.studyId))
          players = chapters.toList.foldLeft(SeqMap.empty: RelayPlayers):
            case (players, (studyId, chapters)) =>
              roundsById
                .get(studyId.into(RelayRoundId))
                .fold(players): round =>
                  chapters.foldLeft(players) { case (players, (chapterId, tags)) =>
                    StudyPlayer
                      .fromTags(tags)
                      .flatMap:
                        _.traverse: p =>
                          p.id.flatMap: id =>
                            studyPlayers.get(id).map(id -> _)
                      .fold(players): gamePlayers =>
                        gamePlayers.zipColor.foldLeft(players):
                          case (players, (color, (playerId, player))) =>
                            val (_, opponent) = gamePlayers(!color)
                            val game          = RelayPlayer.Game(
                              round.id,
                              chapterId,
                              opponent,
                              color,
                              tags.points,
                              round.rated,
                              round.customScoring
                            )
                            players.updated(
                              playerId,
                              players
                                .getOrElse(
                                  playerId,
                                  RelayPlayer(player, None, None, None, None, None, Vector.empty)
                                )
                                .withGame(game)
                            )
                  }
          withScore = if tour.showScores then computeScores(players) else players
          withRatingDiff <-
            if tour.showRatingDiffs then computeRatingDiffs(tour.info.fideTcOrGuess, withScore)
            else fuccess(withScore)
        yield computeTiebreaksAndRank(withRatingDiff, tour.tiebreaks)

  type StudyPlayers = SeqMap[StudyPlayer.Id, StudyPlayer.WithFed]
  private def fetchStudyPlayers(roundIds: List[RelayRoundId]): Fu[StudyPlayers] =
    roundIds
      .traverse: roundId =>
        chapterPreviewApi.dataList.uniquePlayers(roundId.into(StudyId))
      .map:
        _.foldLeft(SeqMap.empty: StudyPlayers): (players, roundPlayers) =>
          roundPlayers.foldLeft(players):
            case (players, (id, p)) =>
              if players.contains(id) then players
              else players.updated(id, p.studyPlayer)

  private def computeScores(players: RelayPlayers): RelayPlayers =
    players.view
      .mapValues: p =>
        p.copy(
          score = p.games
            .foldMap(game => game.playerScore),
          performance = Elo.computePerformanceRating(p.eloGames).map(_.into(IntRating))
        )
      .to(SeqMap)

  private def computeRatingDiffs(tc: FideTC, players: RelayPlayers): Fu[RelayPlayers] =
    players.toList
      .traverse: (id, player) =>
        player.fideId
          .so(fidePlayerGet)
          .map: fidePlayerOpt =>
            for
              fidePlayer <- fidePlayerOpt
              r          <- player.rating.map(_.into(Elo)).orElse(fidePlayer.ratingOf(tc))
              p     = Elo.Player(r, fidePlayer.kFactorOf(tc))
              games = player.eloGames
            yield player.copy(ratingDiff = games.nonEmpty.option(Elo.computeRatingDiff(p, games)))
          .map: newPlayer =>
            id -> (newPlayer | player)
      .map(_.to(SeqMap))

  private def computeTiebreaksAndRank(
      players: RelayPlayers,
      breakers: Option[Seq[Tiebreaker]]
  ): RelayPlayers =
    breakers.fold(players)(brs => computeTiebreaks(players, brs).pipe(rankPlayers))

  private def computeTiebreaks(
      players: RelayPlayers,
      breakers: Seq[Tiebreaker]
  ): RelayPlayers =
    // We must compute a tiebreak for all players and then move on to the next because direct-encounter
    // uses the previous tiebreaks to compute equal standings.
    breakers.foldLeft(players): (accPlayers, breaker) =>
      accPlayers.map:
        case (id, player) =>
          player.toTieBreakPlayer.fold(id -> player): tiebreakerPlayer =>
            val allGames = accPlayers
              .flatMap: (_, rp) =>
                val tbGames = rp.games.flatMap(_.toTiebreakerGame)
                rp.toTieBreakPlayer
                  .map(pl =>
                    PlayerGames(
                      pl,
                      tbGames,
                      rp.tiebreaks.flatMap(tbs => NonEmptySeq.fromSeq(tbs.values.toSeq))
                    )
                  )
              .toSeq
            val newTb        = tb(breaker, tiebreakerPlayer, allGames)
            val newTiebreaks = player.tiebreaks match
              case Some(tbs) => tbs.updated(breaker, newTb)
              case _         => SeqMap(breaker -> newTb)
            id -> player.copy(tiebreaks = Some(newTiebreaks))

  private def rankPlayers(
      players: RelayPlayers
  ): RelayPlayers =
    def compareTiebreaks(
        a: Option[SeqMap[Tiebreaker, TieBreakPoints]],
        b: Option[SeqMap[Tiebreaker, TieBreakPoints]]
    ): Int =
      (a, b)
        .flatMapN: (amap, bmap) =>
          amap
            .zip(bmap)
            .collectFirst:
              case ((xk, xv), (yk, yv)) if xk == yk && xv != yv =>
                yv.value.compareTo(xv.value)
        .getOrElse(0)

    val sorted = players.toList.sortWith { case ((_, p1), (_, p2)) =>
      // 1. Score descending
      val scoreCmp = Ordering[Option[Float]].reverse.compare(p1.score, p2.score)
      if scoreCmp != 0 then scoreCmp < 0
      else
        // 2. Tiebreaks descending
        val tbCmp = compareTiebreaks(p1.tiebreaks, p2.tiebreaks)
        if tbCmp != 0 then tbCmp < 0
        else
          // 3. Performance descending
          val perfCmp = p1.performance.map(_.value).compare(p2.performance.map(_.value))
          if perfCmp != 0 then perfCmp < 0
          else
            // 4. Rating descending
            p1.rating.map(_.value) < p2.rating.map(_.value)
    }
    sorted.zipWithIndex
      .map:
        case ((id, player), i) =>
          id -> player.copy(rank = Some(i + 1))
      .to(SeqMap)
