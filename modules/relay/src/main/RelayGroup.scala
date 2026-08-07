package lila.relay

import reactivemongo.api.bson.Macros.Annotations.Key
import lila.relay.RelayGroup.ScoreGroup
import lila.relay.RelayTour.TourPreview
import lila.common.Form.{ into, cleanNonEmptyText }

case class RelayGroup(
    @Key("_id") id: RelayGroupId,
    name: RelayGroup.Name,
    tours: NonEmptyList[RelayTourId],
    scoreGroups: Option[NonEmptyList[ScoreGroup]]
):
  def scoreGroupOf(tourId: RelayTourId): Option[ScoreGroup] =
    scoreGroups.flatMap(_.find(_.contains(tourId)))
  def call = routes.RelayTour.show(name.toSlug, id.into(RelayTourId))
  def remove(others: Set[RelayTourId]): Option[RelayGroup] =
    tours.filterNot(others.contains).toNel.map(newTours => copy(tours = newTours))

object RelayGroup:

  def makeId = RelayGroupId(scalalib.ThreadLocalRandom.nextString(8))

  type ScoreGroup = NonEmptyList[RelayTourId]

  private[relay] def sgIsParallel(tours: List[RelayTour]): Boolean =
    tours.headOption
      .flatMap(_.dates.map(_.start))
      .exists: firstStart =>
        tours.tailOption.exists(_.exists(_.dates.map(_.start).exists(_.isBefore(firstStart.plusMinutes(20)))))

  opaque type Name = String
  object Name extends OpaqueString[Name]:
    extension (name: Name)
      def shortTourName(tour: RelayTour.Name): RelayTour.Name =
        if tour.value.startsWith(name.value) && tour.value != name.value
        then RelayTour.Name(tour.value.drop(name.value.size + 1).dropWhile(!_.isLetterOrDigit))
        else tour
      def toSlug =
        val s = scalalib.StringOps.slug(name.value)
        if s.isEmpty then "-" else s

  case class WithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.TourPreview]):
    def withShorterTourNames = copy(
      tours = tours.map: tour =>
        tour.copy(name = group.name.shortTourName(tour.name))
    )

private case class RelayGroupData(
    info: Option[RelayGroupData.Info],
    scoreGroups: Option[NonEmptyList[ScoreGroup]]
):
  def tourIds: List[RelayTourId] = info.so(_.tours.toList.map(_.id))
  def update(group: RelayGroup): Option[RelayGroup] =
    info.map: i =>
      group.copy(name = i.name, tours = i.tours.map(_.id), scoreGroups = scoreGroups)
  def make: Option[RelayGroup] = info.map: i =>
    RelayGroup(RelayGroup.makeId, i.name, i.tours.map(_.id), scoreGroups)

object RelayGroupData:
  def empty = RelayGroupData(none, none)
  case class Info(name: RelayGroup.Name, tours: NonEmptyList[RelayTour.TourPreview])

