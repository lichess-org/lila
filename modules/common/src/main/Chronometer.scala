package lila.common

object Chronometer {

  def apply[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    logger debug s"$name - start"
    f.addEffects(
      err => logger warn s"$name - failed in ${nowMillis - start}ms - ${err.getMessage}",
      _ => logger debug s"$name - done in ${nowMillis - start}ms")
  }

  private lazy val logger = play.api.Logger("chrono")
}
