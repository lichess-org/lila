package lila.forum

import reactivemongo.api.bson.Macros.Annotations.Key

case class Usermod(
    @Key("_id") id: UserId,
    impunity: Boolean = false,
    timeouts: List[Instant] = Nil,
    positiveReactions: Map[ForumPostId, Map[UserId, Instant]] = Map.empty,
    negativeReports: Map[ForumPostId, Usermod.Report] = Map.empty,
    ownReportsConfirmed: List[Instant] = Nil,
    ownReportsRejected: List[Instant] = Nil,
    seq: Int = 0,
    requestSeq: Int = 0
):
  def activeComplaints: Option[Usermod.NegativeReports] =
    timeouts.lastOption
      .filter(_.isAfterNow)
      .map: until =>
        Usermod.NegativeReports(until, negativeReports.filter(_._2.complaints.nonEmpty))

  def isTimedOut = timeouts.lastOption.exists(_.isAfterNow)

  def score(
      now: Instant,
      sunsetPeriod: FiniteDuration,
      reporters: Map[UserId, Usermod]
  ) =
    negativeReports.valuesIterator
      .map: post =>
        post.complaints.iterator
          .map: (userId, reason) =>
            reason.points * reporters
              .get(userId)
              .fold(1d)(_.ownReliability(now, sunsetPeriod)) *
              sunset(post.createdAt, now, sunsetPeriod)
          .sum
      .sum
      - positiveReactions.valuesIterator
        .flatMap(_.valuesIterator)
        .map(at => sunset(at, now, sunsetPeriod))
        .sum

  def ownReliability(now: Instant, sunsetPeriod: FiniteDuration) =
    val cutoff = now.minusMillis(sunsetPeriod.toMillis)
    val rejected = ownReportsRejected.count(!_.isBefore(cutoff))
    if rejected == 0 then 1d
    else
      val confirmed = 0.4d + ownReportsConfirmed.count(!_.isBefore(cutoff))
      confirmed / (rejected + confirmed)

  def update(action: Usermod.Action, post: ForumPost, userId: UserId): Usermod =
    val postReport = negativeReports.get(post.id)
    val complaints = postReport.fold(Map.empty[UserId, Usermod.Reason])(_.complaints)
    copy(
      positiveReactions = action match
        case Usermod.Action.Positive(true) =>
          positiveReactions.updated(
            post.id,
            positiveReactions.getOrElse(post.id, Map.empty).updated(userId, nowInstant)
          )

        case Usermod.Action.Positive(false) =>
          positiveReactions
            .get(post.id)
            .fold(positiveReactions): reactions =>
              val remaining = reactions - userId
              if remaining.isEmpty then positiveReactions - post.id
              else positiveReactions.updated(post.id, remaining)

        case _ => positiveReactions
      ,
      negativeReports = action match
        case Usermod.Action.Complaint(reason, topic) =>
          val original = postReport.getOrElse:
            import java.nio.charset.StandardCharsets.UTF_8
            val bytes = // dropWhile trims any partial codepoint
              post.text.getBytes(UTF_8).takeRight(400).dropWhile(b => (b & 0xc0) == 0x80)
            Usermod.Report(post.createdAt, topic, String(bytes, UTF_8), Map.empty)
          negativeReports.updated(post.id, original.copy(complaints = complaints.updated(userId, reason)))

        case Usermod.Action.RemoveComplaint =>
          postReport.fold(negativeReports): report =>
            negativeReports.updated(post.id, report.copy(complaints = complaints - userId))

        case _ => negativeReports
    )

  private def sunset(
      at: Instant,
      now: Instant,
      sunsetPeriod: FiniteDuration
  ) =
    (1 - (now.toEpochMilli - at.toEpochMilli).atLeast(0).toDouble / sunsetPeriod.toMillis)
      .atLeast(0)
      .atMost(1)

object Usermod:
  enum Action:
    case Positive(value: Boolean)
    case Complaint(reason: Reason, topicName: String)
    case RemoveComplaint

  case class Report(
      createdAt: Instant,
      topic: String,
      originalText: String,
      complaints: Map[UserId, Reason]
  )
  case class NegativeReports(until: Instant, posts: Map[ForumPostId, Report])

  // this ordering of reasons by point value must be reflected in the usermod arbitration prompt.
  // an unconfirmable complaint that could be confirmed for a higher valued reason cannot be
  // rejected. i.e. reporting an abusive post as off-topic cannot be rejected, but reporting
  // an off-topic post as abusive should be.
  enum Reason(val key: String, val points: Double):
    case Disagree extends Reason("disagree", 0) // this can never become non-zero
    case OffTopic extends Reason("offTopic", 1)
    case Troll extends Reason("troll", 2)
    case Spam extends Reason("spam", 4)
    case Offensive extends Reason("offensive", 4)
    case Abusive extends Reason("abusive", 6)

  object Reason:
    def apply(key: String): Option[Reason] = values.find(_.key == key)
