package lila.streamer

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import Twitch.TwitchId

final class StreamerRepo(
    coll: Coll,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import BsonHandlers.given

  private[streamer] def withColl[A](f: Coll => A): A = f(coll)

  def byId(id: Streamer.Id): Fu[Option[Streamer]] = coll.byId[Streamer](id)
  def byIds(ids: Iterable[Streamer.Id]): Fu[List[Streamer]] = coll.byIds[Streamer, Streamer.Id](ids)

  def delete(user: User): Funit =
    coll.delete.one($id(user.id)).void

  def create(u: User): Funit =
    coll.insert.one(Streamer.make(u)).void.recover(lila.db.ignoreDuplicateKey)

  def update(streamer: Streamer): Funit =
    coll.update.one($id(streamer.id), streamer).void

  def countRequests: Fu[Int] =
    coll.countSel($doc("approval.requested" -> true, "approval.ignored" -> false))

  private[streamer] def setSeenAt(userId: UserId): Funit =
    coll.update.one($id(userId), $set("seenAt" -> nowInstant)).void

  private[streamer] def setLangLiveNow(streams: List[Stream]): Funit =
    val update: coll.UpdateBuilder = coll.update(ordered = false)
    for
      elements <- streams.parallel: s =>
        update.element(
          q = $id(s.streamer.id),
          u = $set(
            "liveAt" -> nowInstant,
            "lastStreamLang" -> s.language
          )
        )
      _ <- elements.nonEmpty.so(update.many(elements).void)
    yield ()

  private[streamer] def approvedTwitchIds(limit: Int = 1000): Fu[Seq[TwitchId]] =
    approvedIds[TwitchId]("twitch.id", limit)

  private[streamer] def approvedYoutubeIds(limit: Int = 1000): Fu[Seq[String]] =
    approvedIds[String]("youtube.channelId", limit)

  private def approvedIds[Id: BSONReader](field: String, limit: Int): Fu[Seq[Id]] =
    coll
      .aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc(field.$exists(true), "approval.granted" -> true)) -> List(
          Sort(Descending("seenAt")),
          Limit(limit),
          Group(BSONNull)("ids" -> PushField(field))
        )
      .map:
        _.so(~_.getAsOpt[Seq[Id]]("ids"))

  private[streamer] def approvedByChannelId(channelId: String): Fu[Option[Streamer]] =
    coll
      .find($doc("youtube.channelId" -> channelId, "approval.granted" -> true))
      .sort($sort.desc("seenAt"))
      .cursor[Streamer]()
      .uno

  private[streamer] def demote(userId: UserId): Fu[Option[Streamer]] =
    coll
      .findAndUpdate(
        $id(userId),
        $set("approval.requested" -> false, "approval.granted" -> false),
        fetchNewObject = true
      )
      .map(_.value.flatMap(_.asOpt[Streamer]))

  // unapprove after 6 weeks if you never streamed (was originally 1 week)
  private[streamer] def autoDemoteFakes: Funit =
    coll.update
      .one(
        $doc(
          "liveAt".$exists(false),
          "approval.granted" -> true,
          "approval.lastGrantedAt".$lt(nowInstant.minusWeeks(6))
        ),
        $set("approval.granted" -> false, "demoted" -> true),
        multi = true
      )
      .void

  private[streamer] def updateYoutubeChannels(
      tubers: List[Youtube.StreamerWithYoutube],
      results: List[Youtube.YoutubeStream]
  ): Funit =
    val bulk = coll.update(ordered = false)
    tubers
      .parallel: tuber =>
        val liveVid = results.find(_.channelId == tuber.youtube.channelId)
        bulk.element(
          q = $id(tuber.streamer.id),
          u = $doc(
            liveVid match
              case Some(v) => $set("youtube.liveVideoId" -> v.videoId) ++ $unset("youtube.pubsubVideoId")
              case None => $unset("youtube.liveVideoId", "youtube.pubsubVideoId")
          )
        )
      .map(bulk.many(_))

  private[streamer] def setTwitchLogin(id: Streamer.Id, login: Twitch.TwitchLogin): Funit =
    coll.update.one($id(id), $set("twitch.login" -> login)).void

  private[streamer] def setYoutubePubsubVideo(id: Streamer.Id, videoId: String): Funit =
    coll.update.one($doc("_id" -> id), $set("youtube.pubsubVideoId" -> videoId)).void

  private[streamer] def unignore(userId: UserId): Funit =
    coll.updateField($id(userId), "approval.ignored", false).void

  private[streamer] def makeCaches =
    val selectListedApproved = $doc("listed" -> true, "approval.granted" -> true)
    val listedIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
        coll.secondary.distinctEasy[Streamer.Id, Set]("_id", selectListedApproved)
    val candidateIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
        coll.secondary
          .distinctEasy[Streamer.Id, Set]("_id", selectListedApproved ++ $doc("liveAt".$exists(false)))
    (listedIds, candidateIds)

  object oauth:
    private def dbKey(platform: Platform): String =
      if platform == "youtube" then "youtube" else "twitch"

    def unlink(streamer: Streamer, platform: Platform): Funit =
      coll.update.one($id(streamer.id), $unset(dbKey(platform)) ++ $set("updatedAt" -> nowInstant)).void

    def linkTwitch(streamer: Streamer, tu: Streamer.Twitch): Fu[String] =
      val clearQuery = $or($doc("twitch.id" -> tu.id), $doc("twitch.login" -> tu.login))
      for
        _ <- coll
          .update(ordered = false)
          .one(clearQuery, $unset("twitch"), upsert = false, multi = true)
        _ <- coll.update
          .one(
            $id(streamer.id),
            $set("twitch" -> tu, "updatedAt" -> nowInstant)
          )
      yield tu.fullUrl

    def linkYoutube(streamer: Streamer, channelId: String): Fu[String] =
      val newYoutube =
        streamer.youtube.fold(Streamer.Youtube(channelId, none, none))(_.copy(channelId = channelId))
      for
        _ <- coll
          .update(ordered = false)
          .one($doc("youtube.channelId" -> channelId), $unset("youtube"), upsert = false, multi = true)
        _ <- coll.update
          .one($id(streamer.id), $set("youtube" -> newYoutube, "updatedAt" -> nowInstant))
      yield newYoutube.fullUrl
