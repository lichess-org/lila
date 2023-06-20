package lila.streamer

import reactivemongo.api.ReadPreference
import play.api.i18n.Lang

import lila.common.licon
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.memo.PicfitApi
import lila.user.{ User, UserRepo }
import lila.user.Me

final class StreamerApi(
    coll: Coll,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    picfitApi: PicfitApi,
    notifyApi: lila.notify.NotifyApi,
    subsRepo: lila.relation.SubscriptionRepo,
    ytApi: YouTubeApi
)(using Executor):

  import BsonHandlers.given

  def withColl[A](f: Coll => A): A = f(coll)

  def byId(id: Streamer.Id): Fu[Option[Streamer]]           = coll.byId[Streamer](id)
  def byIds(ids: Iterable[Streamer.Id]): Fu[List[Streamer]] = coll.byIds[Streamer, Streamer.Id](ids)

  def find(username: UserStr): Fu[Option[Streamer.WithUser]] =
    userRepo byId username flatMapz find

  def find(user: User): Fu[Option[Streamer.WithUser]] =
    byId(user.id into Streamer.Id) dmap {
      _ map { Streamer.WithUser(_, user) }
    }

  def findOrInit(user: User): Fu[Option[Streamer.WithUser]] =
    find(user).orElse:
      val s = Streamer.WithUser(Streamer make user, user)
      coll.insert.one(s.streamer) inject s.some

  def forSubscriber(streamerName: UserStr)(using me: Option[Me.Id]): Fu[Option[Streamer.WithContext]] =
    me.foldLeft(find(streamerName)): (streamerFu, me) =>
      streamerFu flatMapz { s =>
        subsRepo.isSubscribed(me.id, s.streamer).map { sub => s.copy(subscribed = sub).some }
      }

  def withUsers(live: LiveStreams)(using me: Option[Me.Id]): Fu[List[Streamer.WithUserAndStream]] = for {
    users <- userRepo.byIdsSecondary(live.streams.map(_.streamer.userId))
    subs  <- me.so(subsRepo.filterSubscribed(_, users.map(_.id)))
  } yield live.streams.flatMap { s =>
    users.find(_ is s.streamer) map {
      Streamer.WithUserAndStream(s.streamer, _, s.some, subs(s.streamer.userId))
    }
  }

  def allListedIds: Fu[Set[Streamer.Id]] = cache.listedIds.getUnit

  def setSeenAt(user: User): Funit =
    cache.listedIds.getUnit.flatMap: ids =>
      ids.contains(user.id into Streamer.Id) so
        coll.update.one($id(user.id), $set("seenAt" -> nowInstant)).void

  def setLangLiveNow(streams: List[Stream]): Funit =
    val update = coll.update(ordered = false)
    for
      elements <- streams.map { s =>
        update.element(
          q = $id(s.streamer.id),
          u = $set(
            "liveAt"         -> nowInstant,
            "lastStreamLang" -> Lang.get(s.lang).map(_.language)
          )
        )
      }.parallel
      _            <- elements.nonEmpty so update.many(elements).void
      candidateIds <- cache.candidateIds.getUnit
    yield if (streams.map(_.streamer.id).exists(candidateIds.contains)) cache.candidateIds.invalidateUnit()

  def update(prev: Streamer, data: StreamerForm.UserData, asMod: Boolean): Fu[Streamer.ModChange] =
    val streamer = data(prev, asMod)
    coll.update.one($id(streamer.id), streamer) >>- {
      cache.listedIds.invalidateUnit()
      streamer.youTube.foreach(tuber => ytApi.channelSubscribe(tuber.channelId, true))
    } inject modChange(prev, streamer)

  private def modChange(prev: Streamer, current: Streamer): Streamer.ModChange =
    val list = prev.approval.granted != current.approval.granted option current.approval.granted
    ~list so notifyApi.notifyOne(
      current,
      lila.notify.GenericLink(
        url = "/streamer/edit",
        title = "Listed on /streamer".some,
        text = "Your streamer page is public".some,
        icon = licon.Mic
      )
    )
    Streamer.ModChange(
      list = list,
      tier = prev.approval.tier != current.approval.tier option current.approval.tier,
      decline = !current.approval.granted && !current.approval.requested && prev.approval.requested
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
          tuber    <- streamer.getAsOpt[Streamer.YouTube]("youTube")
        yield ytApi.channelSubscribe(tuber.channelId, false)

  def delete(user: User): Funit =
    coll
      .find($id(user.id))
      .one[Streamer]
      .map(_.foreach: s =>
        s.youTube.foreach(tuber => ytApi.channelSubscribe(tuber.channelId, false))
        coll.delete.one($id(user.id)).void
      )

  def create(u: User): Funit =
    coll.insert.one(Streamer make u).void.recover(lila.db.ignoreDuplicateKey)

  def isPotentialStreamer(user: User): Fu[Boolean] =
    cache.listedIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isCandidateStreamer(user: User): Fu[Boolean] =
    cache.candidateIds.getUnit.dmap(_ contains user.id.into(Streamer.Id))

  def isActualStreamer(user: User): Fu[Boolean] =
    isPotentialStreamer(user) >>& !isCandidateStreamer(user)

  def uploadPicture(s: Streamer, picture: PicfitApi.FilePart, by: User): Funit =
    picfitApi
      .uploadFile(s"streamer:${s.id}", picture, userId = by.id) flatMap { pic =>
      coll.update.one($id(s.id), $set("picture" -> pic.id)).void
    }

  // unapprove after a week if you never streamed
  def autoDemoteFakes: Funit =
    coll.update
      .one(
        $doc(
          "liveAt" $exists false,
          "approval.granted" -> true,
          "approval.lastGrantedAt" $lt nowInstant.minusWeeks(1)
        ),
        $set(
          "approval.granted" -> false,
          "demoted"          -> true
        ),
        multi = true
      )
      .void

  object approval:

    def request(user: User) =
      find(user) flatMap {
        _.filter(!_.streamer.approval.granted) so { s =>
          coll.updateField($id(s.streamer.id), "approval.requested", true).void
        }
      }

    def countRequests: Fu[Int] = coll.countSel:
      $doc(
        "approval.requested" -> true,
        "approval.ignored"   -> false
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
          "_id" $ne streamer.userId
        )
      )
      .sort($sort desc "createdAt")
      .cursor[Streamer](readPreference = ReadPreference.secondaryPreferred)
      .list(10)

  private object cache:

    private def selectListedApproved =
      $doc(
        "listed"           -> true,
        "approval.granted" -> true
      )

    val listedIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
        coll.secondaryPreferred.distinctEasy[Streamer.Id, Set]("_id", selectListedApproved)

    val candidateIds = cacheApi.unit[Set[Streamer.Id]]:
      _.refreshAfterWrite(1 hour).buildAsyncFuture: _ =>
        coll.secondaryPreferred.distinctEasy[Streamer.Id, Set](
          "_id",
          selectListedApproved ++ $doc("liveAt" $exists false)
        )
