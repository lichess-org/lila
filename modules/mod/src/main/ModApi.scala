package lila.mod

import chess.Color
import lila.db.api._
import lila.security.{ Firewall, UserSpy, Store => SecurityStore }
import lila.user.tube.userTube
import lila.user.{ User, UserRepo }
import lila.evaluation.{ GameGroupCrosstable }
import lila.evaluation.GamePool.Analysed
import lila.analyse.{ AnalysisRepo }
import lila.game.{ GameRepo }
import reactivemongo.bson._
import com.typesafe.config.Config
import lila.common.PimpedConfig._

final class ModApi(
    logApi: ModlogApi,
    userSpy: String => Fu[UserSpy],
    firewall: Firewall,
    lilaBus: lila.common.Bus) {

  def adjust(mod: String, username: String): Funit = withUser(username) { user =>
    logApi.engine(mod, user.id, !user.engine) zip
      UserRepo.toggleEngine(user.id) >>- {
        if (!user.engine) lilaBus.publish(lila.hub.actorApi.mod.MarkCheater(user.id), 'adjustCheater)
      } void
  }

  def autoAdjust(username: String): Funit = logApi.wasUnengined(User.normalize(username)) flatMap {
    case true  => funit
    case false => adjust("lichess", username)
  }

  def troll(mod: String, username: String): Fu[Boolean] = withUser(username) { u =>
    val user = u.copy(troll = !u.troll)
    ((UserRepo updateTroll user) >>-
      logApi.troll(mod, user.id, user.troll)) inject user.troll
  }

  def ban(mod: String, username: String): Funit = withUser(username) { user =>
    userSpy(user.id) flatMap { spy =>
      UserRepo.toggleIpBan(user.id) zip
        logApi.ban(mod, user.id, !user.ipBan) zip
        user.ipBan.fold(
          (spy.ipStrings map firewall.unblockIp).sequenceFu,
          (spy.ipStrings map firewall.blockIp).sequenceFu >>
            (SecurityStore disconnect user.id)
        ) void
    }
  }

  def closeAccount(mod: String, username: String): Funit = withUser(username) { user =>
    user.enabled ?? {
      (UserRepo disable user.id) >>
        (SecurityStore disconnect user.id) >>
        logApi.closeAccount(mod, user.id)
    }
  }

  def reopenAccount(mod: String, username: String): Funit = withUser(username) { user =>
    !user.enabled ?? {
      (UserRepo enable user.id) >> logApi.reopenAccount(mod, user.id)
    }
  }

  def setTitle(mod: String, username: String, title: Option[String]): Funit = withUser(username) { user =>
    UserRepo.setTitle(user.id, title) >> logApi.setTitle(mod, user.id, title)
  }

  def ipban(mod: String, ip: String): Funit =
    (firewall blockIp ip) >> logApi.ipban(mod, ip)

  private def withUser[A](username: String)(op: User => Fu[A]): Fu[A] =
    UserRepo named username flatten "[mod] missing user " + username flatMap op

  def assessGame(mod: String, gameId: String, side: String, eval: String): Funit = {
    val color: String = side match {
      case "white" => "white"
      case "black" => "black"
      case _       => "white"
    }
    val assessment: Int = parseIntOption(eval) match {
      case Some(a) if (a >= 1 && a <= 5) => a
      case _ => 1
    }

    val db = lila.db.Env.current
    val config = lila.common.PlayApp loadConfig "gameGroup"
    val CollectionCrosstable = config.getString("collection.crossreftable")
    implicit val gameGroupCrosstableBSONhandler = Macros.handler[lila.evaluation.GameGroupCrosstable]
    val gameGroupCrosstable = GameGroupCrosstable(gameId = gameId, color = color, assessment = assessment)
  
    db(CollectionCrosstable).insert(gameGroupCrosstable) >> 
      logApi.assessGame(mod, gameId, color, assessment.toString)
  }
}
