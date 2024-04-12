package lila.notify

import akka.stream.Materializer
import akka.stream.scaladsl.*

import lila.db.dsl.{ *, given }
import lila.core.user.UserRepo

final private class NotifyCli(api: NotifyApi, userRepo: UserRepo)(using Materializer, Executor)
    extends lila.common.Cli:

  def process =
    case "notify" :: "url" :: "users" :: users :: url :: words =>
      val userIds = users.split(',').flatMap(UserStr.read).map(_.id).toIndexedSeq
      notifyUrlTo(Source(userIds), url, words)
    case "notify" :: "url" :: "titled" :: url :: words =>
      val userIds = enabledTitledSource($id(true).some)
        .mapConcat(_.getAsOpt[UserId]("_id").toList)
      notifyUrlTo(userIds, url, words)

  private def notifyUrlTo(userIds: Source[UserId, ?], url: String, words: List[String]) =
    val title        = words.takeWhile(_ != "|").mkString(" ").some.filter(_.nonEmpty)
    val text         = words.dropWhile(_ != "|").drop(1).mkString(" ").some.filter(_.nonEmpty)
    val notification = lila.core.notify.GenericLink(url, title, text, lila.common.Icon.InfoCircle.value)
    userIds
      .grouped(20)
      .mapAsyncUnordered(1): uids =>
        api.notifyManyIgnoringPrefs(uids, notification).inject(uids.size)
      .runWith(Sink.fold(0)(_ + _))
      .map: nb =>
        s"Notified $nb users"

  private def enabledTitledSource(proj: Option[Bdoc]) =
    import lila.core.user.BSONFields
    import reactivemongo.akkastream.cursorProducer
    userRepo.coll
      .find(
        $doc(
          BSONFields.enabled -> true,
          BSONFields.title -> $doc(
            "$exists" -> true,
            "$nin"    -> List(chess.PlayerTitle.LM, chess.PlayerTitle.BOT)
          )
        ),
        proj
      )
      .cursor[Bdoc](ReadPref.priTemp)
      .documentSource()
