package lila.common

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

object PimpedConfig {

  implicit final class LilaPimpedConfig(config: Config) {

    def millis(name: String): Int = config.getDuration(name, TimeUnit.MILLISECONDS).toInt
    def seconds(name: String): Int = config.getDuration(name, TimeUnit.SECONDS).toInt
    def duration(name: String): FiniteDuration = millis(name).millis
  }
}
