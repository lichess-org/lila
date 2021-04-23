package lila.relay

import akka.stream.scaladsl.Source
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.json._
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.util.chaining._

import lila.common.config.MaxPerSecond
import lila.db.dsl._
import lila.study.{ Settings, Study, StudyApi, StudyMaker, StudyRepo }
import lila.user.User

final class RelayApi(
    relayRepo: RelayRepo,
    tourRepo: RelayTourRepo,
    studyApi: StudyApi,
    studyRepo: StudyRepo,
    jsonView: JsonView,
    formatApi: RelayFormatApi
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  import BSONHandlers._
  import lila.study.BSONHandlers.StudyBSONHandler

  def byId(id: Relay.Id) = relayRepo.coll.byId[Relay](id.value)

  def byIdWithTour(id: Relay.Id): Fu[Option[Relay.WithTour]] =
    relayRepo.coll
      .aggregateOne() { framework =>
        import framework._
        Match($id(id)) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      }
      .map(_ flatMap readRelayWithTour)

  def byIdAndContributor(id: Relay.Id, me: User) =
    byIdWithStudy(id) map {
      _ collect {
        case Relay.WithTourAndStudy(relay, tour, study) if study.canContribute(me.id) => relay withTour tour
      }
    }

  def byIdWithStudy(id: Relay.Id): Fu[Option[Relay.WithTourAndStudy]] =
    byIdWithTour(id) flatMap {
      _ ?? { case Relay.WithTour(relay, tour) =>
        studyApi.byId(relay.studyId) dmap2 {
          Relay.WithTourAndStudy(relay, tour, _)
        }
      }
    }

  def byTour(tour: RelayTour): Fu[List[Relay.WithTour]] =
    relayRepo.byTour(tour).dmap(_.map(_ withTour tour))

  def fresh(me: Option[User]): Fu[Relay.Fresh] = fuccess(Relay.Fresh(Nil, Nil))
  // relayRepo.scheduled.flatMap(withStudy andLiked me) zip
  //   relayRepo.ongoing.flatMap(withStudy andLiked me) map { case (c, s) =>
  //     Relay.Fresh(c, s)
  //   }

  def tourById(id: RelayTour.Id) = tourRepo.coll.byId[RelayTour](id.value)

  private[relay] def toSync: Fu[List[Relay.WithTour]] =
    fetchWithTours(
      $doc(
        "sync.until" $exists true,
        "sync.nextAt" $lt DateTime.now
      ),
      20
    )

  def fetchWithTours(query: Bdoc, maxDocs: Int, readPreference: ReadPreference = ReadPreference.primary) =
    relayRepo.coll
      .aggregateList(maxDocs, readPreference) { framework =>
        import framework._
        Match(query) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      }
      .map(_ flatMap readRelayWithTour)

  def tourCreate(data: RelayTourForm.Data, user: User): Fu[RelayTour] = {
    val tour = data.make(user)
    tourRepo.coll.insert.one(tour) inject tour
  }

  def create(data: RelayForm.Data, user: User, tour: RelayTour): Fu[Relay] = {
    val relay = data.make(user, tour)
    relayRepo.coll.insert.one(relay) >>
      studyApi.importGame(
        StudyMaker.ImportGame(
          id = relay.studyId.some,
          name = Study.Name(relay.name).some,
          settings = Settings.init
            .copy(
              chat = Settings.UserSelection.Everyone,
              sticky = false
            )
            .some,
          from = Study.From.Relay(none).some
        ),
        user
      ) >>
      studyApi.addTopics(relay.studyId, List("Broadcast")) inject relay
  }

  def requestPlay(id: Relay.Id, v: Boolean): Funit =
    WithRelay(id) { relay =>
      relay.sync.upstream.flatMap(_.asUrl).map(_.withRound) foreach formatApi.refresh
      update(relay) { r =>
        if (v) r.withSync(_.play) else r.withSync(_.pause)
      } void
    }

  def update(from: Relay)(f: Relay => Relay): Fu[Relay] = {
    val relay = f(from) pipe { r =>
      if (r.sync.upstream != from.sync.upstream) r.withSync(_.clearLog) else r
    }
    studyApi.rename(relay.studyId, Study.Name(relay.name)) >> {
      if (relay == from) fuccess(relay)
      else
        relayRepo.coll.update.one($id(relay.id), relay).void >> {
          (relay.sync.playing != from.sync.playing) ?? publishRelay(relay)
        } >>- {
          relay.sync.log.events.lastOption.ifTrue(relay.sync.log != from.sync.log).foreach { event =>
            sendToContributors(relay.id, "relayLog", JsonView.syncLogEventWrites writes event)
          }
        } inject relay
    }
  }

  def reset(relay: Relay, by: User): Funit =
    studyApi.deleteAllChapters(relay.studyId, by) >>
      requestPlay(relay.id, v = true)

  def cloneRelay(rt: Relay.WithTour, by: User): Fu[Relay] =
    create(
      RelayForm.Data make rt.relay.copy(name = s"${rt.relay.name} (clone)"),
      by,
      rt.tour
    )

  def getOngoing(id: Relay.Id): Fu[Option[Relay.WithTour]] =
    relayRepo.coll.one[Relay]($doc("_id" -> id, "finished" -> false)) flatMap {
      _ ?? { relay =>
        tourById(relay.tourId) map2 relay.withTour
      }
    }

  // def officialStream(perSecond: MaxPerSecond, nb: Int): Source[JsObject, _] =
  //   relayRepo
  //     .officialCursor(perSecond.value)
  //     .documentSource(nb)
  //     .throttle(perSecond.value, 1.second)
  //     .map(jsonView.public)

  private[relay] def autoStart: Funit =
    relayRepo.coll.list[Relay](
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
    relayRepo.coll.list[Relay](
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

  private[relay] def WithRelay[A: Zero](id: Relay.Id)(f: Relay => Fu[A]): Fu[A] =
    byId(id) flatMap { _ ?? f }

  private[relay] def onStudyRemove(studyId: String) =
    relayRepo.coll.delete.one($id(Relay.Id(studyId))).void

  private[relay] def publishRelay(relay: Relay): Funit =
    sendToContributors(relay.id, "relayData", jsonView admin relay)

  private def sendToContributors(id: Relay.Id, t: String, msg: JsObject): Funit =
    studyApi members Study.Id(id.value) map {
      _.map(_.contributorIds).withFilter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.socket.SendTos
        import JsonView.idWrites
        import lila.socket.Socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        lila.common.Bus.publish(SendTos(userIds, payload), "socketUsers")
      }
    }
}
