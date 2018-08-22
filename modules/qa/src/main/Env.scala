package lidraughts.qa

import akka.actor._
import com.typesafe.config.Config
import lidraughts.common.DetectLanguage

final class Env(
    config: Config,
    hub: lidraughts.hub.Env,
    detectLanguage: DetectLanguage,
    mongoCache: lidraughts.memo.MongoCache.Builder,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    notifyApi: lidraughts.notify.NotifyApi,
    system: akka.actor.ActorSystem,
    db: lidraughts.db.Env
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
    timeline = hub.actor.timeline
  )

  lazy val search = new Search(questionColl)

  lazy val forms = new DataForm(hub.actor.captcher, detectLanguage)

  system.lidraughtsBus.subscribeFun('gdprErase) {
    case lidraughts.user.User.GDPRErase(user) =>
      api.question erase user
      api.answer erase user
  }
}

object Env {

  lazy val current = "qa" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "qa",
    hub = lidraughts.hub.Env.current,
    detectLanguage = DetectLanguage(lidraughts.common.PlayApp loadConfig "detectlanguage"),
    mongoCache = lidraughts.memo.Env.current.mongoCache,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    notifyApi = lidraughts.notify.Env.current.api,
    system = lidraughts.common.PlayApp.system,
    db = lidraughts.db.Env.current
  )
}
