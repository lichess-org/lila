package lila.system

import model._
import memo._
import scalaz.effects._

final class Pinger(
    aliveMemo: AliveMemo,
    usernameMemo: UsernameMemo,
    watcherMemo: WatcherMemo) {

  def ping(
    username: Option[String],
    playerKey: Option[String],
    watcherKey: Option[String],
    getNbWatchers: Option[String]): IO[Map[String, Any]] = for {
    _ ← optionIO(playerKey, aliveMemo.put)
    _ ← optionIO(username, usernameMemo.put)
    _ ← optionIO(watcherKey, watcherMemo.put)
  } yield flatten(Map(
    "nbp" -> Some(aliveMemo.count),
    "nbw" -> (getNbWatchers map watcherMemo.count)
  ))

  private def flatten[A, B](map: Map[A, Option[B]]): Map[A, B] = map collect {
    case (k, Some(v)) ⇒ k -> v
  } toMap

  private def optionIO[A](oa: Option[A], f: A ⇒ IO[Unit]) =
    oa map f getOrElse io(Unit)
}
