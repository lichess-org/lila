package lila.common

object Chronometer {

  def apply[A](msg: String)(f: => Fu[A]): Fu[A] = {
    loginfo(s"[chrono $msg] Start")
    val startAt = nowMillis
    f ~ (_.effectFold(
      err => loginfo(s"[chrono $msg] Failed in ${nowMillis - startAt} ms with $err"),
      res => loginfo(s"[chrono $msg] Success in ${nowMillis - startAt} ms")
    ))
  }
}
