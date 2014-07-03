package lila.qa

import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class Env(
    config: Config,
    db: lila.db.Env) {

  private val CollectionQuestion = config getString "collection.question"
  private val CollectionAnswer = config getString "collection.answer"
  private val NotifyUserId = config getString "notify.user_id"

  private lazy val questionColl = db(CollectionQuestion)

  lazy val api = new QaApi(
    questionColl = questionColl,
    answerColl = db(CollectionAnswer),
    mailer = new Mailer(NotifyUserId))

  lazy val search = new Search(questionColl)

  def forms = DataForms
}

object Env {

  lazy val current = "[boot] qa" describes new Env(
    config = lila.common.PlayApp loadConfig "qa",
    db = lila.db.Env.current)
}
