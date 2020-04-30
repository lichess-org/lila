package lila.swiss

import com.softwaremill.macwire._

import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.common.config._

@Module
final class Env(
    remoteSocketApi: lila.socket.RemoteSocket,
    db: lila.db.Db,
    chatApi: lila.chat.ChatApi,
    lightUserApi: lila.user.LightUserApi
)(
    implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    // mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator,
    mode: play.api.Mode
) {

  private val colls = wire[SwissColls]

  val api = wire[SwissApi]

  private lazy val socket = wire[SwissSocket]

  def version(swissId: Swiss.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](swissId.value)(GetVersion)

  lazy val json = wire[SwissJson]

  lazy val forms = wire[SwissForm]
}

private class SwissColls(db: lila.db.Db) {
  val swiss   = db(CollName("swiss"))
  val player  = db(CollName("swiss_player"))
  val pairing = db(CollName("swiss_pairing"))
}
