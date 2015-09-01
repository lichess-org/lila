package lila.common

object Timer {

  def apply[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    logger debug s"[timer $name] start"
    f.addEffects(
      err => logger warn s"[timer $name] failed in ${nowMillis - start}ms - ${err.getMessage}",
      _ => logger debug s"[timer $name] done in ${nowMillis - start}ms")
  }

  private lazy val logger = play.api.Logger("timer")
}
