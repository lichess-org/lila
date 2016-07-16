package lila.tournament

import chess.variant.Variant
import org.joda.time.DateTime

import BSONHandlers._
import lila.common.paginator.Paginator
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }

object TournamentRepo {

  private lazy val coll = Env.current.tournamentColl

  private def $id(id: String) = $doc("_id" -> id)

  private val enterableSelect = $doc(
    "status" $in (Status.Created.id, Status.Started.id))

  private val createdSelect = $doc("status" -> Status.Created.id)
  private val startedSelect = $doc("status" -> Status.Started.id)
  private[tournament] val finishedSelect = $doc("status" -> Status.Finished.id)
  private val startedOrFinishedSelect = $doc("status" -> $doc("$gte" -> Status.Started.id))
  private val unfinishedSelect = $doc("status" -> $doc("$ne" -> Status.Finished.id))
  private[tournament] val scheduledSelect = $doc("schedule" -> $doc("$exists" -> true))
  private def sinceSelect(date: DateTime) = $doc("startsAt" -> $doc("$gt" -> date))
  private def variantSelect(variant: Variant) =
    if (variant.standard) $doc("variant" -> $doc("$exists" -> false))
    else $doc("variant" -> variant.id)
  private val nonEmptySelect = $doc("nbPlayers" -> $doc("$ne" -> 0))
  private val selectUnique = $doc("schedule.freq" -> "unique")

  def byId(id: String): Fu[Option[Tournament]] = coll.find($id(id)).uno[Tournament]

  def byIds(ids: Iterable[String]): Fu[List[Tournament]] =
    coll.find($inIds(ids))
      .cursor[Tournament]().gather[List]()

