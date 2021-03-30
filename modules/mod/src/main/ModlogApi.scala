package lila.mod

import org.joda.time.DateTime

import lila.db.dsl._
import lila.report.{ Mod, ModId, Report, Suspect }
import lila.security.Permission
import lila.user.{ Holder, User, UserRepo }
import lila.irc.SlackApi

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, slackApi: SlackApi)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  private def coll = repo.coll

  import lila.db.BSON.BSONJodaDateTimeHandler
  implicit private val ModlogBSONHandler = reactivemongo.api.bson.Macros.handler[Modlog]

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
  // BC
  def streamerFeature(mod: Mod, streamerId: User.ID, v: Boolean) =
    add {
      Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerFeature else Modlog.streamerUnfeature)
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

  def hasModClose(user: User.ID): Fu[Boolean] =
    coll.exists($doc("user" -> user, "action" -> Modlog.closeAccount))

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

  def deleteTeam(mod: User.ID, id: String, name: String) =
    add {
      Modlog(mod, none, Modlog.deleteTeam, details = s"$id / $name".take(200).some)
    }

  def disableTeam(mod: User.ID, id: String, name: String) =
    add {
      Modlog(mod, none, Modlog.disableTeam, details = s"$id / $name".take(200).some)
    }

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

  def cheatDetected(user: User.ID, gameId: String) =
    add {
      Modlog("lichess", user.some, Modlog.cheatDetected, details = s"game $gameId".some)
    }

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
    coll.find($doc("user" -> userId)).sort($sort desc "date").cursor[Modlog]().gather[List](30)

  def countRecentCheatDetected(userId: User.ID): Fu[Int] =
    coll.countSel(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.cheatDetected,
        "date" $gte DateTime.now.minusMonths(6)
      )
    )

  private def add(m: Modlog): Funit = {
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    m.notable ?? {
      coll.insert.one(m) >> (m.notableSlack ?? slackMonitor(m))
    }
  }

  private def slackMonitor(m: Modlog): Funit = {
    import lila.mod.{ Modlog => M }
    val icon = m.action match {
      case M.alt | M.engine | M.booster | M.troll | M.closeAccount          => "thorhammer"
      case M.unalt | M.unengine | M.unbooster | M.untroll | M.reopenAccount => "large_blue_circle"
      case M.deletePost | M.deleteTeam | M.terminateTournament              => "x"
      case M.chatTimeout                                                    => "hourglass_flowing_sand"
      case M.closeTopic | M.disableTeam                                     => "lock"
      case M.openTopic | M.enableTeam                                       => "unlock"
      case M.modMessage                                                     => "left_speech_bubble"
      case _                                                                => "gear"
    }
    val text = s"""${m.showAction.capitalize} ${m.user.??(u => s"@$u ")}${~m.details}"""
    userRepo.isMonitoredMod(m.mod) flatMap {
      _ ?? {
        val monitorType = m.action match {
          case M.engine | M.unengine | M.booster | M.unbooster | M.closeAccount | M.reopenAccount =>
            SlackApi.MonitorType.Hunt
          case M.troll | M.untroll | M.chatTimeout | M.closeTopic | M.openTopic | M.disableTeam |
              M.enableTeam =>
            SlackApi.MonitorType.Comm
          case _ => SlackApi.MonitorType.Other
        }
        slackApi.monitorMod(m.mod, icon = icon, text = text, monitorType)
      }
    }
    slackApi.logMod(m.mod, icon = icon, text = text)
  }
}
