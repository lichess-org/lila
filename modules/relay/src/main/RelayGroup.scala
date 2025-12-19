package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.core.config.BaseUrl
import lila.relay.RelayGroup.ScoreGroup

case class RelayGroup(
    @Key("_id") id: RelayGroupId,
    name: RelayGroup.Name,
    tours: List[RelayTourId],
    scoreGroups: Option[List[ScoreGroup]]
)

object RelayGroup:

  def makeId = RelayGroupId(scalalib.ThreadLocalRandom.nextString(8))

  case class ScoreGroup(tourIds: Set[RelayTourId])

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

private case class RelayGroupData(
    info: RelayGroupData.Info,
    scoreGroups: Option[List[ScoreGroup]]
):
  def tourIds = info.tours.map(_.id)
  def update(group: RelayGroup): RelayGroup =
    group.copy(name = info.name, tours = tourIds, scoreGroups = scoreGroups)
  def make: RelayGroup = RelayGroup(RelayGroup.makeId, info.name, tourIds, scoreGroups)

object RelayGroupData:
  case class Info(
      name: RelayGroup.Name,
      tours: List[RelayTour.TourPreview]
  )

private final class RelayGroupForm(baseUrl: BaseUrl):
  import play.api.data.*
  import play.api.data.Forms.*

  def data(group: RelayGroup.WithTours) =
    RelayGroupData(
      RelayGroupData.Info(group.group.name, group.tours),
      group.group.scoreGroups
    )

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

  private val scoreGroupsMapping = nonEmptyText.transform[List[ScoreGroup]](
    str =>
      str
        .split("\n")
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .map: line =>
          val tourIds = line
            .split(",")
            .map(_.trim)
            .filter(_.nonEmpty)
            .flatMap(parseId)
            .toSet
          ScoreGroup(tourIds)
    ,
    scoreGroups => scoreGroups.map(_.tourIds.mkString(",")).mkString("\n")
  )

  private def isValidScoreGroup(tourIds: List[RelayTourId], scoreGroups: List[ScoreGroup]): Boolean =
    val groupTourIds = tourIds.toSet
    scoreGroups.flatMap(_.tourIds).forall(id => groupTourIds.contains(id))

  private val infoMapping = nonEmptyText.transform[RelayGroupData.Info](
    str =>
      val lines = str.split("\n").toList.map(_.trim).filter(_.nonEmpty)
      lines match
        case Nil => RelayGroupData.Info(RelayGroup.Name(""), Nil)
        case name :: tourLines =>
          val tourIds = tourLines
            .flatMap(parseId)
          val tours = tourIds.map(RelayTour.TourPreview(_, RelayTour.Name(""), active = false, live = none))
          RelayGroupData.Info(RelayGroup.Name(name), tours)
    ,
    info =>
      val name = info.name.value
      val tourUrls = info.tours.map(t => s"$baseUrl${routes.RelayTour.show("-", t.id)}")
      (name :: tourUrls).mkString("\n")
  )

  val mapping = Forms
    .mapping(
      "info" -> infoMapping,
      "scoreGroups" -> optional(scoreGroupsMapping)
    )(RelayGroupData.apply)(data => Some(data.info, data.scoreGroups))
    .verifying(
      "scoregroups cannot contain broadcasts not present in this group",
      data => data.scoreGroups.forall(isValidScoreGroup(data.tourIds, _))
    )

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
        case Some(prev) if data.info.tours.isEmpty => coll.delete.one($id(prev.id)).inject(none)
        case Some(prev) => coll.update.one($id(prev.id), data.update(prev)).inject(prev.id.some)
        case None =>
          val newGroup = data.make
          coll.insert.one(newGroup).inject(newGroup.id.some)
      // make sure the tours of this group are not in other groups
      _ <- curId.so: id =>
        data.tourIds.sequentiallyVoid { tourId =>
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
