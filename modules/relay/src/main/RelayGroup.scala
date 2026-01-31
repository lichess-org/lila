package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.core.config.RouteUrl
import lila.relay.RelayGroup.ScoreGroup

case class RelayGroup(
    @Key("_id") id: RelayGroupId,
    name: RelayGroup.Name,
    tours: List[RelayTourId],
    scoreGroups: Option[List[ScoreGroup]]
):
  def scoreGroupOf(tourId: RelayTourId): Option[ScoreGroup] =
    scoreGroups.flatMap(_.find(_.toList.contains(tourId)))

object RelayGroup:

  def makeId = RelayGroupId(scalalib.ThreadLocalRandom.nextString(8))

  type ScoreGroup = NonEmptyList[RelayTourId]

  opaque type Name = String
  object Name extends OpaqueString[Name]:
    extension (name: Name)
      def shortTourName(tour: RelayTour.Name): RelayTour.Name =
        if tour.value.startsWith(name.value)
        then RelayTour.Name(tour.value.drop(name.value.size + 1).dropWhile(!_.isLetterOrDigit))
        else tour
      def transName(tourName: RelayTour.Name)(using lila.core.i18n.Translate) =
        RelayI18n(name.shortTourName(tourName).value)
      def toSlug =
        val s = scalalib.StringOps.slug(name.value)
        if s.isEmpty then "-" else s

  case class WithTours(group: RelayGroup, tours: List[RelayTour.TourPreview]):
    def withShorterTourNames(using lila.core.i18n.Translate) = copy(
      tours = tours.map: tour =>
        tour.copy(name = RelayTour.Name:
          group.name.transName(tour.name))
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

private final class RelayGroupForm(routeUrl: RouteUrl):
  import play.api.data.*
  import play.api.data.Forms.*
  import play.api.data.format.Formatter
  import lila.common.Form.formatter

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
    _.split("\n").toList
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap:
        _.split(",").take(50).map(_.trim).filter(_.nonEmpty).flatMap(parseId).distinct.toList.toNel
    ,
    _.map(_.toList.mkString(",")).mkString("\n")
  )

  private def allIdsFromGroup(tourIds: List[RelayTourId], scoreGroups: List[ScoreGroup]): Boolean =
    val groupTourIds = tourIds.toSet
    scoreGroups.flatMap(_.toList).forall(id => groupTourIds.contains(id))

  private def noOverlappingScoreGroups(scoreGroups: List[ScoreGroup]): Boolean =
    val ids = scoreGroups.flatMap(_.toList)
    ids.distinct.size == ids.size

  private def infoParse(value: String): Option[RelayGroupData.Info] =
    value.split("\n").toList match
      case Nil => none
      case name :: tourIds =>
        val tours = tourIds
          .take(50)
          .map(_.trim.takeWhile(' ' != _))
          .flatMap(parseId)
          .map(RelayTour.TourPreview(_, RelayTour.Name(""), active = false, live = none))
        RelayGroupData.Info(RelayGroup.Name(name.linesIterator.next.trim), tours).some

  private def infoAsText(info: RelayGroupData.Info): String =
    val name = info.name.value
    val tourUrls = info.tours.map(t => s"${routeUrl(routes.RelayTour.show(t.name.toSlug, t.id))}")
    (name :: tourUrls).mkString("\n")

  given Formatter[RelayGroupData.Info] = formatter.stringOptionFormatter(infoAsText, infoParse)
  val infoMapping: Mapping[Option[RelayGroupData.Info]] = optional(of[RelayGroupData.Info])

  val mapping = Forms
    .mapping(
      "info" -> infoMapping,
      "scoreGroups" -> optional(scoreGroupsMapping)
    )((info, scoreGroups) =>
      RelayGroupData(info.getOrElse(RelayGroupData.Info(RelayGroup.Name(""), List())), scoreGroups)
    )(data => Some(data.info.some, data.scoreGroups))
    .verifying(
      "score groups cannot contain broadcasts not present in this group",
      data => data.scoreGroups.forall(allIdsFromGroup(data.tourIds, _))
    )
    .verifying(
      "score groups cannot have overlapping broadcasts",
      data => data.scoreGroups.forall(noOverlappingScoreGroups)
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

final class RelayGroupApi(groupRepo: RelayGroupRepo, cacheApi: lila.memo.CacheApi)(using Executor):
  private val scoreGroupCache = cacheApi[RelayTourId, ScoreGroup](128, "relay.players.scoreGroup"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: tourId =>
      for group <- groupRepo.byTour(tourId)
      yield group.flatMap(_.scoreGroupOf(tourId)) | NonEmptyList.of(tourId)
  def scoreGroupOf(tourId: RelayTourId): Fu[ScoreGroup] = scoreGroupCache.get(tourId)
