package lila.mod

import lila.db.dsl._
import lila.report.{ Report, Mod, Suspect, ModId }
import lila.security.Permission

final class ModlogApi(coll: Coll) {

  import lila.db.BSON.BSONJodaDateTimeHandler
  private implicit val ModlogBSONHandler = reactivemongo.bson.Macros.handler[Modlog]

  def streamerList(mod: Mod, streamerId: String, v: Boolean) = add {
    Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerList else Modlog.streamerUnlist)
  }
  def streamerFeature(mod: Mod, streamerId: String, v: Boolean) = add {
    Modlog(mod.user.id, streamerId.some, if (v) Modlog.streamerFeature else Modlog.streamerUnfeature)
  }

  def practiceConfig(mod: String) = add {
    Modlog(mod, none, Modlog.practiceConfig)
  }

  def engine(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.engine else Modlog.unengine)
  }

  def booster(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.booster else Modlog.unbooster)
  }

  def troll(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, if (sus.user.troll) Modlog.troll else Modlog.untroll)
  }

  def ban(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, if (sus.user.ipBan) Modlog.ipban else Modlog.ipunban)
  }

  def closeAccount(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.closeAccount)
  }

  def selfCloseAccount(user: String, openReports: List[Report]) = add {
    Modlog(ModId.lichess.value, user.some, Modlog.selfCloseAccount,
      details = openReports.map(r => s"${r.reason.name} report").mkString(", ").some.filter(_.nonEmpty))
  }

  def reopenAccount(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.reopenAccount)
  }

  def addTitle(mod: String, user: String, title: String) = add {
    Modlog(mod, user.some, Modlog.setTitle, title.some)
  }

  def removeTitle(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.removeTitle)
  }

  def setEmail(mod: String, user: String) = add {
    Modlog(mod, user.some, Modlog.setEmail)
  }

  def ipban(mod: String, ip: String) = add {
    Modlog(mod, none, Modlog.ipban, ip.some)
  }

  def deletePost(mod: String, user: Option[String], author: Option[String], ip: Option[String], text: String) = add {
    Modlog(mod, user, Modlog.deletePost, details = Some(
      author.??(_ + " ") + ip.??(_ + " ") + text.take(140)
    ))
  }

  def toggleCloseTopic(mod: String, categ: String, topic: String, closed: Boolean) = add {
    Modlog(mod, none, closed ? Modlog.closeTopic | Modlog.openTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def toggleHideTopic(mod: String, categ: String, topic: String, hidden: Boolean) = add {
    Modlog(mod, none, hidden ? Modlog.hideTopic | Modlog.showTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def toggleStickyTopic(mod: String, categ: String, topic: String, sticky: Boolean) = add {
    Modlog(mod, none, sticky ? Modlog.stickyTopic | Modlog.unstickyTopic, details = Some(
      categ + " / " + topic
    ))
  }

  def deleteQaQuestion(mod: String, user: String, title: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaQuestion, details = Some(title take 140))
  }

  def deleteQaAnswer(mod: String, user: String, text: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaAnswer, details = Some(text take 140))
  }

  def deleteQaComment(mod: String, user: String, text: String) = add {
    Modlog(mod, user.some, Modlog.deleteQaComment, details = Some(text take 140))
  }

  def deleteTeam(mod: String, name: String, desc: String) = add {
    Modlog(mod, none, Modlog.deleteTeam, details = s"$name / $desc".take(200).some)
  }

  def terminateTournament(mod: String, name: String) = add {
    Modlog(mod, none, Modlog.terminateTournament, details = name.some)
  }

  def chatTimeout(mod: String, user: String, reason: String) = add {
    Modlog(mod, user.some, Modlog.chatTimeout, details = reason.some)
  }

  def setPermissions(mod: String, user: String, permissions: List[Permission]) = add {
    Modlog(mod, user.some, Modlog.permissions, details = permissions.mkString(", ").some)
  }

  def reportban(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.reportban else Modlog.unreportban)
  }

  def modMessage(mod: String, user: String, subject: String) = add {
    Modlog(mod, user.some, Modlog.modMessage, details = subject.some)
  }

  def coachReview(mod: String, coach: String, author: String) = add {
    Modlog(mod, coach.some, Modlog.coachReview, details = s"by $author".some)
  }

  def cheatDetected(user: String, gameId: String) = add {
    Modlog("lichess", user.some, Modlog.cheatDetected, details = s"game $gameId".some)
  }

  def cli(by: String, command: String) = add {
    Modlog(by, none, Modlog.cli, command.some)
  }

  def garbageCollect(mod: Mod, sus: Suspect) = add {
    Modlog.make(mod, sus, Modlog.garbageCollect)
  }

  def rankban(mod: Mod, sus: Suspect, v: Boolean) = add {
    Modlog.make(mod, sus, if (v) Modlog.rankban else Modlog.unrankban)
  }

  def recent = coll.find($empty).sort($sort naturalDesc).cursor[Modlog]().gather[List](100)

  def wasUnengined(sus: Suspect) = coll.exists($doc(
    "user" -> sus.user.id,
    "action" -> Modlog.unengine
  ))

  def wasUnbooster(userId: String) = coll.exists($doc(
    "user" -> userId,
    "action" -> Modlog.unbooster
  ))

  def userHistory(userId: String): Fu[List[Modlog]] =
    coll.find($doc("user" -> userId)).sort($sort desc "date").cursor[Modlog]().gather[List](30)

  private def add(m: Modlog): Funit = {
    lila.mon.mod.log.create()
    lila.log("mod").info(m.toString)
    coll.insert(m).void
  }
}
