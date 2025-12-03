package lila.streamer

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.memo.PicfitApi

final class StreamerApi(
    val repo: StreamerRepo,
    userApi: lila.core.user.UserApi,
    picfitApi: PicfitApi,
    notifyApi: lila.core.notify.NotifyApi,
    subsRepo: lila.core.relation.SubscriptionRepo,
    ytApi: YoutubeApi,
    twitchApi: TwitchApi
)(using Executor):

  def forSubscriber(streamerName: UserStr)(using me: Option[MyId]): Fu[Option[Streamer.WithContext]] =
    me.foldLeft(find(streamerName)): (streamerFu, me) =>
      streamerFu.flatMapz: s =>
        subsRepo.isSubscribed(me.id, s.streamer).map { sub => s.copy(subscribed = sub).some }

  def withUsers(live: LiveStreams)(using me: Option[MyId]): Fu[List[Streamer.WithUserAndStream]] = for
    users <- userApi.byIds(live.streams.map(_.streamer.userId))
    subs <- me.so(subsRepo.filterSubscribed(_, users.map(_.id)))
  yield live.streams.flatMap: s =>
    users
      .find(_.is(s.streamer))
      .map:
        Streamer.WithUserAndStream(s.streamer, _, s.some, subs(s.streamer.userId))

  def find(username: UserStr): Fu[Option[Streamer.WithUser]] =
    userApi.byId(username).flatMapz(find)

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    repo
      .byId(user.id.into(Streamer.Id))
      .mapz: streamer =>
        Streamer.WithUser(streamer, user).some

  def allListedIds: Fu[Set[Streamer.Id]] =
    cache.listedIds.getUnit

  def listed[U: UserIdOf](u: U): Fu[Option[Streamer]] =
    val id = u.id.into(Streamer.Id)
    cache.isListed(id).flatMapz(repo.byId(id))

  def setSeenAt(user: User): Funit =
    cache.listedIds.getUnit.flatMap(_.contains(user.id.into(Streamer.Id)).so(repo.setSeenAt(user.id)))

  def setLangLiveNow(streams: List[Stream]): Funit =
    for
      _ <- repo.setLangLiveNow(streams)
      candidateIds <- cache.candidateIds.getUnit
    yield if streams.map(_.streamer.id).exists(candidateIds.contains) then cache.candidateIds.invalidateUnit()

  def update(prev: Streamer, data: StreamerForm.UserData, asMod: Boolean): Fu[Option[Streamer.ModChange]] =
    val streamer = data(prev, asMod)
    repo
      .update(streamer)
      .map: _ =>
        asMod.option:
          cache.listedIds.invalidateUnit()
          streamer.youtube.foreach(tuber => ytApi.channelSubscribe(tuber.channelId, true))
          modChange(prev, streamer)

  def forceCheck(uid: UserId): Funit =
    repo
      .byId(uid.into(Streamer.Id))
      .map:
        _.filter(_.approval.granted).so: s =>
          s.youtube.foreach(ytApi.forceCheckWithHtmlScraping)
          s.twitch.foreach(twitchApi.forceCheck)

  def demote(userId: UserId): Funit =
    for
      _ <- repo.demote(userId)
      _ <- pubsubSubscribe(userId, false)
    yield ()

  def pubsubSubscribe(userId: UserId, subscribe: Boolean): Funit =
    repo
      .byId(userId.into(Streamer.Id))
      .flatMapz: s =>
        s.youtube.so(tuber => ytApi.channelSubscribe(tuber.channelId, subscribe))
        s.twitch.so(twitcher => twitchApi.pubsubSubscribe(twitcher.id, subscribe))

  def oauthUnlink(streamer: Streamer, platform: Platform): Funit =
    for _ <- repo.oauth.unlink(streamer, platform)
    yield cache.listedIds.invalidateUnit()

  def isPotentialStreamer(user: User): Fu[Boolean] =
    cache.listedIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isCandidateStreamer(user: User): Fu[Boolean] =
    cache.candidateIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isActualStreamer(user: User): Fu[Boolean] =
    isPotentialStreamer(user) >>& isCandidateStreamer(user).not

  def uploadPicture(s: Streamer, picture: PicfitApi.FilePart, by: User): Funit =
    picfitApi
      .uploadFile(picture, userId = by.id, s"streamer:${s.id}".some, requestAutomod = false)
      .flatMap: pic =>
        repo.withColl(coll => coll.update.one($id(s.id), $set("picture" -> pic.id))).void

  private def modChange(prev: Streamer, current: Streamer): Streamer.ModChange =
    val (prevRequested, prevGranted, currRequested, currGranted) =
      (prev.approval.requested, prev.approval.granted, current.approval.requested, current.approval.granted)

    if (prevRequested || prevGranted) && !(currRequested || currGranted) then
      notifyApi.notifyOne(
        current,
        lila.core.notify.NotificationContent.GenericLink(
          url = streamerPageActivationRoute.url,
          title = "Streamer application declined".some,
          text = current.approval.reason,
          icon = lila.ui.Icon.Mic.value
        )
      )
    else if !prevGranted && currGranted then
      notifyApi.notifyOne(
        current,
        lila.core.notify.NotificationContent.GenericLink(
          url = routes.Streamer.edit.url,
          title = "Streamer application approved".some,
          text = "Your streamer page is now visible to others".some,
          icon = lila.ui.Icon.Mic.value
        )
      )
    Streamer.ModChange(
      list = (prevGranted != currGranted).option(currGranted),
      tier = (prev.approval.tier != current.approval.tier).option(current.approval.tier),
      decline = !currGranted && !currRequested && prevRequested,
      reason = current.approval.reason
    )

  private object cache:
    lazy val (listedIds, candidateIds) = repo.makeCaches
    def isListed(id: Streamer.Id): Fu[Boolean] = listedIds.getUnit.dmap(_ contains id)
