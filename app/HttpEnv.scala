package lila.http

import lila.system.SystemEnv

import play.api.Play

object HttpEnv {

  lazy val static = new SystemEnv {
    protected val config = Play.unsafeApplication.configuration.underlying
  }
}
