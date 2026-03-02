package lila.streamer

import scalalib.ThreadLocalRandom

import lila.common.{ Bus, LilaScheduler }

final private class Publisher(
    api: StreamerApi,
    repo: StreamerRepo,
    isOnline: lila.core.socket.IsOnline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.core.data.UserIds,
    twitchApi: TwitchApi,
    ytApi: YoutubeApi,
    langList: lila.core.i18n.LangList
)(using Executor, Scheduler):

  private var liveStreams = LiveStreams(Nil)

  def getLiveStreams: LiveStreams = liveStreams

  LilaScheduler("OnlinePublisher", _.Every(15.seconds), _.AtMost(10.seconds), _.Delay(20.seconds)):
    for
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter: id =>
        liveStreams.has(id) || isOnline.exec(id.userId)
      streamers <- repo.byIds(activeIds)
      (twitchStreams, youtubeStreams) <-
        twitchApi
          .liveMatching(
            streamers,
            s =>
              s.status.value.toLowerCase.contains(keyword.toLowerCase) ||
                alwaysFeatured().value.contains(s.streamer.id)
          )
          .zip(ytApi.liveMatching(streamers))
      streams = LiveStreams:
        ThreadLocalRandom.shuffle:
          (youtubeStreams ::: twitchStreams).pipe(dedupStreamers)
      _ <- api.setLangLiveNow(streams.streams)
    yield publishStreams(streamers, streams)

  private val streamStartOnceEvery = scalalib.cache.OnceEvery[UserId](2.hour)

  private def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) =
    import lila.core.misc.streamer.*
    Bus.pub:
      StreamersOnline:
        newStreams.streams
          .map: s =>
            s.streamer.userId ->
              StreamInfo(
                name = s.streamer.name.value,
                lang = ~s.streamer.lastStreamLang.map(langList.nameByLanguage)
              )
          .toMap
    if newStreams != liveStreams then
      newStreams.streams
        .filterNot: s =>
          liveStreams.has(s.streamer)
        .foreach: s =>
          import s.streamer.userId
          if streamStartOnceEvery(userId) then
            Bus.pub(lila.core.misc.streamer.StreamStart(userId, s.streamer.name.value))
    liveStreams = newStreams
    lila.mon.streamer.online.update(liveStreams.streams.size)
    streamers.foreach: streamer =>
      streamer.twitch.foreach: t =>
        if liveStreams.streams.exists(s => s.platform == "twitch" && s.is(streamer)) then
          lila.mon.streamer.present(s"${t.login}@twitch").increment()
      streamer.youtube.foreach: t =>
        if liveStreams.streams.exists(s => s.platform == "youtube" && s.is(streamer)) then
          lila.mon.streamer.present(s"${t.channelId}@youtube").increment()

  private def dedupStreamers(streams: List[Stream]): List[Stream] =
    streams
      .foldLeft((Set.empty[Streamer.Id], List.empty[Stream])):
        case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
        case ((streamerIds, streams), stream) => (streamerIds + stream.streamer.id, stream :: streams)
      ._2
