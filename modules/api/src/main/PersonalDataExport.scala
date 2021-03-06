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
    mongoCacheApi: lila.memo.MongoCache.Api
)(implicit ec: ExecutionContext, mat: Materializer) {

  def apply(user: User) = cache get user.id

  private val cache = mongoCacheApi[User.ID, String](64, "personal-data-export", 1 day, identity) { loader =>
    _.expireAfterAccess(1 minute)
      .buildAsyncFuture {
        loader(fetch)
      }
  }

  private def fetch(userId: User.ID) =
    for {
      sessions  <- securityEnv.store.allSessions(userId)
      posts     <- forumEnv.postApi.allByUser(userId)
      msgs      <- msgEnv.api.allMessagesOf(userId)
      following <- relationEnv.api.fetchFollowing(userId)
      chats <-
        gameEnv.gameRepo.coll
          .find($doc(Game.BSONFields.playerUids -> userId), $id(true).some)
          .cursor[Bdoc](ReadPreference.secondaryPreferred)
          .documentSource(90_000)
          .mapConcat { doc =>
            doc string "_id" toList
          }
          .mapAsyncUnordered(8) { id =>
            chatEnv.api.userChat.findLinesBy(Chat.Id(id), userId)
          }
          .mapConcat(identity)
          .runWith(Sink.seq)
    } yield List(
      render.connections(sessions),
      render.followedUsers(following),
      render.forumPosts(posts),
      render.privateMessages(msgs),
      render.gameChats(chats)
    ).flatten mkString "\n\n"

  private object render {

    def connections(sessions: List[lila.security.UserSession]) =
      List(
        textTitle(s"${sessions.size} Connections"),
        sessions.map { s =>
          s"${s.ip} ${s.date.??(textDate)}\n${s.ua}"
        } mkString "\n\n"
      )

    def forumPosts(posts: List[lila.forum.Post]) =
      List(
        textTitle(s"${posts.size} Forum posts"),
        posts.map { p =>
          s"${textDate(p.createdAt)}\n${p.text}"
        } mkString bigSep
      )

    def privateMessages(msgs: Seq[(String, DateTime)]) =
      List(
        textTitle(s"${msgs.size} Direct messages"),
        msgs.map { case (text, date) =>
          s"${textDate(date)}\n$text"
        } mkString bigSep
      )

    def gameChats(lines: Seq[String]) =
      List(
        textTitle(s"${lines.size} Game chat messages"),
        lines mkString bigSep
      )

    def followedUsers(userIds: Iterable[User.ID]) =
      List(
        textTitle(s"${userIds.size} Followed players"),
        userIds mkString "\n"
      )

    private val bigSep = "\n\n------------------------------------------\n\n"

    private def textTitle(t: String) = s"\n\n${"=" * t.length}\n$t\n${"=" * t.length}\n\n\n"

    private val englishDateTimeFormatter = DateTimeFormat forStyle "MS"
    private def textDate(date: DateTime) = englishDateTimeFormatter print date
  }
}
