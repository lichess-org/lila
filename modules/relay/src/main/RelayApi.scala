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
import lila.study.{ Settings, Study, StudyApi, StudyMaker, StudyMultiBoard, StudyRepo }
import lila.user.User

final class RelayApi(
    roundRepo: RelayRoundRepo,
    tourRepo: RelayTourRepo,
    studyApi: StudyApi,
    multiboard: StudyMultiBoard,
    studyRepo: StudyRepo,
    jsonView: JsonView,
    formatApi: RelayFormatApi
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  import BSONHandlers._
  import lila.study.BSONHandlers.StudyBSONHandler

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
      .sort(roundRepo.chronoSort)
      .one[RelayRound]

  def tourLastRound(tour: RelayTour): Fu[Option[RelayRound]] =
    roundRepo.coll
      .find($doc("tourId" -> tour.id))
      .sort($doc("startedAt" -> -1, "startsAt" -> -1))
      .one[RelayRound]

  def officialActive: Fu[List[RelayTour.ActiveWithNextRound]] =
    tourRepo.coll
      .aggregateList(20) { framework =>
        import framework._
        Match(tourRepo.selectors.official ++ tourRepo.selectors.active) -> List(
          PipelineOperator(
            $doc(
              "$lookup" -> $doc(
                "from" -> roundRepo.coll.name,
                "as"   -> "round",
                "let"  -> $doc("id" -> "$_id"),
                "pipeline" -> $arr(
                  $doc(
                    "$match" -> $doc(
                      "$expr" -> $doc(
                        "$and" -> $arr(
                          $doc("$eq" -> $arr("$tourId", "$$id")),
                          $doc("$eq" -> $arr("$finished", false))
                        )
                      )
                    )
                  ),
                  $doc("$addFields" -> $doc("sync.log" -> $arr())),
                  $doc("$sort"      -> roundRepo.chronoSort),
                  $doc("$limit"     -> 1)
                )
              )
            )
          ),
          UnwindField("round")
        )
      }
      .map { docs =>
        for {
          doc   <- docs
          tour  <- doc.asOpt[RelayTour]
          round <- doc.getAsOpt[RelayRound]("round")
        } yield RelayTour.ActiveWithNextRound(tour, round)
      }

  def tourById(id: RelayTour.Id) = tourRepo.coll.byId[RelayTour](id.value)

  private[relay] def toSync: Fu[List[RelayRound.WithTour]] =
    fetchWithTours(
      $doc(
        "sync.until" $exists true,
        "sync.nextAt" $lt DateTime.now
      ),
      20
    )

  def fetchWithTours(query: Bdoc, maxDocs: Int, readPreference: ReadPreference = ReadPreference.primary) =
    roundRepo.coll
      .aggregateList(maxDocs, readPreference) { framework =>
        import framework._
        Match(query) -> List(
          PipelineOperator(tourRepo lookup "tourId"),
          UnwindField("tour")
        )
      }
      .map(_ flatMap readRoundWithTour)

  def tourCreate(data: RelayTourForm.Data, user: User): Fu[RelayTour] = {
    val tour = data.make(user)
    tourRepo.coll.insert.one(tour) inject tour
  }

  def tourUpdate(tour: RelayTour, data: RelayTourForm.Data, user: User): Funit =
    tourRepo.coll.update.one($id(tour.id), data.update(tour, user)).void

  def create(data: RelayRoundForm.Data, user: User, tour: RelayTour): Fu[RelayRound] = {
    val relay = data.make(user, tour)
    roundRepo.coll.insert.one(relay) >>
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

  def reset(relay: RelayRound, by: User): Funit =
    studyApi.deleteAllChapters(relay.studyId, by) >>-
      multiboard.invalidate(relay.studyId) >>
      requestPlay(relay.id, v = true)

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

  // def officialStream(perSecond: MaxPerSecond, nb: Int): Source[JsObject, _] =
  //   relayRepo
  //     .officialCursor(perSecond.value)
  //     .documentSource(nb)
  //     .throttle(perSecond.value, 1.second)
  //     .map(jsonView.public)

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
