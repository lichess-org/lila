package lila.swiss

import com.softwaremill.macwire._

import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val colls = wire[SwissColls]

  val api = wire[SwissApi]

  def version(tourId: Swiss.Id): Fu[SocketVersion] =
    fuccess(SocketVersion(0))
  // socket.rooms.ask[SocketVersion](tourId)(GetVersion)

  lazy val json = wire[SwissJson]

  lazy val forms = wire[SwissForm]
}

private class SwissColls(db: lila.db.Db) {
  val swiss = db(CollName("swiss"))
  val round = db(CollName("swiss_round"))
}
