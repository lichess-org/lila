package lila.relay

import akka.stream.scaladsl.*
import alleycats.Zero
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*
import scala.util.chaining.*

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.study.{ Settings, Study, StudyApi, StudyId, StudyMaker, StudyMultiBoard, StudyRepo, StudyTopic }
import lila.security.Granter
import lila.user.{ User, Me, MyId }
import lila.relay.RelayTour.ActiveWithSomeRounds
import lila.i18n.I18nKeys.streamer.visibility

final class RelayApi(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    studyApi: StudyApi,
    studyRepo: StudyRepo,
    multiboard: StudyMultiBoard,
    jsonView: JsonView,
    formatApi: RelayFormatApi,
    cacheApi: CacheApi,
    leaderboard: RelayLeaderboardApi
)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ readRoundWithTour, given }
  import JsonView.given

  def byId(id: RelayRoundId) = roundRepo.coll.byId[RelayRound](id)

  def byIdWithTour(id: RelayRoundId): Fu[Option[RelayRound.WithTour]] =
    roundRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      .map(_ flatMap readRoundWithTour)

  def byIdAndContributor(id: RelayRoundId)(using me: Me) =
    byIdWithStudy(id).map:
      _.collect:
        case RelayRound.WithTourAndStudy(relay, tour, study) if study.canContribute(me) =>
          relay withTour tour

  def byIdWithStudy(id: RelayRoundId): Fu[Option[RelayRound.WithTourAndStudy]] =
    byIdWithTour(id) flatMapz { case RelayRound.WithTour(relay, tour) =>
      studyApi.byId(relay.studyId) dmap2 {
        RelayRound.WithTourAndStudy(relay, tour, _)
      }
    }

  def byTourOrdered(tour: RelayTour): Fu[List[RelayRound.WithTour]] =
    roundRepo.byTourOrdered(tour).dmap(_.map(_ withTour tour))

  def roundIdsById(tourId: RelayTour.Id): Fu[List[StudyId]] =
    roundRepo.idsByTourId(tourId)

  def kickBroadcast(userId: UserId, tourId: RelayTour.Id, who: MyId): Funit =
    roundIdsById(tourId).flatMap:
      _.map(studyApi.kick(_, userId, who)).parallel.void

  def withRounds(tour: RelayTour) = roundRepo.byTourOrdered(tour).dmap(tour.withRounds)

  def denormalizeTourActive(tourId: RelayTour.Id): Funit =
    roundRepo.coll.exists(roundRepo.selectors.tour(tourId) ++ $doc("finished" -> false)) flatMap {
      tourRepo.setActive(tourId, _)
    }

  object countOwnedByUser:
    private val cache = cacheApi[UserId, Int](32_768, "relay.nb.owned"):
      _.expireAfterWrite(5.minutes).buildAsyncFuture(tourRepo.countByOwner)
    export cache.get

  object defaultRoundToShow:
    export cache.get
    private val cache =
      cacheApi[RelayTour.Id, Option[RelayRound]](16, "relay.lastAndNextRounds"):
        _.expireAfterWrite(5 seconds).buildAsyncFuture: tourId =>
          val chronoSort = $doc("startsAt" -> 1, "createdAt" -> 1)
          val lastStarted = roundRepo.coll
            .find($doc("tourId" -> tourId, "startedAt" $exists true))
            .sort($doc("startedAt" -> -1))
            .one[RelayRound]
          val next = roundRepo.coll
            .find($doc("tourId" -> tourId, "finished" -> false))
            .sort(chronoSort)
            .one[RelayRound]
          lastStarted zip next flatMap {
            case (None, _) => // no round started yet, show the first one
              roundRepo.coll
                .find($doc("tourId" -> tourId))
                .sort(chronoSort)
                .one[RelayRound]
            case (Some(last), Some(next)) => // show the next one if it's less than an hour away
              fuccess:
                if next.startsAt.exists(_ isBefore nowInstant.plusHours(1))
                then next.some
                else last.some
            case (Some(last), None) =>
              fuccess(last.some)
          }

  private var spotlightCache: List[RelayTour.ActiveWithSomeRounds] = Nil

  def spotlight: List[ActiveWithSomeRounds] = spotlightCache

  val officialActive = cacheApi.unit[List[RelayTour.ActiveWithSomeRounds]]:
    _.refreshAfterWrite(5 seconds).buildAsyncFuture: _ =>
      val max = 64
      tourRepo.coll
        .aggregateList(max): framework =>
          import framework.*
          Match(tourRepo.selectors.officialActive) -> List(
            Sort(Descending("tier")),
            PipelineOperator:
              $lookup.pipeline(
                from = roundRepo.coll,
                as = "round",
                local = "_id",
                foreign = "tourId",
                pipe = List(
                  $doc("$match"     -> $doc("finished" -> false)),
                  $doc("$addFields" -> $doc("sync.log" -> $arr())),
                  $doc("$sort"      -> roundRepo.sort.chrono),
                  $doc("$limit"     -> 1)
                )
              )
            ,
            UnwindField("round"),
            Limit(max)
          )
        .map: docs =>
          for
            doc   <- docs
            tour  <- doc.asOpt[RelayTour]
            round <- doc.getAsOpt[RelayRound]("round")
          yield (tour, round)
        .map:
          _.sortBy: (tour, round) =>
            (
              !round.startedAt.isDefined,                    // ongoing tournaments first
              0 - ~tour.tier,                                // then by tier
              round.startsAt.fold(Long.MaxValue)(_.toMillis) // then by next round date
            )
        .flatMap:
          _.map: (tour, round) =>
            defaultRoundToShow
              .get(tour.id)
              .map: link =>
                RelayTour.ActiveWithSomeRounds(tour, display = round, link = link | round)
          .parallel
        .addEffect: trs =>
          spotlightCache = trs
            .filter(_.tour.tier.has(RelayTour.Tier.BEST))
            .filterNot(_.display.finished)
            .filter: tr =>
              tr.display.hasStarted || tr.display.startsAt.exists(_.isBefore(nowInstant.plusMinutes(30)))
            .take(2)

  def isOfficial(id: StudyId): Fu[Boolean] =
    roundRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour"),
          PipelineOperator($doc("$replaceWith" -> $doc("tier" -> "$tour.tier")))
        )
      .map(_.exists(_.contains("tier")))

  def tourById(id: RelayTour.Id) = tourRepo.coll.byId[RelayTour](id)

  private[relay] def toSync(official: Boolean, maxDocs: Int = 30) =
    roundRepo.coll
      .aggregateList(maxDocs, _.pri): framework =>
        import framework.*
        Match(
          $doc(
            "sync.until" $exists true,
            "sync.nextAt" $lt nowInstant
          )
        ) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour"),
          Match($doc("tour.tier" $exists official)),
          Sort(Descending("tour.tier")),
          Limit(maxDocs)
        )
      .map(_ flatMap readRoundWithTour)

  def tourCreate(data: RelayTourForm.Data)(using Me): Fu[RelayTour] =
    val tour = data.make
    tourRepo.coll.insert.one(tour) inject tour

  def tourUpdate(tour: RelayTour, data: RelayTourForm.Data)(using Me): Funit =
    tourRepo.coll.update.one($id(tour.id), data.update(tour)).void andDo
      leaderboard.invalidate(tour.id)

  def create(data: RelayRoundForm.Data, tour: RelayTour)(using me: Me): Fu[RelayRound] =
    roundRepo.lastByTour(tour) flatMapz { last =>
      studyRepo.byId(last.studyId)
    } flatMap { lastStudy =>
      import lila.study.{ StudyMember, StudyMembers }
      val relay = data.make(me, tour)
      roundRepo.coll.insert.one(relay) >>
        studyApi.create(
          StudyMaker.ImportGame(
            id = relay.studyId.some,
            name = relay.name.into(StudyName).some,
            settings = lastStudy
              .fold(
                Settings.init
                  .copy(
                    chat = Settings.UserSelection.Everyone,
                    sticky = false
                  )
              )(_.settings)
              .some,
            from = Study.From.Relay(none).some
          ),
          me,
          withRatings = true,
          _.copy(
            members = lastStudy.fold(StudyMembers.empty)(_.members) + StudyMember(me, StudyMember.Role.Write)
          )
        ) >>
        tourRepo.setActive(tour.id, true) >>
        studyApi.addTopics(relay.studyId, List(StudyTopic.broadcast)) inject relay
    }

  def requestPlay(id: RelayRoundId, v: Boolean): Funit =
    WithRelay(id): relay =>
      relay.sync.upstream.flatMap(_.asUrl).map(_.withRound) foreach formatApi.refresh
      update(relay): r =>
        if v then r.withSync(_.play) else r.withSync(_.pause)
      .void

  def update(from: RelayRound)(f: RelayRound => RelayRound): Fu[RelayRound] =
    val round = f(from).pipe: r =>
      if r.sync.upstream != from.sync.upstream then r.withSync(_.clearLog) else r
    studyApi.rename(round.studyId, round.name into StudyName) >> {
      if round == from then fuccess(round)
      else
        for
          _ <- roundRepo.coll.update.one($id(round.id), round).void
          _ <- (round.sync.playing != from.sync.playing) so
            sendToContributors(round.id, "relaySync", jsonView sync round)
          _ <- (round.finished != from.finished) so denormalizeTourActive(round.tourId)
        yield
          round.sync.log.events.lastOption.ifTrue(round.sync.log != from.sync.log).foreach { event =>
            sendToContributors(round.id, "relayLog", Json.toJsObject(event))
          }
          round
    }

  def reset(old: RelayRound)(using me: Me): Funit =
    WithRelay(old.id) { relay =>
      studyApi.deleteAllChapters(relay.studyId, me) >> {
        old.hasStartedEarly so roundRepo.coll.update
          .one($id(relay.id), $set("finished" -> false) ++ $unset("startedAt"))
          .void
      } andDo {
        multiboard invalidate relay.studyId
        leaderboard invalidate relay.tourId
      }
    } >> requestPlay(old.id, v = true)

  def deleteRound(roundId: RelayRoundId): Fu[Option[RelayTour]] =
    byIdWithTour(roundId).flatMapz: rt =>
      roundRepo.coll.delete.one($id(rt.round.id)) >>
        denormalizeTourActive(rt.tour.id) inject rt.tour.some

  def deleteTourIfOwner(tour: RelayTour)(using me: Me): Fu[Boolean] =
    tour.ownerId
      .is(me)
      .so:
        for
          _      <- tourRepo.delete(tour)
          rounds <- roundRepo.idsByTourOrdered(tour)
          _      <- roundRepo.deleteByTour(tour)
          _      <- rounds.map(_ into StudyId).traverse_(studyApi.deleteById)
        yield true

  def getOngoing(id: RelayRoundId): Fu[Option[RelayRound.WithTour]] =
    roundRepo.coll.one[RelayRound]($doc("_id" -> id, "finished" -> false)) flatMapz { relay =>
      tourById(relay.tourId) map2 relay.withTour
    }

  def canUpdate(tour: RelayTour)(using me: Me): Fu[Boolean] =
    fuccess(Granter(_.Relay) || me.is(tour.ownerId)) >>|
      roundRepo.coll.distinctEasy[StudyId, List]("_id", roundRepo.selectors tour tour.id).flatMap { ids =>
        studyRepo.membersByIds(ids) map {
          _.exists(_ contributorIds me)
        }
      }

  def cloneTour(from: RelayTour)(using me: Me): Fu[RelayTour] =
    val tour = from.copy(
      _id = RelayTour.makeId,
      name = s"${from.name} (clone)",
      ownerId = me.userId,
      createdAt = nowInstant,
      syncedAt = none
    )
    tourRepo.coll.insert.one(tour) >>
      roundRepo
        .byTourOrderedCursor(from)
        .documentSource()
        .mapAsync(1)(cloneWithStudy(_, tour))
        .runWith(Sink.ignore)
        .inject(tour)

  private def cloneWithStudy(from: RelayRound, to: RelayTour)(using me: Me): Fu[RelayRound] =
    val round = from.copy(
      _id = RelayRound.makeId,
      tourId = to.id
    )
    for
      _ <- studyApi
        .byId(from.studyId)
        .flatMapz: s =>
          studyApi
            .justCloneNoChecks(
              me,
              s,
              _.copy(
                _id = round.studyId,
                visibility = Study.Visibility.Public
              )
            ) >>
            studyApi.addTopics(round.studyId, List(StudyTopic.broadcast))
      _ <- roundRepo.coll.insert.one(round)
    yield round

  def officialTourStream(perSecond: MaxPerSecond, nb: Int): Source[RelayTour.WithRounds, ?] =

    val lookup = $lookup.pipeline(
      from = roundRepo.coll,
      as = "rounds",
      local = "_id",
      foreign = "tourId",
      pipe = List($doc("$sort" -> roundRepo.sort.start))
    )
    val activeStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(tourRepo.selectors.officialActive),
          Sort(Descending("tier")),
          PipelineOperator(lookup)
        )
      .documentSource(nb)

    val inactiveStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(tourRepo.selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(lookup)
        )
      .documentSource(nb)

    activeStream
      .concat(inactiveStream)
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds") map tour.withRounds
          .toList
      .throttle(perSecond.value, 1 second)
      .take(nb)
  end officialTourStream

  private[relay] def autoStart: Funit =
    roundRepo.coll
      .list[RelayRound](
        $doc(
          "startsAt" $lt nowInstant.plusMinutes(30) // start 30 minutes early to fetch boards
            $gt nowInstant.minusDays(1),            // bit late now
          "startedAt" $exists false,
          "sync.until" $exists false
        )
      )
      .flatMap:
        _.map: relay =>
          logger.info(s"Automatically start $relay")
          requestPlay(relay.id, v = true)
        .parallel.void

  private[relay] def autoFinishNotSyncing: Funit =
    roundRepo.coll
      .list[RelayRound]:
        $doc(
          "sync.until" $exists false,
          "finished" -> false,
          "startedAt" $lt nowInstant.minusHours(3),
          $or(
            "startsAt" $exists false,
            "startsAt" $lt nowInstant
          )
        )
      .flatMap:
        _.map: relay =>
          logger.info(s"Automatically finish $relay")
          update(relay)(_.finish)
        .parallel.void

  private[relay] def WithRelay[A: Zero](id: RelayRoundId)(f: RelayRound => Fu[A]): Fu[A] =
    byId(id) flatMapz f

  private[relay] def onStudyRemove(studyId: StudyId) =
    roundRepo.coll.delete.one($id(studyId into RelayRoundId)).void

  private[relay] def becomeStudyAdmin(studyId: StudyId, me: Me): Funit =
    roundRepo
      .tourIdByStudyId(studyId)
      .flatMapz: tourId =>
        roundIdsById(tourId).flatMap:
          _.map(studyApi.becomeAdmin(_, me)).sequence.void

  private def sendToContributors(id: RelayRoundId, t: String, msg: JsObject): Funit =
    studyApi members id.into(StudyId) map {
      _.map(_.contributorIds).withFilter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.socket.SendTos
        import lila.common.Json.given
        import lila.socket.Socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        lila.common.Bus.publish(SendTos(userIds, payload), "socketUsers")
      }
    }
