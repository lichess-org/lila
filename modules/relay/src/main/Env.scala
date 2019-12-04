package lila.relay

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    db: lila.db.Env,
    studyApi: lila.study.StudyApi,
    chapterRepo: lila.study.ChapterRepo,
    asyncCache: lila.memo.AsyncCache.Builder,
    slackApi: lila.slack.SlackApi
)(implicit system: ActorSystem) {

  private lazy val coll = db(CollName("relay"))

  lazy val forms = wire[RelayForm]

  private lazy val repo = wire[RelayRepo]

  private lazy val withStudy = wire[RelayWithStudy]

  lazy val jsonView = new JsonView(new RelayMarkup)

  lazy val api: RelayApi = wire[RelayApi]

  lazy val pager = wire[RelayPager]

  private lazy val sync = wire[RelaySync]

  private lazy val formatApi = wire[RelayFormatApi]

  system.actorOf(Props(wire[RelayFetch]))

  system.scheduler.schedule(1 minute, 1 minute) {
    api.autoStart >> api.autoFinishNotSyncing
  }

  lila.common.Bus.subscribeFun("studyLikes", "study", "relayToggle") {
    case lila.study.actorApi.StudyLikes(id, likes) => api.setLikes(Relay.Id(id.value), likes)
    case lila.hub.actorApi.study.RemoveStudy(studyId, _) => api.onStudyRemove(studyId)
    case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi.isContributor(id, who.u) flatMap {
        _ ?? {
          api.requestPlay(Relay.Id(id.value), v)
        }
      }
  }
}
