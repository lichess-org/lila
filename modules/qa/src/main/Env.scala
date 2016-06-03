package lila.qa

import com.typesafe.config.Config
import lila.common.DetectLanguage
import lila.common.PimpedConfig._
import lila.notify.NotifyApi

final class Env(
    config: Config,
    hub: lila.hub.Env,
    detectLanguage: DetectLanguage,
    mongoCache: lila.memo.MongoCache.Builder,
    notifyApi : NotifyApi,
    db: lila.db.Env) {

  private val CollectionQuestion = config getString "collection.question"
  private val CollectionAnswer = config getString "collection.answer"

  private lazy val questionColl = db(CollectionQuestion)

  lazy val api = new QaApi(
    questionColl = questionColl,
    answerColl = db(CollectionAnswer),
    mongoCache = mongoCache,
    notifier = notifier)

  private lazy val notifier = new Notifier(
    notifyApi = notifyApi,
    timeline = hub.actor.timeline)

  lazy val search = new Search(questionColl)

  lazy val forms = new DataForm(hub.actor.captcher, detectLanguage)
}

object Env {

  lazy val current = "qa" boot new Env(
    config = lila.common.PlayApp loadConfig "qa",
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    mongoCache = lila.memo.Env.current.mongoCache,
    notifyApi = lila.notify.Env.current.api,
    db = lila.db.Env.current)
}
