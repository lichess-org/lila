package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONArray, BSONInteger }

import BSONHandlers._
import lila.common.paginator.Paginator
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._
import lila.db.paginator.BSONAdapter

object TournamentRepo {

  private lazy val coll = Env.current.tournamentColl

  private def selectId(id: String) = BSONDocument("_id" -> id)

  private val enterableSelect = BSONDocument(
    "status" -> BSONDocument("$in" -> List(Status.Created.id, Status.Started.id)))

  private val createdSelect = BSONDocument("status" -> Status.Created.id)
  private val startedSelect = BSONDocument("status" -> Status.Started.id)
  private[tournament] val finishedSelect = BSONDocument("status" -> Status.Finished.id)
  private val startedOrFinishedSelect = BSONDocument("status" -> BSONDocument("$gte" -> Status.Started.id))
  private val unfinishedSelect = BSONDocument("status" -> BSONDocument("$ne" -> Status.Finished.id))
  private[tournament] val scheduledSelect = BSONDocument("schedule" -> BSONDocument("$exists" -> true))
  private def sinceSelect(date: DateTime) = BSONDocument("startsAt" -> BSONDocument("$gt" -> date))
  private def variantSelect(variant: Variant) =
    if (variant.standard) BSONDocument("variant" -> BSONDocument("$exists" -> false))
    else BSONDocument("variant" -> variant.id)
  private val nonEmptySelect = BSONDocument("nbPlayers" -> BSONDocument("$ne" -> 0))

  def byId(id: String): Fu[Option[Tournament]] = coll.find(selectId(id)).one[Tournament]

  def byIds(ids: Iterable[String]): Fu[List[Tournament]] =
    coll.find(BSONDocument("_id" -> BSONDocument("$in" -> ids)))
      .cursor[Tournament]().collect[List]()

  def recentAndNext: Fu[List[Tournament]] =
    coll.find(sinceSelect(DateTime.now minusDays 1))
      .cursor[Tournament]().collect[List]()

  def byIdAndPlayerId(id: String, userId: String): Fu[Option[Tournament]] =
    coll.find(
      selectId(id) ++ BSONDocument("players.id" -> userId)
    ).one[Tournament]

