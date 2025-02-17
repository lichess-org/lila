package lila.streamer
import scalalib.model.Language
import lila.core.userId
import lila.memo.CacheApi.*

case class LiveStreams(streams: List[Stream]):

  private lazy val streamerIds: Set[Streamer.Id] = streams.view.map(_.streamer.id).to(Set)

  def has(id: Streamer.Id): Boolean    = streamerIds(id)
  def has(streamer: Streamer): Boolean = has(streamer.id)

  def get(streamer: Streamer) = streams.find(_.is(streamer))

  def homepage(max: Int, accepts: Set[Language]) = LiveStreams:
    streams
      .takeWhile(_.streamer.approval.tier > 0)
      .foldLeft(Vector.empty[Stream]):
        case (selected, s) if accepts(s.language) && {
              selected.sizeIs < max || s.streamer.approval.tier == Streamer.maxTier
            } && {
              s.streamer.approval.tier > 1 || selected.sizeIs < 2
            } =>
          selected :+ s
        case (selected, _) => selected
      .toList

  def withTitles(lightUser: lila.core.user.LightUserApi) =
    LiveStreams.WithTitles(
      this,
      streams.view
        .map(_.streamer.userId)
        .flatMap: userId =>
          lightUser.sync(userId).flatMap(_.title).map(userId -> _)
        .toMap
    )

  def excludeUsers(userIds: List[UserId]) = copy(
    streams = streams.filterNot(s => userIds contains s.streamer.userId)
  )

object LiveStreams:

  case class WithTitles(live: LiveStreams, titles: Map[UserId, chess.PlayerTitle]):
    def titleName(s: Stream) = s"${titles.get(s.streamer.userId).fold("")(_.value + " ")}${s.streamer.name}"
    def excludeUsers(userIds: List[UserId]) = copy(live = live.excludeUsers(userIds))

  given alleycats.Zero[WithTitles] = alleycats.Zero(WithTitles(LiveStreams(Nil), Map.empty))

final class LiveStreamApi(
    cacheApi: lila.memo.CacheApi,
    streaming: Streaming
)(using Executor):

  private val cache = cacheApi.unit[LiveStreams]:
    _.refreshAfterWrite(2.seconds).buildAsyncFuture: _ =>
      fuccess(streaming.getLiveStreams)
        .dmap: s =>
          LiveStreams(s.streams.sortBy(-_.streamer.approval.tier))
        .addEffect: s =>
          userIdsCache = s.streams.map(_.streamer.userId).toSet

  private var userIdsCache = Set.empty[UserId]

  def all: Fu[LiveStreams] = cache.getUnit
  // def all: Fu[LiveStreams] =
  //   fuccess(
  //     LiveStreams(
  //       List(
  //         Stream.Twitch.Stream(
  //           "thibault",
  //           "[RU] test stream on lichess.org",
  //           Streamer(
  //             _id = Streamer.Id("thibault"),
  //             listed = Streamer.Listed(true),
  //             approval = Streamer.Approval(
  //               requested = false,
  //               granted = true,
  //               ignored = false,
  //               tier = 5,
  //               chatEnabled = true,
  //               lastGrantedAt = nowInstant.some
  //             ),
  //             picturePath = none,
  //             name = Streamer.Name("thibault"),
  //             headline = none,
  //             description = none,
  //             twitch = none,
  //             youTube = none,
  //             seenAt = nowInstant,      // last seen online
  //             liveAt = nowInstant.some, // last seen streaming
  //             createdAt = nowInstant,
  //             updatedAt = nowInstant
  //           )
  //         )
  //       )
  //     )
  //   )

  def streamerUserIds(within: Iterable[UserId]): Fu[List[UserId]] =
    all.map:
      _.streams
        .collect:
          case s if within.exists(s.streamer.is(_)) => s.streamer.userId

  def of(s: Streamer.WithContext): Fu[Streamer.WithUserAndStream] = all.map: live =>
    Streamer.WithUserAndStream(s.streamer, s.user, live.get(s.streamer), s.subscribed)

  def userIds                                      = userIdsCache
  def isStreaming(userId: UserId)                  = userIdsCache contains userId
  def one(userId: UserId): Fu[Option[Stream]]      = all.map(_.streams.find(_.is(userId)))
  def many(userIds: Seq[UserId]): Fu[List[Stream]] = all.map(_.streams.filter(s => userIds.exists(s.is)))
