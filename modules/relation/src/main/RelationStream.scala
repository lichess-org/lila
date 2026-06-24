package lila.relation

import akka.stream.scaladsl.*
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }
import lila.core.user.UserRepo
import reactivemongo.api.bson.BSONDocumentReader
import lila.core.LightUser
import lila.core.socket.IsOnline
import play.api.libs.json.JsObject

final class RelationStream(colls: Colls, userRepo: UserRepo, isOnline: IsOnline)(using
    Executor,
    akka.stream.Materializer
):

  private val coll = colls.relation

  def follow(perSecond: MaxPerSecond)(using me: Me): Source[Seq[UserId], ?] =
    coll
      .find(
        $doc(F.from -> me.userId, "r" -> lila.core.relation.Relation.Follow),
        $doc(F.to -> true, "_id" -> false).some
      )
      .batchSize(perSecond.value)
      .cursor[Bdoc](ReadPref.sec)
      .documentSource()
      .grouped(perSecond.value)
      .map(_.flatMap(_.getAsOpt[UserId](F.to)))
      .throttle(1, 1.second)

  private final class RecentlySeen(
      val user: LightUser,
      val seenAt: Option[Instant],
      val online: Boolean,
      val playing: Boolean
  ):
    def isBot = user.title.contains(chess.PlayerTitle.BOT)

  def recentlySeenList(nb: Int, projection: Bdoc, isPlaying: UserId => Boolean)(using
      reader: BSONDocumentReader[LightUser],
      me: Me
  ): Fu[Seq[JsObject]] =
    recentlySeenSource(nb, projection, isPlaying)
      .runWith(Sink.seq)
      .map:
        _.sortBy: f =>
          (!f.online, f.isBot)
        .take(nb)
      .map:
        _.map: f =>
          lila.common.Json.lightUser
            .writeNoId(f.user)
            .add("seenAt", f.seenAt)
            .add("online", f.online)
            .add("playing", f.online && isPlaying(f.user.id))

  private def recentlySeenSource(nb: Int, projection: Bdoc, isPlaying: UserId => Boolean)(using
      reader: BSONDocumentReader[LightUser],
      me: Me
  ): Source[RecentlySeen, ?] =
    val canBeOnlineSince = nowInstant.minusMinutes(60)
    coll
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Match($doc(F.from -> me.userId, "r" -> lila.core.relation.Relation.Follow)),
          PipelineOperator(
            $lookup.simple(
              from = userRepo.coll,
              as = "user",
              local = F.to,
              foreign = "_id",
              pipe = List(
                $doc("$match" -> $doc("enabled" -> true)),
                $doc("$project" -> (projection ++ $doc("seenAt" -> true)))
              )
            )
          ),
          Project($doc("user" -> true, "_id" -> false)),
          UnwindField("user"),
          ReplaceRootField("user"),
          Sort(Descending("seenAt")),
          Limit(nb.atLeast(20))
        )
      .documentSource()
      .mapConcat: doc =>
        for
          user <- doc.asOpt[LightUser]
          seenAt = doc.getAsOpt[Instant]("seenAt")
          online = seenAt.exists(_.isAfter(canBeOnlineSince)) && isOnline.exec(user.id)
          playing = online && isPlaying(user.id)
        yield RecentlySeen(user, seenAt, online, playing)

  private object F:
    val from = "u1"
    val to = "u2"
