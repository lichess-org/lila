package lila.common

import com.typesafe.config.{ Config, ConfigObject }
import scala.concurrent.duration._
import scalaz.{ Success, Failure }

abstract class ConfigSettings(config: Config) {

  def this(configObject: ConfigObject) = this(configObject.toConfig)

  protected def getInt(name: String) = config getInt name
  protected def getString(name: String) = config getString name
  protected def getMilliseconds(name: String) = config getMilliseconds name
  protected def getBoolean(name: String) = config getBoolean name

  protected def duration(name: String): Duration = millis(name).millis
  protected def millis(name: String): Int = getMilliseconds(name).toInt
  protected def seconds(name: String): Int = millis(name) / 1000

  protected implicit def validAny[A](a: A) = new {
    def valid(f: A ⇒ Valid[A]): A = f(a) match {
      case Success(a)   ⇒ a
      case Failure(err) ⇒ throw new Invalid(err.shows)
    }
  }

  private class Invalid(msg: String) extends Exception(msg)
}
