package lila.common

import play.api.Play

object PlayApp {

  def loadConfig = 
    Play.maybeApplication.map(_.configuration.underlying)
      .err("Play application is not started!")
}
