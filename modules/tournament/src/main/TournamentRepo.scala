package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

import BSONHandlers._
import lila.common.paginator.Paginator
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }

object TournamentRepo {

  private lazy val coll = Env.current.tournamentColl

  private val enterableSelect = $doc("status" $lt Status.Finished.id)
  private val createdSelect = $doc("status" -> Status.Created.id)
  private val startedSelect = $doc("status" -> Status.Started.id)
  private[tournament] val finishedSelect = $doc("status" -> Status.Finished.id)
  private val unfinishedSelect = $doc("status" -> $doc("$ne" -> Status.Finished.id))
  private[tournament] val scheduledSelect = $doc("schedule" -> $doc("$exists" -> true))
  private def sinceSelect(date: DateTime) = $doc("startsAt" -> $doc("$gt" -> date))
  private def variantSelect(variant: Variant) =
    if (variant.standard) $doc("variant" -> $doc("$exists" -> false))
    else $doc("variant" -> variant.id)
  private val nonEmptySelect = $doc("nbPlayers" -> $doc("$ne" -> 0))
  private val selectUnique = $doc("schedule.freq" -> "unique")

  def byId(id: String): Fu[Option[Tournament]] =
    coll.find($id(id)).one[Tournament]

  def byIds(ids: Iterable[String]): Fu[List[Tournament]] =
    coll.find($inIds(ids)).cursor[Tournament]().list

