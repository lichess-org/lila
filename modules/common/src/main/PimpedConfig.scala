package lila.common

import scala.concurrent.duration._

import com.typesafe.config.Config

object PimpedConfig {

  implicit final class LilaPimpedConfig(config: Config) {

    def millis(name: String): Int = config.getMilliseconds(name).toInt
    def seconds(name: String): Int = millis(name) / 1000
    def duration(name: String): FiniteDuration = millis(name).millis
  }
}
