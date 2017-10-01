package lila.relay

import akka.actor._
import org.joda.time.DateTime
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

  def byId(id: Relay.Id) = coll.byId[Relay](id.value)

  def byIdWithStudy(id: Relay.Id): Fu[Option[Relay.WithStudy]] =
    byId(id) flatMap {
      _ ?? { relay =>
        studyApi.byId(relay.studyId) map2 { (study: Study) =>
          Relay.WithStudy(relay, study)
        }
      }
    }

  def all: Fu[Relay.Selection] =
    created.flatMap(withStudy) zip
      started.flatMap(withStudy) zip
      closed.flatMap(withStudy) map {
        case c ~ s ~ t => Relay.Selection(c, s, t)
      }

  def syncable = coll.find($doc("sync.until" $exists true)).list[Relay]()

  def created = coll.find($doc(
    "startsAt" $gt DateTime.now
  )).sort($sort asc "startsAt").list[Relay]()

  def started = coll.find($doc(
    "sync.until" $exists true
  )).sort($sort asc "startsAt").list[Relay]()

  def closed = coll.find($doc(
    "startsAt" $lt DateTime.now.minusMinutes(30),
    "sync.until" $exists false
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
        ).some
      ), user) inject relay
  }

  def setSync(id: Relay.Id, user: User, v: Boolean, socket: ActorRef): Funit = byId(id) flatMap {
    _.map(_ setSync v) ?? { relay =>
      coll.update($id(relay.id.value), relay).void >>- {
        socket ! lila.study.Socket.Broadcast("relayData", JsonView.relayWrites writes relay)
      }
    }
  }

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
    ).void >>- {
        event.error foreach { err => logger.info(s"$id $err") }
        system.actorSelection(s"/user/study-socket/${id.value}") !
          lila.study.Socket.Broadcast("relayLog", JsonView.syncLogEventWrites writes event)
      }

  private def withStudy(relays: List[Relay]): Fu[List[Relay.WithStudy]] =
    studyApi byIds relays.map(_.studyId) map { studies =>
      relays.flatMap { relay =>
        studies.find(_.id == relay.studyId) map { Relay.WithStudy(relay, _) }
      }
    }
}
