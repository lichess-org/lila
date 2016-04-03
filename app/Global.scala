package lila.app

import play.api.{ Application, GlobalSettings }

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    lila.app.Env.current // preload modules
  }
}
