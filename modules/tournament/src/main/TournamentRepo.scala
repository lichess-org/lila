package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import lila.common.config.CollName
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.user.User

final class TournamentRepo(val coll: Coll, playerCollName: CollName)(implicit
    ec: scala.concurrent.ExecutionContext
) {
  import BSONHandlers._

  private val enterableSelect                  = $doc("status" $lt Status.Finished.id)
  private val createdSelect                    = $doc("status" -> Status.Created.id)
  private val startedSelect                    = $doc("status" -> Status.Started.id)
  private[tournament] val finishedSelect       = $doc("status" -> Status.Finished.id)
  private val unfinishedSelect                 = $doc("status" $ne Status.Finished.id)
  private[tournament] val scheduledSelect      = $doc("schedule" $exists true)
  private def forTeamSelect(id: TeamID)        = $doc("forTeams" -> id)
  private def forTeamsSelect(ids: Seq[TeamID]) = $doc("forTeams" $in ids)
  private def sinceSelect(date: DateTime)      = $doc("startsAt" $gt date)
  private def variantSelect(variant: Variant) =
    if (variant.standard) $doc("variant" $exists false)
    else $doc("variant" -> variant.id)
  private val nonEmptySelect           = $doc("nbPlayers" $ne 0)
  private[tournament] val selectUnique = $doc("schedule.freq" -> "unique")

  def byId(id: Tournament.ID): Fu[Option[Tournament]] = coll.byId[Tournament](id)

  def byIds(ids: Iterable[Tournament.ID]): Fu[List[Tournament]] =
    coll.list[Tournament]($inIds(ids))

  def byOrderedIds(ids: Iterable[Tournament.ID]): Fu[List[Tournament]] =
    coll.byOrderedIds[Tournament, Tournament.ID](ids, readPreference = ReadPreference.secondaryPreferred)(
      _.id
    )

  def uniqueById(id: Tournament.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ selectUnique)

  def byIdAndPlayerId(id: Tournament.ID, userId: User.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ $doc("players.id" -> userId))

  def createdById(id: Tournament.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ createdSelect)

  def enterableById(id: Tournament.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ enterableSelect)

  def startedById(id: Tournament.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ startedSelect)

  def finishedById(id: Tournament.ID): Fu[Option[Tournament]] =
    coll.one[Tournament]($id(id) ++ finishedSelect)

  def startedOrFinishedById(id: Tournament.ID): Fu[Option[Tournament]] =
    byId(id) map { _ filterNot (_.isCreated) }

  def createdByIdAndCreator(id: Tournament.ID, userId: User.ID): Fu[Option[Tournament]] =
    createdById(id) map (_ filter (_.createdBy == userId))

  def countCreated: Fu[Int] = coll.countSel(createdSelect)

  def fetchCreatedBy(id: Tournament.ID): Fu[Option[User.ID]] =
    coll.primitiveOne[User.ID]($id(id), "createdBy")

  private[tournament] def startedCursor =
    coll.find(startedSelect).sort($doc("createdAt" -> -1)).batchSize(1).cursor[Tournament]()

  def startedIds: Fu[List[Tournament.ID]] =
    coll.primitive[Tournament.ID](startedSelect, sort = $doc("createdAt" -> -1), "_id")

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

  private[tournament] def upcomingAdapterExpensiveCacheMe(userId: User.ID, max: Int) =
    coll
      .aggregateList(max, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match(enterableSelect ++ nonEmptySelect) -> List(
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> playerCollName.value,
                "let"  -> $doc("tid" -> "$_id"),
                "pipeline" -> $arr(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc(
                        "$and" -> $arr(
                          $doc("$eq" -> $arr("$uid", userId)),
                          $doc("$eq" -> $arr("$tid", "$$tid"))
                        )
                      )
                    )
                  ),
                  $doc("$project" -> $doc("tid" -> true, "_id" -> false))
                ),
                "as" -> "player"
              )
            )
          ),
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
    )

  def isUnfinished(tourId: Tournament.ID): Fu[Boolean] =
    coll.exists($id(tourId) ++ unfinishedSelect)

  def clockById(id: Tournament.ID): Fu[Option[chess.Clock.Config]] =
    coll.primitiveOne[chess.Clock.Config]($id(id), "clock")

  def byTeamCursor(teamId: TeamID) =
    coll
      .find(forTeamSelect(teamId))
      .sort($sort desc "startsAt")
      .cursor[Tournament]()

  private[tournament] def upcomingByTeam(teamId: TeamID, nb: Int) =
    (nb > 0) ?? coll
      .find(
        forTeamSelect(teamId) ++ enterableSelect ++ $doc(
          "startsAt" $gt DateTime.now.minusDays(1)
        )
      )
      .sort($sort asc "startsAt")
      .cursor[Tournament]()
      .list(nb)

  private[tournament] def finishedByTeam(teamId: TeamID, nb: Int) =
    (nb > 0) ?? coll
      .find(forTeamSelect(teamId) ++ finishedSelect)
      .sort($sort desc "startsAt")
      .cursor[Tournament]()
      .list(nb)

  private[tournament] def setForTeam(tourId: Tournament.ID, teamId: TeamID) =
    coll.update.one($id(tourId), $addToSet("forTeams" -> teamId))

  def isForTeam(tourId: Tournament.ID, teamId: TeamID) =
    coll.exists($id(tourId) ++ $doc("forTeams" -> teamId))

  private[tournament] def withdrawableIds(
      userId: User.ID,
      teamId: Option[TeamID] = None
  ): Fu[List[Tournament.ID]] =
    coll
      .aggregateList(Int.MaxValue, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match(enterableSelect ++ nonEmptySelect ++ teamId.??(forTeamSelect)) -> List(
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> playerCollName.value,
                "let"  -> $doc("t" -> "$_id"),
                "pipeline" -> $arr(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc(
                        "$and" -> $arr(
                          $doc("$eq" -> $arr("$uid", userId)),
                          $doc("$eq" -> $arr("$tid", "$$t"))
                        )
                      )
                    )
                  )
                ),
                "as" -> "player"
              )
            )
          ),
          Match("player" $ne $arr()),
          Project($id(true))
        )
      }
      .map(_.flatMap(_.string("_id")))

  def setStatus(tourId: Tournament.ID, status: Status) =
    coll.updateField($id(tourId), "status", status.id).void

  def setNbPlayers(tourId: Tournament.ID, nb: Int) =
    coll.updateField($id(tourId), "nbPlayers", nb).void

  def setWinnerId(tourId: Tournament.ID, userId: User.ID) =
    coll.updateField($id(tourId), "winner", userId).void

  def setFeaturedGameId(tourId: Tournament.ID, gameId: Game.ID) =
    coll.updateField($id(tourId), "featured", gameId).void

  def setTeamBattle(tourId: Tournament.ID, battle: TeamBattle) =
    coll.updateField($id(tourId), "teamBattle", battle).void

  def teamBattleOf(tourId: Tournament.ID): Fu[Option[TeamBattle]] =
    coll.primitiveOne[TeamBattle]($id(tourId), "teamBattle")

  def isTeamBattle(tourId: Tournament.ID): Fu[Boolean] =
    coll.exists($id(tourId) ++ $doc("teamBattle" $exists true))

  def featuredGameId(tourId: Tournament.ID) = coll.primitiveOne[Game.ID]($id(tourId), "featured")

  private def startingSoonSelect(aheadMinutes: Int) =
    createdSelect ++
      $doc("startsAt" $lt (DateTime.now plusMinutes aheadMinutes))

  def scheduledCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.list[Tournament](startingSoonSelect(aheadMinutes) ++ scheduledSelect)

  def scheduledStarted: Fu[List[Tournament]] =
    coll.list[Tournament](startedSelect ++ scheduledSelect)

  def visibleForTeams(teamIds: Seq[TeamID], aheadMinutes: Int) =
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
      .find($doc("startsAt" $lt DateTime.now) ++ createdSelect)
      .batchSize(1)
      .cursor[Tournament]()

  private def scheduledStillWorthEntering: Fu[List[Tournament]] =
    coll.list[Tournament](startedSelect ++ scheduledSelect) dmap {
      _.filter(_.isStillWorthEntering)
    }

  private def canShowOnHomepage(tour: Tournament): Boolean =
    tour.schedule exists { schedule =>
      tour.startsAt isBefore DateTime.now.plusMinutes {
        import Schedule.Freq._
        val base = schedule.freq match {
          case Unique                     => tour.spotlight.flatMap(_.homepageHours).fold(24 * 60)(60 *)
          case Unique | Yearly | Marathon => 24 * 60
          case Monthly | Shield           => 6 * 60
          case Weekly | Weekend           => 3 * 60
          case Daily                      => 1 * 60
          case _                          => 30
        }
        if (tour.variant.exotic) base / 3 else base
      }
    }

  private[tournament] def onHomepage: Fu[List[Tournament]] =
    scheduledStillWorthEntering zip scheduledCreated(crud.CrudForm.maxHomepageHours * 60) map {
      case (started, created) =>
        (started ::: created)
          .sortBy(_.startsAt.getSeconds)
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
      .list()

  def allScheduledDedup: Fu[List[Tournament]] =
    coll
      .find(createdSelect ++ scheduledSelect)
      .sort($doc("startsAt" -> 1))
      .cursor[Tournament]()
      .list() map {
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

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: DateTime): Fu[List[Tournament]] =
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
        finishedSelect ++ sinceSelect(DateTime.now minusDays 1) ++ variantSelect(variant) ++
          $doc("schedule.freq" -> Schedule.Freq.Daily.name)
      )
      .sort($sort desc "startsAt")
      .one[Tournament]

  def update(tour: Tournament) =
    coll.update.one(
      $id(tour.id),
      $set(tournamentHandler.write(tour)) ++ $unset(
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

  def setSchedule(tourId: Tournament.ID, schedule: Option[Schedule]) =
    schedule match {
      case None    => coll.unsetField($id(tourId), "schedule").void
      case Some(s) => coll.updateField($id(tourId), "schedule", s).void
    }

  def insert(tour: Tournament) = coll.insert.one(tour)

  def insert(tours: Seq[Tournament]) = tours.nonEmpty ??
    coll.insert(ordered = false).many(tours).void

  def remove(tour: Tournament) = coll.delete.one($id(tour.id))

  def exists(id: Tournament.ID) = coll exists $id(id)

  def calendar(from: DateTime, to: DateTime): Fu[List[Tournament]] =
    coll
      .find(
        $doc(
          "startsAt" $gte from $lte to,
          "schedule.freq" $in Schedule.Freq.all.filter(_.isWeeklyOrBetter)
        )
      )
      .sort($sort asc "startsAt")
      .cursor[Tournament](ReadPreference.secondaryPreferred)
      .list()

  private[tournament] def sortedCursor(
      owner: lila.user.User,
      batchSize: Int,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Tournament] =
    coll
      .find($doc("createdBy" -> owner.id))
      .sort($sort desc "startsAt")
      .batchSize(batchSize)
      .cursor[Tournament](readPreference)
}
