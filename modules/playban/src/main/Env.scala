package lila.playban

import com.typesafe.config.Config

final class Env(
    config: Config,
    messenger: lila.message.MessageApi,
    chatApi: lila.chat.ChatApi,
    lightUser: lila.common.LightUser.Getter,
    bus: lila.common.Bus,
    db: lila.db.Env
) {

  private val settings = new {
    val CollectionPlayban = config getString "collection.playban"
  }
  import settings._

  private lazy val feedback = new PlaybanFeedback(
    chatApi = chatApi,
    lightUser = lightUser
  )

  lazy val api = new PlaybanApi(
    coll = db(CollectionPlayban),
    sandbag = new SandbagWatch(messenger),
    feedback = feedback,
    bus = bus
  )
}

object Env {

  lazy val current: Env = "playban" boot new Env(
    config = lila.common.PlayApp loadConfig "playban",
    messenger = lila.message.Env.current.api,
    chatApi = lila.chat.Env.current.api,
    lightUser = lila.user.Env.current.lightUserApi.async,
    bus = lila.common.PlayApp.system.lilaBus,
    db = lila.db.Env.current
  )
}
