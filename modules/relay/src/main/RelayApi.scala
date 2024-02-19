package lila.relay

import akka.stream.scaladsl.*
import alleycats.Zero
import play.api.libs.json.*
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*
import scala.util.chaining.*

import lila.common.config.MaxPerSecond
import lila.db.dsl.{ *, given }
import lila.memo.{ PicfitApi, CacheApi }
import lila.study.{ Settings, Study, StudyApi, StudyId, StudyMaker, StudyMultiBoard, StudyRepo, StudyTopic }
import lila.security.Granter
import lila.user.{ User, Me, MyId }
import lila.relay.RelayTour.ActiveWithSomeRounds
import lila.i18n.I18nKeys.streamer.visibility
import lila.common.config.Max
import lila.relay.RelayRound.WithTour

final class RelayApi(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    studyApi: StudyApi,
    studyRepo: StudyRepo,
    multiboard: StudyMultiBoard,
    jsonView: JsonView,
    formatApi: RelayFormatApi,
    cacheApi: CacheApi,
    leaderboard: RelayLeaderboardApi,
    picfitApi: PicfitApi
)(using Executor, akka.stream.Materializer):

  import BSONHandlers.{ readRoundWithTour, given }
  import JsonView.given

  def byId(id: RelayRoundId) = roundRepo.coll.byId[RelayRound](id)

  def byIdWithTour(id: RelayRoundId): Fu[Option[WithTour]] =
    roundRepo.coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      .map(_ flatMap readRoundWithTour)

  def byIdAndContributor(id: RelayRoundId)(using me: Me): Fu[Option[WithTour]] =
    byIdWithTourAndStudy(id).map:
      _.collect:
        case RelayRound.WithTourAndStudy(relay, tour, study) if study.canContribute(me) =>
          relay withTour tour

  def byIdWithTourAndStudy(id: RelayRoundId): Fu[Option[RelayRound.WithTourAndStudy]] =
    byIdWithTour(id) flatMapz { case WithTour(relay, tour) =>
      studyApi.byId(relay.studyId) dmap2:
        RelayRound.WithTourAndStudy(relay, tour, _)
    }

  def byIdWithStudy(id: RelayRoundId): Fu[Option[RelayRound.WithStudy]] =
    byId(id) flatMapz: relay =>
      studyApi.byId(relay.studyId) dmap2:
        RelayRound.WithStudy(relay, _)

  def byTourOrdered(tour: RelayTour): Fu[List[WithTour]] =
    roundRepo.byTourOrdered(tour).dmap(_.map(_ withTour tour))

  def roundIdsById(tourId: RelayTour.Id): Fu[List[StudyId]] =
    roundRepo.idsByTourId(tourId)

  def kickBroadcast(userId: UserId, tourId: RelayTour.Id, who: MyId): Funit =
    roundIdsById(tourId).flatMap:
      _.traverse_(studyApi.kick(_, userId, who))

  def withRounds(tour: RelayTour) = roundRepo.byTourOrdered(tour).dmap(tour.withRounds)

  def denormalizeTourActive(tourId: RelayTour.Id): Funit =
    roundRepo.coll.exists(roundRepo.selectors.tour(tourId) ++ $doc("finished" -> false)) flatMap {
      tourRepo.setActive(tourId, _)
    }

  object countOwnedByUser:
    private val cache = cacheApi[UserId, Int](32_768, "relay.nb.owned"):
      _.expireAfterWrite(5.minutes).buildAsyncFuture(tourRepo.countByOwner)
    export cache.get

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

  private def toSyncSelect = $doc(
    "sync.until" $exists true,
    "sync.nextAt" $lt nowInstant
  )

  private[relay] def toSyncOfficial(max: Max): Fu[List[WithTour]] =
    roundRepo.coll
      .aggregateList(max.value, _.pri): framework =>
        import framework.*
        Match(toSyncSelect) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour"),
          Match($doc("tour.tier" $exists true)),
          Sort(Descending("tour.tier"), Ascending("sync.nextAt")),
          Limit(max.value)
        )
      .map(_ flatMap readRoundWithTour)

  private[relay] def toSyncUser(max: Max, maxPerUser: Max = Max(5)): Fu[List[WithTour]] =
    roundRepo.coll
      .aggregateList(max.value, _.pri): framework =>
        import framework.*
        Match(toSyncSelect) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour"),
          Match($doc("tour.tier" $exists false)),
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
      .map(_ flatMap readRoundWithTour)

  def tourCreate(data: RelayTourForm.Data)(using Me): Fu[RelayTour] =
    val tour = data.make
    tourRepo.coll.insert.one(tour) inject tour

  def tourUpdate(prev: RelayTour, data: RelayTourForm.Data)(using Me): Funit =
    val tour = data update prev
    import toBSONValueOption.given
    tourRepo.coll.update
      .one(
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
          "ownerId"         -> tour.ownerId.some
        )
      )
      .void

  def create(data: RelayRoundForm.Data, tour: RelayTour)(using me: Me): Fu[RelayRound.WithTourAndStudy] =
    roundRepo.lastByTour(tour) flatMapz { last =>
      studyRepo.byId(last.studyId)
    } flatMap { lastStudy =>
      import lila.study.{ StudyMember, StudyMembers }
      val relay = data.make(me, tour)
      for
        study <- studyApi.create(
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
        ) orFail s"Can't create study for relay $relay"
        _ <- roundRepo.coll.insert.one(relay)
        _ <- tourRepo.setActive(tour.id, true)
        _ <- studyApi.addTopics(relay.studyId, List(StudyTopic.broadcast.value))
      yield relay.withTour(tour).withStudy(study.study)
    }

  def requestPlay(id: RelayRoundId, v: Boolean): Funit =
    WithRelay(id): relay =>
      relay.sync.upstream.flatMap(_.asUrl).map(_.withRound) foreach formatApi.refresh
      update(relay): r =>
        if v then r.withSync(_.play) else r.withSync(_.pause)
      .void

  def reFetchAndUpdate(round: RelayRound)(f: Update[RelayRound]): Fu[RelayRound] =
    byId(round.id).orFail(s"Relay round ${round.id} not found").flatMap(update(_)(f))

  def update(from: RelayRound)(f: Update[RelayRound]): Fu[RelayRound] =
    val round = f(from).pipe: r =>
      if r.sync.upstream != from.sync.upstream then r.withSync(_.clearLog) else r
    if round == from then fuccess(round)
    else
      for
        _ <- (from.name != round.name) so studyApi.rename(round.studyId, round.name into StudyName)
        _ <- roundRepo.coll.update.one($id(round.id), round).void
        _ <- (round.sync.playing != from.sync.playing) so
          sendToContributors(round.id, "relaySync", jsonView sync round)
        _ <- (round.finished != from.finished) so denormalizeTourActive(round.tourId)
      yield
        round.sync.log.events.lastOption
          .ifTrue(round.sync.log != from.sync.log)
          .foreach: event =>
            sendToContributors(round.id, "relayLog", Json.toJsObject(event))
        round

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

  def getOngoing(id: RelayRoundId): Fu[Option[WithTour]] =
    roundRepo.coll.one[RelayRound]($doc("_id" -> id, "finished" -> false)) flatMapz { relay =>
      tourById(relay.tourId) map2 relay.withTour
    }

  def canUpdate(tour: RelayTour)(using me: Me): Fu[Boolean] =
    fuccess(Granter(_.StudyAdmin) || me.is(tour.ownerId)) >>|
      roundRepo.coll.distinctEasy[StudyId, List]("_id", roundRepo.selectors tour tour.id).flatMap { ids =>
        studyRepo.membersByIds(ids) map {
          _.exists(_ contributorIds me)
        }
      }

  def cloneTour(from: RelayTour)(using me: Me): Fu[RelayTour] =
    val tour = from.copy(
      id = RelayTour.makeId,
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
                _id = round.studyId,
                visibility = Study.Visibility.Public
              )
            ) >>
            studyApi.addTopics(round.studyId, List(StudyTopic.broadcast.value))
      _ <- roundRepo.coll.insert.one(round)
    yield round

  def officialTourStream(perSecond: MaxPerSecond, nb: Max, withLeaderboards: Boolean): Source[JsObject, ?] =

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
      .documentSource(nb.value)

    val inactiveStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match(tourRepo.selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(lookup)
        )
      .documentSource(nb.value)

    activeStream
      .concat(inactiveStream)
      .mapConcat: doc =>
        doc
          .asOpt[RelayTour]
          .flatMap: tour =>
            doc.getAsOpt[List[RelayRound]]("rounds") map tour.withRounds
          .toList
      .throttle(perSecond.value, 1 second)
      .take(nb.value)
      .mapAsync(1): t =>
        withLeaderboards.so(leaderboard(t.tour)).map(t -> _)
      .map: (t, l) =>
        jsonView(t, withUrls = true, leaderboard = l)
  end officialTourStream

  def myRounds(perSecond: MaxPerSecond, max: Option[Max])(using
      me: Me
  ): Source[RelayRound.WithTourAndStudy, ?] =
    studyRepo
      .sourceByMember(me.userId, isMe = true, select = studyRepo.selectBroadcast)
      .mapAsync(1): study =>
        byIdWithTour(study.id into RelayRoundId).map2(_.withStudy(study))
      .mapConcat(identity)
      .throttle(perSecond.value, 1 second)
      .take(max.fold(9999)(_.value))

  export tourRepo.{ isSubscribed, setSubscribed as subscribe }

  object image:
    private def rel(t: RelayTour) = s"relay:${t.id}"

    def upload(user: User, t: RelayTour, picture: PicfitApi.FilePart): Fu[RelayTour] = for
      image <- picfitApi.uploadFile(rel(t), picture, userId = user.id)
      _     <- tourRepo.coll.updateField($id(t.id), "image", image.id)
    yield t.copy(image = image.id.some)

    def delete(t: RelayTour): Fu[RelayTour] = for
      _ <- deleteImage(t)
      _ <- tourRepo.coll.unsetField($id(t.id), "image")
    yield t.copy(image = none)

    def deleteImage(post: RelayTour): Funit = picfitApi.deleteByRel(rel(post))

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
        _.traverse_ { relay =>
          logger.info(s"Automatically start $relay")
          requestPlay(relay.id, v = true)
        }

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
        _.traverse_ { relay =>
          logger.info(s"Automatically finish $relay")
          update(relay)(_.finish)
        }

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
