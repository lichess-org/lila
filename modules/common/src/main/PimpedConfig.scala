package lila.common

import com.typesafe.config.Config
import scala.concurrent.duration._
// import scalaz.{ Success, Failure }

object PimpedConfig {

  implicit final class LilaPimpedConfig(config: Config) {

    def millis(name: String): Int = config.getMilliseconds(name).toInt
    def seconds(name: String): Int = millis(name) / 1000
    def duration(name: String): FiniteDuration = millis(name).millis
  }

  // protected implicit def validAny[A](a: A) = new {
  //   def valid(f: A ⇒ Valid[A]): A = f(a) match {
  //     case Success(a)   ⇒ a
  //     case Failure(err) ⇒ throw new Invalid(err.shows)
  //   }
  // }

  // private class Invalid(msg: String) extends Exception(msg)
}
