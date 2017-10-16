package lila.relay

import akka.actor._
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import play.api.libs.json._
import reactivemongo.bson._

import lila.db.dsl._
import lila.study.{ StudyApi, Study, Settings }
import lila.user.User

final class RelayApi(
    coll: Coll,
    studyApi: StudyApi,
    system: ActorSystem
) {

  import BSONHandlers._
  import lila.study.BSONHandlers.LikesBSONHandler

  def byId(id: Relay.Id) = coll.byId[Relay](id.value)

  def byIdAndOwner(id: Relay.Id, owner: User) = byId(id) map {
    _.filter(_.ownerId == owner.id)
  }

  def byIdWithStudy(id: Relay.Id): Fu[Option[Relay.WithStudy]] = WithRelay(id) { relay =>
    studyApi.byId(relay.studyId) map2 { (study: Study) =>
      Relay.WithStudy(relay, study)
    }
  }

  def all(me: Option[User]): Fu[Relay.Selection] =
    created.flatMap(withStudyAndLiked(me)) zip
      started.flatMap(withStudyAndLiked(me)) zip
      finished.flatMap(withStudyAndLiked(me)) map {
        case c ~ s ~ t => Relay.Selection(c, s, t)
      }

  def toSync = coll.find($doc(
    "sync.until" $exists true,
    "sync.nextAt" $lt DateTime.now
  )).list[Relay]()

  def setLikes(id: Relay.Id, likes: lila.study.Study.Likes): Funit =
    coll.updateField($id(id), "likes", likes).void

  def created = coll.find($doc(
    "startsAt" $gt DateTime.now.minusHours(1),
    "startedAt" $exists false
  )).sort($sort asc "startsAt").list[Relay]()

  def started = coll.find($doc(
    "startedAt" $exists true,
    "finished" -> false
  )).sort($sort asc "startedAt").list[Relay]()

  def finished = coll.find($doc(
    "startedAt" $exists true,
    "finished" -> true
  )).sort($sort desc "startedAt").list[Relay]()

  def create(data: RelayForm.Data, user: User): Fu[Relay] = {
    val relay = data make user
    coll.insert(relay) >>
      studyApi.create(lila.study.StudyMaker.Data(
        id = relay.studyId.some,
        name = Study.Name(relay.name).some,
        settings = Settings.init.copy(
          chat = Settings.UserSelection.Everyone,
          sticky = false
        ).some,
        from = Study.From.Relay(none).some
      ), user) inject relay
  }

  def requestPlay(id: Relay.Id, user: User, v: Boolean): Funit = WithRelay(id) { relay =>
    update(relay) { r =>
      if (v) r.withSync(_.play) else r.withSync(_.pause)
    } void
  }

  def update(from: Relay)(f: Relay => Relay): Fu[Relay] = {
    val relay = f(from)
    if (relay == from) fuccess(relay)
    else coll.update($id(relay.id), relay).void >> {
      (relay.sync.playing != from.sync.playing) ?? publishRelay(relay)
    } >>- {
      relay.sync.log.events.lastOption.ifTrue(relay.sync.log != from.sync.log).foreach { event =>
        sendToContributors(relay.id, "relayLog", JsonView.syncLogEventWrites writes event)
      }
    } inject relay
  }

  def getOngoing(id: Relay.Id): Fu[Option[Relay]] =
    coll.find($doc("_id" -> id, "finished" -> false)).uno[Relay]

  private[relay] def WithRelay[A: Zero](id: Relay.Id)(f: Relay => Fu[A]): Fu[A] =
    byId(id) flatMap { _ ?? f }

  private[relay] def onStudyRemove(studyId: String) =
    coll.remove($id(Relay.Id(studyId))).void

  private[relay] def publishRelay(relay: Relay): Funit =
    sendToContributors(relay.id, "relayData", JsonView.relayWrites writes relay)

  private def sendToContributors(id: Relay.Id, t: String, msg: JsObject): Funit =
    studyApi members Study.Id(id.value) map {
      _.map(_.contributorIds).filter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.SendTos
        import JsonView.idWrites
        import lila.socket.Socket.makeMessage
        val payload = makeMessage(t, msg ++ Json.obj("id" -> id))
        system.lilaBus.publish(SendTos(userIds, payload), 'users)
      }
    }

  private[relay] def getNbViewers(relay: Relay): Fu[Int] = {
    import makeTimeout.short
    import akka.pattern.ask
    import lila.study.Socket.{ GetNbMembers, NbMembers }
    studySocketActor(relay.id) ? GetNbMembers mapTo manifest[NbMembers] map (_.value) recover {
      case _: Exception => 0
    }
  }

  private def studySocketActor(id: Relay.Id) = system actorSelection s"/user/study-socket/${id.value}"

  private def withStudy(relays: List[Relay]): Fu[List[Relay.WithStudy]] =
    studyApi byIds relays.map(_.studyId) map { studies =>
      relays.flatMap { relay =>
        studies.find(_.id == relay.studyId) map { Relay.WithStudy(relay, _) }
      }
    }

  private def withStudyAndLiked(me: Option[User])(relays: List[Relay]): Fu[List[Relay.WithStudyAndLiked]] =
    studyApi byIds relays.map(_.studyId) flatMap studyApi.withLiked(me) map { s =>
      relays.flatMap { relay =>
        s.find(_.study.id == relay.studyId) map {
          case Study.WithLiked(study, liked) => Relay.WithStudyAndLiked(relay, study, liked)
        }
      }
    }

}