  def createdById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ createdSelect).one[Tournament]

  def enterableById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ enterableSelect).one[Tournament]

  def startedById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ startedSelect).one[Tournament]

  def finishedById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ finishedSelect).one[Tournament]

  def startedOrFinishedById(id: String): Fu[Option[Tournament]] =
    byId(id) map { _ filterNot (_.isCreated) }

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Tournament]] =
    createdById(id) map (_ filter (_.createdBy == userId))

  def allEnterable: Fu[List[Tournament]] =
    coll.find(enterableSelect).cursor[Tournament]().collect[List]()

  def nonEmptyEnterable: Fu[List[Tournament]] =
    coll.find(enterableSelect ++ nonEmptySelect).cursor[Tournament]().collect[List]()

  def createdIncludingScheduled: Fu[List[Tournament]] = coll.find(createdSelect).toList[Tournament](None)

  def started: Fu[List[Tournament]] =
    coll.find(startedSelect).sort(BSONDocument("createdAt" -> -1)).toList[Tournament](None)

  def publicStarted: Fu[List[Tournament]] =
    coll.find(startedSelect ++ BSONDocument("private" -> BSONDocument("$exists" -> false)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Tournament]().collect[List]()

  def finished(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect)
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament]().collect[List](limit)

  def finishedNotable(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect ++ BSONDocument(
      "$or" -> BSONArray(
        BSONDocument("nbPlayers" -> BSONDocument("$gte" -> 15)),
        scheduledSelect
      )))
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament]().collect[List](limit)

  def finishedPaginator(maxPerPage: Int, page: Int) = Paginator(
    adapter = new BSONAdapter[Tournament](
      collection = coll,
      selector = finishedSelect,
      projection = BSONDocument(),
      sort = BSONDocument("startsAt" -> -1)
    ),
    currentPage = page,
    maxPerPage = maxPerPage)

  def setStatus(tourId: String, status: Status) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("status" -> status.id))
  ).void

  def setNbPlayers(tourId: String, nb: Int) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("nbPlayers" -> nb))
  ).void

  def setWinnerId(tourId: String, userId: String) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("winner" -> userId))
  ).void

  def setFeaturedGameId(tourId: String, gameId: String) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("featured" -> gameId))
  ).void

  def featuredGameId(tourId: String) = coll.find(
    selectId(tourId),
    BSONDocument("featured" -> true)
  ).one[BSONDocument].map(_.flatMap(_.getAs[String]("featured")))

  private def allCreatedSelect(aheadMinutes: Int) = createdSelect ++ BSONDocument(
    "$or" -> BSONArray(
      BSONDocument("schedule" -> BSONDocument("$exists" -> false)),
      BSONDocument("startsAt" -> BSONDocument("$lt" -> (DateTime.now plusMinutes aheadMinutes)))
    )
  )

  def publicCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] = coll.find(
    allCreatedSelect(aheadMinutes) ++ BSONDocument("private" -> BSONDocument("$exists" -> false))
  ).sort(BSONDocument("startsAt" -> 1)).cursor[Tournament]().collect[List]()

  def allCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.find(allCreatedSelect(aheadMinutes)).cursor[Tournament]().collect[List]()

  private def stillWorthEntering: Fu[List[Tournament]] =
    coll.find(startedSelect ++ BSONDocument(
      "private" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("startsAt" -> 1)).toList[Tournament](none) map {
      _.filter(_.isStillWorthEntering)
    }

  private def isPromotable(tour: Tournament) = tour.startsAt isBefore DateTime.now.plusMinutes {
    tour.schedule.map(_.freq) map {
      case Schedule.Freq.Marathon => 24 * 60
      case Schedule.Freq.Unique   => 24 * 60
      case Schedule.Freq.Monthly  => 6 * 60
      case Schedule.Freq.Weekly   => 3 * 60
      case Schedule.Freq.Daily    => 1 * 60
      case _                      => 30
    } getOrElse 30
  }

  def promotable: Fu[List[Tournament]] =
    stillWorthEntering zip publicCreatedSorted(24 * 60) map {
      case (started, created) => (started ::: created).foldLeft(List.empty[Tournament]) {
        case (acc, tour) if !isPromotable(tour)          => acc
        case (acc, tour) if acc.exists(_ similarTo tour) => acc
        case (acc, tour)                                 => tour :: acc
      }.reverse
    }

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll.find(scheduledSelect ++ unfinishedSelect)
      .sort(BSONDocument("startsAt" -> 1)).cursor[Tournament]().collect[List]()

  def scheduledCreated: Fu[List[Tournament]] =
    coll.find(createdSelect ++ scheduledSelect)
      .sort(BSONDocument("startsAt" -> 1)).cursor[Tournament]().collect[List]()

  def scheduledDedup: Fu[List[Tournament]] = scheduledCreated map {
    import Schedule.Freq
    _.flatMap { tour =>
      tour.schedule map (tour -> _)
    }.foldLeft(List[Tournament]() -> none[Freq]) {
      case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
      case ((tours, skip), (tour, sched)) => (tour :: tours, sched.freq match {
        case Freq.Daily   => Freq.Eastern.some
        case Freq.Eastern => Freq.Daily.some
        case _            => skip
      })
    }._1.reverse
  }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, since: DateTime): Fu[List[Tournament]] = coll.find(
    finishedSelect ++ sinceSelect(since) ++ variantSelect(chess.variant.Standard) ++ BSONDocument(
      "schedule.freq" -> freq.name,
      "schedule.speed" -> BSONDocument("$in" -> Schedule.Speed.mostPopular.map(_.name))
    )
  ).sort(BSONDocument("startsAt" -> -1))
    .toList[Tournament](Schedule.Speed.mostPopular.size.some)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] = coll.find(
    finishedSelect ++ sinceSelect(DateTime.now minusDays 1) ++ variantSelect(variant) ++
      BSONDocument("schedule.freq" -> Schedule.Freq.Daily.name)
  ).sort(BSONDocument("startsAt" -> -1)).one[Tournament]

  def update(tour: Tournament) = coll.update(BSONDocument("_id" -> tour.id), tour)

  def insert(tour: Tournament) = coll.insert(tour)

  def remove(tour: Tournament) = coll.remove(BSONDocument("_id" -> tour.id))

  def exists(id: String) = coll.count(BSONDocument("_id" -> id).some) map (0 !=)

  def isFinished(id: String): Fu[Boolean] =
    coll.count(BSONDocument("_id" -> id, "status" -> Status.Finished.id).some) map (0 !=)

  def toursToWithdrawWhenEntering(tourId: String): Fu[List[Tournament]] =
    coll.find(enterableSelect ++ BSONDocument(
      "_id" -> BSONDocument("$ne" -> tourId),
      "schedule.freq" -> BSONDocument("$nin" -> List(
        Schedule.Freq.Marathon.name,
        Schedule.Freq.Unique.name
      ))
    ) ++ nonEmptySelect).cursor[Tournament]().collect[List]()
}
