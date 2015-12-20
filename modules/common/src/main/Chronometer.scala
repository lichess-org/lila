package lila.common

object Chronometer {

  case class Lap[A](result: A, millis: Int) {
    def tuple = result -> millis

    def resultAndLogIfSlow(ms: Int, namespace: => String)(msg: Lap[A] => String): A = {
      if (millis > ms) play.api.Logger(namespace).debug(msg(this))
      result
    }
  }

  def log[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    logger debug s"$name - start"
    f.addEffects(
      err => logger warn s"$name - failed in ${nowMillis - start}ms - ${err.getMessage}",
      _ => logger debug s"$name - done in ${nowMillis - start}ms")
  }

  def result[A](f: => Fu[A]): Fu[Lap[A]] = {
    val start = nowMillis
    f map {
      Lap(_, (nowMillis - start).toInt)
    }
  }

  private lazy val logger = play.api.Logger("chrono")
}
