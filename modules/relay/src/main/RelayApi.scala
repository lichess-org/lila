package lila.relay

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.db.dsl._
import lila.study.{ StudyApi, Study, Settings }
import lila.user.User

final class RelayApi(
    coll: Coll,
    studyApi: StudyApi
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

  def created = coll.find($doc(
    "startsAt" $gt DateTime.now
  )).sort($sort asc "startsAt").list[Relay]()

  def started = coll.find($doc(
    "startsAt" $lt DateTime.now,
    "closedAt" $exists false
  )).sort($sort asc "startsAt").list[Relay]()

  def closed = coll.find($doc(
    "closedAt" $exists true
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

  private def withStudy(relays: List[Relay]): Fu[List[Relay.WithStudy]] =
    studyApi byIds relays.map(_.studyId) map { studies =>
      relays.flatMap { relay =>
        studies.find(_.id == relay.studyId) map { Relay.WithStudy(relay, _) }
      }
    }
}