  def uniqueById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ selectUnique).uno[Tournament]

  def recentAndNext: Fu[List[Tournament]] =
    coll.find(sinceSelect(DateTime.now minusDays 1))
      .cursor[Tournament]().gather[List]()

  def byIdAndPlayerId(id: String, userId: String): Fu[Option[Tournament]] =
    coll.find(
      $id(id) ++ $doc("players.id" -> userId)
    ).uno[Tournament]

  def createdById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ createdSelect).uno[Tournament]

  def enterableById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ enterableSelect).uno[Tournament]

  def startedById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ startedSelect).uno[Tournament]

  def finishedById(id: String): Fu[Option[Tournament]] =
    coll.find($id(id) ++ finishedSelect).uno[Tournament]

  def startedOrFinishedById(id: String): Fu[Option[Tournament]] =
    byId(id) map { _ filterNot (_.isCreated) }

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Tournament]] =
    createdById(id) map (_ filter (_.createdBy == userId))

  def allEnterable: Fu[List[Tournament]] =
    coll.find(enterableSelect).cursor[Tournament]().gather[List]()

  def nonEmptyEnterable: Fu[List[Tournament]] =
    coll.find(enterableSelect ++ nonEmptySelect).cursor[Tournament]().gather[List]()

  def createdIncludingScheduled: Fu[List[Tournament]] = coll.find(createdSelect).list[Tournament](None)

  def started: Fu[List[Tournament]] =
    coll.find(startedSelect).sort($doc("createdAt" -> -1)).list[Tournament](None)

  def publicStarted: Fu[List[Tournament]] =
    coll.find(startedSelect ++ $doc("private" -> $doc("$exists" -> false)))
      .sort($doc("createdAt" -> -1))
      .list[Tournament]()

  def finished(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect)
      .sort($doc("startsAt" -> -1))
      .list[Tournament](limit)

  def finishedNotable(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect ++ $doc(
      "$or" -> $arr(
        $doc("nbPlayers" -> $doc("$gte" -> 15)),
        scheduledSelect
      )))
      .sort($doc("startsAt" -> -1))
      .list[Tournament](limit)

  def finishedPaginator(maxPerPage: Int, page: Int) = Paginator(
    adapter = new CachedAdapter(
      new Adapter[Tournament](
        collection = coll,
        selector = finishedSelect,
        projection = $empty,
        sort = $doc("startsAt" -> -1)
      ),
      nbResults = fuccess(200 * 1000)),
    currentPage = page,
    maxPerPage = maxPerPage)

  def setStatus(tourId: String, status: Status) = coll.update(
    $id(tourId),
    $doc("$set" -> $doc("status" -> status.id))
  ).void

  def setNbPlayers(tourId: String, nb: Int) = coll.update(
    $id(tourId),
    $doc("$set" -> $doc("nbPlayers" -> nb))
  ).void

  def setWinnerId(tourId: String, userId: String) = coll.update(
    $id(tourId),
    $doc("$set" -> $doc("winner" -> userId))
  ).void

  def setFeaturedGameId(tourId: String, gameId: String) = coll.update(
    $id(tourId),
    $doc("$set" -> $doc("featured" -> gameId))
  ).void

  def featuredGameId(tourId: String) = coll.primitiveOne[String]($id(tourId), "featured")

  private def allCreatedSelect(aheadMinutes: Int) = createdSelect ++ $doc(
    "$or" -> $arr(
      $doc("schedule" -> $doc("$exists" -> false)),
      $doc("startsAt" -> $doc("$lt" -> (DateTime.now plusMinutes aheadMinutes)))
    )
  )

  def publicCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] = coll.find(
    allCreatedSelect(aheadMinutes) ++ $doc("private" -> $doc("$exists" -> false))
  ).sort($doc("startsAt" -> 1)).list[Tournament](none)

  def allCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.find(allCreatedSelect(aheadMinutes)).cursor[Tournament]().gather[List]()

  private def stillWorthEntering: Fu[List[Tournament]] =
    coll.find(startedSelect ++ $doc(
      "private" -> $doc("$exists" -> false)
    )).sort($doc("startsAt" -> 1)).list[Tournament](none) map {
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

  private[tournament] def promotable: Fu[List[Tournament]] =
    stillWorthEntering zip publicCreatedSorted(24 * 60) map {
      case (started, created) => (started ::: created).foldLeft(List.empty[Tournament]) {
        case (acc, tour) if !isPromotable(tour)          => acc
        case (acc, tour) if acc.exists(_ similarTo tour) => acc
        case (acc, tour)                                 => tour :: acc
      }.reverse
    }

  def uniques(max: Int): Fu[List[Tournament]] =
    coll.find(selectUnique)
      .sort($doc("startsAt" -> -1))
      .hint($doc("startsAt" -> -1))
      .list[Tournament]()

  def scheduledUnfinished: Fu[List[Tournament]] =
    coll.find(scheduledSelect ++ unfinishedSelect)
      .sort($doc("startsAt" -> 1)).list[Tournament]()

  def scheduledCreated: Fu[List[Tournament]] =
    coll.find(createdSelect ++ scheduledSelect)
      .sort($doc("startsAt" -> 1)).list[Tournament]()

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
    finishedSelect ++ sinceSelect(since) ++ variantSelect(chess.variant.Standard) ++ $doc(
      "schedule.freq" -> freq.name,
      "schedule.speed".$in(Schedule.Speed.mostPopular.map(_.name): _*)
    )
  ).sort($doc("startsAt" -> -1))
    .list[Tournament](Schedule.Speed.mostPopular.size.some)

  def lastFinishedDaily(variant: Variant): Fu[Option[Tournament]] = coll.find(
    finishedSelect ++ sinceSelect(DateTime.now minusDays 1) ++ variantSelect(variant) ++
      $doc("schedule.freq" -> Schedule.Freq.Daily.name)
  ).sort($doc("startsAt" -> -1)).uno[Tournament]

  def update(tour: Tournament) = coll.update($doc("_id" -> tour.id), tour)

  def insert(tour: Tournament) = coll.insert(tour)

  def remove(tour: Tournament) = coll.remove($doc("_id" -> tour.id))

  def exists(id: String) = coll.count($doc("_id" -> id).some) map (0 !=)

  def toursToWithdrawWhenEntering(tourId: String): Fu[List[Tournament]] =
    coll.find(enterableSelect ++ $doc(
      "_id" -> $doc("$ne" -> tourId),
      "schedule.freq" $nin (
        Schedule.Freq.Marathon.name,
        Schedule.Freq.Unique.name
      )
    ) ++ nonEmptySelect).cursor[Tournament]().gather[List]()
}
