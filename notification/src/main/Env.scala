package lila.notification

import com.typesafe.config.Config

final class Env {

}

object Env {

  lazy val current = new Env
}
