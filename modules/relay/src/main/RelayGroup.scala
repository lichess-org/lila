package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.core.config.BaseUrl

case class RelayGroup(@Key("_id") id: RelayGroupId, name: RelayGroup.Name, tours: List[RelayTourId])

object RelayGroup:

  def makeId = RelayGroupId(scalalib.ThreadLocalRandom.nextString(8))

  opaque type Name = String
  object Name extends OpaqueString[Name]:
    extension (name: Name)
      def shortTourName(tour: RelayTour.Name): RelayTour.Name =
        if tour.value.startsWith(name.value)
        then RelayTour.Name(tour.value.drop(name.value.size + 1).dropWhile(!_.isLetterOrDigit))
        else tour
      def toSlug =
        val s = scalalib.StringOps.slug(name.value)
        if s.isEmpty then "-" else s

  case class WithTours(group: RelayGroup, tours: List[RelayTour.TourPreview]):
    def withShorterTourNames = copy(
      tours = tours.map: tour =>
        tour.copy(name = group.name.shortTourName(tour.name))
    )

private case class RelayGroupData(name: RelayGroup.Name, tours: List[RelayTour.TourPreview]):
  def tourIds = tours.map(_.id)
  def update(group: RelayGroup): RelayGroup = group.copy(name = name, tours = tourIds)
  def make: RelayGroup = RelayGroup(RelayGroup.makeId, name, tourIds)

private final class RelayGroupForm(baseUrl: BaseUrl):
  import play.api.data.*
  import play.api.data.Forms.*
  import play.api.data.format.Formatter
  import lila.common.Form.formatter
  def data(group: RelayGroup.WithTours) = RelayGroupData(group.group.name, group.tours)
  def asText(data: RelayGroupData): String =
    s"${data.name}\n${data.tours.map(t => s"$baseUrl${routes.RelayTour.show(t.name.toSlug, t.id)}").mkString("\n")}"
  def parse(value: String): Option[RelayGroupData] =
    value.split("\n").toList match
      case Nil => none
      case name :: tourIds =>
        val tours = tourIds
          .take(50)
          .map(_.trim.takeWhile(' ' != _))
          .flatMap(parseId)
          .map(RelayTour.TourPreview(_, RelayTour.Name(""), live = none))
        RelayGroupData(RelayGroup.Name(name.linesIterator.next.trim), tours).some
  private def parseId(str: String): Option[RelayTourId] =
    def looksLikeId(id: String): Boolean = id.size == 8 && id.forall(_.isLetterOrDigit)
    if looksLikeId(str) then RelayTourId(str).some
    else
      for
        url <- lila.common.url.parse(str).toOption
        id <- url.path.split('/').filter(_.nonEmpty) match
          case Array("broadcast", id, "edit") if looksLikeId(id) => id.some
          case Array("broadcast", _, id) if looksLikeId(id) => id.some
          case _ => none
      yield RelayTourId(id)

  given Formatter[RelayGroupData] = formatter.stringOptionFormatter(asText, parse)
  val mapping: Mapping[Option[RelayGroupData]] = optional(of[RelayGroupData])

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

final private class RelayGroupRepo(coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: RelayGroupId): Fu[Option[RelayGroup]] = coll.byId[RelayGroup](id)

  def byTour(tourId: RelayTourId): Fu[Option[RelayGroup]] =
    coll.find($doc("tours" -> tourId)).one[RelayGroup]

  def byTours(tourIds: Seq[RelayTourId]): Fu[List[RelayGroup]] =
    coll.find($doc("tours".$in(tourIds))).cursor[RelayGroup]().listAll()

  def allTourIdsOfGroup(tourId: RelayTourId): Fu[List[RelayTourId]] =
    byTour(tourId).map(_.fold(List(tourId))(_.tours))

  def update(tourId: RelayTourId, data: RelayGroupData): Funit =
    for
      prev <- byTour(tourId)
      curId <- prev match
        case Some(prev) if data.tours.isEmpty => coll.delete.one($id(prev.id)).inject(none)
        case Some(prev) => coll.update.one($id(prev.id), data.update(prev)).inject(prev.id.some)
        case None =>
          val newGroup = data.make
          coll.insert.one(newGroup).inject(newGroup.id.some)
      // make sure the tours of this group are not in other groups
      _ <- curId.so: id =>
        data.tours.map(_.id).sequentiallyVoid { tourId =>
          coll.update.one($doc("_id".$ne(id), "tours" -> tourId), $pull("tours" -> tourId), multi = true)
        }
    yield ()

final class RelayGroupCrowdSumCache(
    colls: RelayColls,
    groupRepo: RelayGroupRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  export cache.get

  private val cache = cacheApi[RelayTourId, Crowd](64, "relay.groupCrowd"):
    _.expireAfterWrite(14.seconds).buildAsyncFuture(compute)

  private def compute(tourId: RelayTourId): Fu[Crowd] = Crowd.from:
    for
      tourIds <- groupRepo.allTourIdsOfGroup(tourId)
      res <- colls.round.aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("tourId".$in(tourIds), "crowdAt".$gt(nowInstant.minus(1.hours)))) ->
          List(Group(BSONNull)("sum" -> SumField("crowd")))
    yield res.headOption.flatMap(_.int("sum")).orZero
