package lila.streamer

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*

final class StreamerRepo(
    coll: Coll,
    cacheApi: lila.memo.CacheApi,
    userApi: lila.core.user.UserApi,
    subsRepo: lila.core.relation.SubscriptionRepo
)(using Executor, Scheduler):

  import BsonHandlers.given

  def withColl[A](f: Coll => A): A = f(coll)

  def byId(id: Streamer.Id): Fu[Option[Streamer]] = coll.byId[Streamer](id)
  def byIds(ids: Iterable[Streamer.Id]): Fu[List[Streamer]] = coll.byIds[Streamer, Streamer.Id](ids)

  def find(username: UserStr): Fu[Option[Streamer.WithUser]] =
    userApi.byId(username).flatMapz(find)

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    byId(user.id.into(Streamer.Id)).mapz: streamer =>
      Streamer.WithUser(streamer, user).some

  def findOrInit(user: User): Fu[Option[Streamer.WithUser]] =
    find(user).orElse:
      val s = Streamer.WithUser(Streamer.make(user), user)
      coll.insert.one(s.streamer).inject(s.some)

  def delete(user: User): Funit =
    coll.delete.one($id(user.id)).void

  def create(u: User): Funit =
    coll.insert.one(Streamer.make(u)).void.recover(lila.db.ignoreDuplicateKey)

  def update(streamer: Streamer): Funit =
    coll.update.one($id(streamer.id), streamer).void

  def withUsers(live: LiveStreams)(using me: Option[MyId]): Fu[List[Streamer.WithUserAndStream]] = for
    users <- userApi.byIds(live.streams.map(_.streamer.userId))
    subs <- me.so(subsRepo.filterSubscribed(_, users.map(_.id)))
  yield live.streams.flatMap: s =>
    users
      .find(_.is(s.streamer))
      .map:
        Streamer.WithUserAndStream(s.streamer, _, s.some, subs(s.streamer.userId))

  def setTwitchLogin(id: Streamer.Id, login: String): Funit =
    coll.update.one($id(id), $set("twitch.login" -> login)).void

  def setSeenAt(user: User): Funit =
    coll.update.one($id(user.id), $set("seenAt" -> nowInstant)).void

  def setLangLiveNow(streams: List[Stream]): Funit =
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

  def demote(userId: UserId): Fu[Option[Streamer]] =
    coll
      .findAndUpdate(
        $id(userId),
        $set("approval.requested" -> false, "approval.granted" -> false),
        fetchNewObject = true
      )
      .map(_.value.flatMap(_.asOpt[Streamer]))

  def countRequests: Fu[Int] = coll.countSel:
    $doc(
      "approval.requested" -> true,
      "approval.ignored" -> false
    )

  // unapprove after 6 weeks if you never streamed (was originally 1 week)
  def autoDemoteFakes: Funit =
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

  def approvedTwitchIds(): Fu[Seq[String]] =
    coll
      .find(
        $doc("twitch.id".$exists(true), "approval.granted" -> true),
        $doc("twitch.id" -> true).some
      )
      .sort($doc("lastSeenAt" -> -1))
      .cursor[Bdoc]()
      .list(limit = 3200) // 3200 is not arbitrary. we have 3 subs per streamer and the limit is 10k
      .map(_.flatMap(_.getAsOpt[Bdoc]("twitch").flatMap(_.string("id"))))

  object oauth:
    private def dbKey(platform: Platform): String =
      if platform == "youtube" then "youTube" else "twitch"

    def unlink(streamer: Streamer, platform: Platform): Funit =
      coll.update.one($id(streamer.id), $unset(dbKey(platform)) ++ $set("updatedAt" -> nowInstant)).void

    def linkTwitch(streamer: Streamer, id: String, login: String): Fu[String] =
      val clearQuery = $or($doc("twitch.id" -> id), $doc("twitch.login" -> login))
      for
        _ <- coll
          .update(ordered = false)
          .one(clearQuery, $unset("twitch"), upsert = false, multi = true)
        _ <- coll.update
          .one($id(streamer.id), $set("twitch" -> Streamer.Twitch(id, login), "updatedAt" -> nowInstant))
      yield lila.streamer.Streamer.Twitch(id, login).fullUrl

    def linkYoutube(streamer: Streamer, channelId: String): Fu[String] =
      val newYoutube =
        streamer.youTube.fold(Streamer.YouTube(channelId, none, none))(_.copy(channelId = channelId))
      for
        _ <- coll
          .update(ordered = false)
          .one($doc("youTube.channelId" -> channelId), $unset("youTube"), upsert = false, multi = true)
        _ <- coll.update
          .one($id(streamer.id), $set("youTube" -> newYoutube, "updatedAt" -> nowInstant))
      yield newYoutube.fullUrl

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
