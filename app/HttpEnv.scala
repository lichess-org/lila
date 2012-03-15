package lila.http

import lila.system.SystemEnv
import com.typesafe.config._

final class HttpEnv(config: Config) {

  lazy val system = SystemEnv(config)
}
