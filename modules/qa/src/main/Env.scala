package lila.qa

import akka.actor._
import com.typesafe.config.Config
import lila.common.DetectLanguage

final class Env(
    config: Config,
    hub: lila.hub.Env,
    detectLanguage: DetectLanguage,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    notifyApi: lila.notify.NotifyApi,
    system: akka.actor.ActorSystem,
    db: lila.db.Env
) {

  private val CollectionQuestion = config getString "collection.question"
  private val CollectionAnswer = config getString "collection.answer"

  private lazy val questionColl = db(CollectionQuestion)

  lazy val api = new QaApi(
    questionColl = questionColl,
    answerColl = db(CollectionAnswer),
    mongoCache = mongoCache,
    asyncCache = asyncCache,
    notifier = notifier
  )

  private lazy val notifier = new Notifier(
    notifyApi = notifyApi,
    timeline = hub.timeline
  )

  lazy val search = new Search(questionColl)

  lazy val forms = new DataForm(hub.captcher, detectLanguage)

  system.lilaBus.subscribeFun('gdprErase) {
    case lila.user.User.GDPRErase(user) =>
      api.question erase user
      api.answer erase user
  }
}

object Env {

  lazy val current = "qa" boot new Env(
    config = lila.common.PlayApp loadConfig "qa",
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    mongoCache = lila.memo.Env.current.mongoCache,
    asyncCache = lila.memo.Env.current.asyncCache,
    notifyApi = lila.notify.Env.current.api,
    system = lila.common.PlayApp.system,
    db = lila.db.Env.current
  )
}
