package lila.qa

import com.typesafe.config.Config
import lila.common.DetectLanguage
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    hub: lila.hub.Env,
    detectLanguage: DetectLanguage,
    db: lila.db.Env) {

  private val CollectionQuestion = config getString "collection.question"
  private val CollectionAnswer = config getString "collection.answer"
  private val NotifierSender = config getString "notifier.sender"

  private lazy val questionColl = db(CollectionQuestion)

  lazy val api = new QaApi(
    questionColl = questionColl,
    answerColl = db(CollectionAnswer),
    notifier = notifier)

  private lazy val notifier = new Notifier(
    sender = NotifierSender,
    messenger = hub.actor.messenger,
    timeline = hub.actor.timeline)

  lazy val search = new Search(questionColl)

  lazy val forms = new DataForm(hub.actor.captcher, detectLanguage)
}

object Env {

  lazy val current = "[boot] qa" describes new Env(
    config = lila.common.PlayApp loadConfig "qa",
    hub = lila.hub.Env.current,
    detectLanguage = DetectLanguage(lila.common.PlayApp loadConfig "detectlanguage"),
    db = lila.db.Env.current)
}
