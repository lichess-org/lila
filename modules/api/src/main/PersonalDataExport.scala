package lila.api

import akka.stream.Materializer
import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.coach.Coach
import lila.db.dsl.{ *, given }
import lila.game.Game
import lila.streamer.Streamer

final class PersonalDataExport(
    securityEnv: lila.security.Env,
    msgEnv: lila.msg.Env,
    forumEnv: lila.forum.Env,
    gameEnv: lila.game.Env,
    noteApi: lila.round.NoteApi,
    chatEnv: lila.chat.Env,
    relationEnv: lila.relation.Env,
    userRepo: lila.user.UserRepo,
    ublogApi: lila.ublog.UblogApi,
    streamerApi: lila.streamer.StreamerApi,
    coachApi: lila.coach.CoachApi,
    appealApi: lila.appeal.AppealApi,
    shutupEnv: lila.shutup.Env,
    modLogApi: lila.mod.ModlogApi,
    reportEnv: lila.report.Env,
    titleEnv: lila.title.Env,
    picfitUrl: lila.memo.PicfitUrl
)(using Executor, Materializer):

  private val lightPerSecond = 60
  private val heavyPerSecond = 30

  def apply(user: User): Source[String, ?] =

    val intro = Source.futureSource:
      userRepo.currentOrPrevEmail(user.id).map { email =>
        Source(
          List(
            textTitle(s"Personal data export for ${user.username}"),
            "All dates are UTC",
            bigSep,
            s"Signup date: ${textDate(user.createdAt)}",
            s"Last seen: ${user.seenAt.so(textDate)}",
            s"Public profile: ${user.profile.so(_.toString)}",
            s"Email: ${email.so(_.value)}"
          )
        )
      }

    val connections =
      Source(List(textTitle("Connections"))).concat(
        securityEnv.store.allSessions(user.id).documentSource().throttle(lightPerSecond, 1.second).map { s =>
          s"${s.date.so(textDate)} ${s.ip} ${s.ua}"
        }
      )

    val followedUsers = Source.futureSource:
      relationEnv.api.fetchFollowing(user.id).map { userIds =>
        Source(List(textTitle("Followed players")) ++ userIds.map(_.value))
      }

    val streamer = Source.futureSource:
      streamerApi
        .find(user)
        .map:
          _.map(_.streamer).so: s =>
            List(textTitle("Streamer profile")) :::
              List(
                "name" -> s.name,
                "image" -> s.picture.so(p => picfitUrl.thumbnail(p, Streamer.imageSize, Streamer.imageSize)),
                "headline" -> s.headline.so(_.value),
                "description" -> s.description.so(_.value),
                "twitch" -> s.twitch.so(_.fullUrl),
                "youTube" -> s.youTube.so(_.fullUrl),
                "createdAt" -> textDate(s.createdAt),
                "updatedAt" -> textDate(s.updatedAt),
                "seenAt" -> textDate(s.seenAt),
                "liveAt" -> s.liveAt.so(textDate)
              ).map: (k, v) =>
                s"$k: $v"
        .map(Source.apply)

    val coach = Source.futureSource:
      coachApi
        .find(user)
        .map:
          _.map(_.coach).so: c =>
            List(textTitle("Coach profile")) :::
              c.profile.textLines :::
              List(
                "image" -> c.picture.so(p => picfitUrl.thumbnail(p, Coach.imageSize, Coach.imageSize)),
                "languages" -> c.languages.mkString(", "),
                "createdAt" -> textDate(c.createdAt),
                "updatedAt" -> textDate(c.updatedAt)
              ).map: (k, v) =>
                s"$k: $v"
        .map(Source.apply)

    val forumPosts =
      Source(List(textTitle("Forum posts"))).concat(
        forumEnv.postRepo
          .allByUserCursor(user)
          .documentSource()
          .throttle(heavyPerSecond, 1.second)
          .map(p => s"${textDate(p.createdAt)}\n${p.text}$bigSep")
      )

    val privateMessages =
      Source(List(textTitle("Direct messages"))).concat(
        msgEnv.api
          .allMessagesOf(user.id)
          .throttle(heavyPerSecond, 1.second)
          .map: (text, date) =>
            s"${textDate(date)}\n$text$bigSep"
      )

    def gameChatsLookup(lookup: Bdoc) =
      gameEnv.gameRepo.coll
        .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
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
        .documentSource()
        .map { _.string("l").so(_.drop(user.id.value.size + 1)) }
        .throttle(heavyPerSecond, 1.second)

    val spectatorGameChats =
      Source(List(textTitle("Spectator game chat messages"))).concat(gameChatsLookup:
        $lookup.pipelineFull(
          from = chatEnv.coll.name,
          as = "chat",
          let = $doc("id" -> $doc("$concat" -> $arr("$_id", "/w"))),
          pipe = List($doc("$match" -> $expr($doc("$eq" -> $arr("$_id", "$$id")))))
        ))

    val gameNotes =
      Source(List(textTitle("Game notes"))).concat(
        gameEnv.gameRepo.coll
          .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
            import framework.*
            List(
              Match($doc(Game.BSONFields.playerUids -> user.id)),
              Project($id(true)),
              PipelineOperator(
                $lookup.pipelineFull(
                  from = noteApi.collName,
                  as = "note",
                  let = $doc("id" -> $doc("$concat" -> $arr("$_id", user.id))),
                  pipe = List($doc("$match" -> $expr($doc("$eq" -> $arr("$_id", "$$id")))))
                )
              ),
              Unwind("note"),
              ReplaceRootField("note"),
              Project($doc("_id" -> false, "t" -> true))
            )
          .documentSource()
          .map(~_.string("t"))
          .throttle(heavyPerSecond, 1.second)
      )

    val ublogPosts =
      Source(List(textTitle("Blog posts"))).concat(
        ublogApi
          .postCursor(user)
          .documentSource()
          .map: post =>
            List(
              "date" -> textDate(post.created.at),
              "title" -> post.title,
              "intro" -> post.intro,
              "body" -> post.markdown,
              "image" -> post.image.so(i => lila.ublog.UblogPost.thumbnail(picfitUrl, i.id, _.Size.Large)),
              "topics" -> post.topics.mkString(", ")
            ).map: (k, v) =>
              s"$k: $v"
            .mkString("\n") + bigSep
          .throttle(heavyPerSecond, 1.second)
      )

    val appeals = Source.futureSource:
      appealApi
        .byId(user)
        .map: opt =>
          Source:
            opt.so: appeal =>
              List(textTitle("Appeal")) ++ appeal.msgs.map: msg =>
                val author = if appeal.isAbout(msg.by) then "you" else "Lichess"
                s"${textDate(msg.at)} by $author\n${msg.text}$bigSep"

    val reports = Source.futureSource:
      reportEnv.api
        .personalExport(user)
        .map: atoms =>
          Source:
            List(textTitle("Reports you created")) :::
              atoms.map: a =>
                s"${textDate(a.at)}\n${a.text}$bigSep"

    val dubiousChats = Source.futureSource:
      shutupEnv.api
        .getPublicLines(user.id)
        .map: lines =>
          Source:
            List(textTitle("Dubious public chats")) :::
              lines.map: l =>
                s"${textDate(l.date)}\n${l.text}$bigSep"

    val timeouts = Source.futureSource:
      modLogApi
        .timeoutPersonalExport(user.id)
        .map: modlogs =>
          Source:
            List(textTitle("Messages you were timeouted for")) :::
              modlogs.map: m =>
                // do not export the reason of the timeout as not personal data
                val timeoutMsg = m.details.so(_.split(":").drop(1).mkString(":").trim())
                s"${textDate(m.date)}\n${timeoutMsg}$bigSep"

    val titleRequests = Source.futureSource:
      titleEnv.api
        .allOf(user)
        .map: reqs =>
          Source:
            List(textTitle("Title request")) ++ reqs.map: req =>
              import req.data.*
              s"""Title: $title
              | Real name:  $realName
              | FIDE ID: ${fideId | "-"}
              | Federation URL: ${federationUrl | "-"}
              | Public: $public
              | Coach: ${req.data.coach}
              | Comment: ${comment | "-"}
              | $bigSep""".stripMargin

    val outro = Source(List(textTitle("End of data export.")))

    List[Source[String, ?]](
      intro,
      titleRequests,
      connections,
      followedUsers,
      streamer,
      coach,
      ublogPosts,
      forumPosts,
      privateMessages,
      spectatorGameChats,
      gameNotes,
      reports,
      dubiousChats,
      timeouts,
      appeals,
      outro
    ).foldLeft(Source.empty[String])(_ concat _)
      .keepAlive(15.seconds, () => " ")

  private val bigSep = "\n------------------------------------------\n"

  private def textTitle(t: String) = s"\n${"=" * t.length}\n$t\n${"=" * t.length}\n"

  import java.time.format.{ DateTimeFormatter, FormatStyle }
  private val englishDateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
  private def textDate(date: Instant) = englishDateTimeFormatter.print(date)
