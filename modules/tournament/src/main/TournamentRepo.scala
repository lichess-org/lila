package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.akkastream.{ cursorProducer, AkkaStreamCursor }
import reactivemongo.api.ReadPreference

import BSONHandlers._
import lila.common.config.CollName
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.game.Game
import lila.hub.LightTeam.TeamID
import lila.user.User

final class TournamentRepo(val coll: Coll, playerCollName: CollName)(
    implicit ec: scala.concurrent.ExecutionContext
) {

  private val enterableSelect             = $doc("status" $lt Status.Finished.id)
  private val createdSelect               = $doc("status" -> Status.Created.id)
  private val startedSelect               = $doc("status" -> Status.Started.id)
  private[tournament] val finishedSelect  = $doc("status" -> Status.Finished.id)
  private val unfinishedSelect            = $doc("status" $ne Status.Finished.id)
  private[tournament] val scheduledSelect = $doc("schedule" $exists true)
  private def sinceSelect(date: DateTime) = $doc("startsAt" $gt date)
  private def variantSelect(variant: Variant) =
    if (variant.standard) $doc("variant" $exists false)
    else $doc("variant" -> variant.id)
  private val nonEmptySelect           = $doc("nbPlayers" $ne 0)
  private[tournament] val selectUnique = $doc("schedule.freq" -> "unique")

  def byId(id: Tournament.ID): Fu[Option[Tournament]] = coll.byId[Tournament](id)

  def byIds(ids: Iterable[Tournament.ID]): Fu[List[Tournament]] =
    coll.ext.find($inIds(ids)).list[Tournament]()

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

  private[tournament] def startedCursor =
    coll.ext.find(startedSelect).sort($doc("createdAt" -> -1)).batchSize(1).cursor[Tournament]()

  def startedIds: Fu[List[Tournament.ID]] =
    coll.primitive[Tournament.ID](startedSelect, sort = $doc("createdAt" -> -1), "_id")

  def publicStarted: Fu[List[Tournament]] =
    coll.ext
      .find(startedSelect ++ $doc("password" $exists false))
      .sort($doc("createdAt" -> -1))
      .list[Tournament]()

  def standardPublicStartedFromSecondary: Fu[List[Tournament]] =
    coll.ext
      .find(
        startedSelect ++ $doc(
          "password" $exists false,
          "variant" $exists false
        )
      )
      .list[Tournament](None, ReadPreference.secondaryPreferred)

  private[tournament] def notableFinished(limit: Int): Fu[List[Tournament]] =
    coll.ext
      .find(finishedSelect ++ scheduledSelect)
      .sort($sort desc "startsAt")
      .list[Tournament](limit)

  def byOwnerAdapter(owner: User) = new lila.db.paginator.Adapter[Tournament](
    collection = coll,
    selector = $doc("createdBy" -> owner.id),
    projection = none,
    sort = $sort desc "startsAt",
    readPreference = ReadPreference.secondaryPreferred
  )

  def isUnfinished(tourId: Tournament.ID): Fu[Boolean] =
    coll.exists($id(tourId) ++ unfinishedSelect)

  def clockById(id: Tournament.ID): Fu[Option[chess.Clock.Config]] =
    coll.primitiveOne[chess.Clock.Config]($id(id), "clock")

  // only tournaments that the team leaders have created or joined
  private[tournament] def idsVisibleByTeam(
      teamId: TeamID,
      leaderIds: Set[User.ID],
      nb: Int
  ): Fu[List[Tournament.ID]] =
    coll
      .aggregateList(maxDocs = nb, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match(byTeamSelect(teamId)) -> List(
          Limit(nb * 100), // stop searching at some point, when all tournaments should be invisible
          PipelineOperator(lookupPlayers(leaderIds)),
          Match(
            $or(
              $doc("createdBy" $in leaderIds),
              "player" $ne $arr()
            )
          ),
          Sort(Descending("startsAt")),
          Project($id(true)),
          Limit(nb)
        )
      }
      .map(_.flatMap(_.string("_id")))

  def visibleByTeam(
      teamId: TeamID,
      leaderIds: Set[User.ID],
      nb: Int
  ): Fu[List[Tournament]] = idsVisibleByTeam(teamId, leaderIds, nb) flatMap byOrderedIds

  private[tournament] def withdrawableIds(userId: User.ID): Fu[List[Tournament.ID]] =
    coll
      .aggregateList(Int.MaxValue, readPreference = ReadPreference.secondaryPreferred) { implicit framework =>
        import framework._
        Match(enterableSelect ++ nonEmptySelect) -> List(
          PipelineOperator(lookupPlayers(collection.immutable.Set(userId))),
          Match("player" $ne $arr()),
          Project($id(true))
        )
      }
      .map(_.flatMap(_.string("_id")))

  private def lookupPlayers(userIds: Set[User.ID]) = $doc(
    "$lookup" -> $doc(
      "from" -> playerCollName.value,
      "let"  -> $doc("t" -> "$_id"),
      "pipeline" -> $arr(
        $doc(
          "$match" -> $doc(
            "$expr" -> $doc(
              "$and" -> $arr(
                $doc("$in" -> $arr("$uid", userIds)),
                $doc("$eq" -> $arr("$tid", "$$t"))
              )
            )
          )
        ),
        $doc("$project" -> $id(true))
      ),
      "as" -> "player"
    )
  )

  // this query is carefully crafted so that it hits both indexes
  private def byTeamSelect(teamId: String) =
    $or(
      $doc(
        "teamBattle.teams" -> teamId,
        "teamBattle" $exists true // yes it's needed
      ),
      $doc(
        "conditions.teamMember.teamId" -> teamId,
        "conditions.teamMember" $exists true // yes it's needed
      )
    )

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

  def featuredGameId(tourId: Tournament.ID) = coll.primitiveOne[Game.ID]($id(tourId), "featured")

  private def startingSoonSelect(aheadMinutes: Int) =
    createdSelect ++
      $doc("startsAt" $lt (DateTime.now plusMinutes aheadMinutes))

  def publicCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.ext
      .find(
        startingSoonSelect(aheadMinutes) ++ $doc("password" $exists false)
      )
      .sort($doc("startsAt" -> 1))
      .list[Tournament](none)

  private[tournament] def shouldStartCursor =
    coll.ext
      .find($doc("startsAt" $lt DateTime.now) ++ createdSelect)
      .batchSize(1)
      .cursor[Tournament]()

  private def scheduledStillWorthEntering: Fu[List[Tournament]] =
    coll.ext
      .find(
        startedSelect ++ scheduledSelect
      )
      .sort($doc("startsAt" -> 1))
      .list[Tournament]() map {
      _.filter(_.isStillWorthEntering)
    }

  private def scheduledCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.ext
      .find(
        startingSoonSelect(aheadMinutes) ++ scheduledSelect
      )
      .sort($doc("startsAt" -> 1))
      .list[Tournament]()

  private def isPromotable(tour: Tournament): Boolean = tour.schedule ?? { schedule =>
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

  private[tournament] def promotable: Fu[List[Tournament]] =
    scheduledStillWorthEntering zip scheduledCreatedSorted(crud.CrudForm.maxHomepageHours * 60) map {
      case (started, created) =>
        (started ::: created)
          .foldLeft(List.empty[Tournament]) {
            case (acc, tour) if !isPromotable(tour)          => acc
            case (acc, tour) if acc.exists(_ similarTo tour) => acc
            case (acc, tour)                                 => tour :: acc
          }
          .reverse
    }

  def uniques(max: Int): Fu[List[Tournament]] =
    coll.ext
      .find(selectUnique)
      .sort($doc("startsAt" -> -1))
      .hint($doc("startsAt" -> -1))
      .list[Tournament](max)

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll.ext
      .find(scheduledSelect ++ unfinishedSelect)
      .sort($doc("startsAt" -> 1))
      .list[Tournament]()

  def scheduledCreated: Fu[List[Tournament]] =
    coll.ext
      .find(createdSelect ++ scheduledSelect)
      .sort($doc("startsAt" -> 1))
      .list[Tournament]()

  def scheduledDedup: Fu[List[Tournament]] = scheduledCreated map {
    import Schedule.Freq
    _.flatMap { tour =>
      tour.schedule map (tour -> _)
    }.foldLeft(List[Tournament]() -> none[Freq]) {
        case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
        case ((tours, skip), (tour, sched)) =>
          (tour :: tours, sched.freq match {
            case Freq.Daily   => Freq.Eastern.some
            case Freq.Eastern => Freq.Daily.some
            case _            => skip
          })
      }
      ._1
      .reverse
  }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: DateTime): Fu[List[Tournament]] =
    coll.ext
      .find(
        finishedSelect ++ sinceSelect(since) ++ variantSelect(chess.variant.Standard) ++ $doc(
          "schedule.freq" -> freq.name,
          "schedule.speed" $in Schedule.Speed.mostPopular.map(_.key)
        )
      )
      .sort($sort desc "startsAt")
      .list[Tournament](Schedule.Speed.mostPopular.size.some)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] =
    coll.ext
      .find(
        finishedSelect ++ sinceSelect(DateTime.now minusDays 1) ++ variantSelect(variant) ++
          $doc("schedule.freq" -> Schedule.Freq.Daily.name)
      )
      .sort($sort desc "startsAt")
      .one[Tournament]

  def update(tour: Tournament) = coll.update.one($id(tour.id), tour)

  def insert(tour: Tournament) = coll.insert.one(tour)

  def remove(tour: Tournament) = coll.delete.one($id(tour.id))

  def exists(id: Tournament.ID) = coll exists $id(id)

  def calendar(from: DateTime, to: DateTime): Fu[List[Tournament]] =
    coll.ext
      .find(
        $doc(
          "startsAt" $gte from $lte to,
          "schedule.freq" $in Schedule.Freq.all.filter(_.isWeeklyOrBetter)
        )
      )
      .sort($sort asc "startsAt")
      .list[Tournament](none, ReadPreference.secondaryPreferred)

  private[tournament] def sortedCursor(
      owner: lila.user.User,
      batchSize: Int,
      readPreference: ReadPreference = ReadPreference.secondaryPreferred
  ): AkkaStreamCursor[Tournament] =
    coll.ext
      .find($doc("createdBy" -> owner.id))
      .sort($sort desc "startsAt")
      .batchSize(batchSize)
      .cursor[Tournament](readPreference)
}
