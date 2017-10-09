package lila.relay

import akka.actor._
import org.joda.time.DateTime
import play.api.libs.json.JsObject
import reactivemongo.bson._

import lila.db.dsl._
import lila.socket.Socket.makeMessage
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

  def byIdWithStudy(id: Relay.Id): Fu[Option[Relay.WithStudy]] =
    byId(id) flatMap {
      _ ?? { relay =>
        studyApi.byId(relay.studyId) map2 { (study: Study) =>
          Relay.WithStudy(relay, study)
        }
      }
    }

  def all(me: Option[User]): Fu[Relay.Selection] =
    created.flatMap(withStudyAndLiked(me)) zip
      started.flatMap(withStudyAndLiked(me)) zip
      closed.flatMap(withStudyAndLiked(me)) map {
        case c ~ s ~ t => Relay.Selection(c, s, t)
      }

  def toSync = coll.find($doc(
    "sync.until" $exists true,
    "sync.nextAt" $lt DateTime.now
  )).list[Relay]()

  def setLikes(id: Relay.Id, likes: lila.study.Study.Likes): Funit =
    coll.updateField($id(id), "likes", likes).void

  def created = coll.find($doc(
    "startsAt" $gt DateTime.now
  )).sort($sort asc "startsAt").list[Relay]()

  def started = coll.find($doc(
    "sync.until" $exists true
  )).sort($sort asc "startsAt").list[Relay]()

  def closed = coll.find($doc(
    "finishedAt" $exists true
  )).sort($sort asc "startsAt").list[Relay]()

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

  def update(relay: Relay): Funit =
    coll.update($id(relay.id), relay).void

  def setSync(id: Relay.Id, user: User, v: Boolean): Funit = byId(id) flatMap {
    _.map(_ setSync v) ?? { relay =>
      coll.update($id(relay.id.value), relay).void >> publishRelay(relay)
    }
  }

  def setFinished(id: Relay.Id) =
    coll.update(
      $id(id),
      $set("finishedAt" -> DateTime.now) ++ $unset("sync.until")
    ).void >> publishRelay(id)

  def unFinish(id: Relay.Id) =
    coll.unsetField($id(id), "finishedAt").void >> publishRelay(id)

  def addLog(id: Relay.Id, event: SyncLog.Event): Funit =
    coll.update(
      $id(id),
      $doc("$push" -> $doc(
        "sync.log" -> $doc(
          "$each" -> List(event),
          "$slice" -> -SyncLog.historySize
        )
      )),
      upsert = true
    ).void >>
      sendToContributors(id, makeMessage("relayLog", JsonView.syncLogEventWrites writes event)) >>-
      event.error.foreach { err => logger.info(s"$id $err") }

  private[relay] def publishRelay(relay: Relay): Funit =
    sendToContributors(relay.id, makeMessage("relayData", JsonView.relayWrites writes relay))

  private def publishRelay(relayId: Relay.Id): Funit =
    byId(relayId) flatMap { _ ?? publishRelay }

  private def sendToContributors(id: Relay.Id, msg: JsObject): Funit =
    studyApi members Study.Id(id.value) map {
      _.map(_.contributorIds).filter(_.nonEmpty) foreach { userIds =>
        import lila.hub.actorApi.SendTos
        import lila.socket.Socket.makeMessage
        system.lilaBus.publish(SendTos(userIds, msg), 'users)
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
