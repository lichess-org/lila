package lila.tv

import akka.actor.{ ActorRef, ActorSystem, Props }
import com.softwaremill.macwire.*

import lila.tv.Tv.Channel

@Module
final class Env(
    gameRepo: lila.game.GameRepo,
    lightUserApi: lila.core.user.LightUserApi,
    lightUserSync: lila.core.LightUser.GetterSync,
    gameProxy: lila.core.game.GameProxy,
    system: ActorSystem,
    onTvGame: lila.game.core.OnTvGame,
    rematches: lila.game.Rematches
)(using Executor, Scheduler):

  private val tvSyncActor = wire[TvSyncActor]

  lazy val tv = wire[Tv]

  val channelBroadcasts: Map[Channel, ActorRef] = Tv.Channel.values.map { c =>
    c -> system.actorOf(Props(wire[TvBroadcast]))
  }.toMap

  def channelSource(channel: Channel, bc: Boolean): Option[Fu[TvBroadcast.SourceType]] =
    given timeout: akka.util.Timeout = akka.util.Timeout(1 second)
    import akka.pattern.ask
    val ctag = summon[scala.reflect.ClassTag[TvBroadcast.SourceType]]
    channelBroadcasts
      .get(channel)
      .map: actor =>
        (actor ? TvBroadcast.Connect(bc)).mapTo(ctag)

  system.scheduler.scheduleWithFixedDelay(12 seconds, 3 seconds): () =>
    tvSyncActor ! TvSyncActor.Select
