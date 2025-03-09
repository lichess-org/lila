package lila.streamer

import play.api.i18n.Lang
import scalalib.ThreadLocalRandom

import lila.common.{ Bus, LilaScheduler }

final private class Streaming(
    api: StreamerApi,
    isOnline: lila.core.socket.IsOnline,
    keyword: Stream.Keyword,
    alwaysFeatured: () => lila.core.data.UserIds,
    twitchApi: TwitchApi,
    ytApi: YouTubeApi
)(using Executor, Scheduler):

  import Stream.*

  private var liveStreams = LiveStreams(Nil)

  def getLiveStreams: LiveStreams = liveStreams

  LilaScheduler("Streaming", _.Every(15.seconds), _.AtMost(10.seconds), _.Delay(20.seconds)):
    for
      streamerIds <- api.allListedIds
      activeIds = streamerIds.filter { id =>
        liveStreams.has(id) || isOnline.exec(id.userId)
      }
      streamers <- api.byIds(activeIds)
      (twitchStreams, youTubeStreams) <-
        twitchApi
          .fetchStreams(streamers, 0, None)
          .map:
            _.collect { case Twitch.TwitchStream(name, title, _, langStr) =>
              streamers
                .find { s =>
                  s.twitch.exists(_.userId.toLowerCase == name.toLowerCase) && {
                    title.value.toLowerCase.contains(keyword.toLowerCase) ||
                    alwaysFeatured().value.contains(s.userId)
                  }
                }
                .map { Twitch.Stream(name, title, _, Lang.get(langStr) | lila.core.i18n.defaultLang) }
            }.flatten
          .zip(ytApi.fetchStreams(streamers))
      streams = LiveStreams:
        ThreadLocalRandom.shuffle:
          (youTubeStreams ::: twitchStreams).pipe(dedupStreamers)
      _ <- api.setLangLiveNow(streams.streams)
    yield publishStreams(streamers, streams)

  private val streamStartOnceEvery = scalalib.cache.OnceEvery[UserId](2.hour)

  private def publishStreams(streamers: List[Streamer], newStreams: LiveStreams) =
    Bus.pub:
      lila.core.misc.streamer
        .StreamersOnline(newStreams.streams.map(s => (s.streamer.userId, s.streamer.name.value)))
    if newStreams != liveStreams then
      newStreams.streams
        .filterNot { s =>
          liveStreams.has(s.streamer)
        }
        .foreach { s =>
          import s.streamer.userId
          if streamStartOnceEvery(userId) then
            Bus.pub(lila.core.misc.streamer.StreamStart(userId, s.streamer.name.value))
        }
    liveStreams = newStreams
    streamers.foreach { streamer =>
      streamer.twitch.foreach { t =>
        if liveStreams.streams.exists(s => s.serviceName == "twitch" && s.is(streamer)) then
          lila.mon.tv.streamer.present(s"${t.userId}@twitch").increment()
      }
      streamer.youTube.foreach { t =>
        if liveStreams.streams.exists(s => s.serviceName == "youTube" && s.is(streamer)) then
          lila.mon.tv.streamer.present(s"${t.channelId}@youtube").increment()
      }
    }

  private def dedupStreamers(streams: List[Stream]): List[Stream] =
    streams
      .foldLeft((Set.empty[Streamer.Id], List.empty[Stream])):
        case ((streamerIds, streams), stream) if streamerIds(stream.streamer.id) => (streamerIds, streams)
        case ((streamerIds, streams), stream) => (streamerIds + stream.streamer.id, stream :: streams)
      ._2
