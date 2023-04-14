package lila.tournament

import chess.variant.Variant
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import lila.common.config.CollName
import lila.db.dsl.{ *, given }
import lila.user.User

final class TournamentRepo(val coll: Coll, playerCollName: CollName)(using Executor):
  import BSONHandlers.given

  private val enterableSelect                  = $doc("status" $lt Status.Finished.id)
  private val createdSelect                    = $doc("status" -> Status.Created.id)
  private val startedSelect                    = $doc("status" -> Status.Started.id)
  private[tournament] val finishedSelect       = $doc("status" -> Status.Finished.id)
  private val unfinishedSelect                 = $doc("status" $ne Status.Finished.id)
  private[tournament] val scheduledSelect      = $doc("schedule" $exists true)
  private def forTeamSelect(id: TeamId)        = $doc("forTeams" -> id)
  private def forTeamsSelect(ids: Seq[TeamId]) = $doc("forTeams" $in ids)
  private def sinceSelect(date: Instant)       = $doc("startsAt" $gt date)
  private def variantSelect(variant: Variant) =
    if (variant.standard) $doc("variant" $exists false)
    else $doc("variant" -> variant.id)
  private def nbPlayersSelect(nb: Int) = $doc("nbPlayers" $gte nb)
  private val nonEmptySelect           = nbPlayersSelect(1)
  private[tournament] val selectUnique = $doc("schedule.freq" -> "unique")

  def byId(id: TourId): Fu[Option[Tournament]] = coll.byId[Tournament](id)
  def exists(id: TourId): Fu[Boolean]          = coll.exists($id(id))

  def uniqueById(id: TourId): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ selectUnique)

  def finishedById(id: TourId): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ finishedSelect)

  def countCreated: Fu[Int] = coll.countSel(createdSelect)

  def fetchCreatedBy(id: TourId): Fu[Option[UserId]] =
    coll.primitiveOne[UserId]($id(id), "createdBy")

  private[tournament] def startedCursorWithNbPlayersGte(nbPlayers: Option[Int]) =
    coll
      .find(startedSelect ++ nbPlayers.??(nbPlayersSelect))
      .batchSize(1)
      .cursor[Tournament]()

  private[tournament] def idsCursor(ids: Iterable[TourId]) =
    coll.find($inIds(ids)).cursor[Tournament]()

  def standardPublicStartedFromSecondary: Fu[List[Tournament]] =
    coll.list[Tournament](
      startedSelect ++ $doc(
        "password" $exists false,
        "variant" $exists false
      ),
      ReadPreference.secondaryPreferred
    )

  private[tournament] def notableFinished(limit: Int): Fu[List[Tournament]] =
    coll
      .find(finishedSelect ++ scheduledSelect)
      .sort($sort desc "startsAt")
      .cursor[Tournament]()
      .list(limit)

  private[tournament] def byOwnerAdapter(owner: User) =
    new lila.db.paginator.Adapter[Tournament](
      collection = coll,
      selector = $doc("createdBy" -> owner.id),
      projection = none,
      sort = $sort desc "startsAt",
      readPreference = ReadPreference.secondaryPreferred
    )

  private def lookupPlayer(userId: UserId, project: Option[Bdoc]) =
    $lookup.pipelineFull(
      from = playerCollName.value,
      as = "player",
      let = $doc("tid" -> "$_id"),
      pipe = List(
        $doc(
          "$match" -> $expr(
            $and(
              $doc("$eq" -> $arr("$uid", userId)),
              $doc("$eq" -> $arr("$tid", "$$tid"))
            )
          )
        ).some,
        project.map { p => $doc(s"$$project" -> p) }
      ).flatten
    )

  private[tournament] def upcomingAdapterExpensiveCacheMe(userId: UserId, max: Int) =
    coll
      .aggregateList(max, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework.*
        Match(enterableSelect ++ nonEmptySelect) -> List(
          PipelineOperator(lookupPlayer(userId, $doc("tid" -> true, "_id" -> false).some)),
          Match("player" $ne $arr()),
          Sort(Ascending("startsAt")),
          Limit(max)
        )
      }
      .map(_.flatMap(_.asOpt[Tournament]))
      .dmap { new lila.db.paginator.StaticAdapter(_) }

  def finishedByFreqAdapter(freq: Schedule.Freq) =
    new lila.db.paginator.Adapter[Tournament](
      collection = coll,
      selector = $doc("schedule.freq" -> freq, "status" -> Status.Finished.id),
      projection = none,
      sort = $sort desc "startsAt",
      readPreference = ReadPreference.secondaryPreferred
    ).withNbResults(fuccess(Int.MaxValue))

  def isUnfinished(tourId: TourId): Fu[Boolean] =
    coll.exists($id(tourId) ++ unfinishedSelect)

  def byTeamCursor(teamId: TeamId) =
    coll
      .find(forTeamSelect(teamId))
      .sort($sort desc "startsAt")
      .cursor[Tournament]()

  private[tournament] def upcomingByTeam(teamId: TeamId, nb: Int) =
    (nb > 0) ?? coll
      .find(
        forTeamSelect(teamId) ++ enterableSelect ++ $doc(
          "startsAt" $gt nowInstant.minusDays(1)
        )
      )
      .sort($sort asc "startsAt")
      .cursor[Tournament]()
      .list(nb)

  private[tournament] def finishedByTeam(teamId: TeamId, nb: Int) =
    (nb > 0) ?? coll
      .find(forTeamSelect(teamId) ++ finishedSelect)
      .sort($sort desc "startsAt")
      .cursor[Tournament]()
      .list(nb)

  private[tournament] def setForTeam(tourId: TourId, teamId: TeamId) =
    coll.update.one($id(tourId), $addToSet("forTeams" -> teamId))

  def isForTeam(tourId: TourId, teamId: TeamId) =
    coll.exists($id(tourId) ++ $doc("forTeams" -> teamId))

  private[tournament] def withdrawableIds(
      userId: UserId,
      teamId: Option[TeamId] = None,
      reason: String
  ): Fu[List[TourId]] =
    coll
      .aggregateList(Int.MaxValue, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework.*
        Match(enterableSelect ++ nonEmptySelect ++ teamId.??(forTeamSelect)) -> List(
          PipelineOperator(lookupPlayer(userId, none)),
          Match("player" $ne $arr()),
          Project($id(true))
        )
      }
      .map(_.flatMap(_.getAsOpt[TourId]("_id")))
      .monSuccess(_.tournament.withdrawableIds(reason))

  def setStatus(tourId: TourId, status: Status) =
    coll.updateField($id(tourId), "status", status.id).void

  def setNbPlayers(tourId: TourId, nb: Int) =
    coll.updateField($id(tourId), "nbPlayers", nb).void

  def setWinnerId(tourId: TourId, userId: UserId) =
    coll.updateField($id(tourId), "winner", userId).void

  def setFeaturedGameId(tourId: TourId, gameId: GameId) =
    coll.updateField($id(tourId), "featured", gameId).void

  def setTeamBattle(tourId: TourId, battle: TeamBattle) =
    coll.updateField($id(tourId), "teamBattle", battle).void

  def teamBattleOf(tourId: TourId): Fu[Option[TeamBattle]] =
    coll.primitiveOne[TeamBattle]($id(tourId), "teamBattle")

  def isTeamBattle(tourId: TourId): Fu[Boolean] =
    coll.exists($id(tourId) ++ $doc("teamBattle" $exists true))

  def featuredGameId(tourId: TourId) = coll.primitiveOne[GameId]($id(tourId), "featured")

  private def startingSoonSelect(aheadMinutes: Int) =
    createdSelect ++
      $doc("startsAt" $lt (nowInstant plusMinutes aheadMinutes))

  def scheduledCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.list[Tournament](startingSoonSelect(aheadMinutes) ++ scheduledSelect)

  def scheduledStarted: Fu[List[Tournament]] =
    coll.list[Tournament](startedSelect ++ scheduledSelect)

  def visibleForTeams(teamIds: Seq[TeamId], aheadMinutes: Int) =
    coll.list[Tournament](
      startingSoonSelect(aheadMinutes) ++ forTeamsSelect(teamIds),
      ReadPreference.secondaryPreferred
    ) zip
      coll
        .list[Tournament](startedSelect ++ forTeamsSelect(teamIds), ReadPreference.secondaryPreferred) dmap {
        case (created, started) => created ::: started
      }

  private[tournament] def shouldStartCursor =
    coll
      .find($doc("startsAt" $lt nowInstant) ++ createdSelect)
      .batchSize(1)
      .cursor[Tournament]()

  private[tournament] def soonStarting(from: Instant, to: Instant, notIds: Iterable[TourId]) =
    coll
      .find(createdSelect ++ $doc("nbPlayers" $gt 0, "startsAt" $gt from $lt to, "_id" $nin notIds))
      .cursor[Tournament]()
      .list(5)

  private def scheduledStillWorthEntering: Fu[List[Tournament]] =
    coll.list[Tournament](startedSelect ++ scheduledSelect) dmap {
      _.filter(_.isStillWorthEntering)
    }

  private def canShowOnHomepage(tour: Tournament): Boolean =
    tour.schedule exists { schedule =>
      tour.startsAt isBefore nowInstant.plusMinutes {
        import Schedule.Freq.*
        val base = schedule.freq match
          case Unique => tour.spotlight.flatMap(_.homepageHours).fold(24 * 60)((_: Int) * 60)
          case Unique | Yearly | Marathon => 24 * 60
          case Monthly | Shield           => 6 * 60
          case Weekly | Weekend           => 3 * 45
          case Daily                      => 1 * 30
          case _                          => 20
        if (tour.variant.exotic && schedule.freq != Unique) base / 3 else base
      }
    }

  private[tournament] def onHomepage: Fu[List[Tournament]] =
    scheduledStillWorthEntering zip scheduledCreated(crud.CrudForm.maxHomepageHours * 60) map {
      case (started, created) =>
        (started ::: created)
          .sortBy(_.startsAt.toSeconds)
          .foldLeft(List.empty[Tournament]) {
            case (acc, tour) if !canShowOnHomepage(tour)     => acc
            case (acc, tour) if acc.exists(_ similarTo tour) => acc
            case (acc, tour)                                 => tour :: acc
          }
          .reverse
    }

  def uniques(max: Int): Fu[List[Tournament]] =
    coll
      .find(selectUnique)
      .sort($doc("startsAt" -> -1))
      .hint(coll hint $doc("startsAt" -> -1))
      .cursor[Tournament]()
      .list(max)

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll
      .find(scheduledSelect ++ unfinishedSelect)
      .sort($doc("startsAt" -> 1))
      .cursor[Tournament]()
      .listAll()

  def allScheduledDedup: Fu[List[Tournament]] =
    coll
      .find(createdSelect ++ scheduledSelect)
      .sort($doc("startsAt" -> 1))
      .cursor[Tournament]()
      .listAll() map {
      _.flatMap { tour =>
        tour.schedule map (tour -> _)
      }.foldLeft(List.empty[Tournament] -> none[Schedule.Freq]) {
        case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
        case ((tours, skip), (tour, sched)) =>
          (
            tour :: tours,
            sched.freq match {
              case Schedule.Freq.Daily   => Schedule.Freq.Eastern.some
              case Schedule.Freq.Eastern => Schedule.Freq.Daily.some
              case _                     => skip
            }
          )
      }._1
        .reverse
    }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: Instant): Fu[List[Tournament]] =
    coll
      .find(
        finishedSelect ++ sinceSelect(since) ++ variantSelect(chess.variant.Standard) ++ $doc(
          "schedule.freq" -> freq.name,
          "schedule.speed" $in Schedule.Speed.mostPopular.map(_.key)
        )
      )
      .sort($sort desc "startsAt")
      .cursor[Tournament]()
      .list(Schedule.Speed.mostPopular.size)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] =
    coll
      .find(
        finishedSelect ++ sinceSelect(nowInstant minusDays 1) ++ variantSelect(variant) ++
          $doc("schedule.freq" -> Schedule.Freq.Daily.name)
      )
      .sort($sort desc "startsAt")
      .one[Tournament]

  def update(tour: Tournament) =
    coll.update.one(
      $id(tour.id),
      $set(tourHandler.write(tour)) ++ $unset(
        List(
          // tour.conditions.titled.isEmpty option "conditions.titled",
          tour.isRated option "mode",
          tour.berserkable option "noBerserk",
          tour.streakable option "noStreak",
          tour.hasChat option "chat",
          tour.password.isEmpty option "password",
          tour.conditions.list.isEmpty option "conditions",
          tour.position.isEmpty option "fen",
          tour.variant.standard option "variant"
        ).flatten
      )
    )

  def setSchedule(tourId: TourId, schedule: Option[Schedule]) =
    schedule match
      case None    => coll.unsetField($id(tourId), "schedule").void
      case Some(s) => coll.updateField($id(tourId), "schedule", s).void

  def insert(tour: Tournament) = coll.insert.one(tour)

  def insert(tours: Seq[Tournament]) = tours.nonEmpty ??
    coll.insert(ordered = false).many(tours).void

  def remove(tour: Tournament) = coll.delete.one($id(tour.id))

  def calendar(from: Instant, to: Instant): Fu[List[Tournament]] =
    coll
      .find(
        $doc(
          "startsAt" $gte from $lte to,
          "schedule.freq" $in Schedule.Freq.all.filter(_.isWeeklyOrBetter)
        )
      )
      .sort($sort asc "startsAt")
      .cursor[Tournament](ReadPreference.secondaryPreferred)
      .list(500)

  private[tournament] def sortedCursor(
      owner: lila.user.User,
      status: List[Status],
      batchSize: Int,
      readPreference: ReadPreference = temporarilyPrimary
  ): AkkaStreamCursor[Tournament] =
    coll
      .find($doc("createdBy" -> owner.id) ++ (status.nonEmpty ?? $doc("status" $in status)))
      .sort($sort desc "startsAt")
      .batchSize(batchSize)
      .cursor[Tournament](readPreference)
