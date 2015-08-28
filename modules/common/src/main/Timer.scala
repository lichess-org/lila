package lila.common

object Timer {

  def apply[A](name: String)(f: => Fu[A]): Fu[A] = {
    val start = nowMillis
    loginfo(s"[timer $name] start")
    f.addEffects(
      err => logwarn(s"[timer $name] failed in ${nowMillis - start}ms - ${err.getMessage}"),
      _ => loginfo(s"[timer $name] done in ${nowMillis - start}ms"))
  }
}
