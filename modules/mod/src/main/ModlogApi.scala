package lila.mod

import reactivemongo.api.*
import reactivemongo.api.bson.*

import lila.core.id.ForumCategId
import lila.core.irc.IrcApi
import lila.core.perf.UserWithPerfs
import lila.core.perm.Permission
import lila.db.dsl.{ *, given }
import lila.report.{ Mod, Report, Suspect }
import lila.user.UserRepo
import lila.core.chat.TimeoutReason
import lila.core.user.KidMode
import lila.core.LightUser
import lila.core.id.ForumTopicSlug

final class ModlogApi(repo: ModlogRepo, userRepo: UserRepo, ircApi: IrcApi, presetsApi: ModPresetsApi)(using
    Executor
) extends lila.core.mod.LogApi:
  import repo.coll

  private given BSONDocumentHandler[Modlog] = Macros.handler
  private given BSONHandler[Modlog.Context] = Macros.handler[Modlog.Context]
  private given BSONDocumentHandler[Modlog.UserEntry] = Macros.handler
  private given Conversion[Me, ModId] = _.modId

  private val markActions =
    List(
      Modlog.alt,
      Modlog.booster,
      Modlog.closeAccount,
      Modlog.engine,
      Modlog.troll,
      Modlog.rankban,
      Modlog.isolate
    )

  def streamerDecline(streamerId: UserId, reason: Option[String])(using MyId) = add:
    Modlog(streamerId.some, Modlog.streamerDecline, reason)

  def streamerList(streamerId: UserId, v: Boolean)(using MyId) = add:
    Modlog(streamerId.some, if v then Modlog.streamerList else Modlog.streamerUnlist)

  def streamerTier(streamerId: UserId, v: Int)(using MyId) = add:
    Modlog(streamerId.some, Modlog.streamerTier, v.toString.some)

  def blogEdit(sus: Suspect, details: String)(using MyId) = add:
    Modlog.make(sus, Modlog.blogTier, details.some)

  def blogPostEdit(sus: Suspect, postId: UblogPostId, postName: String, details: String)(using MyId) = add:
    Modlog.make(
      sus,
      Modlog.blogPostEdit,
      details.some,
      Modlog.Context(postName.some, routes.Ublog.redirect(postId).url.some, postId.value.some).some
    )

  def practiceConfig(using MyId) = add:
    Modlog(none, Modlog.practiceConfig)

  def alt(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.alt else Modlog.unalt)

  def engine(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.engine else Modlog.unengine)

  def booster(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.booster else Modlog.unbooster)

  def troll(sus: Suspect)(using MyId) = add:
    Modlog.make(sus, if sus.user.marks.troll then Modlog.troll else Modlog.untroll)

  def isolate(sus: Suspect)(using MyId) = add:
    Modlog.make(sus, if sus.user.marks.isolate then Modlog.isolate else Modlog.unisolate)

  def deleteComms(sus: Suspect)(using MyId) = add:
    Modlog.make(sus, Modlog.deleteComms)

  def fullCommExport(sus: Suspect)(using MyId) = add:
    Modlog.make(sus, Modlog.fullCommsExport)

  def setKidMode(mod: ModId, kid: UserId, v: KidMode) = add:
    Modlog(mod, kid.some, if v.yes then Modlog.setKidMode else Modlog.unsetKidMode)

  def blankPassword(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.blankPassword)

  def loginWithBlankedPassword(user: UserId) = add:
    Modlog(UserId.lichess.into(ModId), user.some, Modlog.blankedPassword)

  def loginWithWeakPassword(user: UserId) = add:
    Modlog(UserId.lichess.into(ModId), user.some, Modlog.weakPassword)

  def giftPatronMonth(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.giftPatronMonth)

  def disableTwoFactor(mod: ModId, user: UserId) = add:
    Modlog(mod, user.some, Modlog.disableTwoFactor)

  def closeAccount(user: UserId)(using me: Me) = add:
    Modlog(me, user.some, Modlog.closeAccount)

  def teacherCloseAccount(user: UserId)(using me: Me) = add:
    Modlog(me, user.some, Modlog.teacherCloseAccount)

  def selfCloseAccount(user: UserId, forever: Boolean, openReports: List[Report]) = add:
    Modlog(
      UserId.lichess.into(ModId),
      user.some,
      Modlog.selfCloseAccount,
      details = {
        forever.so("forever ") + openReports.map(r => s"${r.room.name} report").mkString(", ")
      }.some.filter(_.nonEmpty)
    )

  def closedByMod(user: User): Fu[Boolean] =
    fuccess(user.marks.alt) >>| coll.exists($doc("user" -> user.id, "action" -> Modlog.closeAccount))

  def closedByTeacher(user: User): Fu[Boolean] =
    coll.exists($doc("user" -> user.id, "action" -> Modlog.teacherCloseAccount))

  def reopenAccount(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.reopenAccount)

  def setTitle(user: UserId, title: String)(using Me) = add:
    Modlog(user.some, Modlog.setTitle, title.some)

  def removeTitle(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.removeTitle)

  def setEmail(user: UserId)(using Me) = add:
    Modlog(user.some, Modlog.setEmail)

  def deletePost(user: Option[UserId], text: String)(using MyId) = add:
    Modlog(
      user,
      Modlog.deletePost,
      details = Some(text.take(400))
    )

  def toggleCloseTopic(categ: ForumCategId, slug: ForumTopicSlug, closed: Boolean)(using MyId) = add:
    Modlog(
      none,
      if closed then Modlog.closeTopic else Modlog.openTopic,
      details = s"$categ/$slug".some
    )

  def toggleStickyTopic(categ: ForumCategId, slug: ForumTopicSlug, sticky: Boolean)(using MyId) = add:
    Modlog(
      none,
      if sticky then Modlog.stickyTopic else Modlog.unstickyTopic,
      details = s"$categ/$slug".some
    )

  // Not to be confused with the eponymous lichess account.
  def postOrEditAsAnonMod(
      categ: ForumCategId,
      topic: ForumTopicSlug,
      postId: ForumPostId,
      text: String,
      edit: Boolean
  )(using MyId) = add:
    Modlog(
      none,
      if edit then Modlog.editAsAnonMod else Modlog.postAsAnonMod,
      details = s"$categ/$topic id: $postId ${text.take(400)}".some
    )

  def deleteTeam(id: TeamId, explain: String)(using MyId) = add:
    Modlog(
      none,
      Modlog.deleteTeam,
      details = s"$id: ${explain.take(200)}".some
    ).indexAs("team")

  def toggleTeam(id: TeamId, closing: Boolean, explain: String)(using MyId) = add:
    Modlog(
      none,
      if closing then Modlog.disableTeam else Modlog.enableTeam,
      details = s"$id: ${explain.take(200)}".some
    ).indexAs("team")

  def teamLog(teamId: TeamId): Fu[List[Modlog]] =
    repo.coll
      .find($doc("index" -> "team", "details".$startsWith(s"$teamId: ")))
      .sort($sort.desc("date"))
      .cursor[Modlog]()
      .list(30)

  def terminateTournament(name: String)(using Me) = add:
    Modlog(none, Modlog.terminateTournament, details = name.some)

  def chatTimeout(user: UserId, reason: TimeoutReason, text: String)(using MyId) = add:
    Modlog(user.some, Modlog.chatTimeout, details = s"${reason.key}: $text".some)

  def setPermissions(user: LightUser, permissions: Map[Permission, Boolean])(using Me) =
    val details = permissions
      .map: (p, dir) =>
        s"${if dir then "+" else "-"}${p}"
      .mkString(", ")
    for
      _ <- add:
        Modlog(
          user.id.some,
          Modlog.permissions,
          details.some
        )
      _ <- ircApi.permissionsLog(user, details)
    yield ()

  def wasUnteachered(user: UserId): Fu[Boolean] =
    coll.exists($doc("user" -> user, "details".$regex(s"-${Permission.Teacher.toString}")))

  def wasMarkedBy(user: UserId)(using me: Me): Fu[Boolean] =
    coll.secondary.exists:
      $doc(
        "user" -> user,
        "mod" -> me.userId,
        "action".$in(markActions)
      )

  def wereMarkedBy(users: List[UserId])(using me: Me): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set](
      "user",
      $doc(
        "user".$in(users),
        "mod" -> me.userId,
        "action".$in(markActions)
      ),
      _.sec
    )

  def reportban(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.reportban else Modlog.unreportban)

  def modMessage(user: UserId, subject: String)(using MyId) = add:
    Modlog(user.some, Modlog.modMessage, details = subject.some)

  def coachReview(coach: UserId, author: UserId)(using MyId) = add:
    Modlog(coach.some, Modlog.coachReview, details = s"by $author".some)

  private def cheatDetected(user: UserId, gameId: GameId) = add:
    Modlog(UserId.lichess.into(ModId), user.some, Modlog.cheatDetected, details = s"game $gameId".some)

  private[mod] def cheatDetectedAndCount(user: UserId, gameId: GameId): Fu[Int] = for
    prevCount <- countRecentCheatDetected(user)
    _ <- cheatDetected(user, gameId)
  yield prevCount + 1

  def cli(command: String)(using by: MyId) = add:
    Modlog(none, Modlog.cli, command.some)

  def garbageCollect(sus: Suspect)(using MyId) = add:
    Modlog.make(sus, Modlog.garbageCollect)

  def rankban(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.rankban else Modlog.unrankban)

  def arenaBan(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.arenaBan else Modlog.unArenaBan)

  def prizeban(sus: Suspect, v: Boolean)(using MyId) = add:
    Modlog.make(sus, if v then Modlog.prizeban else Modlog.unprizeban)

  def teamKick(user: UserId, teamName: String)(using MyId) = add:
    Modlog(user.some, Modlog.teamKick, details = Some(teamName.take(140)))

  def teamEdit(teamOwner: UserId, teamName: String)(using MyId) = add:
    Modlog(teamOwner.some, Modlog.teamEdit, details = Some(teamName.take(140)))

  def wasUnengined(sus: Suspect) = coll.exists:
    $doc(
      "user" -> sus.user.id,
      "action" -> Modlog.unengine
    )

  def wasUnbooster(userId: UserId) = coll.exists:
    $doc(
      "user" -> userId,
      "action" -> Modlog.unbooster
    )

  def timeoutPersonalExport(userId: UserId): Fu[List[Modlog]] =
    coll.secondary
      .find(
        $doc(
          "user" -> userId,
          "action" -> Modlog.chatTimeout
        )
      )
      .sort($sort.desc("date"))
      .cursor[Modlog]()
      .list(100)

  def userHistory(userId: UserId): Fu[List[Modlog]] =
    coll.secondary
      .find(
        $doc(
          "user" -> userId,
          "action".$nin(
            List(
              Modlog.teamKick,
              Modlog.teamEdit
            )
          )
        )
      )
      .sort($sort.desc("date"))
      .cursor[Modlog]()
      .list(100)

  def countRecentCheatDetected(userId: UserId): Fu[Int] =
    coll.secondary.countSel:
      $doc(
        "user" -> userId,
        "action" -> Modlog.cheatDetected,
        "date".$gte(nowInstant.minusMonths(6))
      )

  def countRecentRatingManipulationsWarnings(userId: UserId): Fu[Int] =
    coll.secondary.countSel:
      $doc(
        "user" -> userId,
        "action" -> Modlog.modMessage,
        $or(
          $doc("details" -> SandbagWatch.msgPreset.sandbagAuto.name),
          $doc("details" -> SandbagWatch.msgPreset.boostAuto.name)
        ),
        "date".$gte(nowInstant.minusMonths(6))
      )

  def recentOf(id: Option[String] = None) =
    coll.secondary
      .find(
        "mod".$nin(List(UserId.lichess, UserId.irwin, UserId.kaladin)) ++ id.so(cid =>
          $doc("context.id" -> cid)
        )
      )
      .sort($sort.desc("date"))
      .cursor[Modlog]()
      .list(200)

  def recentBy(mod: Mod) =
    coll.secondary
      .find($doc("mod" -> mod.id))
      .sort($sort.desc("date"))
      .cursor[Modlog]()
      .list(200)

  def withModlogs(users: List[UserWithPerfs]): Fu[List[UserWithModlog]] =
    val onlyUsers = users.filter: u =>
      u.marks.value.nonEmpty || u.enabled.no
    coll.secondary
      .find(
        $doc(
          "user".$in(onlyUsers.map(_.id)),
          "action".$in(
            List(
              Modlog.engine,
              Modlog.troll,
              Modlog.booster,
              Modlog.closeAccount,
              Modlog.selfCloseAccount,
              Modlog.alt,
              Modlog.reportban
            )
          )
        ),
        $doc("user" -> true, "action" -> true, "date" -> true).some
      )
      .sort($sort.asc("date"))
      .cursor[Modlog.UserEntry]()
      .listAll()
      .map:
        _.foldLeft(users.map(UserWithModlog(_, Nil))): (users, log) =>
          users.map:
            case UserWithModlog(user, prevLog) if log.user.is(user.user) =>
              UserWithModlog(user, log :: prevLog)
            case u => u

  private def add(m: Modlog): Funit =
    lila.mon.mod.log.create.increment()
    lila.log("mod").info(m.toString)
    m.notable.so:
      coll.insert.one {
        bsonWriteObjTry[Modlog](m).get ++ (!m.isLichess).so($doc("human" -> true))
      } >> (m.notableZulip.so(zulipMonitor(m)))

  private def zulipMonitor(m: Modlog): Funit =
    import lila.mod.Modlog as M
    given MyId = m.mod.into(MyId)
    val icon = m.action match
      case M.alt | M.arenaBan | M.engine | M.booster | M.troll | M.isolate | M.closeAccount => "thorhammer"
      case M.unalt | M.unArenaBan | M.unengine | M.unbooster | M.untroll | M.unisolate | M.reopenAccount =>
        "blue_circle"
      case M.deletePost | M.deleteTeam | M.terminateTournament => "x"
      case M.chatTimeout => "hourglass_flowing_sand"
      case M.closeTopic | M.disableTeam => "locked"
      case M.openTopic | M.enableTeam => "unlocked"
      case M.modMessage | M.postAsAnonMod | M.editAsAnonMod => "left_speech_bubble"
      case M.blogTier | M.blogPostEdit => "note"
      case _ => "gear"
    val text = s"""${m.showAction.capitalize} ${m.user.so(u => s"@$u")} ${~m.details}"""
    userRepo.getRoles(m.mod).map(Permission.ofDbKeys(_)).flatMap { permissions =>
      import lila.core.irc.ModDomain as domain
      val monitorType = m.action match
        case M.closeAccount | M.alt => None
        case M.engine | M.unengine | M.reopenAccount | M.unalt =>
          Some(domain.Cheat)
        case M.booster | M.unbooster | M.arenaBan | M.unArenaBan => Some(domain.Boost)
        case M.troll | M.untroll | M.isolate | M.unisolate | M.chatTimeout | M.closeTopic | M.openTopic |
            M.disableTeam | M.enableTeam | M.setKidMode | M.unsetKidMode | M.deletePost | M.postAsAnonMod |
            M.editAsAnonMod | M.blogTier | M.blogPostEdit =>
          Some(domain.Comm)
        case _ => Some(domain.Other)
      import Permission.*
      monitorType.so: dom =>
        val monitorable = dom match
          case domain.Cheat => permissions(MonitoredCheatMod)
          case domain.Boost => permissions(MonitoredBoostMod)
          case domain.Comm => permissions(MonitoredCommMod)
          case domain.Other if m.action == M.modMessage =>
            val presetPerms = m.details.so(presetsApi.permissionsByName)
            if presetPerms(Permission.Shusher) then permissions(MonitoredCommMod)
            else if presetPerms(Permission.BoostHunter) then permissions(MonitoredBoostMod)
            else if presetPerms(Permission.CheatHunter) then permissions(MonitoredCheatMod)
            else false
          case _ => false
        for
          _ <- monitorable.so(ircApi.monitorMod(icon = icon, text = text, dom))
          _ <- m.isForum.so(ircApi.publicForumLog(icon = icon, text = text))
        yield ()
    }
