package lila.common

object Chronometer {

  def log[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    logger debug s"$name - start"
    f.addEffects(
      err => logger warn s"$name - failed in ${nowMillis - start}ms - ${err.getMessage}",
      _ => logger debug s"$name - done in ${nowMillis - start}ms")
  }

  def result[A](f: => Fu[A]): Fu[(A, Int)] = {
    val start = nowMillis
    f map { _ -> (nowMillis - start).toInt }
  }

  private lazy val logger = play.api.Logger("chrono")
}
