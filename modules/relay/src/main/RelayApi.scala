package lila.relay

import akka.stream.scaladsl.Source
import alleycats.Zero
import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.util.chaining._

import lila.common.config.MaxPerSecond
import lila.db.dsl._
import lila.memo.CacheApi
import lila.study.{ Settings, Study, StudyApi, StudyMaker, StudyMultiBoard, StudyRepo }
import lila.user.User

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
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  import BSONHandlers._
  import lila.study.BSONHandlers.{ StudyBSONHandler, StudyIdBSONHandler }

  def byId(id: RelayRound.Id) = roundRepo.coll.byId[RelayRound](id.value)

  def byIdWithTour(id: RelayRound.Id): Fu[Option[RelayRound.WithTour]] =
    roundRepo.coll
      .aggregateOne() { framework =>
        import framework._
        Match($id(id)) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      }
      .map(_ flatMap readRoundWithTour)

  def byIdAndContributor(id: RelayRound.Id, me: User) =
    byIdWithStudy(id) map {
      _ collect {
        case RelayRound.WithTourAndStudy(relay, tour, study) if study.canContribute(me.id) =>
          relay withTour tour
      }
    }

  def byIdWithStudy(id: RelayRound.Id): Fu[Option[RelayRound.WithTourAndStudy]] =
    byIdWithTour(id) flatMap {
      _ ?? { case RelayRound.WithTour(relay, tour) =>
        studyApi.byId(relay.studyId) dmap2 {
          RelayRound.WithTourAndStudy(relay, tour, _)
        }
      }
    }

  def byTourOrdered(tour: RelayTour): Fu[List[RelayRound.WithTour]] =
    roundRepo.byTourOrdered(tour).dmap(_.map(_ withTour tour))

  def withRounds(tour: RelayTour) = roundRepo.byTourOrdered(tour).dmap(tour.withRounds)

  def denormalizeTourActive(tourId: RelayTour.Id): Funit =
    roundRepo.coll.exists(roundRepo.selectors.tour(tourId) ++ $doc("finished" -> false)) flatMap {
      tourRepo.setActive(tourId, _)
    }

  def activeTourNextRound(tour: RelayTour): Fu[Option[RelayRound]] = tour.active ??
    roundRepo.coll
      .find($doc("tourId" -> tour.id, "finished" -> false))
      .sort(roundRepo.sort.chrono)
      .one[RelayRound]

  def tourLastRound(tour: RelayTour): Fu[Option[RelayRound]] =
    roundRepo.coll
      .find($doc("tourId" -> tour.id))
      .sort($doc("startedAt" -> -1, "startsAt" -> -1))
      .one[RelayRound]

  val officialActive = cacheApi.unit[List[RelayTour.ActiveWithNextRound]] {
    _.refreshAfterWrite(5 seconds)
      .buildAsyncFuture { _ =>
        tourRepo.coll
          .aggregateList(30) { framework =>
            import framework._
            Match(tourRepo.selectors.officialActive) -> List(
              Sort(Descending("tier")),
              PipelineOperator(
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
              ),
              UnwindField("round"),
              Limit(30)
            )
          }
          .map { docs =>
            for {
              doc   <- docs
              tour  <- doc.asOpt[RelayTour]
              round <- doc.getAsOpt[RelayRound]("round")
            } yield RelayTour.ActiveWithNextRound(tour, round)
          }
      }
  }

  def tourById(id: RelayTour.Id) = tourRepo.coll.byId[RelayTour](id.value)

  private[relay] def toSync(official: Boolean, maxDocs: Int = 30) =
    roundRepo.coll
      .aggregateList(maxDocs, ReadPreference.primary) { framework =>
        import framework._
        Match(
          $doc(
            "sync.until" $exists true,
            "sync.nextAt" $lt DateTime.now,
            "tour.tier" $exists official
          )
        ) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour"),
          Sort(Descending("tour.tier")),
          Limit(maxDocs)
        )
      }
      .map(_ flatMap readRoundWithTour)

  def tourCreate(data: RelayTourForm.Data, user: User): Fu[RelayTour] = {
    val tour = data.make(user)
    tourRepo.coll.insert.one(tour) inject tour
  }

  def tourUpdate(tour: RelayTour, data: RelayTourForm.Data, user: User): Funit =
    tourRepo.coll.update.one($id(tour.id), data.update(tour, user)).void >>-
      leaderboard.invalidate(tour.id)

  def create(data: RelayRoundForm.Data, user: User, tour: RelayTour): Fu[RelayRound] =
    roundRepo.lastByTour(tour) flatMap {
      _ ?? { last => studyRepo.byId(last.studyId) }
    } flatMap { lastStudy =>
      import lila.study.{ StudyMember, StudyMembers }
      val relay = data.make(user, tour)
      roundRepo.coll.insert.one(relay) >>
        studyApi.create(
          StudyMaker.ImportGame(
            id = relay.studyId.some,
            name = Study.Name(relay.name).some,
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
          user,
          withRatings = true,
          _.copy(members =
            lastStudy.fold(StudyMembers.empty)(_.members) + StudyMember(
              id = user.id,
              role = StudyMember.Role.Write
            )
          )
        ) >>
        tourRepo.setActive(tour.id, true) >>
        studyApi.addTopics(relay.studyId, List("Broadcast")) inject relay
    }

  def requestPlay(id: RelayRound.Id, v: Boolean): Funit =
    WithRelay(id) { relay =>
      relay.sync.upstream.flatMap(_.asUrl).map(_.withRound) foreach formatApi.refresh
      update(relay) { r =>
        if (v) r.withSync(_.play) else r.withSync(_.pause)
      } void
    }

  def update(from: RelayRound)(f: RelayRound => RelayRound): Fu[RelayRound] = {
    val round = f(from) pipe { r =>
      if (r.sync.upstream != from.sync.upstream) r.withSync(_.clearLog) else r
    }
    studyApi.rename(round.studyId, Study.Name(round.name)) >> {
      if (round == from) fuccess(round)
      else
        roundRepo.coll.update.one($id(round.id), round).void >> {
          (round.sync.playing != from.sync.playing) ?? sendToContributors(
            round.id,
            "relaySync",
            jsonView sync round
          )
        } >> {
          (round.finished != from.finished) ?? denormalizeTourActive(round.tourId)
        } >>- {
          round.sync.log.events.lastOption.ifTrue(round.sync.log != from.sync.log).foreach { event =>
            sendToContributors(round.id, "relayLog", JsonView.syncLogEventWrites writes event)
          }
        } inject round
    }
  }

  def reset(old: RelayRound, by: User): Funit =
    WithRelay(old.id) { relay =>
      studyApi.deleteAllChapters(relay.studyId, by) >> {
        old.hasStartedEarly ?? roundRepo.coll.update
          .one($id(relay.id), $set("finished" -> false) ++ $unset("startedAt"))
          .void
      } >>- {
        multiboard invalidate relay.studyId
        leaderboard invalidate relay.tourId
      }
    } >> requestPlay(old.id, v = true)

  def deleteRound(roundId: RelayRound.Id): Fu[Option[RelayTour]] =
    byIdWithTour(roundId) flatMap {
      _ ?? { rt =>
        roundRepo.coll.delete.one($id(rt.round.id)) >>
          denormalizeTourActive(rt.tour.id) inject rt.tour.some
      }
    }

  def getOngoing(id: RelayRound.Id): Fu[Option[RelayRound.WithTour]] =
    roundRepo.coll.one[RelayRound]($doc("_id" -> id, "finished" -> false)) flatMap {
      _ ?? { relay =>
        tourById(relay.tourId) map2 relay.withTour
      }
    }

  def canUpdate(user: User, tour: RelayTour): Fu[Boolean] =
    fuccess(tour.ownerId == user.id) >>|
      roundRepo.coll.distinctEasy[Study.Id, List]("_id", roundRepo.selectors tour tour.id).flatMap { ids =>
        studyRepo.membersByIds(ids) map {
          _.exists(_ contributorIds user.id)
        }
      }

  def officialTourStream(perSecond: MaxPerSecond, nb: Int): Source[RelayTour.WithRounds, _] = {

    val lookup = $lookup.pipeline(
      from = roundRepo.coll,
      as = "rounds",
      local = "_id",
      foreign = "tourId",
      pipe = List($doc("$sort" -> roundRepo.sort.start))
    )
    val activeStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPreference.secondaryPreferred) { framework =>
        import framework._
        List(
          Match(tourRepo.selectors.officialActive),
          Sort(Descending("tier")),
          PipelineOperator(lookup)
        )
      }
      .documentSource(nb)

    val inactiveStream = tourRepo.coll
      .aggregateWith[Bdoc](readPreference = ReadPreference.secondaryPreferred) { framework =>
        import framework._
        List(
          Match(tourRepo.selectors.officialInactive),
          Sort(Descending("syncedAt")),
          PipelineOperator(lookup)
        )
      }
      .documentSource(nb)

    (activeStream concat inactiveStream)
      .mapConcat { doc =>
        doc
          .asOpt[RelayTour]
          .flatMap { tour =>
            doc.getAsOpt[List[RelayRound]]("rounds") map tour.withRounds
          }
          .toList
      }
      .throttle(perSecond.value, 1 second)
      .take(nb)
  }

  private[relay] def autoStart: Funit =
    roundRepo.coll.list[RelayRound](
      $doc(
        "startsAt" $lt DateTime.now.plusMinutes(30) // start 30 minutes early to fetch boards
          $gt DateTime.now.minusDays(1),            // bit late now
        "startedAt" $exists false,
        "sync.until" $exists false
      )
    ) flatMap {
      _.map { relay =>
        logger.info(s"Automatically start $relay")
        requestPlay(relay.id, v = true)
      }.sequenceFu.void
    }

  private[relay] def autoFinishNotSyncing: Funit =
    roundRepo.coll.list[RelayRound](
      $doc(
        "sync.until" $exists false,
        "finished" -> false,
        "startedAt" $lt DateTime.now.minusHours(3),
        $or(
          "startsAt" $exists false,
          "startsAt" $lt DateTime.now
        )
      )
    ) flatMap {
      _.map { relay =>
        logger.info(s"Automatically finish $relay")
        update(relay)(_.finish)
      }.sequenceFu.void
    }

  private[relay] def WithRelay[A: Zero](id: RelayRound.Id)(f: RelayRound => Fu[A]): Fu[A] =
    byId(id) flatMap { _ ?? f }

  private[relay] def onStudyRemove(studyId: String) =
    roundRepo.coll.delete.one($id(RelayRound.Id(studyId))).void

  private def sendToContributors(id: RelayRound.Id, t: String, msg: JsObject): Funit =
    studyApi members Study.Id(id.value) map {
      _.map(_.contributorIds).withFilter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.socket.SendTos
        import JsonView.roundIdWrites
        import lila.socket.Socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        lila.common.Bus.publish(SendTos(userIds, payload), "socketUsers")
      }
    }
}
