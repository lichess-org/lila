package lila.mod

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.irc.IrcApi
import lila.msg.MsgPreset
import lila.report.{ Mod, ModId, Report, Suspect }
import lila.security.Permission
import lila.user.{ User, UserRepo }

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, ircApi: IrcApi)(using Executor):

  private def coll = repo.coll

  private given BSONDocumentHandler[Modlog]           = Macros.handler
  private given BSONDocumentHandler[Modlog.UserEntry] = Macros.handler

  private val markActions = List(Modlog.alt, Modlog.booster, Modlog.closeAccount, Modlog.engine, Modlog.troll)

  def streamerDecline(mod: Mod, streamerId: UserId) = add:
    Modlog(mod.id, streamerId.some, Modlog.streamerDecline)

  def streamerList(mod: Mod, streamerId: UserId, v: Boolean) = add:
    Modlog(mod.id, streamerId.some, if (v) Modlog.streamerList else Modlog.streamerUnlist)

  def streamerTier(mod: Mod, streamerId: UserId, v: Int) = add:
    Modlog(mod.id, streamerId.some, Modlog.streamerTier, v.toString.some)

  def blogTier(mod: Mod, sus: Suspect, tier: String) = add:
    Modlog.make(mod, sus, Modlog.blogTier, tier.some)

  def blogPostEdit(mod: Mod, sus: Suspect, postId: UblogPostId, postName: String, action: String) = add:
    Modlog.make(mod, sus, Modlog.blogPostEdit, s"$action #$postId $postName".some)

  def practiceConfig(mod: UserId) = add:
    Modlog(mod into ModId, none, Modlog.practiceConfig)

  def alt(mod: Mod, sus: Suspect, v: Boolean) = add:
    Modlog.make(mod, sus, if (v) Modlog.alt else Modlog.unalt)

  def engine(mod: Mod, sus: Suspect, v: Boolean) = add:
    Modlog.make(mod, sus, if (v) Modlog.engine else Modlog.unengine)

  def booster(mod: Mod, sus: Suspect, v: Boolean) = add:
    Modlog.make(mod, sus, if (v) Modlog.booster else Modlog.unbooster)

  def troll(mod: Mod, sus: Suspect) = add:
    Modlog.make(mod, sus, if (sus.user.marks.troll) Modlog.troll else Modlog.untroll)

  def setKidMode(mod: ModId, kid: UserId) = add:
    Modlog(mod, kid.some, Modlog.setKidMode)

  def loginWithBlankedPassword(user: UserId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.blankedPassword)

  def loginWithWeakPassword(user: UserId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.weakPassword)

  def disableTwoFactor(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.disableTwoFactor)

  def closeAccount(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.closeAccount)

  def selfCloseAccount(user: UserId, openReports: List[Report]) = add:
    Modlog(
      User.lichessId into ModId,
      user.some,
      Modlog.selfCloseAccount,
      details = openReports.map(r => s"${r.reason.name} report").mkString(", ").some.filter(_.nonEmpty)
    )

  def closedByMod(user: User): Fu[Boolean] =
    fuccess(user.marks.alt) >>| coll.exists($doc("user" -> user.id, "action" -> Modlog.closeAccount))

  def reopenAccount(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.reopenAccount)

  def addTitle(mod: ModId, user: UserId, title: String) = add:
    Modlog(mod, user.some, Modlog.setTitle, title.some)

  def removeTitle(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.removeTitle)

  def setEmail(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.setEmail)

  def deletePost(mod: ModId, user: Option[UserId], text: String) = add:
    Modlog(
      mod,
      user,
      Modlog.deletePost,
      details = Some(text.take(400))
    )

  def toggleCloseTopic(mod: ModId, categ: ForumCategId, topicSlug: String, closed: Boolean) = add:
    Modlog(
      mod,
      none,
      if (closed) Modlog.closeTopic else Modlog.openTopic,
      details = s"$categ/$topicSlug".some
    )

  def toggleStickyTopic(mod: ModId, categ: ForumCategId, topicSlug: String, sticky: Boolean) = add:
    Modlog(
      mod,
      none,
      if (sticky) Modlog.stickyTopic else Modlog.unstickyTopic,
      details = s"$categ/$topicSlug".some
    )

  // Not to be confused with the eponymous lichess account.
  def postOrEditAsAnonMod(
      mod: ModId,
      categ: ForumCategId,
      topic: String,
      postId: ForumPostId,
      text: String,
      edit: Boolean
  ) = add:
    Modlog(
      mod,
      none,
      if (edit) Modlog.editAsAnonMod else Modlog.postAsAnonMod,
      details = s"$categ/$topic id: $postId ${text.take(400)}".some
    )

  def deleteTeam(mod: ModId, id: String, explain: String) = add:
    Modlog(
      mod,
      none,
      Modlog.deleteTeam,
      details = s"$id: ${explain take 200}".some
    ) indexAs "team"

  def toggleTeam(mod: ModId, id: String, closing: Boolean, explain: String) = add:
    Modlog(
      mod,
      none,
      if (closing) Modlog.disableTeam else Modlog.enableTeam,
      details = s"$id: ${explain take 200}".some
    ) indexAs "team"

  def teamLog(teamId: TeamId): Fu[List[Modlog]] =
    repo.coll
      .find($doc("index" -> "team", "details" $startsWith s"$teamId: "))
      .sort($sort desc "date")
      .cursor[Modlog]()
      .list(30)

  def terminateTournament(mod: ModId, name: String) = add:
    Modlog(mod, none, Modlog.terminateTournament, details = name.some)

  def chatTimeout(mod: ModId, user: UserId, reason: String, text: String) = add:
    Modlog(mod, user.some, Modlog.chatTimeout, details = s"$reason: $text".some)

  def setPermissions(mod: Mod, user: UserId, permissions: Map[Permission, Boolean]) = add:
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

  def wasUnteachered(user: UserId): Fu[Boolean] =
    coll.exists($doc("user" -> user, "details" $regex s"-${Permission.Teacher.toString}"))

  def wasMarkedBy(mod: ModId, user: UserId): Fu[Boolean] =
    coll.secondaryPreferred.exists(
      $doc(
        "user" -> user,
        "mod"  -> mod,
        "action" $in markActions
      )
    )

  def wereMarkedBy(mod: ModId, users: List[UserId]): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      "user",
      $doc(
        "user" $in users,
        "mod" -> mod,
        "action" $in markActions
      ),
      readPreference = ReadPreference.secondaryPreferred
    )

  def reportban(mod: Mod, sus: Suspect, v: Boolean) = add:
    Modlog.make(mod, sus, if (v) Modlog.reportban else Modlog.unreportban)

  def modMessage(mod: ModId, user: UserId, subject: String) = add:
    Modlog(mod, user.some, Modlog.modMessage, details = subject.some)

  def coachReview(mod: ModId, coach: UserId, author: UserId) = add:
    Modlog(mod, coach.some, Modlog.coachReview, details = s"by $author".some)

  def cheatDetected(user: UserId, gameId: GameId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.cheatDetected, details = s"game $gameId".some)

  def cheatDetectedAndCount(user: UserId, gameId: GameId): Fu[Int] = for
    prevCount <- countRecentCheatDetected(user)
    _         <- cheatDetected(user, gameId)
  yield prevCount + 1

  def cli(by: ModId, command: String) = add:
    Modlog(by, none, Modlog.cli, command.some)

  def garbageCollect(mod: Mod, sus: Suspect) = add:
    Modlog.make(mod, sus, Modlog.garbageCollect)

  def rankban(mod: Mod, sus: Suspect, v: Boolean) = add:
    Modlog.make(mod, sus, if (v) Modlog.rankban else Modlog.unrankban)

  def teamKick(mod: ModId, user: UserId, teamName: String) = add:
    Modlog(mod, user.some, Modlog.teamKick, details = Some(teamName take 140))

  def teamEdit(mod: ModId, teamOwner: UserId, teamName: String) = add:
    Modlog(mod, teamOwner.some, Modlog.teamEdit, details = Some(teamName take 140))

  def appealPost(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.appealPost, details = none)

  def wasUnengined(sus: Suspect) = coll.exists:
    $doc(
      "user"   -> sus.user.id,
      "action" -> Modlog.unengine
    )

  def wasUnbooster(userId: UserId) = coll.exists:
    $doc(
      "user"   -> userId,
      "action" -> Modlog.unbooster
    )

  def userHistory(userId: UserId): Fu[List[Modlog]] =
    coll.find($doc("user" -> userId)).sort($sort desc "date").cursor[Modlog]().list(60)

  def countRecentCheatDetected(userId: UserId): Fu[Int] =
    coll.secondaryPreferred.countSel(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.cheatDetected,
        "date" $gte nowInstant.minusMonths(6)
      )
    )

  def countRecentRatingManipulationsWarnings(userId: UserId): Fu[Int] =
    coll.secondaryPreferred.countSel(
      $doc(
        "user"   -> userId,
        "action" -> Modlog.modMessage,
        $or($doc("details" -> MsgPreset.sandbagAuto.name), $doc("details" -> MsgPreset.boostAuto.name)),
        "date" $gte nowInstant.minusMonths(6)
      )
    )

  def recentBy(mod: Mod) =
    coll.tempPrimary
      .find($doc("mod" -> mod.id))
      .sort($sort desc "date")
      .cursor[Modlog]()
      .list(100)

  def addModlog(users: List[User]): Fu[List[UserWithModlog]] =
    coll.tempPrimary
      .find(
        $doc(
          "user" $in users.filter(_.marks.value.nonEmpty).map(_.id),
          "action" $in List(
            Modlog.engine,
            Modlog.troll,
            Modlog.booster,
            Modlog.closeAccount,
            Modlog.alt,
            Modlog.reportban
          )
        ),
        $doc("user" -> true, "action" -> true, "date" -> true).some
      )
      .sort($sort desc "date")
      .cursor[Modlog.UserEntry]()
      .listAll()
      .map {
        _.foldLeft(users.map(UserWithModlog(_, Nil))) { (users, log) =>
          users.map {
            case UserWithModlog(user, prevLog) if log.user is user =>
              UserWithModlog(user, log :: prevLog)
            case u => u
          }
        }
      }

  private def add(m: Modlog): Funit =
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    m.notable ?? {
      coll.insert.one {
        bsonWriteObjTry[Modlog](m).get ++ (!m.isLichess).??($doc("human" -> true))
      } >> (m.notableZulip ?? zulipMonitor(m))
    }

  private def zulipMonitor(m: Modlog): Funit =
    import lila.mod.{ Modlog as M }
    val icon = m.action match
      case M.alt | M.engine | M.booster | M.troll | M.closeAccount          => "thorhammer"
      case M.unalt | M.unengine | M.unbooster | M.untroll | M.reopenAccount => "blue_circle"
      case M.deletePost | M.deleteTeam | M.terminateTournament              => "x"
      case M.chatTimeout                                                    => "hourglass_flowing_sand"
      case M.closeTopic | M.disableTeam                                     => "locked"
      case M.openTopic | M.enableTeam                                       => "unlocked"
      case M.modMessage | M.postAsAnonMod | M.editAsAnonMod                 => "left_speech_bubble"
      case M.blogTier | M.blogPostEdit                                      => "note"
      case _                                                                => "gear"
    val text = s"""${m.showAction.capitalize} ${m.user.??(u => s"@$u")} ${~m.details}"""
    userRepo.isMonitoredMod(m.mod) flatMapz {
      val monitorType = m.action match
        case M.closeAccount | M.alt => None
        case M.engine | M.unengine | M.reopenAccount | M.unalt =>
          Some(IrcApi.ModDomain.Cheat)
        case M.booster | M.unbooster => Some(IrcApi.ModDomain.Boost)
        case M.troll | M.untroll | M.chatTimeout | M.closeTopic | M.openTopic | M.disableTeam | M.enableTeam |
            M.setKidMode | M.deletePost | M.postAsAnonMod | M.editAsAnonMod | M.blogTier | M.blogPostEdit =>
          Some(IrcApi.ModDomain.Comm)
        case _ => Some(IrcApi.ModDomain.Other)
      monitorType ?? {
        ircApi.monitorMod(m.mod.id, icon = icon, text = text, _)
      }
    }
    ircApi.logMod(m.mod.id, icon = icon, text = text)