  def uniqueById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ selectUnique).one[Tournament]

  def byIdAndPlayerId(id: String, userId: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ $doc("players.id" -> userId)).one[Tournament]

  def createdById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ createdSelect).one[Tournament]

  def enterableById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ enterableSelect).one[Tournament]

  def startedById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ startedSelect).one[Tournament]

  def finishedById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ finishedSelect).one[Tournament]

  def startedOrFinishedById(id: String): Fu[Option[Tournament]] =
    byId(id) map { _ filterNot (_.isCreated) }

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Tournament]] =
    createdById(id) map (_ filter (_.createdBy == userId))

  def nonEmptyEnterableIds: Fu[List[Tournament.ID]] =
    coll.primitive[Tournament.ID](enterableSelect ++ nonEmptySelect, "_id")

  def createdIncludingScheduled: Fu[List[Tournament]] =
    coll.find(createdSelect).cursor[Tournament]().list

  def startedTours: Fu[List[Tournament]] =
    coll.find(startedSelect).sort($doc("createdAt" -> -1))
      .cursor[Tournament]().list

  def startedIds: Fu[List[Tournament.ID]] =
    coll.primitive[Tournament.ID](startedSelect, sort = $doc("createdAt" -> -1), "_id")

  def publicStarted: Fu[List[Tournament]] =
    coll.find(startedSelect ++ $doc("private" $exists false))
      .sort($doc("createdAt" -> -1)).cursor[Tournament]().list

  def standardPublicStartedFromSecondary: Fu[List[Tournament]] =
    coll.find(startedSelect ++ $doc(
      "private" $exists false,
      "variant" $exists false
    )).cursor[Tournament](ReadPreference.secondaryPreferred).list

  def finished(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect).sort($doc("startsAt" -> -1))
      .cursor[Tournament]().list(limit)

  def finishedNotable(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect ++ $doc(
      "$or" -> $arr(
        $doc("nbPlayers" $gte 30),
        scheduledSelect
      )
    )).sort($doc("startsAt" -> -1)).cursor[Tournament]().list(limit)

  def finishedPaginator(maxPerPage: lila.common.MaxPerPage, page: Int) = Paginator(
    adapter = new CachedAdapter(
      new Adapter[Tournament](
        collection = coll,
        selector = finishedSelect,
        projection = $empty,
        sort = $doc("startsAt" -> -1)
      ),
      nbResults = fuccess(200 * 1000)
    ),
    currentPage = page,
    maxPerPage = maxPerPage
  )

  def setStatus(tourId: String, status: Status): Funit =
    coll.update.one($id(tourId), $set("status" -> status.id)).void

  def setNbPlayers(tourId: String, nb: Int): Funit =
    coll.update.one($id(tourId), $set("nbPlayers" -> nb)).void

  def setWinnerId(tourId: String, userId: String): Funit =
    coll.update.one($id(tourId), $set("winner" -> userId)).void

  def setFeaturedGameId(tourId: String, gameId: String): Funit =
    coll.update.one($id(tourId), $set("featured" -> gameId)).void

  def featuredGameId(tourId: String) = coll.primitiveOne[String]($id(tourId), "featured")

  private def allCreatedSelect(aheadMinutes: Int) = createdSelect ++ $doc(
    "$or" -> $arr(
      $doc("schedule" $exists false),
      $doc("startsAt" $lt (DateTime.now plusMinutes aheadMinutes))
    )
  )

  def publicCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.find(allCreatedSelect(aheadMinutes) ++ $doc("private" $exists false))
      .sort($doc("startsAt" -> 1)).cursor[Tournament]().list

  def allCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.find(allCreatedSelect(aheadMinutes)).cursor[Tournament]().list

  private def stillWorthEntering: Fu[List[Tournament]] =
    coll.find(startedSelect ++ $doc("private" $exists false))
      .sort($doc("startsAt" -> 1)).cursor[Tournament]().list map {
        _.filter(_.isStillWorthEntering)
      }

  private def isPromotable(tour: Tournament) = tour.startsAt isBefore DateTime.now.plusMinutes {
    import Schedule.Freq._
    tour.schedule.map(_.freq) map {
      case Unique => tour.spotlight.flatMap(_.homepageHours).fold(24 * 60)(60*)
      case Unique | Yearly | Marathon => 24 * 60
      case Monthly | Shield => 6 * 60
      case Weekly | Weekend => 3 * 60
      case Daily => 1 * 60
      case _ => 30
    } getOrElse 30
  }

  private[tournament] def promotable: Fu[List[Tournament]] =
    stillWorthEntering zip publicCreatedSorted(crud.CrudForm.maxHomepageHours * 60) map {
      case (started, created) => (started ::: created).foldLeft(List.empty[Tournament]) {
        case (acc, tour) if !isPromotable(tour) => acc
        case (acc, tour) if acc.exists(_ similarTo tour) => acc
        case (acc, tour) => tour :: acc
      }.reverse
    }

  def uniques(max: Int): Fu[List[Tournament]] =
    coll.find(selectUnique).sort($doc("startsAt" -> -1))
      .hint($doc("startsAt" -> -1)).cursor[Tournament]().list(max)

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll.find(scheduledSelect ++ unfinishedSelect)
      .sort($doc("startsAt" -> 1)).cursor[Tournament]().list

  def scheduledCreated: Fu[List[Tournament]] =
    coll.find(createdSelect ++ scheduledSelect)
      .sort($doc("startsAt" -> 1)).cursor[Tournament]().list

  def scheduledDedup: Fu[List[Tournament]] = scheduledCreated map {
    import Schedule.Freq
    _.flatMap { tour =>
      tour.schedule map (tour -> _)
    }.foldLeft(List[Tournament]() -> none[Freq]) {
      case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
      case ((tours, skip), (tour, sched)) => (tour :: tours, sched.freq match {
        case Freq.Daily => Freq.Eastern.some
        case Freq.Eastern => Freq.Daily.some
        case _ => skip
      })
    }._1.reverse
  }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: DateTime): Fu[List[Tournament]] = coll.find(
    finishedSelect ++ sinceSelect(since) ++ variantSelect(
      chess.variant.Standard
    ) ++ $doc(
        "schedule.freq" -> freq.name,
        "schedule.speed" $in Schedule.Speed.mostPopular.map(_.name)
      )
  ).sort($doc("startsAt" -> -1)).cursor[Tournament]()
    .list(Schedule.Speed.mostPopular.size)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] = coll.find(
    finishedSelect ++ sinceSelect(DateTime.now minusDays 1) ++ variantSelect(variant) ++
      $doc("schedule.freq" -> Schedule.Freq.Daily.name)
  ).sort($doc("startsAt" -> -1)).one[Tournament]

  def update(tour: Tournament) = coll.update.one($id(tour.id), tour)

  def insert(tour: Tournament) = coll.insert.one(tour)

  def remove(tour: Tournament) = coll.delete.one($id(tour.id))

  def exists(id: String) = coll exists $id(id)

  def tourIdsToWithdrawWhenEntering(tourId: Tournament.ID): Fu[List[Tournament.ID]] = coll.primitive[Tournament.ID](
    enterableSelect ++
      nonEmptySelect ++
      $doc(
        "_id" $ne tourId,
        "startsAt" $lt DateTime.now
      ),
    "_id"
  )

  def calendar(from: DateTime, to: DateTime): Fu[List[Tournament]] =
    coll.find($doc(
      "startsAt" $gte from $lte to,
      "schedule.freq" $in Schedule.Freq.all.filter(_.isWeeklyOrBetter)
    )).sort($sort asc "startsAt")
      .cursor[Tournament](ReadPreference.secondaryPreferred).list
}
