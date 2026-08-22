package lila.forum

import play.api.libs.json.*

import lila.core.data.Text
import lila.core.msg.{ MsgApi, SystemMsg }
import lila.core.perm.Granter
import lila.core.user.User
import lila.db.dsl.{ *, given }
import lila.memo.SettingStore.Text.given

final class UsermodApi(
    coll: Coll,
    topicRepo: ForumTopicRepo,
    userApi: lila.core.user.UserApi,
    automod: lila.report.Automod,
    ircApi: lila.core.irc.IrcApi,
    msgApi: MsgApi,
    settingStore: lila.memo.SettingStore.Builder
)(using Executor):

  import BSONHandlers.given

  val promptSetting = settingStore[Text](
    "forumUsermodPrompt",
    text = "Usermod prompt".some,
    default = Text("")
  )
  val modelSetting = settingStore[String](
    "forumUsermodModel",
    text = "Usermod model".some,
    default = "Qwen/Qwen3.7-Max"
  )
  val timeoutIncrementHoursSetting = settingStore[Int](
    "forumUsermodTimeoutIncrementHours",
    text = "Usermod timeout increment: Hours added for each previous timeout within the sunset period".some,
    default = 24
  )
  val sunsetPeriodDaysSetting = settingStore[Int](
    "forumUsermodSunsetPeriodDays",
    text = "Usermod sunset period: Days after which total rehabilitation occurs.".some,
    default = 14
  )
  val timeoutPointsThresholdSetting = settingStore[Int](
    "forumUsermodTimeoutPointsThreshold",
    text = s"""Usermod threshold: Point totals above this value will trigger an automod timeout check.
              |These modifiers are diluted by reporter reliability and the sunset period:
              |${Usermod.Reason.values
               .map(r => s"${r.key}=${r.points}")
               .mkString(", ")}""".stripMargin.some,
    default = 8
  )

  def isTimedOut(userId: UserId): Fu[Boolean] =
    coll.byId[Usermod](userId).dmap(_.exists(_.isTimedOut))

  def active(userId: UserId): Fu[Option[Usermod.NegativeReports]] =
    coll.byId[Usermod](userId).dmap(_.flatMap(_.activeComplaints))

  def get(userId: UserId): Fu[Option[Usermod]] = coll.byId[Usermod](userId)

  def manualTimeout(user: User)(using me: Me): Fu[Instant] =
    coll
      .byId[Usermod](user.id)
      .flatMap: usermod =>
        val duration = timeoutDuration(usermod.fold(Nil)(_.timeouts))
        val until = usermod
          .flatMap(_.timeouts.lastOption)
          .filter(_.isAfterNow)
          .getOrElse(nowInstant)
          .plusMillis(duration.toMillis)
        coll.update
          .one($id(user.id), $push("timeouts" -> until), upsert = true)
          .inject:
            lila.common.Bus.pub(
              lila.core.mod.ForumTimeout(me.userId, user.id, duration.toHours.toInt)
            )
            until

  def report(post: ForumPost, reaction: ForumPost.Reaction, value: Boolean, reason: Option[Usermod.Reason])(
      using me: Me
  ): Funit =
    post.userId
      .filter(_ => !me.marks.troll)
      .so: userId =>
        userApi
          .byId(userId)
          .flatMap:
            case Some(user) if Granter.ofUser(_.ModerateForum)(user) => funit
            case Some(user) =>
              def update(action: Usermod.Action) =
                updateDb(user): dbUsermod =>
                  val timeoutExpired = dbUsermod.timeouts.lastOption.exists(!_.isAfter(nowInstant))
                  if timeoutExpired then
                    dbUsermod.copy(positiveReactions = Map.empty, negativeReports = Map.empty)
                  else dbUsermod.update(action, post, me.userId)
                .flatMap: reported =>
                  (reported && action.isInstanceOf[Usermod.Action.Complaint]).so:
                    appendDates(me.userId, "ownReports", List(nowInstant), sunsetPeriodDaysSetting.get().days)
              reaction match
                case ForumPost.Reaction.PlusOne => update(Usermod.Action.Positive(value))
                case ForumPost.Reaction.MinusOne if value =>
                  reason
                    .filter(_.points > 0)
                    .fold(update(Usermod.Action.RemoveComplaint)): reason =>
                      topicRepo
                        .byId(post.topicId)
                        .flatMap(_.fold(funit)(topic => update(Usermod.Action.Complaint(reason, topic.name))))
                case ForumPost.Reaction.MinusOne => update(Usermod.Action.RemoveComplaint)
                case _ => funit
            case None => funit

  private def assess(usermod: Usermod, user: User): Funit =
    val sunsetDuration = sunsetPeriodDaysSetting.get().days
    coll
      .byIds[Usermod, UserId](usermod.negativeReports.valuesIterator.flatMap(_.complaints.keys).toSet)
      .map(_.map(usermod => usermod.id -> usermod).toMap)
      .flatMap: reporters =>
        (usermod.activeComplaints.isEmpty && usermod.score(
          nowInstant,
          sunsetDuration,
          reporters
        ) > timeoutPointsThresholdSetting
          .get() && promptSetting.get().value.nonEmpty).so:
          coll.update
            .one(
              $id(usermod.id) ++ $doc("seq" -> usermod.seq, "requestSeq".$lt(usermod.seq)),
              $set("requestSeq" -> usermod.seq)
            )
            .flatMap { result =>
              if result.n > 0 then discard(request(usermod, user))
              funit
            }

  private def request(usermod: Usermod, user: User): Funit =
    val postReports = usermod.negativeReports.toList.sortBy(_._1.value)
    // hide user and post ids from the inference provider
    val postIdToMasked = aliases(postReports.map(_._1), "post")(_.value)
    val maskedToPostId = postIdToMasked.map(_.swap)
    val userIdToMasked = aliases(postReports.flatMap(_._2.complaints.keys), "user")(_.value)
    val maskedToUserId = userIdToMasked.map(_.swap)
    automod
      .text(
        automodText(postReports, postIdToMasked, userIdToMasked),
        promptSetting.get(),
        modelSetting.get()
      )
      .flatMap(processAutomodResponse(usermod, user, maskedToPostId, maskedToUserId))
      .recover { case e =>
        lila.log("forum.usermod").warn(s"Failed for ${usermod.id}: ${e.getMessage}")
      }

  private def automodText(
      postReports: List[(ForumPostId, Usermod.Report)],
      postIdToMasked: Map[ForumPostId, String],
      userIdToMasked: Map[UserId, String]
  ) =
    Json.stringify:
      Json.obj(
        "reports" -> postReports.map: (postId, report) =>
          Json.obj(
            "postId" -> postIdToMasked(postId),
            "topic" -> report.topic,
            "postText" -> report.originalText,
            "allegations" -> report.complaints.toList
              .sortBy(_._1.value)
              .map: (userId, reason) =>
                Json.obj("userId" -> userIdToMasked(userId), "reason" -> reason.key)
          )
      )

  private def processAutomodResponse(
      usermod: Usermod,
      user: User,
      maskedToPostId: Map[String, ForumPostId],
      maskedToUserId: Map[String, UserId]
  )(response: JsObject): Funit =
    val postReports = usermod.negativeReports.toList.sortBy(_._1.value)
    val duration = timeoutDuration(usermod.timeouts)
    val rejectedRaw = verdicts(response, "reject", usermod, maskedToPostId, maskedToUserId)
    val confirmedRaw = verdicts(response, "confirm", usermod, maskedToPostId, maskedToUserId)
    val (rejected, confirmed) = withoutIntersection(rejectedRaw, confirmedRaw)
    val unset = rejected.toList.flatMap: (postId, userIds) =>
      val post = usermod.negativeReports(postId)
      if userIds.size == post.complaints.size then List(s"negative.$postId")
      else userIds.map(userId => s"negative.$postId.complaints.$userId")
    val timeout = response.str("action").contains("timeout")
    val update =
      timeout.so($push("timeouts" -> nowInstant.plusMillis(duration.toMillis))) ++
        unset.nonEmpty.so($unset(unset)) ++
        confirmed.nonEmpty.so($set("requestSeq" -> usermod.seq))
    (timeout || unset.nonEmpty || confirmed.nonEmpty).so:
      coll.update
        .one($id(usermod.id) ++ $doc("seq" -> usermod.seq), update)
        .flatMap: result =>
          if result.n > 0 then
            appendVerdicts(rejected, "ownReportsRejected") >>
              appendVerdicts(confirmed, "ownReportsConfirmed") >>
              timeout.so(
                notifyTimeout(
                  user,
                  postReports,
                  rejected,
                  duration
                )
              )
          else funit

  private def verdicts(
      response: JsObject,
      field: String,
      usermod: Usermod,
      maskedToPostId: Map[String, ForumPostId],
      maskedToUserId: Map[String, UserId]
  ) =
    (response \ field)
      .asOpt[Map[String, List[String]]]
      .getOrElse(Map.empty)
      .toList
      .flatMap: (maskedPostId, maskedUserIds) =>
        maskedToPostId
          .get(maskedPostId)
          .toList
          .flatMap: postId =>
            val userIds = maskedUserIds
              .flatMap(maskedToUserId.get)
              .distinct
              .filter(usermod.negativeReports(postId).complaints.contains)
            userIds.nonEmpty.option(postId -> userIds)
      .toMap

  private def appendVerdicts(
      verdicts: Map[ForumPostId, List[UserId]],
      field: String
  ): Funit =
    val sunsetDuration = sunsetPeriodDaysSetting.get().days
    verdicts.values.flatten.toList
      .groupMapReduce(identity)(_ => 1)(_ + _)
      .toList
      .traverse: (reporterId, count) =>
        appendDates(reporterId, field, List.fill(count)(nowInstant), sunsetDuration)
      .void

  private def withoutIntersection(a: Map[ForumPostId, List[UserId]], b: Map[ForumPostId, List[UserId]]) =
    def subtract(a: Map[ForumPostId, List[UserId]], b: Map[ForumPostId, List[UserId]]) =
      a.flatMap: (postId, userIds) =>
        val remaining = userIds.diff(b.getOrElse(postId, Nil))
        remaining.nonEmpty.option(postId -> remaining)
    (subtract(a, b), subtract(b, a))

  private def timeoutDuration(timeouts: List[Instant]): FiniteDuration =
    val cutoff = nowInstant.minusMillis(sunsetPeriodDaysSetting.get().days.toMillis)
    24.hours + (timeoutIncrementHoursSetting.get() * timeouts.count(!_.isBefore(cutoff))).hours

  private def notifyTimeout(
      user: User,
      postReports: List[(ForumPostId, Usermod.Report)],
      rejected: Map[ForumPostId, List[UserId]],
      duration: FiniteDuration
  ): Funit =
    val timeoutPosts = postReports.flatMap: (postId, report) =>
      val complaints = report.complaints -- rejected.getOrElse(postId, Set.empty)
      if complaints.isEmpty then none
      else
        val reasons = Usermod.Reason.values.toList
          .filter(_.points > 0)
          .flatMap: reason =>
            val count = complaints.values.count(_ == reason)
            if count == 0 then none else s"${reason.key} × $count".some
        s"- ${report.topic}: ${reasons.mkString(", ")}".some
    val notifyReporters = postReports
      .flatMap: (postId, report) =>
        report.complaints.keySet -- rejected.getOrElse(postId, Set.empty)
      .toSet
    lila.common.Bus.pub(lila.core.mod.ForumTimeout(UserId.ai, user.id, duration.toHours.toInt))
    ircApi.forumTimeout(user.light, timeoutPosts) >>
      notifyReporters.toList.sequentiallyVoid: reporterId =>
        msgApi
          .systemPost(
            SystemMsg.standard(
              reporterId,
              s"${user.username.value} has lost forum privileges for ${duration.toHours} hours due to your reporting action."
            )
          )
          .void

  private def updateDb(user: User, retries: Int = 3)(f: Usermod => Usermod): Fu[Boolean] =
    // guard against concurrency races. seq must still equal the value that was read.
    // otherwise, reread the usermod doc and try to reapply (up to 3 times).
    val userId = user.id
    coll
      .byId[Usermod](userId)
      .flatMap:
        case Some(current) if current.impunity => fuccess(false)
        case Some(current) =>
          val updated = f(current).copy(seq = current.seq + 1)
          coll.update
            .one($id(userId) ++ $doc("seq" -> current.seq), updated)
            .flatMap: result =>
              if result.n > 0 then assess(updated, user).inject(true)
              else if retries > 0 then updateDb(user, retries - 1)(f)
              else fuccess(false)
        case _ =>
          val updated = f(Usermod(userId)).copy(seq = 1)
          coll.insert
            .one(updated)
            .flatMap(_ => assess(updated, user).inject(true))
            .recoverWith:
              case _ if retries > 0 => updateDb(user, retries - 1)(f)

  private def appendDates(userId: UserId, field: String, dates: List[Instant], sunsetPeriod: FiniteDuration) =
    coll.update
      .one($id(userId), $pull(field.$lt(nowInstant.minusMillis(sunsetPeriod.toMillis))))
      .flatMap: _ =>
        coll.update.one($id(userId), $pushEach(field, dates*), upsert = true).void

  private def aliases[A](values: Iterable[A], prefix: String)(sort: A => String) =
    values.toList.distinct
      .sortBy(sort)
      .zipWithIndex
      .map((value, index) => value -> s"${prefix}_${index + 1}")
      .toMap
