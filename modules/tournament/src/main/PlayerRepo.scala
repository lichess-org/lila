package lila.tournament

import reactivemongo.akkastream.{ AkkaStreamCursor, cursorProducer }
import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.core.chess.Rank
import lila.core.user.WithPerf
import lila.core.userId.UserSearch
import lila.db.dsl.{ *, given }
import lila.tournament.BSONHandlers.given

final class PlayerRepo(private[tournament] val coll: Coll)(using Executor):

  def selectTour(tourId: TourId) = $doc("tid" -> tourId)
  private def selectTourUser(tourId: TourId, userId: UserId) =
    $doc(
      "tid" -> tourId,
      "uid" -> userId
    )
  private val selectActive = $doc("w".$ne(true))
  private val selectBot = $doc("bot" -> true)
  private val selectWithdraw = $doc("w" -> true)
  private val bestSort = $doc("m" -> -1)

  def byId(id: TourId): Fu[Option[Player]] = coll.one[Player]($id(id))

  private[tournament] def byPlayerIdsOnPage(
      playerIds: List[TourPlayerId],
      page: Int
  ): Fu[RankedPlayers] =
    coll.find($inIds(playerIds)).cursor[Player]().listAll().map { players =>
      playerIds
        .flatMap(id => players.find(_._id == id))
        .mapWithIndex: (player, index) =>
          RankedPlayer(Rank((page - 1) * 10 + index + 1), player)
    }

  private[tournament] def bestByTour(tourId: TourId, nb: Int, skip: Int = 0): Fu[List[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).skip(skip).cursor[Player]().list(nb)

  private[tournament] def bestByTourWithRank(
      tourId: TourId,
      nb: Int,
      skip: Int = 0
  ): Fu[RankedPlayers] =
    bestByTour(tourId, nb, skip).map { res =>
      res
        .foldRight(List.empty[RankedPlayer] -> (res.size + skip)) { case (p, (res, rank)) =>
          (RankedPlayer(Rank(rank), p) :: res, rank - 1)
        }
        ._1
    }

  private[tournament] def bestByTourWithRankByPage(
      tourId: TourId,
      nb: Int,
      page: Int
  ): Fu[RankedPlayers] =
    bestByTourWithRank(tourId, nb, (page - 1) * nb)

  // very expensive
  private[tournament] def bestTeamIdsByTour(
      tourId: TourId,
      battle: TeamBattle
  ): Fu[List[TeamBattle.RankedTeam]] =
    import TeamBattle.{ RankedTeam, TeamLeader }
    coll
      .aggregateList(maxDocs = TeamBattle.maxTeams): framework =>
        import framework.*
        Match(selectTour(tourId)) -> List(
          Sort(Descending("m")),
          GroupField("t")(
            "m" -> Push(
              $doc(
                "u" -> "$uid",
                "m" -> "$m"
              )
            )
          ),
          Limit(TeamBattle.maxTeams),
          Project(
            $doc(
              "p" -> $doc(
                "$slice" -> $arr("$m", battle.nbLeaders)
              )
            )
          )
        )
      .map:
        _.flatMap: doc =>
          for
            teamId <- doc.getAsOpt[TeamId]("_id")
            leadersBson <- doc.getAsOpt[List[Bdoc]]("p")
            leaders = leadersBson.flatMap: p =>
              for
                id <- p.getAsOpt[UserId]("u")
                magic <- p.int("m")
              yield TeamLeader(id, magic)
          yield new RankedTeam(0, teamId, leaders)
        .sorted.mapWithIndex: (rt, pos) =>
          rt.updateRank(pos + 1)
      .map: ranked =>
        if ranked.sizeIs == battle.teams.size then ranked
        else
          ranked ::: battle.teams
            .foldLeft(List.empty[RankedTeam]):
              case (missing, team) if !ranked.exists(_.teamId == team) =>
                new RankedTeam(missing.headOption.fold(ranked.size)(_.rank) + 1, team, Nil, 0) :: missing
              case (acc, _) => acc
            .reverse

  // very expensive
  private[tournament] def teamInfo(
      tourId: TourId,
      teamId: TeamId
  ): Fu[TeamBattle.TeamInfo] =
    coll
      .aggregateOne(): framework =>
        import framework.*
        Match(selectTour(tourId) ++ $doc("t" -> teamId)) -> List(
          Sort(Descending("m")),
          Facet(
            List(
              "agg" -> List(
                Group(BSONNull)(
                  "nb" -> SumAll,
                  "rating" -> AvgField("r"),
                  "perf" -> Avg($doc("$cond" -> $arr("$e", "$e", "$r"))),
                  "score" -> AvgField("s")
                )
              ),
              "topPlayers" -> List(Limit(50))
            )
          )
        )
      .map: docO =>
        for
          doc <- docO
          aggs <- doc.getAsOpt[List[Bdoc]]("agg")
          agg <- aggs.headOption
          nbPlayers <- agg.int("nb")
          rating = agg.double("rating").so(math.round)
          perf = agg.double("perf").so(math.round)
          score = agg.double("score").so(math.round)
          topPlayers <- doc.getAsOpt[List[Player]]("topPlayers")
        yield TeamBattle.TeamInfo(teamId, nbPlayers, rating.toInt, perf.toInt, score.toInt, topPlayers)
      .dmap(_ | TeamBattle.TeamInfo(teamId, 0, 0, 0, 0, Nil))

  def bestTeamPlayers(tourId: TourId, teamId: TeamId, nb: Int): Fu[List[Player]] =
    coll.find($doc("tid" -> tourId, "t" -> teamId)).sort($sort.desc("m")).cursor[Player]().list(nb)

  def countTeamPlayers(tourId: TourId, teamId: TeamId): Fu[Int] =
    coll.countSel($doc("tid" -> tourId, "t" -> teamId))

  def teamsOfPlayers(tourId: TourId, userIds: Seq[UserId]): Fu[List[(UserId, TeamId)]] =
    coll
      .find($doc("tid" -> tourId, "uid".$in(userIds)), $doc("_id" -> false, "uid" -> true, "t" -> true).some)
      .cursor[Bdoc]()
      .listAll()
      .map: doc =>
        for
          doc <- doc
          userId <- doc.getAsOpt[UserId]("uid")
          teamId <- doc.getAsOpt[TeamId]("t")
        yield (userId, teamId)

  def teamVs(tourId: TourId, game: Game): Fu[Option[TeamBattle.TeamVs]] =
    game.twoUserIds.so: (w, b) =>
      teamsOfPlayers(tourId, List(w, b)).dmap(_.toMap).map { m =>
        (m.get(w), m.get(b)).mapN: (wt, bt) =>
          TeamBattle.TeamVs(chess.ByColor(wt, bt))
      }

  def count(tourId: TourId): Fu[Int] = coll.countSel(selectTour(tourId))

  def removeByTour(tourId: TourId) = coll.delete.one(selectTour(tourId)).void

  def remove(tourId: TourId, userId: UserId) =
    coll.delete.one(selectTourUser(tourId, userId)).void

  def removeNotInTeams(tourId: TourId, teamIds: Set[TeamId]) =
    coll.delete.one(selectTour(tourId) ++ $doc("t".$nin(teamIds))).void

  def existsActive(tourId: TourId, userId: UserId) =
    coll.exists(selectTourUser(tourId, userId) ++ selectActive)

  def exists(tourId: TourId, userId: UserId) =
    coll.exists(selectTourUser(tourId, userId))

  def unWithdraw(tourId: TourId) =
    coll.update
      .one(
        selectTour(tourId) ++ selectWithdraw,
        $doc("$unset" -> $doc("w" -> true)),
        multi = true
      )
      .void

  def find(tourId: TourId, userId: UserId): Fu[Option[Player]] =
    coll.find(selectTourUser(tourId, userId)).one[Player]

  def update(tourId: TourId, userId: UserId)(f: Player => Fu[Player]): Funit =
    find(tourId, userId).orFail(s"No such player: $tourId/$userId").flatMap(f).flatMap(update)

  def update(player: Player): Funit = coll.update.one($id(player._id), player).void

  def join(
      tourId: TourId,
      user: WithPerf,
      team: Option[TeamId],
      prev: Option[Player]
  ) = prev match
    case Some(p) if p.withdraw => coll.update.one($id(p._id), $unset("w"))
    case Some(_) => funit
    case None => coll.insert.one(Player.make(tourId, user, team, user.user.isBot))

  def withdraw(tourId: TourId, userId: UserId) =
    coll.update.one(selectTourUser(tourId, userId), $set("w" -> true)).void

  private[tournament] def withPoints(tourId: TourId): Fu[List[Player]] =
    coll.list[Player](selectTour(tourId) ++ $doc("m".$gt(0)))

  private[tournament] def nbActivePlayers(tourId: TourId): Fu[Int] =
    coll.countSel(selectTour(tourId) ++ selectActive)

  private[tournament] def activeBotIds(tourId: TourId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("uid", selectTour(tourId) ++ selectActive ++ selectBot)

  def winner(tourId: TourId): Fu[Option[Player]] =
    coll.find(selectTour(tourId)).sort(bestSort).one[Player]

  // freaking expensive (marathons)
  // note: tournaments before ISODate("2015-06-15T03:34:01.134Z") (s0tKhoTU)
  // have player IDs with a length <= 8, breaking this optimization
  // instead of fixing it with `$doc("$concat" -> $arr("$_id", ":", "$uid"))`
  // we can just hide the damage in the UI
  // to save serverside perfs
  private[tournament] def computeRanking(tourId: TourId): Fu[FullRanking] =
    coll
      .aggregateWith[Bdoc](): framework =>
        import framework.*
        List(
          Match(selectTour(tourId)),
          Sort(Descending("m")),
          Group(BSONNull)("all" -> Push($doc("$concat" -> $arr("$_id", "$uid"))))
        )
      .headOption
      .map:
        _.flatMap(_.getAsOpt[BSONArray]("all"))
          .fold(FullRanking(Map.empty, Array.empty)): all =>
            // mutable optimized implementation
            val playerIndex = new Array[TourPlayerId](all.size)
            val ranking = Map.newBuilder[UserId, Rank]
            var r = 0
            for u <- all.values do
              val both = u.asInstanceOf[BSONString].value
              val userId = UserId(both.drop(8))
              playerIndex(r) = TourPlayerId(both.take(8))
              ranking += (userId -> Rank(r))
              r = r + 1
            FullRanking(ranking.result(), playerIndex)

  def computeRankOf(player: Player): Fu[Rank] =
    Rank.from(coll.countSel(selectTour(player.tourId) ++ $doc("m".$gt(player.magicScore))))

  // expensive, cache it
  private[tournament] def averageRating(tourId: TourId): Fu[Int] =
    coll
      .aggregateWith[Bdoc](): framework =>
        import framework.*
        List(Match(selectTour(tourId)), Group(BSONNull)("rating" -> AvgField("r")))
      .headOption
      .map:
        ~_.flatMap(_.double("rating").map(_.toInt))

  def byTourAndUserIds(tourId: TourId, userIds: Iterable[UserId]): Fu[List[Player]] =
    coll
      .list[Player](selectTour(tourId) ++ $doc("uid".$in(userIds)))
      .chronometer
      .logIfSlow(200, logger): players =>
        s"PlayerRepo.byTourAndUserIds $tourId ${userIds.size} user IDs, ${players.size} players"
      .result

  def pairByTourAndUserIds(tourId: TourId, id1: UserId, id2: UserId): Fu[Option[(Player, Player)]] =
    byTourAndUserIds(tourId, List(id1, id2)).map:
      case List(p1, p2) if p1.is(id1) && p2.is(id2) => Some(p1 -> p2)
      case List(p1, p2) if p1.is(id2) && p2.is(id1) => Some(p2 -> p1)
      case _ => none

  private def rankPlayers(players: List[Player], ranking: Ranking): RankedPlayers =
    players
      .flatMap: p =>
        ranking.get(p.userId).map(RankedPlayer(_, p))
      .sortBy(_.rank)(using intOrdering)

  def rankedByTourAndUserIds(
      tourId: TourId,
      userIds: Iterable[UserId],
      ranking: Ranking
  ): Fu[RankedPlayers] =
    byTourAndUserIds(tourId, userIds)
      .map(rankPlayers(_, ranking))
      .chronometer
      .logIfSlow(200, logger): players =>
        s"PlayerRepo.rankedByTourAndUserIds $tourId ${userIds.size} user IDs, ${ranking.size} ranking, ${players.size} players"
      .result

  def searchPlayers(tourId: TourId, term: UserSearch, nb: Int): Fu[List[UserId]] =
    coll.primitive[UserId](
      selector = $doc(
        "tid" -> tourId,
        "uid".$startsWith(term.value)
      ),
      sort = $sort.desc("m"),
      nb = nb,
      field = "uid"
    )

  def teamsWithPlayers(tourId: TourId): Fu[Set[TeamId]] =
    coll.distinctEasy[TeamId, Set]("t", selectTour(tourId))

  private[tournament] def sortedCursor(
      tournamentId: TourId,
      batchSize: Int,
      readPref: ReadPref = _.sec
  ): AkkaStreamCursor[Player] =
    coll
      .find(selectTour(tournamentId))
      .sort($sort.desc("m"))
      .batchSize(batchSize)
      .cursor[Player](readPref)

  private[tournament] def anonymize(tourId: TourId, userId: UserId)(ghostId: UserId) =
    coll.update.one($doc("tid" -> tourId, "uid" -> userId), $set("uid" -> ghostId)).void
