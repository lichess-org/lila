package lila.relay

import akka.stream.scaladsl.*
import alleycats.Zero
import play.api.libs.json.*
import reactivemongo.api.bson.*

import scala.util.chaining.*

import lila.db.dsl.{ *, given }
import lila.memo.{ CacheApi, PicfitApi }
import lila.relay.RelayRound.{ WithTour, Sync }
import lila.core.perm.Granter
import lila.core.study.data.StudyName
import lila.study.{ Settings, Study, StudyApi, StudyId, StudyMaker, StudyRepo, StudyTopic }

final class RelayApi(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    groupRepo: RelayGroupRepo,
    studyApi: StudyApi,
    studyRepo: StudyRepo,
    jsonView: JsonView,
    formatApi: RelayFormatApi,
    cacheApi: CacheApi,
    leaderboard: RelayLeaderboardApi,
    picfitApi: PicfitApi
)(using Executor, akka.stream.Materializer, play.api.Mode):

  import BSONHandlers.{ readRoundWithTour, given }
  import JsonView.given

  def byId(id: RelayRoundId) = roundRepo.coll.byId[RelayRound](id)

  def byIdWithTour(id: RelayRoundId): Fu[Option[WithTour]] =
    roundRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo.lookup("tourId")),
          UnwindField("tour")
        )
      .map(_.flatMap(readRoundWithTour))

  def byIdAndContributor(id: RelayRoundId)(using me: Me): Fu[Option[WithTour]] =
    byIdWithTourAndStudy(id).map:
      _.collect:
        case RelayRound.WithTourAndStudy(relay, tour, study)
            if study.canContribute(me) || Granter(_.StudyAdmin) =>
          relay.withTour(tour)

  def formNavigation(id: RelayRoundId): Fu[Option[(RelayRound, ui.FormNavigation)]] =
    byIdWithTour(id).flatMapz(rt => formNavigation(rt).dmap(some))

  def formNavigation(rt: RelayRound.WithTour): Fu[(RelayRound, ui.FormNavigation)] =
    formNavigation(rt.tour).map: nav =>
      (rt.round, nav.copy(round = rt.round.id.some))

  def formNavigation(tour: RelayTour): Fu[ui.FormNavigation] = for
    group  <- withTours.get(tour.id)
    rounds <- roundRepo.byTourOrdered(tour.id)
  yield ui.FormNavigation(group, tour, rounds, none)

  def byIdWithTourAndStudy(id: RelayRoundId): Fu[Option[RelayRound.WithTourAndStudy]] =
    byIdWithTour(id).flatMapz { case WithTour(relay, tour) =>
      studyApi
        .byId(relay.studyId)
        .dmap2:
          RelayRound.WithTourAndStudy(relay, tour, _)
    }

  def byIdWithStudy(id: RelayRoundId): Fu[Option[RelayRound.WithStudy]] =
    byId(id).flatMapz: relay =>
      studyApi
        .byId(relay.studyId)
        .dmap2:
          RelayRound.WithStudy(relay, _)

  def byTourOrdered(tour: RelayTour): Fu[List[WithTour]] =
    roundRepo.byTourOrdered(tour.id).dmap(_.map(_.withTour(tour)))

  def roundIdsById(tourId: RelayTourId): Fu[List[StudyId]] =
    roundRepo.idsByTourId(tourId)

  def kickBroadcast(userId: UserId, tourId: RelayTourId, who: MyId): Funit =
    roundIdsById(tourId).flatMap:
      _.sequentiallyVoid(studyApi.kick(_, userId, who))

  def withRounds(tour: RelayTour) = roundRepo.byTourOrdered(tour.id).dmap(tour.withRounds)

  def denormalizeTourActive(tourId: RelayTourId): Funit =
    val unfinished = RelayRoundRepo.selectors.tour(tourId) ++ $doc("finished" -> false)
    for
      active <- roundRepo.coll.exists(unfinished)
      live   <- active.so(roundRepo.coll.exists(unfinished ++ $doc("startedAt".$exists(true))))
      _      <- tourRepo.setActive(tourId, active, live)
    yield ()

  object countOwnedByUser:
    private val cache = cacheApi[UserId, Int](16_384, "relay.nb.owned"):
      _.expireAfterWrite(5.minutes).buildAsyncFuture(tourRepo.countByOwner(_, false))
    export cache.get

  def isOfficial(id: RelayRoundId): Fu[Boolean] =
    roundRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo.lookup("tourId")),
          UnwindField("tour"),
          PipelineOperator($doc("$replaceWith" -> $doc("tier" -> "$tour.tier")))
        )
      .map(_.exists(_.contains("tier")))

  def tourById(id: RelayTourId) = tourRepo.coll.byId[RelayTour](id)

  object withTours:
    private val cache = cacheApi[RelayTourId, Option[RelayGroup.WithTours]](256, "relay.groupWithTours"):
      _.expireAfterWrite(1.minute).buildAsyncFuture: id =>
        for
          group <- groupRepo.byTour(id)
          tours <- tourRepo.idNames(group.so(_.tours))
        yield group.map(RelayGroup.WithTours(_, tours))
    export cache.get
    def addTo(tour: RelayTour): Fu[RelayTour.WithGroupTours] =
      get(tour.id).map(RelayTour.WithGroupTours(tour, _))
    def invalidate(id: RelayTourId) = cache.underlying.synchronous.invalidate(id)

  private def toSyncSelect(onlyIds: Option[List[RelayTourId]]) = $doc(
    "sync.until".$exists(true),
    "sync.nextAt".$lt(nowInstant)
  ) ++ onlyIds.so(ids => $doc("tourId".$in(ids)))

  private[relay] def toSyncOfficial(max: Max, onlyIds: Option[List[RelayTourId]]): Fu[List[WithTour]] =
    roundRepo.coll
      .aggregateList(max.value, _.pri): framework =>
        import framework.*
        Match(toSyncSelect(onlyIds)) -> List(
          PipelineOperator(tourRepo.lookup("tourId")),
          UnwindField("tour"),
          Match($doc("tour.tier".$exists(true))),
          Sort(Descending("tour.tier"), Ascending("sync.nextAt")),
          Limit(max.value)
        )
      .map(_.flatMap(readRoundWithTour))

  private[relay] def toSyncUser(
      max: Max,
      onlyIds: Option[List[RelayTourId]],
      maxPerUser: Max = Max(5)
  ): Fu[List[WithTour]] =
    roundRepo.coll
      .aggregateList(max.value, _.pri): framework =>
        import framework.*
        Match(toSyncSelect(onlyIds)) -> List(
          PipelineOperator(tourRepo.lookup("tourId")),
          UnwindField("tour"),
          Match($doc("tour.tier".$exists(false))),
          Sort(Ascending("sync.nextAt")),
          GroupField("tour.ownerId")("relays" -> PushField("$ROOT")),
          Project:
            $doc(
              "_id"    -> false,
              "relays" -> $doc("$slice" -> $arr("$relays", maxPerUser))
            )
          ,
          UnwindField("relays"),
          ReplaceRootField("relays"),
          Limit(max.value)
        )
      .map(_.flatMap(readRoundWithTour))

  def tourCreate(data: RelayTourForm.Data)(using Me): Fu[RelayTour] =
    val tour = data.make
    tourRepo.coll.insert.one(tour).inject(tour)

  def tourUpdate(prev: RelayTour, data: RelayTourForm.Data)(using Me): Funit =
    val tour = data.update(prev)
    import toBSONValueOption.given
    for
      _ <- tourRepo.coll.update.one(
        $id(tour.id),
        $setsAndUnsets(
          "name"            -> tour.name.some,
          "description"     -> tour.description.some,
          "markup"          -> tour.markup,
          "tier"            -> tour.tier,
          "autoLeaderboard" -> tour.autoLeaderboard.some,
          "teamTable"       -> tour.teamTable.some,
          "players"         -> tour.players,
          "teams"           -> tour.teams,
          "spotlight"       -> tour.spotlight,
          "ownerId"         -> tour.ownerId.some,
          "pinnedStreamer"  -> tour.pinnedStreamer
        )
      )
      _ <- data.grouping.so(updateGrouping(tour, _))
    yield
      leaderboard.invalidate(tour.id)
      (tour.id :: data.grouping.so(_.tourIds)).foreach(withTours.invalidate)

  private def updateGrouping(tour: RelayTour, data: RelayGroup.form.Data)(using me: Me): Funit =
    Granter(_.Relay).so:
      val canGroup = fuccess(Granter(_.StudyAdmin)) >>| tourRepo.isOwnerOfAll(me.userId, data.tourIds)
      canGroup.flatMapz(groupRepo.update(tour.id, data))

  def create(data: RelayRoundForm.Data, tour: RelayTour)(using me: Me): Fu[RelayRound.WithTourAndStudy] =
    roundRepo
      .lastByTour(tour)
      .flatMapz: last =>
        studyRepo.byId(last.studyId)
      .flatMap: lastStudy =>
        import lila.study.{ StudyMember, StudyMembers }
        val relay = data.make(me, tour)
        for
          study <- studyApi
            .create(
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
                members =
                  lastStudy.fold(StudyMembers.empty)(_.members) + StudyMember(me, StudyMember.Role.Write)
              )
            )
            .orFail(s"Can't create study for relay $relay")
          _ <- roundRepo.coll.insert.one(relay)
          _ <- tourRepo.setActive(tour.id, true, relay.hasStarted)
          _ <- studyApi.addTopics(relay.studyId, List(StudyTopic.broadcast.value))
        yield relay.withTour(tour).withStudy(study.study)

  def requestPlay(id: RelayRoundId, v: Boolean): Funit =
    WithRelay(id): relay =>
      relay.sync.upstream.collect:
        case f: Sync.FetchableUpstream => formatApi.refresh(f)
      isOfficial(relay.id).flatMap: official =>
        update(relay): r =>
          if v
          then r.withSync(_.play(official))
          else r.withSync(_.pause)
        .void

  def reFetchAndUpdate(round: RelayRound)(f: Update[RelayRound]): Fu[RelayRound] =
    byId(round.id).orFail(s"Relay round ${round.id} not found").flatMap(update(_)(f))

  def update(from: RelayRound)(f: Update[RelayRound]): Fu[RelayRound] =
    val round = f(from).pipe: r =>
      if r.sync.upstream != from.sync.upstream then r.withSync(_.clearLog) else r
    if round == from then fuccess(round)
    else
      for
        _ <- (from.name != round.name).so(studyApi.rename(round.studyId, round.name.into(StudyName)))
        _ <- roundRepo.coll.update.one($id(round.id), round).void
        _ <- (round.sync.playing != from.sync.playing)
          .so(sendToContributors(round.id, "relaySync", jsonView.sync(round)))
        _ <- (round.stateHash != from.stateHash).so(denormalizeTourActive(round.tourId))
      yield
        round.sync.log.events.lastOption
          .ifTrue(round.sync.log != from.sync.log)
          .foreach: event =>
            sendToContributors(round.id, "relayLog", Json.toJsObject(event))
        round

  def reset(old: RelayRound)(using me: Me): Funit =
    WithRelay(old.id) { relay =>
      for
        _ <- studyApi.deleteAllChapters(relay.studyId, me)
        _ <- old.hasStartedEarly.so:
          roundRepo.coll.update.one($id(relay.id), $set("finished" -> false) ++ $unset("startedAt")).void
        _ <- roundRepo.coll.update.one($id(relay.id), $set("sync.log" -> $arr()))
      yield leaderboard.invalidate(relay.tourId)
    } >> requestPlay(old.id, v = true)

  def deleteRound(roundId: RelayRoundId): Fu[Option[RelayTour]] =
    byIdWithTour(roundId).flatMapz: rt =>
      for
        _ <- roundRepo.coll.delete.one($id(rt.round.id))
        _ <- denormalizeTourActive(rt.tour.id)
      yield rt.tour.some

  def deleteTourIfOwner(tour: RelayTour)(using me: Me): Fu[Boolean] =
    tour.ownerId
      .is(me)
      .so:
        for
          _      <- tourRepo.delete(tour)
          rounds <- roundRepo.idsByTourOrdered(tour)
          _      <- roundRepo.deleteByTour(tour)
          _      <- rounds.map(_.into(StudyId)).sequentiallyVoid(studyApi.deleteById)
        yield true

  def getOngoing(id: RelayRoundId): Fu[Option[WithTour]] =
    roundRepo.coll
      .one[RelayRound]($doc("_id" -> id, "finished" -> false))
      .flatMapz: relay =>
        tourById(relay.tourId).map2(relay.withTour)

  def canUpdate(tour: RelayTour)(using me: Me): Fu[Boolean] =
    fuccess(Granter(_.StudyAdmin) || me.is(tour.ownerId)) >>|
      roundRepo
        .studyIdsOf(tour.id)
        .flatMap: ids =>
          studyRepo
            .membersByIds(ids)
            .map:
              _.exists(_.contributorIds(me))

  def cloneTour(from: RelayTour)(using me: Me): Fu[RelayTour] =
    val tour = from.copy(
      id = RelayTour.makeId,
      name = RelayTour.Name(s"${from.name} (clone)"),
      ownerId = me.userId,
      createdAt = nowInstant,
      syncedAt = none
    )
    tourRepo.coll.insert.one(tour) >>
      roundRepo
        .byTourOrderedCursor(from.id)
        .documentSource()
        .mapAsync(1)(cloneWithStudy(_, tour))
        .runWith(Sink.ignore)
        .inject(tour)

  private def cloneWithStudy(from: RelayRound, to: RelayTour)(using me: Me): Fu[RelayRound] =
    val round = from.copy(
      id = RelayRound.makeId,
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
                id = round.studyId,
                visibility = lila.core.study.Visibility.public
              )
            ) >>
            studyApi.addTopics(round.studyId, List(StudyTopic.broadcast.value))
      _ <- roundRepo.coll.insert.one(round)
    yield round

  def myRounds(perSecond: MaxPerSecond, max: Option[Max])(using
      me: Me
  ): Source[RelayRound.WithTourAndStudy, ?] =
    studyRepo
      .sourceByMember(me.userId, isMe = true, select = studyRepo.selectBroadcast)
      .mapAsync(1): study =>
        byIdWithTour(study.id.into(RelayRoundId)).map2(_.withStudy(study))
      .mapConcat(identity)
      .throttle(perSecond.value, 1 second)
      .take(max.fold(9999)(_.value))

  export tourRepo.{ isSubscribed, setSubscribed as subscribe }

  object image:
    def rel(rt: RelayTour, tag: Option[String]) =
      tag.fold(s"relay:${rt.id}")(t => s"relay.$t:${rt.id}")

    def upload(
        user: User,
        t: RelayTour,
        picture: PicfitApi.FilePart,
        tag: Option[String] = None
    ): Fu[RelayTour] = for
      image <- picfitApi.uploadFile(rel(t, tag), picture, userId = user.id)
      _     <- tourRepo.coll.updateField($id(t.id), tag.getOrElse("image"), image.id)
    yield t.copy(image = image.id.some)

    def delete(t: RelayTour, tag: Option[String] = None): Fu[RelayTour] = for
      _ <- picfitApi.deleteByRel(rel(t, tag))
      _ <- tourRepo.coll.unsetField($id(t.id), tag.getOrElse("image"))
    yield t.copy(image = none)

  private[relay] def autoStart: Funit =
    roundRepo.coll
      .list[RelayRound](
        $doc(
          "startsAt"
            // start early to fetch boards
            .$lt(nowInstant.plusSeconds(RelayDelay.maxSeconds.value))
            .$gt(nowInstant.minusDays(1)), // bit late now
          "startedAt".$exists(false),
          "sync.until".$exists(false),
          "sync.upstream".$exists(true)
        )
      )
      .flatMap:
        _.sequentiallyVoid: relay =>
          val earlyMinutes = Math.min(60, 30 + relay.sync.delay.so(_.value / 60))
          relay.startsAt
            .exists(_.isBefore(nowInstant.plusMinutes(earlyMinutes)))
            .so:
              logger.info(s"Automatically start $relay")
              requestPlay(relay.id, v = true)

  private[relay] def autoFinishNotSyncing: Funit =
    roundRepo.coll
      .list[RelayRound]:
        $doc(
          "sync.until".$exists(false),
          "finished" -> false,
          "startedAt".$lt(nowInstant.minusHours(3)),
          $or(
            "startsAt".$exists(false),
            "startsAt".$lt(nowInstant)
          )
        )
      .flatMap:
        _.sequentiallyVoid: relay =>
          logger.info(s"Automatically finish $relay")
          update(relay)(_.finish)

  private[relay] def WithRelay[A: Zero](id: RelayRoundId)(f: RelayRound => Fu[A]): Fu[A] =
    byId(id).flatMapz(f)

  private[relay] def onStudyRemove(studyId: StudyId) =
    roundRepo.coll.delete.one($id(studyId.into(RelayRoundId))).void

  private[relay] def becomeStudyAdmin(studyId: StudyId, me: Me): Funit =
    roundRepo
      .tourIdByStudyId(studyId)
      .flatMapz: tourId =>
        roundIdsById(tourId).flatMap:
          _.map(studyApi.becomeAdmin(_, me)).sequence.void

  private def sendToContributors(id: RelayRoundId, t: String, msg: JsObject): Funit =
    studyApi.members(id.into(StudyId)).map {
      _.map(_.contributorIds).withFilter(_.nonEmpty).foreach { userIds =>
        import lila.core.socket.SendTos
        import lila.common.Json.given
        import lila.core.socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        lila.common.Bus.publish(SendTos(userIds, payload), "socketUsers")
      }
    }
