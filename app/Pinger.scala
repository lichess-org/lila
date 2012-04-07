package lila

import model._
import memo._
import scalaz.effects._

final class Pinger(
    aliveMemo: AliveMemo,
    watcherMemo: WatcherMemo) {

  def ping(
    username: Option[String],
    playerKey: Option[String],
    watcherKey: Option[String],
    getNbWatchers: Option[String]): IO[String] = for {
    _ ← optionIO(playerKey, aliveMemo.put)
    _ ← optionIO(watcherKey, watcherMemo.put)
  } yield fastJson(Map(
    "nbw" -> (getNbWatchers map watcherMemo.count)
  ))

  private def fastJson(m: Map[String, Option[Int]]): String = "{%s}" format {
    m collect {
      case (k, Some(v)) ⇒ """"%s": %d""".format(k, v)
    } mkString ","
  }

  private def optionIO[A](oa: Option[A], f: A ⇒ IO[Unit]) =
    oa map f getOrElse io()
}
