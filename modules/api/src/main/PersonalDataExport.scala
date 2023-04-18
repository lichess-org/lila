package lila.api

import akka.stream.Materializer
import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }
import lila.game.Game
import lila.streamer.Streamer
import lila.user.User
import lila.coach.Coach

final class PersonalDataExport(
    securityEnv: lila.security.Env,
    msgEnv: lila.msg.Env,
    forumEnv: lila.forum.Env,
    gameEnv: lila.game.Env,
    roundEnv: lila.round.Env,
    chatEnv: lila.chat.Env,
    relationEnv: lila.relation.Env,
    userRepo: lila.user.UserRepo,
    ublogApi: lila.ublog.UblogApi,
    streamerApi: lila.streamer.StreamerApi,
    coachApi: lila.coach.CoachApi,
    picfitUrl: lila.memo.PicfitUrl
)(using Executor, Materializer):

  private val lightPerSecond = 60
  private val heavyPerSecond = 30

  def apply(user: User): Source[String, ?] =

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
          Source(List(textTitle("Followed players")) ++ userIds.map(_.value))
        }
      }

    val streamer = Source.futureSource {
      streamerApi.find(user) map {
        _.map(_.streamer).?? { s =>
          List(textTitle("Streamer profile")) :::
            List(
              "name"     -> s.name,
              "image"    -> s.picture.??(p => picfitUrl.thumbnail(p, Streamer.imageSize, Streamer.imageSize)),
              "headline" -> s.headline.??(_.value),
              "description" -> s.description.??(_.value),
              "twitch"      -> s.twitch.??(_.fullUrl),
              "youTube"     -> s.youTube.??(_.fullUrl),
              "createdAt"   -> textDate(s.createdAt),
              "updatedAt"   -> textDate(s.updatedAt),
              "seenAt"      -> textDate(s.seenAt),
              "liveAt"      -> s.liveAt.??(textDate)
            ).map { case (k, v) =>
              s"$k: $v"
            }
        }
      } map Source.apply
    }

    val coach = Source.futureSource {
      coachApi.find(user) map {
        _.map(_.coach).?? { c =>
          List(textTitle("Coach profile")) :::
            c.profile.textLines :::
            List(
              "image"     -> c.picture.??(p => picfitUrl.thumbnail(p, Coach.imageSize, Coach.imageSize)),
              "languages" -> c.languages.mkString(", "),
              "createdAt" -> textDate(c.createdAt),
              "updatedAt" -> textDate(c.updatedAt)
            ).map { case (k, v) =>
              s"$k: $v"
            }
        }
      } map Source.apply
    }

    val coachReviews =
      Source.futureSource {
        coachApi.reviews.allByPoster(user) map { reviews =>
          Source(List(textTitle("Coach reviews")) ::: reviews.list.map { r =>
            s"${r.coachId}: ${r.text}\n"
          })
        }
      }

    val forumPosts =
      Source(List(textTitle("Forum posts"))) concat
        forumEnv.postRepo.allByUserCursor(user).documentSource().throttle(heavyPerSecond, 1 second).map { p =>
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

    def gameChatsLookup(lookup: Bdoc) =
      gameEnv.gameRepo.coll
        .aggregateWith[Bdoc](readPreference = temporarilyPrimary) { framework =>
          import framework.*
          List(
            Match($doc(Game.BSONFields.playerUids -> user.id)),
            Project($id(true)),
            PipelineOperator(lookup),
            Unwind("chat"),
            ReplaceRootField("chat"),
            Project($doc("_id" -> false, "l" -> true)),
            Unwind("l"),
            Match("l".$startsWith(s"${user.id} ", "i"))
          )
        }
        .documentSource()
        .map { doc => doc.string("l").??(_.drop(user.id.value.size + 1)) }
        .throttle(heavyPerSecond, 1 second)

    val privateGameChats =
      Source(List(textTitle("Private game chat messages"))) concat
        gameChatsLookup(
          $lookup.simple(
            from = chatEnv.coll,
            as = "chat",
            local = "_id",
            foreign = "_id"
          )
        )

    val spectatorGameChats =
      Source(List(textTitle("Spectator game chat messages"))) concat
        gameChatsLookup(
          $lookup.pipelineFull(
            from = chatEnv.coll.name,
            as = "chat",
            let = $doc("id" -> $doc("$concat" -> $arr("$_id", "/w"))),
            pipe = List($doc("$match" -> $expr($doc("$eq" -> $arr("$_id", "$$id")))))
          )
        )

    val gameNotes =
      Source(List(textTitle("Game notes"))) concat
        gameEnv.gameRepo.coll
          .aggregateWith[Bdoc](
            readPreference = temporarilyPrimary
          ) { framework =>
            import framework.*
            List(
              Match($doc(Game.BSONFields.playerUids -> user.id)),
              Project($id(true)),
              PipelineOperator(
                $lookup.pipelineFull(
                  from = roundEnv.noteApi.collName,
                  as = "note",
                  let = $doc("id" -> $doc("$concat" -> $arr("$_id", user.id))),
                  pipe = List($doc("$match" -> $expr($doc("$eq" -> $arr("$_id", "$$id")))))
                )
              ),
              Unwind("note"),
              ReplaceRootField("note"),
              Project($doc("_id" -> false, "t" -> true))
            )
          }
          .documentSource()
          .map { doc => ~doc.string("t") }
          .throttle(heavyPerSecond, 1 second)

    val ublogPosts =
      Source(List(textTitle("Blog posts"))) concat
        ublogApi
          .postCursor(user)
          .documentSource()
          .map { post =>
            List(
              "date"   -> textDate(post.created.at),
              "title"  -> post.title,
              "intro"  -> post.intro,
              "body"   -> post.markdown,
              "image"  -> post.image.??(i => lila.ublog.UblogPost.thumbnail(picfitUrl, i.id, _.Large)),
              "topics" -> post.topics.mkString(", ")
            ).map { case (k, v) =>
              s"$k: $v"
            }.mkString("\n") + bigSep
          }
          .throttle(heavyPerSecond, 1 second)

    val outro = Source(List(textTitle("End of data export.")))

    List[Source[String, _]](
      intro,
      connections,
      followedUsers,
      streamer,
      coach,
      coachReviews,
      ublogPosts,
      forumPosts,
      privateMessages,
      // privateGameChats,
      spectatorGameChats,
      gameNotes,
      outro
    ).foldLeft(Source.empty[String])(_ concat _)
      .keepAlive(15 seconds, () => " ")

  private val bigSep = "\n------------------------------------------\n"

  private def textTitle(t: String) = s"\n${"=" * t.length}\n$t\n${"=" * t.length}\n"

  import java.time.format.{ DateTimeFormatter, FormatStyle }
  private val englishDateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
  private def textDate(date: Instant) = englishDateTimeFormatter print date
