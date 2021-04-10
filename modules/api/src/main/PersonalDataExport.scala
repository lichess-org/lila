package lila.api

import akka.stream.Materializer
import akka.stream.scaladsl._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.chat.Chat
import lila.db.dsl._
import lila.game.Game
import lila.user.User

final class PersonalDataExport(
    securityEnv: lila.security.Env,
    msgEnv: lila.msg.Env,
    forumEnv: lila.forum.Env,
    gameEnv: lila.game.Env,
    chatEnv: lila.chat.Env,
    relationEnv: lila.relation.Env,
    userRepo: lila.user.UserRepo,
    mongoCacheApi: lila.memo.MongoCache.Api
)(implicit ec: ExecutionContext, mat: Materializer) {

  private val lightPerSecond = 60
  private val heavyPerSecond = 30

  def apply(user: User): Source[String, _] = {

    val intro =
      Source.futureSource {
        userRepo.currentOrPrevEmail(user.id) map { email =>
          Source(
            List(
              textTitle(s"Personal data export for ${user.username}"),
              "All dates are UTC",
              bigSep,
              s"Signup date: ${textDate(user.createdAt)}",
              s"Last seen: ${user.seenAt ?? textDate}",
              s"Public profile: ${user.profile.??(_.toString)}",
              s"Email: ${email.??(_.value)}"
            )
          )
        }
      }

    val connections =
      Source(List(textTitle("Connections"))) concat
        securityEnv.store.allSessions(user.id).documentSource().throttle(lightPerSecond, 1 second).map { s =>
          s"${s.date.??(textDate)} ${s.ip} ${s.ua}"
        }

    val followedUsers =
      Source.futureSource {
        relationEnv.api.fetchFollowing(user.id) map { userIds =>
          Source(List(textTitle("Followed players")) ++ userIds)
        }
      }

    val forumPosts =
      Source(List(textTitle("Forum posts"))) concat
        forumEnv.postApi.allByUser(user.id).documentSource().throttle(heavyPerSecond, 1 second).map { p =>
          s"${textDate(p.createdAt)}\n${p.text}$bigSep"
        }

    val privateMessages =
      Source(List(textTitle("Direct messages"))) concat
        msgEnv.api
          .allMessagesOf(user.id)
          .throttle(heavyPerSecond, 1 second)
          .map { case (text, date) =>
            s"${textDate(date)}\n$text$bigSep"
          }

    val gameChats =
      Source(List(textTitle("Game chat messages"))) concat
        gameEnv.gameRepo.coll
          .aggregateWith[Bdoc](
            readPreference = ReadPreference.secondaryPreferred
          ) { framework =>
            import framework._
            List(
              Match($doc(Game.BSONFields.playerUids -> user.id)),
              Project($id(true)),
              PipelineOperator(
                $doc(
                  "$lookup" -> $doc(
                    "from"         -> chatEnv.coll.name,
                    "as"           -> "chat",
                    "localField"   -> "_id",
                    "foreignField" -> "_id"
                  )
                )
              ),
              Unwind("chat"),
              ReplaceRootField("chat"),
              Project($doc("_id" -> false, "l" -> true)),
              Unwind("l"),
              Match("l" $startsWith s"${user.id} ")
            )
          }
          .documentSource()
          .map { doc => doc.string("l").??(_.drop(user.id.size + 1)) + "\n" }
          .throttle(heavyPerSecond, 1 second)

    val outro = Source(List(textTitle("End of data export.")))

    List(intro, connections, followedUsers, forumPosts, privateMessages, gameChats, outro)
      .foldLeft(Source.empty[String])(_ concat _)
  }

  private val bigSep = "\n------------------------------------------\n"

  private def textTitle(t: String) = s"\n${"=" * t.length}\n$t\n${"=" * t.length}\n"

  private val englishDateTimeFormatter = DateTimeFormat forStyle "MS"
  private def textDate(date: DateTime) = englishDateTimeFormatter print date
}
