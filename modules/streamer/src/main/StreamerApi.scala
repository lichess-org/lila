package lila.streamer

import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.memo.PicfitApi

final class StreamerApi(
    coll: Coll,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    picfitApi: PicfitApi,
    notifyApi: lila.core.notify.NotifyApi,
    subsRepo: lila.core.relation.SubscriptionRepo,
    ytApi: YouTubeApi
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

  def allListedIds: Fu[Set[Streamer.Id]] = cache.listedIds.getUnit

  def listed[U: UserIdOf](u: U): Fu[Option[Streamer]] =
    val id = u.id.into(Streamer.Id)
    cache.isListed(id).flatMapz(byId(id))

  def setSeenAt(user: User): Funit =
    cache.listedIds.getUnit.flatMap: ids =>
      ids
        .contains(user.id.into(Streamer.Id))
        .so(coll.update.one($id(user.id), $set("seenAt" -> nowInstant)).void)

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
      candidateIds <- cache.candidateIds.getUnit
    yield if streams.map(_.streamer.id).exists(candidateIds.contains) then cache.candidateIds.invalidateUnit()

  def update(prev: Streamer, data: StreamerForm.UserData, asMod: Boolean): Fu[Option[Streamer.ModChange]] =
    val streamer = data(prev, asMod)
    coll.update
      .one($id(streamer.id), streamer)
      .map: _ =>
        asMod.option:
          cache.listedIds.invalidateUnit()
          streamer.youTube.foreach(tuber => ytApi.channelSubscribe(tuber.channelId, true))
          modChange(prev, streamer)

  def forceCheck(uid: UserId): Funit =
    byId(uid.into(Streamer.Id)).map:
      _.filter(_.approval.granted).so: s =>
        s.youTube.foreach(ytApi.forceCheckWithHtmlScraping)

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

  def demote(userId: UserId): Funit =
    coll
      .findAndUpdate(
        $id(userId),
        $set("approval.requested" -> false, "approval.granted" -> false),
        fetchNewObject = true
      )
      .map: doc =>
        for
          streamer <- doc.value
          tuber <- streamer.getAsOpt[Streamer.YouTube]("youTube")
        yield ytApi.channelSubscribe(tuber.channelId, false)

  private[streamer] def unignore(userId: UserId): Funit =
    coll.updateField($id(userId), "approval.ignored", false).void

  def delete(user: User): Funit =
    coll
      .find($id(user.id))
      .one[Streamer]
      .flatMapz: s =>
        s.youTube.foreach(tuber => ytApi.channelSubscribe(tuber.channelId, false))
        coll.delete.one($id(user.id)).void

  def create(u: User): Funit =
    coll.insert.one(Streamer.make(u)).void.recover(lila.db.ignoreDuplicateKey)

  def isPotentialStreamer(user: User): Fu[Boolean] =
    cache.listedIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isCandidateStreamer(user: User): Fu[Boolean] =
    cache.candidateIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isActualStreamer(user: User): Fu[Boolean] =
    isPotentialStreamer(user) >>& isCandidateStreamer(user).not

  def uploadPicture(s: Streamer, picture: PicfitApi.FilePart, by: User): Funit =
    picfitApi
      .uploadFile(s"streamer:${s.id}", picture, userId = by.id)
      .flatMap: pic =>
        coll.update.one($id(s.id), $set("picture" -> pic.id)).void

  // unapprove after 6 weeks if you never streamed (was originally 1 week)
  def autoDemoteFakes: Funit =
    coll.update
      .one(
        $doc(
          "liveAt".$exists(false),
          "approval.granted" -> true,
          "approval.lastGrantedAt".$lt(nowInstant.minusWeeks(6))
        ),
        $set(
          "approval.granted" -> false,
          "demoted" -> true
        ),
        multi = true
      )
      .void

  object approval:

    def request(user: User) =
      find(user).flatMap:
        _.filter(!_.streamer.approval.granted).so { s =>
          coll.updateField($id(s.streamer.id), "approval.requested", true).void
        }

    def countRequests: Fu[Int] = coll.countSel:
      $doc(
        "approval.requested" -> true,
        "approval.ignored" -> false
      )

  def sameChannels(streamer: Streamer): Fu[List[Streamer]] =
    coll
      .find(
        $doc(
          "$or" -> List(
            streamer.twitch.map(_.userId).map { t =>
              $doc("twitch.userId" -> t)
            },
            streamer.youTube.map(_.channelId).map { t =>
              $doc("youTube.channelId" -> t)
            }
          ).flatten,
          "_id".$ne(streamer.userId)
        )
      )
      .sort($sort.desc("createdAt"))
      .cursor[Streamer](ReadPref.sec)
      .list(10)

  private object cache:

    private def selectListedApproved =
      $doc(
        "listed" -> true,
        "approval.granted" -> true
      )

    val listedIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
        coll.secondary.distinctEasy[Streamer.Id, Set]("_id", selectListedApproved)

    def isListed(id: Streamer.Id): Fu[Boolean] = listedIds.getUnit.dmap(_ contains id)

    val candidateIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1.hour).buildAsyncTimeout(): _ =>
        coll.secondary.distinctEasy[Streamer.Id, Set](
          "_id",
          selectListedApproved ++ $doc("liveAt".$exists(false))
        )
