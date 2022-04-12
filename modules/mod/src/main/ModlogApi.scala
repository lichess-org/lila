package lila.mod

import org.joda.time.DateTime
import reactivemongo.api._

import lila.db.dsl._
import lila.msg.MsgPreset
import lila.report.{ Mod, ModId, Report, Suspect }
import lila.security.Permission
import lila.user.{ Holder, User, UserRepo }
import lila.irc.IrcApi
import lila.game.Game

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, ircApi: IrcApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private def coll = repo.coll

  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit private val ModlogBSONHandler = reactivemongo.api.bson.Macros.handler[Modlog]

  private val markActions = List(Modlog.alt, Modlog.booster, Modlog.closeAccount, Modlog.engine, Modlog.troll)

  def streamerDecline(mod: Mod, streamerId: User.ID) =
    add {
      Modlog(mod.user.id, streamerId.some, Modlog.streamerDecline)
    }
  def streamerList(mod: Mod, streamerId: User.ID, v: Boolean) =
    add {
      Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerList else Modlog.streamerUnlist)
    }
  def streamerTier(mod: Mod, streamerId: User.ID, v: Int) =
    add {
      Modlog(mod.user.id, streamerId.some, Modlog.streamerTier, v.toString.some)
    }
  def blogTier(mod: Mod, sus: Suspect, blogId: String, tier: String) =
    add {
      Modlog.make(mod, sus, Modlog.blogTier, tier.some)
    }
  def blogPostEdit(mod: Mod, sus: Suspect, postId: String, postName: String, action: String) =
    add {
      Modlog.make(mod, sus, Modlog.blogPostEdit, s"$action #$postId $postName".some)
    }

  def practiceConfig(mod: User.ID) =
    add {
      Modlog(mod, none, Modlog.practiceConfig)
    }

  def alt(mod: Mod, sus: Suspect, v: Boolean) =
    add {
      Modlog.make(mod, sus, if (v) Modlog.alt else Modlog.unalt)
    }

  def engine(mod: Mod, sus: Suspect, v: Boolean) =
    add {
      Modlog.make(mod, sus, if (v) Modlog.engine else Modlog.unengine)
    }

  def booster(mod: Mod, sus: Suspect, v: Boolean) =
    add {
      Modlog.make(mod, sus, if (v) Modlog.booster else Modlog.unbooster)
    }

  def troll(mod: Mod, sus: Suspect) =
    add {
      Modlog.make(mod, sus, if (sus.user.marks.troll) Modlog.troll else Modlog.untroll)
    }

  def setKidMode(mod: User.ID, kid: User.ID) =
    add {
      Modlog(mod, kid.some, Modlog.setKidMode)
    }

  def disableTwoFactor(mod: User.ID, user: User.ID) =
    add {
      Modlog(mod, user.some, Modlog.disableTwoFactor)
    }

  def closeAccount(mod: User.ID, user: User.ID) =
    add {
      Modlog(mod, user.some, Modlog.closeAccount)
    }

  def selfCloseAccount(user: User.ID, openReports: List[Report]) =
    add {
      Modlog(
        ModId.lichess.value,
        user.some,
        Modlog.selfCloseAccount,
        details = openReports.map(r => s"${r.reason.name} report").mkString(", ").some.filter(_.nonEmpty)
      )
    }

  def closedByMod(user: User): Fu[Boolean] =
    fuccess(user.marks.alt) >>| coll.exists($doc("user" -> user.id, "action" -> Modlog.closeAccount))

  def reopenAccount(mod: User.ID, user: User.ID) =
    add {
      Modlog(mod, user.some, Modlog.reopenAccount)
    }

  def addTitle(mod: User.ID, user: User.ID, title: String) =
    add {
      Modlog(mod, user.some, Modlog.setTitle, title.some)
    }

  def removeTitle(mod: User.ID, user: User.ID) =
    add {
      Modlog(mod, user.some, Modlog.removeTitle)
    }

  def setEmail(mod: User.ID, user: User.ID) =
    add {
      Modlog(mod, user.some, Modlog.setEmail)
    }

  def deletePost(
      mod: User.ID,
      user: Option[User.ID],
      author: Option[User.ID],
      text: String
  ) =
    add {
      Modlog(
        mod,
        user,
        Modlog.deletePost,
        details = Some(
          author.??(_ + " ") + text.take(400)
        )
      )
    }

  def toggleCloseTopic(mod: User.ID, categ: String, topic: String, closed: Boolean) =
    add {
      Modlog(
        mod,
        none,
        if (closed) Modlog.closeTopic else Modlog.openTopic,
        details = s"$categ/$topic".some
      )
    }

  def toggleHideTopic(mod: User.ID, categ: String, topic: String, hidden: Boolean) =
    add {
      Modlog(
        mod,
        none,
        if (hidden) Modlog.hideTopic else Modlog.showTopic,
        details = s"$categ/$topic".some
      )
    }

  def toggleStickyTopic(mod: User.ID, categ: String, topic: String, sticky: Boolean) =
    add {
      Modlog(
        mod,
        none,
        if (sticky) Modlog.stickyTopic else Modlog.unstickyTopic,
        details = s"$categ/$topic".some
      )
    }

  // Not to be confused with the eponymous lichess account.
  def postOrEditAsAnonMod(
      mod: User.ID,
      categ: String,
      topic: String,
      postId: String,
      text: String,
      edit: Boolean
  ) =
    add {
      Modlog(
        mod,
        none,
        if (edit) Modlog.editAsAnonMod else Modlog.postAsAnonMod,
        details = s"$categ/$topic id: $postId ${text.take(400)}".some
      )
    }

  def deleteTeam(mod: User.ID, id: String, explain: String) =
    add {
      Modlog(
        mod,
        none,
        Modlog.deleteTeam,
        details = s"$id: ${explain take 200}".some
      ) indexAs "team"
    }

  def disableTeam(mod: User.ID, id: String, explain: String) =
    add {
      Modlog(
        mod,
        none,
        Modlog.disableTeam,
        details = s"$id: ${explain take 200}".some
      ) indexAs "team"
    }

  def teamLog(teamId: String): Fu[List[Modlog]] =
    repo.coll
      .find($doc("index" -> "team", "details" $startsWith s"$teamId: "))
      .sort($sort desc "date")
      .cursor[Modlog]()
      .list(30)

  def terminateTournament(mod: User.ID, name: String) =
    add {
      Modlog(mod, none, Modlog.terminateTournament, details = name.some)
    }

  def chatTimeout(mod: User.ID, user: User.ID, reason: String, text: String) =
    add {
      Modlog(mod, user.some, Modlog.chatTimeout, details = s"$reason: $text".some)
    }

  def setPermissions(mod: Holder, user: User.ID, permissions: Map[Permission, Boolean]) =
    add {
      Modlog(
        mod.id,
        user.some,
        Modlog.permissions,
        details = permissions
          .map { case (p, dir) =>
            s"${if (dir) "+" else "-"}${p}"
          }
          .mkString(", ")
          .some
      )
    }

  def wasUnteachered(user: User.ID): Fu[Boolean] =
    coll.exists($doc("user" -> user, "details" $regex s"-${Permission.Teacher.toString}"))

  def wasMarkedBy(mod: User.ID, user: User.ID): Fu[Boolean] =
    coll.secondaryPreferred.exists(
      $doc(
        "user" -> user,
        "mod"  -> mod,
        "action" $in markActions
      )
    )

  def wereMarkedBy(mod: User.ID, users: List[User.ID]): Fu[Set[User.ID]] =
    coll.distinctEasy[User.ID, Set](
      "user",
      $doc(
        "user" $in users,
        "mod" -> mod,
        "action" $in markActions
      ),
      readPreference = ReadPreference.secondaryPreferred
    )

  def reportban(mod: Mod, sus: Suspect, v: Boolean) =
    add {
      Modlog.make(mod, sus, if (v) Modlog.reportban else Modlog.unreportban)
    }

  def modMessage(mod: User.ID, user: User.ID, subject: String) =
    add {
      Modlog(mod, user.some, Modlog.modMessage, details = subject.some)
    }

  def coachReview(mod: User.ID, coach: User.ID, author: User.ID) =
    add {
      Modlog(mod, coach.some, Modlog.coachReview, details = s"by $author".some)
    }

  def cheatDetected(user: User.ID, gameId: Game.ID) =
    add {
      Modlog("lichess", user.some, Modlog.cheatDetected, details = s"game $gameId".some)
    }

  def cheatDetectedAndCount(user: User.ID, gameId: Game.ID): Fu[Int] = for {
    prevCount <- countRecentCheatDetected(user)
    _         <- cheatDetected(user, gameId)
  } yield prevCount + 1

  def cli(by: User.ID, command: String) =
    add {
      Modlog(by, none, Modlog.cli, command.some)
    }

  def garbageCollect(mod: Mod, sus: Suspect) =
    add {
      Modlog.make(mod, sus, Modlog.garbageCollect)
    }

  def rankban(mod: Mod, sus: Suspect, v: Boolean) =
    add {
      Modlog.make(mod, sus, if (v) Modlog.rankban else Modlog.unrankban)
    }

  def teamKick(mod: User.ID, user: User.ID, teamName: String) =
    add {
      Modlog(mod, user.some, Modlog.teamKick, details = Some(teamName take 140))
    }

  def teamEdit(mod: User.ID, teamOwner: User.ID, teamName: String) =
    add {
      Modlog(mod, teamOwner.some, Modlog.teamEdit, details = Some(teamName take 140))
    }

  def appealPost(mod: User.ID, user: User.ID) =
    add { Modlog(mod, user.some, Modlog.appealPost, details = none) }

  def wasUnengined(sus: Suspect) =
    coll.exists(
      $doc(
        "user"   -> sus.user.id,
        "action" -> Modlog.unengine
      )
    )

  def wasUnbooster(userId: User.ID) =
    coll.exists(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.unbooster
      )
    )

  def userHistory(userId: User.ID): Fu[List[Modlog]] =
    coll.find($doc("user" -> userId)).sort($sort desc "date").cursor[Modlog]().list(60)

  def countRecentCheatDetected(userId: User.ID): Fu[Int] =
    coll.secondaryPreferred.countSel(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.cheatDetected,
        "date" $gte DateTime.now.minusMonths(6)
      )
    )

  def countRecentRatingManipulationsWarnings(userId: User.ID): Fu[Int] =
    coll.secondaryPreferred.countSel(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.modMessage,
        $or($doc("details" -> MsgPreset.sandbagAuto.name), $doc("details" -> MsgPreset.boostAuto.name)),
        "date" $gte DateTime.now.minusMonths(6)
      )
    )

  private def add(m: Modlog): Funit = {
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    m.notable ?? {
      coll.insert.one {
        ModlogBSONHandler.writeTry(m).get ++ (!m.isLichess).??($doc("human" -> true))
      } >> (m.notableZulip ?? zulipMonitor(m))
    }
  }

  private def zulipMonitor(m: Modlog): Funit = {
    import lila.mod.{ Modlog => M }
    val icon = m.action match {
      case M.alt | M.engine | M.booster | M.troll | M.closeAccount          => "thorhammer"
      case M.unalt | M.unengine | M.unbooster | M.untroll | M.reopenAccount => "blue_circle"
      case M.deletePost | M.deleteTeam | M.terminateTournament              => "x"
      case M.chatTimeout                                                    => "hourglass_flowing_sand"
      case M.closeTopic | M.disableTeam                                     => "locked"
      case M.openTopic | M.enableTeam                                       => "unlocked"
      case M.modMessage | M.postAsAnonMod | M.editAsAnonMod                 => "left_speech_bubble"
      case M.blogTier | M.blogPostEdit                                      => "note"
      case _                                                                => "gear"
    }
    val text = s"""${m.showAction.capitalize} ${m.user.??(u => s"@$u")} ${~m.details}"""
    userRepo.isMonitoredMod(m.mod) flatMap {
      _ ?? {
        val monitorType = m.action match {
          case M.closeAccount | M.alt => None
          case M.engine | M.unengine | M.reopenAccount | M.unalt =>
            Some(IrcApi.ModDomain.Cheat)
          case M.booster | M.unbooster => Some(IrcApi.ModDomain.Boost)
          case M.troll | M.untroll | M.chatTimeout | M.closeTopic | M.openTopic | M.disableTeam |
              M.enableTeam | M.setKidMode | M.deletePost | M.postAsAnonMod | M.editAsAnonMod | M.blogTier |
              M.blogPostEdit =>
            Some(IrcApi.ModDomain.Comm)
          case _ => Some(IrcApi.ModDomain.Other)
        }
        monitorType ?? {
          ircApi.monitorMod(m.mod, icon = icon, text = text, _)
        }
      }
    }
    ircApi.logMod(m.mod, icon = icon, text = text)
  }
}
