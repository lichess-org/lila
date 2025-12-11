package lila.relay

import lila.db.dsl.{ *, given }

final class RelayDefaults(
    groupRepo: RelayGroupRepo,
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  val roundToLink = cacheApi[RelayTourId, Option[RelayRound]](32, "relay.defaultRoundToLink"):
    _.expireAfterWrite(5.seconds).buildAsyncFuture: tourId =>
      tourWithRounds(tourId).mapz(RelayDefaults.defaultRoundToLink)

  val tourOfGroup = cacheApi[RelayGroupId, Option[RelayTour]](8, "relay.defaultTourOfGroup"):
    _.expireAfterWrite(10.seconds).buildAsyncFuture: groupId =>
      groupRepo
        .byId(groupId)
        .flatMapz: group =>
          tourRepo.byIds(group.tours).map(RelayDefaults.defaultTourOfGroup)

  private def tourWithRounds(id: RelayTourId): Fu[Option[RelayTour.WithRounds]] =
    tourRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          Project(RelayTourRepo.unsetHeavyOptionalFields),
          PipelineOperator(roundRepo.tourRoundPipeline)
        )
      .map(_.flatMap(RelayTourRepo.readTourWithRounds))

private object RelayDefaults:

  def defaultRoundToLink(trs: RelayTour.WithRounds): Option[RelayRound] =
    if !trs.tour.active then trs.rounds.headOption
    else
      trs.rounds
        .flatMap: round =>
          round.startedAt.map(_ -> round)
        .sortBy(-_._1.getEpochSecond)
        .headOption
        .match
          case None => trs.rounds.headOption
          case Some(_, last) =>
            trs.rounds
              .find(!_.isFinished)
              .fold(last): next =>
                if next.startsAtTime.exists(_.isBefore(nowInstant.plusHours(1)))
                then next
                else last
              .some

  def defaultTourOfGroup(tours: List[RelayTour]): Option[RelayTour] =
    val active = tours.filter(_.active)
    val filtered = if active.nonEmpty then active else tours
    // sorted preserves the original ordering while adding its own
    filtered.sorted(using Ordering.by(RelayTour.tierPriority)).headOption
