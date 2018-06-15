package lila.relay

import lila.study.{ Study, StudyApi }
import lila.user.User

private final class RelayWithStudy(studyApi: StudyApi) {

  def apply(relays: List[Relay]): Fu[List[Relay.WithStudy]] =
    studyApi byIds relays.map(_.studyId) map { studies =>
      relays.flatMap { relay =>
        studies.find(_.id == relay.studyId) map { Relay.WithStudy(relay, _) }
      }
    }

  def andLiked(me: Option[User])(relays: Seq[Relay]): Fu[Seq[Relay.WithStudyAndLiked]] =
    studyApi byIds relays.map(_.studyId) flatMap studyApi.withLiked(me) map { s =>
      relays.flatMap { relay =>
        s.find(_.study.id == relay.studyId) map {
          case Study.WithLiked(study, liked) => Relay.WithStudyAndLiked(relay, study, liked)
        }
      }
    }
}
