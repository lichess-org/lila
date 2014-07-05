package lila.qa

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    hub: lila.hub.Env,
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

  def forms = DataForms
}

object Env {

  lazy val current = "[boot] qa" describes new Env(
    config = lila.common.PlayApp loadConfig "qa",
    hub = lila.hub.Env.current,
    db = lila.db.Env.current)
}
