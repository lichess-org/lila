package lila.notify

import reactivemongo.api.ReadPreference
import org.joda.time.DateTime

import lila.common.Bus
import lila.db.dsl._
import lila.hub.actorApi.streamer.NotifiableFollower
import lila.i18n._
import lila.user.User

/* a helper class to encapsulate pref, timeline, and relation dependencies
   so that we may violate them in secret.
 */

final private class StreamStartHelper(
    userRepo: lila.user.UserRepo,
    prefApi: lila.pref.PrefApi,
    timeline: lila.hub.actors.Timeline,
    relationApi: lila.relation.RelationApi
)(implicit
    ec: scala.concurrent.ExecutionContext
) {
  def getNotiflowersAndPush(streamerId: User.ID, streamerName: String): Fu[Iterable[NotifiableFollower]] =
    relationApi.freshFollowersFromSecondary(streamerId, 14).flatMap { followers =>
      timeline ! {
        import lila.hub.actorApi.timeline.{ Propagate, StreamStart }
        Propagate(StreamStart(streamerId, streamerName)) toUsers followers
      }
      lookupNotifiable(streamerName, followers) andThen { pushList =>
        Bus.publish(
          lila.hub.actorApi.streamer.StreamStart(streamerId, pushList.get),
          "streamStart"
        )
      }
    }

  case class NotiflowerViews(notiflowers: Iterable[NotifiableFollower], sid: String, name: String) {

    val noteList: List[Notification] =
      notiflowers map { x => StreamStartNote.make(x.userId, sid, name, x.text) } toList

    val byUser: Map[String, NotifiableFollower] =
      notiflowers.groupMapReduce(_.userId)(identity)((x, _) => x)

    val users: Iterable[String] = byUser.keys

    val notesByUser: Map[String, Notification] =
      noteList.groupMapReduce(_.notifies.value)(identity)((x, _) => x)

    val notesByLang: Map[play.api.i18n.Lang, Iterable[Option[Notification]]] =
      notiflowers.groupMap(_.lang)(nf => notesByUser.get(nf.userId))
  }

  private def lookupNotifiable(
      streamerName: String,
      followers: List[User.ID]
  ): Fu[List[NotifiableFollower]] = {
    prefApi.coll
      .aggregateList(-1, ReadPreference.secondaryPreferred) { framework =>
        import framework._
        Match($inIds(followers) ++ $doc("notification.streamStart" -> $doc("$gt" -> 0))) ->
          List(
            Project($doc("notification.streamStart" -> true)),
            PipelineOperator(
              $lookup.pipeline(
                from = userRepo.coll,
                as = "u",
                local = "_id",
                foreign = "_id",
                pipe = List(
                  $doc("$match"   -> $doc("enabled" -> true)),
                  $doc("$project" -> $doc("lang" -> true, "seenAt" -> true, "_id" -> false))
                )
              )
            ),
            Unwind("u"),
            Sort(Descending("u.seenAt"))
          )
      }
      .map { docs =>
        for {
          doc     <- docs
          id      <- doc string "_id"
          filter  <- doc child "notification" map (_ int "streamStart")
          langStr <- doc child "u" map (_ string "lang")
          seenAt  <- doc child "u" map (_.getAsOpt[DateTime]("seenAt"))

          // doing language translation here hurts me more than it hurts you.
          lang           = I18nLangPicker.byStrOrDefault(langStr)
          text           = I18nKeys.xStartedStreaming.txt(streamerName)(lang)
          recentlyOnline = seenAt.fold(false)(_.compareTo(DateTime.now().minusMinutes(15)) > 0)
        } yield NotifiableFollower(id, streamerName, text, ~filter, lang, recentlyOnline)
      }
  }
}
