package lila.http

import lila.system.SystemEnv
import com.typesafe.config.Config

final class HttpEnv(c: Config) extends SystemEnv {

  protected val config = c
}
