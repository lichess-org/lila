package lila.msg

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    db: lila.db.Db,
    lightUserApi: lila.user.LightUserApi,
    isOnline: lila.socket.IsOnline,
    userRepo: lila.user.UserRepo,
    relationApi: lila.relation.RelationApi,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val colls = wire[MsgColls]

  lazy val api: MsgApi = wire[MsgApi]

  lazy val json = wire[MsgJson]

  lazy val search = wire[MsgSearch]
}

private class MsgColls(db: lila.db.Db) {
  val thread = db(CollName("msg_thread"))
  val msg    = db(CollName("msg_msg"))
}