private final class RelayGroupForm:
  import play.api.data.*
  import play.api.data.Forms.*
  import lila.common.Form.formatter

  def data(group: Option[RelayGroup.WithTours]) =
    group.fold(RelayGroupData.empty): group =>
      RelayGroupData(
        RelayGroupData.Info(group.group.name, group.tours).some,
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

  private def allIdsFromGroup(tourIds: List[RelayTourId], scoreGroups: NonEmptyList[ScoreGroup]): Boolean =
    val groupTourIds = tourIds.toSet
    scoreGroups.toList.flatMap(_.toList).forall(groupTourIds.contains)

  private def noOverlappingScoreGroups(scoreGroups: NonEmptyList[ScoreGroup]): Boolean =
    val ids = scoreGroups.toList.flatMap(_.toList)
    ids.distinct.size == ids.size

  private def toursParse(value: String): Option[NonEmptyList[TourPreview]] =
    value
      .split("\n")
      .toList
      .take(50)
      .map(_.trim.takeWhile(' ' != _))
      .flatMap(parseId)
      .toNel
      .map(_.map(RelayTour.TourPreview(_, RelayTour.Name(""), active = false, live = none)))

  private def toursAsText(tours: NonEmptyList[TourPreview]): String =
    tours.toList.map(t => s"${t.name},${t.id}").mkString("\n")

  val infoMapping: Mapping[RelayGroupData.Info] =
    Forms.mapping(
      "name" -> cleanNonEmptyText.into[RelayGroup.Name],
      "tours" -> of(using formatter.stringOptionFormatter(toursAsText, toursParse))
    )(RelayGroupData.Info.apply)(unapply)

  val scoreGroupsMapping: Mapping[Option[NonEmptyList[ScoreGroup]]] =
    def scoreGroupAsText(scoreGroup: ScoreGroup): String =
      scoreGroup.toList.map(_.value).mkString(",")
    def scoreGroupParse(value: String): Option[ScoreGroup] =
      value.split(",").toList.map(_.trim).flatMap(parseId).toNel
    val scoreGroupMapping: FieldMapping[NonEmptyList[RelayTourId]] =
      of(using formatter.stringOptionFormatter(scoreGroupAsText, scoreGroupParse))
    list(optional(scoreGroupMapping))
      .transform(_.flatten, _.map(some))
      .transform(_.toNel, _.so(_.toList))
      .verifying("Too many score groups (max 10)", _.forall(_.size <= 10))
      .verifying("Score groups cannot have overlapping broadcasts", _.forall(noOverlappingScoreGroups))

  val mapping = Forms
    .mapping(
      "info" -> optional(infoMapping),
      "scoreGroups" -> scoreGroupsMapping
    )(RelayGroupData.apply)(unapply)
    .verifying(
      "Score groups cannot contain broadcasts not present in this group",
      data => data.scoreGroups.forall(allIdsFromGroup(data.tourIds, _))
    )

import lila.db.dsl.{ *, given }
import reactivemongo.api.bson.*

final private class RelayGroupRepo(coll: Coll)(using Executor):

  import BSONHandlers.given

  def byId(id: RelayGroupId): Fu[Option[RelayGroup]] =
    coll.byId[RelayGroup](id).recoverDefault

  def byTour(tourId: RelayTourId): Fu[Option[RelayGroup]] =
    coll.find($doc("tours" -> tourId)).one[RelayGroup]

  def idByTour(tourId: RelayTourId): Fu[Option[RelayGroupId]] =
    coll.primitiveOne[RelayGroupId]($doc("tours" -> tourId), "_id")

  def byTours(tourIds: Seq[RelayTourId]): Fu[List[RelayGroup]] =
    coll.find($doc("tours".$in(tourIds))).cursor[RelayGroup](ReadPref.sec).listAll()

  def allTourIdsOfGroup(tourId: RelayTourId): Fu[NonEmptyList[RelayTourId]] =
    byTour(tourId).map(_.fold(NonEmptyList.one(tourId))(_.tours))

  def update(tourId: RelayTourId, data: RelayGroupData): Funit =
    for
      prev <- byTour(tourId)
      current <- prev match
        case Some(prev) =>
          data.update(prev) match
            case None => coll.delete.one($id(prev.id)).inject(none)
            case Some(next) => coll.update.one($id(prev.id), next).inject(prev.some)
        case None =>
          data.make.so: group =>
            coll.insert.one(group).inject(group.some)
      // make sure the tours of this group are not in other groups
      _ <- current.so: cur =>
        for
          tourIdSet = current.so(_.tours.toList.toSet)
          otherGroups <- coll.list[RelayGroup]("tours".$in(tourIdSet) ++ "_id".$ne(cur.id))
          _ <- otherGroups.traverseVoid: otherGroup =>
            otherGroup.remove(tourIdSet) match
              case None => coll.delete.one($id(otherGroup.id))
              case Some(next) => coll.update.one($id(otherGroup.id), next)
        yield ()
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
        Match($doc("tourId".$in(tourIds.toList), "crowdAt".$gt(nowInstant.minus(1.hours)))) ->
          List(Group(BSONNull)("sum" -> SumField("crowd")))
    yield res.headOption.flatMap(_.int("sum")).orZero

final class RelayGroupApi(groupRepo: RelayGroupRepo, cacheApi: lila.memo.CacheApi)(using Executor):
  private val scoreGroupCache = cacheApi[RelayTourId, ScoreGroup](128, "relay.players.scoreGroup"):
    _.expireAfterWrite(1.minute).buildAsyncFuture: tourId =>
      for group <- groupRepo.byTour(tourId)
      yield group.flatMap(_.scoreGroupOf(tourId)) | NonEmptyList.of(tourId)
  def scoreGroupOf(tourId: RelayTourId): Fu[ScoreGroup] = scoreGroupCache.get(tourId)
