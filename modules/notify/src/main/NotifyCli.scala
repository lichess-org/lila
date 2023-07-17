package lila.notify

import akka.stream.Materializer
import akka.stream.scaladsl.*

import lila.user.UserRepo
import lila.db.dsl.{ *, given }
import lila.common.LilaStream

final private class NotifyCli(api: NotifyApi, userRepo: UserRepo)(using Materializer, Executor)
    extends lila.common.Cli:

  def process =
    case "notify" :: "url" :: "users" :: users :: url :: words =>
      val userIds = users.split(',').flatMap(UserStr.read).map(_.id)
      notifyUrlTo(Source(userIds), url, words)
    case "notify" :: "url" :: "titled" :: url :: words =>
      val userIds = userRepo
        .enabledTitledCursor($id(true).some)
        .documentSource()
        .mapConcat(_.getAsOpt[UserId]("_id").toList)
      notifyUrlTo(userIds, url, words)

  private def notifyUrlTo(userIds: Source[UserId, ?], url: String, words: List[String]) =
    val title        = words.takeWhile(_ != "|").mkString(" ").some.filter(_.nonEmpty)
    val text         = words.dropWhile(_ != "|").drop(1).mkString(" ").some.filter(_.nonEmpty)
    val notification = GenericLink(url, title, text, lila.common.licon.InfoCircle)
    userIds
      .grouped(20)
      .mapAsyncUnordered(1): uids =>
        api.notifyManyIgnoringPrefs(uids, notification) inject uids.size
      .runWith(Sink.fold(0)(_ + _))
      .map: nb =>
        s"Notified $nb users"
