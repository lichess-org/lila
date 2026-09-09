package lila.tournament

import chess.variant.Variant
import reactivemongo.pekkostream.{ PekkoStreamCursor, cursorProducer }
import scalalib.model.Minutes

import lila.core.config.CollName
import lila.core.tournament.Status
import lila.db.dsl.{ *, given }
import lila.mon.extensions.*

final class TournamentRepo(val coll: Coll, playerCollName: CollName)(using Executor):
  import BSONHandlers.given

  private val enterableSelect = bdoc("status".lt(Status.finished.id))
  private val createdSelect = bdoc("status" -> Status.created.id)
  private val startedSelect = bdoc("status" -> Status.started.id)
  private[tournament] val finishedSelect = bdoc("status" -> Status.finished.id)
  private val unfinishedSelect = bdoc("status".neq(Status.finished.id))
  private[tournament] val scheduledSelect = bdoc("schedule".exists(true))
  private[tournament] val scheduledButNotHourly =
    scheduledSelect ++ bdoc("schedule.freq".neq(Schedule.Freq.Hourly))
  private def forTeamSelect(id: TeamId) = bdoc("forTeams" -> id)
  private def forTeamsSelect(ids: Seq[TeamId]) = bdoc("forTeams".in(ids))
  private def sinceSelect(date: Instant) = bdoc("startsAt".gt(date))
  private def variantSelect(variant: Variant) =
    if variant.standard then bdoc("variant".exists(false))
    else bdoc("variant" -> variant.id)
  private def nbPlayersSelect(nb: Int) = bdoc("nbPlayers".gte(nb))
  private val nonEmptySelect = nbPlayersSelect(1)
  private[tournament] val selectUnique = bdoc("schedule.freq" -> "unique")

  def byId(id: TourId): Fu[Option[Tournament]] = coll.byId[Tournament](id)
  def exists(id: TourId): Fu[Boolean] = coll.exists(bid(id))

  def uniqueById(id: TourId): Fu[Option[Tournament]] =
    coll.one[Tournament](bid(id) ++ selectUnique)

  def finishedById(id: TourId): Fu[Option[Tournament]] =
    coll.one[Tournament](bid(id) ++ finishedSelect)

  def countCreated: Fu[Int] = coll.countSel(createdSelect)

  def fetchCreatedBy(id: TourId): Fu[Option[UserId]] =
    coll.primitiveOne[UserId](bid(id), "createdBy")

  private[tournament] def startedCursorWithNbPlayersGte(nbPlayers: Option[Int]) =
    coll
      .find(startedSelect ++ nbPlayers.so(nbPlayersSelect))
      .batchSize(1)
      .cursor[Tournament]()

  private[tournament] def idsCursor(ids: Iterable[TourId]) =
    coll.find(inIds(ids)).cursor[Tournament]()

  private[tournament] def standardPublicStartedFromSecondary: Fu[List[Tournament]] =
    coll.list[Tournament](
      startedSelect ++ bdoc(
        "password".exists(false),
        "variant".exists(false)
      ),
      _.sec
    )

  private[tournament] def notableFinished(limit: Int): Fu[List[Tournament]] =
    coll
      .find(finishedSelect ++ scheduledSelect)
      .sort(sort.desc("startsAt"))
      .cursor[Tournament]()
      .list(limit)

  private[tournament] def byOwnerAdapter(owner: User) =
    new lila.db.paginator.Adapter[Tournament](
      collection = coll,
      selector = bdoc("createdBy" -> owner.id),
      projection = none,
      sort = sort.desc("startsAt"),
      _.sec
    )

  private def lookupPlayer(userId: UserId, project: Option[Bdoc]) =
    lookup.pipelineFull(
      from = playerCollName.value,
      as = "player",
      let = bdoc("tid" -> "$_id"),
      pipe = List(
        bdoc(
          "$match" -> expr(
            and(
              bdoc("$eq" -> barr("$uid", userId)),
              bdoc("$eq" -> barr("$tid", "$$tid"))
            )
          )
        ).some,
        project.map { p => bdoc(s"$$project" -> p) }
      ).flatten
    )

  private[tournament] def upcomingAdapterExpensiveCacheMe(userId: UserId, max: Int) =
    coll
      .aggregateList(max, _.sec): framework =>
        import framework.*
        Match(enterableSelect ++ nonEmptySelect) -> List(
          PipelineOperator(lookupPlayer(userId, bdoc("tid" -> true, "_id" -> false).some)),
          Match("player".neq(barr())),
          Sort(Ascending("startsAt")),
          Limit(max)
        )
      .map(_.flatMap(_.asOpt[Tournament]))
      .dmap { new lila.db.paginator.StaticAdapter(_) }

  def finishedByFreqAdapter(freq: Schedule.Freq) =
    lila.db.paginator
      .Adapter[Tournament](
        collection = coll,
        selector = bdoc("schedule.freq" -> freq, "status" -> Status.finished.id),
        projection = none,
        sort = sort.desc("startsAt"),
        _.sec
      )
      .withLotsOfResults

  def isUnfinished(tourId: TourId): Fu[Boolean] =
    coll.secondary.exists(bid(tourId) ++ unfinishedSelect)

  def byTeamCursor(
      teamId: TeamId,
      status: Option[Status],
      createdBy: Option[UserStr],
      name: Option[String]
  ) =
    val statusSel = status.so(s => bdoc("status" -> s.id))
    val creatorSel = createdBy.so(u => bdoc("createdBy" -> u))
    val nameSel = name.so(n => bdoc("name" -> n))
    coll
      .find(forTeamSelect(teamId) ++ statusSel ++ creatorSel ++ nameSel)
      .sort(sort.desc("startsAt"))
      .cursor[Tournament]()

  private[tournament] def upcomingByTeam(teamId: TeamId, nb: Int) =
    (nb > 0).so:
      coll
        .find(
          forTeamSelect(teamId) ++ enterableSelect ++ bdoc(
            "startsAt".gt(nowInstant.minusDays(1))
          )
        )
        .sort(sort.asc("startsAt"))
        .cursor[Tournament]()
        .list(nb)

  private[tournament] def finishedByTeam(teamId: TeamId, nb: Int) =
    (nb > 0).so:
      coll
        .find(forTeamSelect(teamId) ++ finishedSelect)
        .sort(sort.desc("startsAt"))
        .cursor[Tournament]()
        .list(nb)

  private[tournament] def setForTeam(tourId: TourId, teamId: TeamId) =
    coll.update.one(bid(tourId), addToSet("forTeams" -> teamId))

  def isForTeam(tourId: TourId, teamId: TeamId) =
    coll.exists(bid(tourId) ++ bdoc("forTeams" -> teamId))

  private[tournament] def withdrawableIds(
      userId: UserId,
      teamId: Option[TeamId] = None,
      reason: String
  ): Fu[List[TourId]] =
    coll
      .aggregateList(Int.MaxValue, _.sec): framework =>
        import framework.*
        Match(enterableSelect ++ nonEmptySelect ++ teamId.so(forTeamSelect)) -> List(
          PipelineOperator(lookupPlayer(userId, none)),
          Match("player".neq(barr())),
          Project(bid(true))
        )
      .map(_.flatMap(_.getAsOpt[TourId]("_id")))
      .monSuccess(lila.mon.tournament.withdrawableIds(reason))

  def setStatus(tourId: TourId, status: Status) =
    coll.updateField(bid(tourId), "status", status.id).void

  def setNbPlayers(tourId: TourId, nb: Int) =
    coll.updateField(bid(tourId), "nbPlayers", nb).void

  def setWinnerId(tourId: TourId, userId: UserId) =
    coll.updateField(bid(tourId), "winner", userId).void

  def setFeaturedGameId(tourId: TourId, gameId: GameId) =
    coll.updateField(bid(tourId), "featured", gameId).void

  def setTeamBattle(tourId: TourId, battle: TeamBattle) =
    coll.updateField(bid(tourId), "teamBattle", battle).void

  def teamBattleOf(tourId: TourId): Fu[Option[TeamBattle]] =
    coll.primitiveOne[TeamBattle](bid(tourId), "teamBattle")

  def isTeamBattle(tourId: TourId): Fu[Boolean] =
    coll.exists(bid(tourId) ++ bdoc("teamBattle".exists(true)))

  def featuredGameId(tourId: TourId) = coll.primitiveOne[GameId](bid(tourId), "featured")

  private def startingSoonSelect(ahead: Minutes) =
    createdSelect ++
      bdoc("startsAt".lt(nowInstant.plusMinutes(ahead.value)))

  private[tournament] def scheduledCreated(ahead: Minutes): Fu[List[Tournament]] =
    coll.list[Tournament](startingSoonSelect(ahead) ++ scheduledSelect)

  private[tournament] def scheduledStarted: Fu[List[Tournament]] =
    coll.list[Tournament](startedSelect ++ scheduledSelect)

  private[tournament] def visibleForTeams(
      teamIds: Seq[TeamId],
      ahead: Minutes,
      max: Max
  ): Fu[List[Tournament]] =
    teamIds.nonEmpty.so:
      coll
        .find(forTeamsSelect(teamIds) ++ or(startedSelect, startingSoonSelect(ahead)))
        .sort(sort.asc("startsAt"))
        .cursor[Tournament](ReadPref.sec)
        .list(max.value)

  private[tournament] def shouldStartCursor =
    coll
      .find(bdoc("startsAt".lt(nowInstant)) ++ createdSelect)
      .batchSize(1)
      .cursor[Tournament]()

  private[tournament] def soonStarting(from: Instant, to: Instant, notIds: Iterable[TourId]) =
    coll
      .find(createdSelect ++ bdoc("nbPlayers".gt(0), "startsAt".gt(from).lt(to), "_id".nin(notIds)))
      .cursor[Tournament]()
      .list(5)

  private[tournament] def scheduledNotHourlyCreated(ahead: Minutes): Fu[List[Tournament]] =
    coll.list[Tournament](startingSoonSelect(ahead) ++ scheduledButNotHourly)

  private[tournament] def scheduledNotHourlyStillWorthEntering: Fu[List[Tournament]] =
    coll
      .list[Tournament](startedSelect ++ scheduledButNotHourly)
      .map:
        _.filter(_.isStillWorthEntering)

  def uniques(max: Int): Fu[List[Tournament]] =
    coll
      .find(selectUnique)
      .sort(bdoc("startsAt" -> -1))
      .hint(coll.hint(bdoc("startsAt" -> -1)))
      .cursor[Tournament]()
      .list(max)

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll
      .find(scheduledSelect ++ unfinishedSelect)
      .sort(bdoc("startsAt" -> 1))
      .cursor[Tournament]()
      .listAll()

  def allScheduledDedup: Fu[List[Tournament]] =
    coll
      .find(
        createdSelect ++ scheduledButNotHourly ++ bdoc(
          "startsAt".lt(nowInstant.plusDays(21))
        )
      )
      .sort(bdoc("startsAt" -> 1))
      .cursor[Tournament]()
      .listAll()
      .map:
        _.flatMap { tour =>
          tour.scheduleFreq.map(tour -> _)
        }.foldLeft(List.empty[Tournament] -> none[Schedule.Freq]) {
          case ((tours, skip), (_, freq)) if skip.has(freq) => (tours, skip)
          case ((tours, skip), (tour, freq)) =>
            (
              tour :: tours,
              freq match
                case Schedule.Freq.Daily => Schedule.Freq.Eastern.some
                case Schedule.Freq.Eastern => Schedule.Freq.Daily.some
                case _ => skip
            )
        }._1
          .reverse

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: Instant): Fu[List[Tournament]] =
    coll
      .find(
        finishedSelect ++ sinceSelect(since) ++ variantSelect(chess.variant.Standard) ++ bdoc(
          "schedule.freq" -> freq.name,
          "schedule.speed".in(Schedule.Speed.mostPopular.map(_.key))
        )
      )
      .sort(sort.desc("startsAt"))
      .cursor[Tournament]()
      .list(Schedule.Speed.mostPopular.size)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] =
    coll
      .find(
        finishedSelect ++ sinceSelect(nowInstant.minusDays(1)) ++ variantSelect(variant) ++
          bdoc("schedule.freq" -> Schedule.Freq.Daily.name)
      )
      .sort(sort.desc("startsAt"))
      .one[Tournament]

  def update(tour: Tournament) =
    coll.update.one(
      bid(tour.id),
      set(tourHandler.write(tour)) ++ unset(
        List(
          // tour.conditions.titled.isEmpty option "conditions.titled",
          tour.rated.yes.option("mode"),
          tour.berserkable.option("noBerserk"),
          tour.streakable.option("noStreak"),
          tour.hasChat.option("chat"),
          tour.password.isEmpty.option("password"),
          tour.conditions.list.isEmpty.option("conditions"),
          tour.position.isEmpty.option("fen"),
          tour.variant.standard.option("variant"),
          tour.payouts.isEmpty.option("payouts")
        ).flatten
      )
    )

  def setSchedule(tourId: TourId, schedule: Option[Scheduled]) =
    coll.updateOrUnsetField(bid(tourId), "schedule", schedule).void

  def insert(tour: Tournament) = coll.insert.one(tour)

  def insert(tours: Seq[Tournament]) = tours.nonEmpty.so(coll.insert(ordered = false).many(tours).void)

  def remove(tour: Tournament) = coll.delete.one(bid(tour.id))

  def calendar(from: Instant, to: Instant): Fu[List[Tournament]] =
    coll
      .find:
        bdoc(
          "startsAt".gte(from).lte(to),
          "schedule.freq".in(Schedule.Freq.list.filter(_.isWeeklyOrBetter))
        )
      .sort(sort.asc("startsAt"))
      .cursor[Tournament](ReadPref.sec)
      .list(500)

  def anonymize(tour: Tournament, u: UserId) = for
    _ <- tour.winnerId.has(u).so(coll.updateField(bid(tour.id), "winner", UserId.ghost).void)
    _ <- tour.createdBy.is(u).so(coll.updateField(bid(tour.id), "createdBy", UserId.ghost).void)
  yield ()

  private[tournament] def sortedCursor(
      owner: User,
      status: List[Status],
      batchSize: Int,
      readPref: ReadPref = _.sec
  ): PekkoStreamCursor[Tournament] =
    coll
      .find(bdoc("createdBy" -> owner.id) ++ (status.nonEmpty.so(bdoc("status".in(status)))))
      .sort(sort.desc("startsAt"))
      .batchSize(batchSize)
      .cursor[Tournament](readPref)
