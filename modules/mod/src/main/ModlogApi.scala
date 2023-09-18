package lila.mod

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.irc.IrcApi
import lila.msg.MsgPreset
import lila.report.{ Mod, ModId, Report, Suspect }
import lila.security.Permission
import lila.user.{ User, UserRepo, Me }

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, ircApi: IrcApi, presetsApi: ModPresetsApi)(using
    Executor
):
  import repo.coll

  private given BSONDocumentHandler[Modlog]           = Macros.handler
  private given BSONDocumentHandler[Modlog.UserEntry] = Macros.handler
  private given Conversion[Me, ModId]                 = _.modId

  private val markActions = List(Modlog.alt, Modlog.booster, Modlog.closeAccount, Modlog.engine, Modlog.troll)

  def streamerDecline(streamerId: UserId)(using Me.Id) = add:
    Modlog(streamerId.some, Modlog.streamerDecline)

  def streamerList(streamerId: UserId, v: Boolean)(using Me.Id) = add:
    Modlog(streamerId.some, if v then Modlog.streamerList else Modlog.streamerUnlist)

  def streamerTier(streamerId: UserId, v: Int)(using Me.Id) = add:
    Modlog(streamerId.some, Modlog.streamerTier, v.toString.some)

  def blogTier(sus: Suspect, tier: String)(using Me.Id) = add:
    Modlog.make(sus, Modlog.blogTier, tier.some)

  def blogPostEdit(sus: Suspect, postId: UblogPostId, postName: String, action: String)(using Me.Id) = add:
    Modlog.make(sus, Modlog.blogPostEdit, s"$action #$postId $postName".some)

  def practiceConfig(using Me.Id) = add:
    Modlog(none, Modlog.practiceConfig)

  def alt(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.alt else Modlog.unalt)

  def engine(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.engine else Modlog.unengine)

  def booster(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.booster else Modlog.unbooster)

  def troll(sus: Suspect)(using Me.Id) = add:
    Modlog.make(sus, if sus.user.marks.troll then Modlog.troll else Modlog.untroll)

  def setKidMode(mod: ModId, kid: UserId) = add:
    Modlog(mod, kid.some, Modlog.setKidMode)

  def loginWithBlankedPassword(user: UserId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.blankedPassword)

  def loginWithWeakPassword(user: UserId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.weakPassword)

  def disableTwoFactor(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.disableTwoFactor)

  def closeAccount(user: UserId)(using me: Me) = add:
    Modlog(me, user.some, Modlog.closeAccount)

  def selfCloseAccount(user: UserId, openReports: List[Report]) = add:
    Modlog(
      User.lichessId into ModId,
      user.some,
      Modlog.selfCloseAccount,
      details = openReports.map(r => s"${r.reason.name} report").mkString(", ").some.filter(_.nonEmpty)
    )

  def closedByMod(user: User): Fu[Boolean] =
    fuccess(user.marks.alt) >>| coll.exists($doc("user" -> user.id, "action" -> Modlog.closeAccount))

  def reopenAccount(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.reopenAccount)

  def addTitle(user: UserId, title: String)(using Me) = add:
    Modlog(user.some, Modlog.setTitle, title.some)

  def removeTitle(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.removeTitle)

  def setEmail(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.setEmail)

  def deletePost(user: Option[UserId], text: String)(using Me) = add:
    Modlog(
      user,
      Modlog.deletePost,
      details = Some(text.take(400))
    )

  def toggleCloseTopic(categ: ForumCategId, topicSlug: String, closed: Boolean)(using Me) = add:
    Modlog(
      none,
      if closed then Modlog.closeTopic else Modlog.openTopic,
      details = s"$categ/$topicSlug".some
    )

  def toggleStickyTopic(categ: ForumCategId, topicSlug: String, sticky: Boolean)(using Me) = add:
    Modlog(
      none,
      if sticky then Modlog.stickyTopic else Modlog.unstickyTopic,
      details = s"$categ/$topicSlug".some
    )

  // Not to be confused with the eponymous lichess account.
  def postOrEditAsAnonMod(
      categ: ForumCategId,
      topic: String,
      postId: ForumPostId,
      text: String,
      edit: Boolean
  )(using Me.Id) = add:
    Modlog(
      none,
      if edit then Modlog.editAsAnonMod else Modlog.postAsAnonMod,
      details = s"$categ/$topic id: $postId ${text.take(400)}".some
    )

  def deleteTeam(id: String, explain: String)(using Me.Id) = add:
    Modlog(
      none,
      Modlog.deleteTeam,
      details = s"$id: ${explain take 200}".some
    ) indexAs "team"

  def toggleTeam(id: String, closing: Boolean, explain: String)(using Me.Id) = add:
    Modlog(
      none,
      if closing then Modlog.disableTeam else Modlog.enableTeam,
      details = s"$id: ${explain take 200}".some
    ) indexAs "team"

  def teamLog(teamId: TeamId): Fu[List[Modlog]] =
    repo.coll
      .find($doc("index" -> "team", "details" $startsWith s"$teamId: "))
      .sort($sort desc "date")
      .cursor[Modlog]()
      .list(30)

  def terminateTournament(name: String)(using Me) = add:
    Modlog(none, Modlog.terminateTournament, details = name.some)

  def chatTimeout(user: UserId, reason: String, text: String)(using Me.Id) = add:
    Modlog(user.some, Modlog.chatTimeout, details = s"$reason: $text".some)

  def setPermissions(user: UserId, permissions: Map[Permission, Boolean])(using Me) = add:
    Modlog(
      user.some,
      Modlog.permissions,
      details = permissions
        .map: (p, dir) =>
          s"${if dir then "+" else "-"}${p}"
        .mkString(", ")
        .some
    )

  def wasUnteachered(user: UserId): Fu[Boolean] =
    coll.exists($doc("user" -> user, "details" $regex s"-${Permission.Teacher.toString}"))

  def wasMarkedBy(user: UserId)(using me: Me): Fu[Boolean] =
    coll.secondaryPreferred.exists:
      $doc(
        "user" -> user,
        "mod"  -> me.userId,
        "action" $in markActions
      )

  def wereMarkedBy(users: List[UserId])(using me: Me): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      "user",
      $doc(
        "user" $in users,
        "mod" -> me.userId,
        "action" $in markActions
      ),
      _.sec
    )

  def reportban(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.reportban else Modlog.unreportban)

  def modMessage(user: UserId, subject: String)(using Me.Id) = add:
    Modlog(user.some, Modlog.modMessage, details = subject.some)

  def coachReview(coach: UserId, author: UserId)(using Me.Id) = add:
    Modlog(coach.some, Modlog.coachReview, details = s"by $author".some)

  def cheatDetected(user: UserId, gameId: GameId) = add:
    Modlog(User.lichessId into ModId, user.some, Modlog.cheatDetected, details = s"game $gameId".some)

  def cheatDetectedAndCount(user: UserId, gameId: GameId): Fu[Int] = for
    prevCount <- countRecentCheatDetected(user)
    _         <- cheatDetected(user, gameId)
  yield prevCount + 1

  def cli(command: String)(using by: Me.Id) = add:
    Modlog(none, Modlog.cli, command.some)

  def garbageCollect(sus: Suspect)(using Me.Id) = add:
    Modlog.make(sus, Modlog.garbageCollect)

  def rankban(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.rankban else Modlog.unrankban)

  def arenaBan(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.arenaBan else Modlog.unArenaBan)

  def prizeban(sus: Suspect, v: Boolean)(using Me.Id) = add:
    Modlog.make(sus, if v then Modlog.prizeban else Modlog.unprizeban)

  def teamKick(user: UserId, teamName: String)(using Me.Id) = add:
    Modlog(user.some, Modlog.teamKick, details = Some(teamName take 140))

  def teamEdit(teamOwner: UserId, teamName: String)(using Me.Id) = add:
    Modlog(teamOwner.some, Modlog.teamEdit, details = Some(teamName take 140))

  def appealPost(user: UserId)(using me: Me) = add:
    Modlog(me, user.some, Modlog.appealPost, details = none)

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
    coll.secondaryPreferred.countSel:
      $doc(
        "user"   -> userId,
        "action" -> Modlog.cheatDetected,
        "date" $gte nowInstant.minusMonths(6)
      )

  def countRecentRatingManipulationsWarnings(userId: UserId): Fu[Int] =
    coll.secondaryPreferred.countSel:
      $doc(
        "user"   -> userId,
        "action" -> Modlog.modMessage,
        $or($doc("details" -> MsgPreset.sandbagAuto.name), $doc("details" -> MsgPreset.boostAuto.name)),
        "date" $gte nowInstant.minusMonths(6)
      )

  def recentBy(mod: Mod) =
    coll.tempPrimary
      .find($doc("mod" -> mod.id))
      .sort($sort desc "date")
      .cursor[Modlog]()
      .list(100)

  def addModlog(users: List[User.WithPerfs]): Fu[List[UserWithModlog]] =
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
      .sort($sort asc "date")
      .cursor[Modlog.UserEntry]()
      .listAll()
      .map:
        _.foldLeft(users.map(UserWithModlog(_, Nil))): (users, log) =>
          users.map:
            case UserWithModlog(user, prevLog) if log.user is user =>
              UserWithModlog(user, log :: prevLog)
            case u => u

  private def add(m: Modlog): Funit =
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    m.notable.so:
      coll.insert.one {
        bsonWriteObjTry[Modlog](m).get ++ (!m.isLichess).so($doc("human" -> true))
      } >> (m.notableZulip so zulipMonitor(m))

  private def zulipMonitor(m: Modlog): Funit =
    import lila.mod.{ Modlog as M }
    given Me.Id = m.mod into Me.Id
    val icon = m.action match
      case M.alt | M.arenaBan | M.engine | M.booster | M.troll | M.closeAccount            => "thorhammer"
      case M.unalt | M.unArenaBan | M.unengine | M.unbooster | M.untroll | M.reopenAccount => "blue_circle"
      case M.deletePost | M.deleteTeam | M.terminateTournament                             => "x"
      case M.chatTimeout                                    => "hourglass_flowing_sand"
      case M.closeTopic | M.disableTeam                     => "locked"
      case M.openTopic | M.enableTeam                       => "unlocked"
      case M.modMessage | M.postAsAnonMod | M.editAsAnonMod => "left_speech_bubble"
      case M.blogTier | M.blogPostEdit                      => "note"
      case _                                                => "gear"
    val text = s"""${m.showAction.capitalize} ${m.user.so(u => s"@$u")} ${~m.details}"""
    userRepo.getRoles(m.mod).map(Permission(_)) flatMap { permissions =>
      import IrcApi.{ ModDomain as domain }
      val monitorType = m.action match
        case M.closeAccount | M.alt => None
        case M.engine | M.unengine | M.reopenAccount | M.unalt =>
          Some(domain.Cheat)
        case M.booster | M.unbooster | M.arenaBan | M.unArenaBan => Some(domain.Boost)
        case M.troll | M.untroll | M.chatTimeout | M.closeTopic | M.openTopic | M.disableTeam | M.enableTeam |
            M.setKidMode | M.deletePost | M.postAsAnonMod | M.editAsAnonMod | M.blogTier | M.blogPostEdit =>
          Some(domain.Comm)
        case _ => Some(domain.Other)
      import Permission.*
      monitorType.so: dom =>
        val monitorable = dom match
          case domain.Cheat => permissions(MonitoredCheatMod)
          case domain.Boost => permissions(MonitoredBoostMod)
          case domain.Comm  => permissions(MonitoredCommMod)
          case domain.Other if m.action == M.modMessage =>
            val presetPerms = m.details.so(presetsApi.permissionsByName)
            if presetPerms(Permission.Shusher) then permissions(MonitoredCommMod)
            else if presetPerms(Permission.BoostHunter) then permissions(MonitoredBoostMod)
            else if presetPerms(Permission.CheatHunter) then permissions(MonitoredCheatMod)
            else false
          case _ => false
        monitorable so ircApi.monitorMod(icon = icon, text = text, dom)
    }
