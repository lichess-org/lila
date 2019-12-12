package lila.relay

import akka.actor._
import io.lemonlabs.uri.Url
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.json._
import reactivemongo.bson._

import lila.db.dsl._
import lila.security.Granter
import lila.study.{ StudyApi, Study, StudyMaker, Settings }
import lila.user.User

final class RelayApi(
    repo: RelayRepo,
    studyApi: StudyApi,
    withStudy: RelayWithStudy,
    jsonView: JsonView,
    clearFormatCache: Relay.Sync.UpstreamWithRound => Unit,
    system: ActorSystem
) {

  import BSONHandlers._
  import lila.study.BSONHandlers.LikesBSONHandler

  def byId(id: Relay.Id) = repo.coll.byId[Relay](id.value)

  def byIdAndContributor(id: Relay.Id, me: User) = byIdWithStudy(id) map {
    _ collect {
      case Relay.WithStudy(relay, study) if study.canContribute(me.id) => relay
    }
  }

  def byIdWithStudy(id: Relay.Id): Fu[Option[Relay.WithStudy]] = WithRelay(id) { relay =>
    studyApi.byId(relay.studyId) map2 { (study: Study) =>
      Relay.WithStudy(relay, study)
    }
  }

  def fresh(me: Option[User]): Fu[Relay.Fresh] =
    repo.scheduled.flatMap(withStudy andLiked me) zip
      repo.ongoing.flatMap(withStudy andLiked me) map {
        case c ~ s => Relay.Fresh(c, s)
      }

  private[relay] def toSync = repo.coll.find($doc(
    "sync.until" $exists true,
    "sync.nextAt" $lt DateTime.now
  )).list[Relay]()

  def setLikes(id: Relay.Id, likes: lila.study.Study.Likes): Funit =
    repo.coll.updateField($id(id), "likes", likes).void

  def create(data: RelayForm.Data, user: User): Fu[Relay] = {
    val relay = data make user
    repo.coll.insert(relay) >>
      studyApi.importGame(StudyMaker.ImportGame(
        id = relay.studyId.some,
        name = Study.Name(relay.name).some,
        settings = Settings.init.copy(
          chat = Settings.UserSelection.Everyone,
          sticky = false
        ).some,
        from = Study.From.Relay(none).some
      ), user) inject relay
  }

  def requestPlay(id: Relay.Id, v: Boolean): Funit = WithRelay(id) { relay =>
    clearFormatCache(relay.sync.upstream.withRound)
    update(relay) { r =>
      if (v) r.withSync(_.play) else r.withSync(_.pause)
    } void
  }

  def update(from: Relay)(f: Relay => Relay): Fu[Relay] = {
    val relay = f(from) |> { r =>
      if (r.sync.upstream.url != from.sync.upstream.url) r.withSync(_.clearLog) else r
    }
    if (relay == from) fuccess(relay)
    else repo.coll.update($id(relay.id), relay).void >> {
      (relay.sync.playing != from.sync.playing) ?? publishRelay(relay)
    } >>- {
      relay.sync.log.events.lastOption.ifTrue(relay.sync.log != from.sync.log).foreach { event =>
        sendToContributors(relay.id, "relayLog", JsonView.syncLogEventWrites writes event)
      }
    } inject relay
  }

  def reset(relay: Relay, by: User): Funit =
    studyApi.deleteAllChapters(relay.studyId, by) >>
      requestPlay(relay.id, true)

  def getOngoing(id: Relay.Id): Fu[Option[Relay]] =
    repo.coll.find($doc("_id" -> id, "finished" -> false)).uno[Relay]

  private[relay] def autoStart: Funit =
    repo.coll.find($doc(
      "startsAt" $lt DateTime.now.plusMinutes(30) // start 30 minutes early to fetch boards
        $gt DateTime.now.minusDays(1), // bit late now
      "startedAt" $exists false,
      "sync.until" $exists false
    )).list[Relay]() flatMap {
      _.map { relay =>
        logger.info(s"Automatically start $relay")
        requestPlay(relay.id, true)
      }.sequenceFu.void
    }

  private[relay] def autoFinishNotSyncing: Funit =
    repo.coll.find($doc(
      "sync.until" $exists false,
      "finished" -> false,
      "startedAt" $lt DateTime.now.minusHours(3),
      $or(
        "startsAt" $exists false,
        "startsAt" $lt DateTime.now
      )
    )).list[Relay]() flatMap {
      _.map { relay =>
        logger.info(s"Automatically finish $relay")
        update(relay)(_.finish)
      }.sequenceFu.void
    }

  private[relay] def WithRelay[A: Zero](id: Relay.Id)(f: Relay => Fu[A]): Fu[A] =
    byId(id) flatMap { _ ?? f }

  private[relay] def onStudyRemove(studyId: String) =
    repo.coll.remove($id(Relay.Id(studyId))).void

  private[relay] def publishRelay(relay: Relay): Funit =
    sendToContributors(relay.id, "relayData", jsonView.relayWrites writes relay)

  private def sendToContributors(id: Relay.Id, t: String, msg: JsObject): Funit =
    studyApi members Study.Id(id.value) map {
      _.map(_.contributorIds).filter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.socket.SendTos
        import JsonView.idWrites
        import lila.socket.Socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        lila.common.Bus.publish(SendTos(userIds, payload), 'socketUsers)
      }
    }
}
