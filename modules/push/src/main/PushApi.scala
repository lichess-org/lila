package lila.push

import akka.actor.*
import play.api.libs.json.*
import scalalib.data.LazyFu

import lila.common.LilaFuture
import lila.common.String.shorten
import lila.core.LightUser
import lila.core.challenge.Challenge
import lila.core.misc.push.TourSoon
import lila.core.notify.{ NotificationContent, PrefEvent, NotifyAllows }
import lila.core.round.{ Tell, RoundBus, MoveEvent }
import lila.core.study.data.StudyName
import lila.core.net.LichessMobileVersion

final private class PushApi(
    firebasePush: FirebasePush,
    webPush: WebPush,
    gameProxy: lila.core.game.GameProxy,
    roundJson: lila.core.round.RoundJson,
    gameRepo: lila.core.game.GameRepo,
    namer: lila.core.game.Namer,
    notifyAllows: lila.core.notify.GetNotifyAllows,
    postApi: lila.core.forum.ForumPostApi,
    lightUser: lila.core.LightUser.GetterFallback
)(using Executor, Scheduler):

  import PushApi.*
  import PushApi.Data.payload

  private[push] def notifyPush(to: Iterable[NotifyAllows], content: NotificationContent): Funit =
    import NotificationContent.*
    content match
      case PrivateMessage(sender, text) =>
        lightUser(sender).flatMap(luser => privateMessage(to.head, sender, luser.titleName, text))
      case MentionedInThread(mentioner, topic, _, _, postId) =>
        lightUser(mentioner).flatMap(luser => forumMention(to.head, luser.titleName, topic, postId))
      case StreamStart(streamerId, streamerName) =>
        streamStart(to, streamerId, streamerName)
      case BroadcastRound(url, title, body) =>
        broadcastRound(to, url, title, body)
      case InvitedToStudy(invitedBy, studyName, studyId) =>
        lightUser(invitedBy).flatMap(luser => invitedToStudy(to.head, luser.titleName, studyName, studyId))
      case _ => funit

  private val offlineRoundNotif = Data.FirebaseMod.NotifOnly(_.filterNot(_._1 == "round")).some

  def finish(game: Game): Funit =
    if !game.isCorrespondence || game.hasAi then funit
    else
      game.userIds.sequentiallyVoid { userId =>
        Pov(game, userId).so: pov =>
          val data = LazyFu: () =>
            for
              nbMyTurn <- gameRepo.countWhereUserTurn(userId)
              opponent <- asyncOpponentName(pov)
            yield Data(
              title = pov.win match
                case Some(true) => "You won!"
                case Some(false) => "You lost."
                case _ => "It's a draw."
              ,
              body = s"Your game with $opponent is over.",
              stacking = Stacking.GameFinish,
              urgency = Urgency.VeryLow,
              payload = payload(userId)(
                "type" -> "gameFinish",
                "gameId" -> game.id.value,
                "fullId" -> pov.fullId.value
              ),
              mobileCompatible = LichessMobileVersion.zero.some,
              iosBadge = nbMyTurn.some,
              firebaseMod = offlineRoundNotif
            )
          for
            _ <- IfAway(pov)(maybePushNotif(userId, _.finish, PrefEvent.gameEvent, data))
            _ <- alwaysPushFirebaseData(userId, _.finish, data)
          yield ()
      }

  def move(move: MoveEvent): Funit =
    LilaFuture.delay(2.seconds):
      gameProxy
        .game(move.gameId)
        .flatMap:
          _.filter(_.playable).so: game =>
            game.sans.lastOption.so: sanMove =>
              game.povs.toList.sequentiallyVoid: pov =>
                pov.player.userId.so: userId =>
                  val data = LazyFu: () =>
                    for
                      _ <- gameProxy.flushIfPresent(
                        game.id
                      ) // ensure game is updated before we count user games
                      nbMyTurn <- gameRepo.countWhereUserTurn(userId)
                      opponent <- asyncOpponentName(pov)
                      payload <- corresGamePayload(pov, "gameMove", userId)
                    yield Data(
                      title = "It's your turn!",
                      body = s"$opponent played $sanMove",
                      stacking = Stacking.GameMove,
                      urgency = if pov.isMyTurn then Urgency.Normal else Urgency.Low,
                      payload = payload,
                      mobileCompatible = LichessMobileVersion.zero.some,
                      iosBadge = nbMyTurn.some,
                      firebaseMod = offlineRoundNotif
                    )
                  for
                    _ <- pov.isMyTurn.so:
                      IfAway(pov)(maybePushNotif(userId, _.move, PrefEvent.gameEvent, data))
                    _ <- alwaysPushFirebaseData(userId, _.move, data)
                  yield ()

  def takebackOffer(gameId: GameId): Funit =
    LilaFuture.delay(1.seconds):
      gameProxy
        .game(gameId)
        .flatMap:
          _.filter(_.playable).so: game =>
            game.players
              .collect:
                case p if p.isProposingTakeback => Pov(game, game.opponent(p))
              .so { pov => // the pov of the receiver
                pov.player.userId.so: userId =>
                  val data = LazyFu: () =>
                    for
                      opponent <- asyncOpponentName(pov)
                      payload <- corresGamePayload(pov, "gameTakebackOffer", userId)
                    yield Data(
                      title = "Takeback offer",
                      body = s"$opponent proposes a takeback",
                      stacking = Stacking.GameTakebackOffer,
                      urgency = Urgency.Normal,
                      payload = payload,
                      mobileCompatible = LichessMobileVersion.zero.some,
                      firebaseMod = offlineRoundNotif
                    )
                  IfAway(pov)(maybePushNotif(userId, _.takeback, PrefEvent.gameEvent, data)) >>
                    alwaysPushFirebaseData(userId, _.takeback, data)
              }

  def drawOffer(gameId: GameId): Funit =
    LilaFuture.delay(1.seconds):
      gameProxy
        .game(gameId)
        .flatMap:
          _.filter(_.playable).so: game =>
            game.players
              .collect:
                case p if p.isOfferingDraw => Pov(game, game.opponent(p))
              .so { pov => // the pov of the receiver
                pov.player.userId.so: userId =>
                  val data = LazyFu: () =>
                    for
                      opponent <- asyncOpponentName(pov)
                      payload <- corresGamePayload(pov, "gameDrawOffer", userId)
                    yield Data(
                      title = "Draw offer",
                      body = s"$opponent offers a draw",
                      stacking = Stacking.GameDrawOffer,
                      urgency = Urgency.Normal,
                      payload = payload,
                      firebaseMod = offlineRoundNotif,
                      mobileCompatible = LichessMobileVersion.zero.some
                    )
                  IfAway(pov)(maybePushNotif(userId, _.draw, PrefEvent.gameEvent, data)) >>
                    alwaysPushFirebaseData(userId, _.draw, data)
              }

  def corresAlarm(pov: Pov): Funit =
    pov.player.userId.so: userId =>
      val data = LazyFu: () =>
        for
          opponent <- asyncOpponentName(pov)
          payload <- corresGamePayload(pov, "corresAlarm", userId)
        yield Data(
          title = "Time is almost up!",
          body = s"You are about to lose on time against $opponent",
          stacking = Stacking.GameMove,
          urgency = Urgency.High,
          payload = payload,
          mobileCompatible = LichessMobileVersion.zero.some,
          firebaseMod = offlineRoundNotif
        )
      maybePushNotif(userId, _.corresAlarm, PrefEvent.gameEvent, data) >>
        alwaysPushFirebaseData(userId, _.corresAlarm, data)

  private def corresGamePayload(pov: Pov, typ: String, userId: UserId): Fu[Data.Payload] =
    roundJson
      .mobileOffline(pov.game, pov.fullId.anyId)
      .map: round =>
        payload(userId)(
          "type" -> typ,
          "gameId" -> pov.gameId.value,
          "fullId" -> pov.fullId.value,
          "round" -> Json.stringify(round)
        )

  def privateMessage(to: NotifyAllows, senderId: UserId, senderName: String, text: String): Funit =
    filterPushNotif(
      to,
      _.message,
      LazyFu.sync:
        Data(
          title = senderName,
          body = text,
          stacking = Stacking.PrivateMessage,
          urgency = Urgency.Normal,
          mobileCompatible = LichessMobileVersion(0, 17).some,
          payload = payload(to.userId)(
            "type" -> "newMessage",
            "threadId" -> senderId.value
          )
        )
    )

  def invitedToStudy(to: NotifyAllows, invitedBy: String, studyName: StudyName, studyId: StudyId): Funit =
    filterPushNotif(
      to,
      _.message,
      LazyFu.sync:
        Data(
          title = studyName.value,
          body = s"$invitedBy invited you to $studyName",
          stacking = Stacking.InvitedStudy,
          urgency = Urgency.Normal,
          mobileCompatible = None,
          payload = payload(to.userId)(
            "type" -> "invitedStudy",
            "invitedBy" -> invitedBy,
            "studyName" -> studyName.value,
            "studyId" -> studyId.value,
            "url" -> s"https://lichess.org/study/$studyId"
          )
        )
    )

  def challengeCreate(c: Challenge): Funit =
    c.destUser.so: dest =>
      c.challengerUser
        .ifTrue(c.clock.isEmpty)
        .so: challenger =>
          lightUser(challenger.id).flatMap: lightChallenger =>
            maybePushNotif(
              dest.id,
              _.challenge.create,
              PrefEvent.challenge,
              LazyFu.sync:
                Data(
                  title = s"${lightChallenger.titleName} (${challenger.rating.show}) challenges you!",
                  body = describeChallenge(c),
                  stacking = Stacking.ChallengeCreate,
                  urgency = Urgency.Normal,
                  payload = payload(dest.id)(
                    "type" -> "challengeCreate",
                    "challengeId" -> c.id.value
                  ),
                  mobileCompatible = None
                )
            )

  def challengeAccept(c: Challenge, joinerId: Option[UserId]): Funit =
    c.challengerUser
      .ifTrue(c.finalColor.white && c.clock.isEmpty)
      .so: challenger =>
        joinerId
          .so(lightUser.optional)
          .flatMap: lightJoiner =>
            maybePushNotif(
              challenger.id,
              _.challenge.accept,
              PrefEvent.challenge,
              LazyFu.sync:
                Data(
                  title = s"${lightJoiner.fold("A player")(_.titleName)} accepts your challenge!",
                  body = describeChallenge(c),
                  stacking = Stacking.ChallengeAccept,
                  urgency = Urgency.Normal,
                  mobileCompatible = None,
                  payload = payload(challenger.id)(
                    "type" -> "challengeAccept",
                    "challengeId" -> c.id.value
                  )
                )
            )

  def tourSoon(tour: TourSoon): Funit =
    tour.userIds.toList.sequentiallyVoid: userId =>
      maybePushNotif(
        userId,
        _.tourSoon,
        PrefEvent.tournamentSoon,
        LazyFu.sync:
          Data(
            title = tour.tourName,
            body = "The tournament is about to start!",
            stacking = Stacking.ChallengeAccept,
            urgency = Urgency.Normal,
            mobileCompatible = None,
            payload = payload(userId)(
              "type" -> "tourSoon",
              "tourId" -> tour.tourId,
              "tourName" -> tour.tourName,
              "path" -> s"/${if tour.swiss then "swiss" else "tournament"}/${tour.tourId}"
            )
          )
      )

  def forumMention(to: NotifyAllows, mentionedBy: String, topicName: String, postId: ForumPostId): Funit =
    filterPushNotif(
      to,
      _.forumMention,
      LazyFu: () =>
        postApi
          .getPost(postId)
          .map: post =>
            Data(
              title = topicName,
              body = post.fold(topicName)(p => shorten(p.text, 57 - 3, "...")),
              stacking = Stacking.ForumMention,
              urgency = Urgency.Low,
              mobileCompatible = None,
              payload = payload(to.userId)(
                "type" -> "forumMention",
                "mentionedBy" -> mentionedBy,
                "topic" -> topicName,
                "postId" -> postId.value,
                "url" -> s"https://lichess.org/forum/redirect/post/$postId"
              )
            )
    )

  def streamStart(recips: Iterable[NotifyAllows], streamerId: UserId, streamerName: String): Funit =
    val pushData = LazyFu.sync:
      Data(
        title = streamerName,
        body = streamerName + " started streaming",
        stacking = Stacking.StreamStart,
        urgency = Urgency.Low,
        payload = payload(
          "type" -> "streamStart",
          "streamerId" -> streamerId.value,
          "url" -> s"https://lichess.org/streamer/$streamerId/redirect"
        ),
        mobileCompatible = None
      )
    val webRecips = recips.collect { case u if u.allows.web => u.userId }
    for _ <- webPush(webRecips, pushData).addEffects: res =>
        lila.mon.push.send.streamStart("web", res.isSuccess, webRecips.size)
    yield recips
      .collect { case u if u.allows.device => u.userId }
      .foreach:
        firebasePush(_, pushData).addEffects: res =>
          lila.mon.push.send.streamStart("firebase", res.isSuccess, 1)

  private type MonitorType = lila.mon.push.send.type => ((String, Boolean, Int) => Unit)

  private def broadcastRound(
      recips: Iterable[NotifyAllows],
      url: String,
      title: String,
      body: String
  ): Funit =
    val pushData = LazyFu.sync:
      Data(
        title = title,
        body = body,
        stacking = Stacking.Generic,
        urgency = Urgency.Normal,
        payload = payload("url" -> url),
        mobileCompatible = None
      )
    val webRecips = recips.collect { case u if u.allows.web => u.userId }
    for _ <- webPush(webRecips, pushData).addEffects: res =>
        lila.mon.push.send.broadcastRound("web", res.isSuccess, webRecips.size)
    yield recips
      .collect { case u if u.allows.device => u.userId }
      .foreach:
        firebasePush(_, pushData).addEffects: res =>
          lila.mon.push.send.broadcastRound("firebase", res.isSuccess, 1)

  private def maybePushNotif(
      userId: UserId,
      monitor: MonitorType,
      event: PrefEvent,
      data: LazyFu[Data]
  ): Funit =
    notifyAllows(userId, event).flatMap: allows =>
      filterPushNotif(NotifyAllows(userId, allows), monitor, data)

  private def filterPushNotif(to: NotifyAllows, monitor: MonitorType, data: LazyFu[Data]): Funit = for
    _ <- to.allows.web.so(webPush(to.userId, data).addEffects: res =>
      monitor(lila.mon.push.send)("web", res.isSuccess, 1))
    _ <- to.allows.device.so(firebasePush(to.userId, data).addEffects: res =>
      monitor(lila.mon.push.send)("firebase", res.isSuccess, 1))
  yield ()

  // ignores notification preferences
  private def alwaysPushFirebaseData(userId: UserId, monitor: MonitorType, data: LazyFu[Data]): Funit =
    firebasePush(userId, data.dmap(_.copy(firebaseMod = Data.FirebaseMod.DataOnly.some))).addEffects: res =>
      monitor(lila.mon.push.send)("firebaseData", res.isSuccess, 1)

  private def describeChallenge(c: Challenge) =
    import lila.core.challenge.Challenge.TimeControl.*
    List(
      if c.rated.yes then "Rated" else "Casual",
      c.timeControl match
        case Unlimited => "Unlimited"
        case Correspondence(d) => s"$d days"
        case c: Clock => c.show
      ,
      c.variant.name
    ).mkString(" • ")

  private def IfAway(pov: Pov)(f: => Funit): Funit =
    lila.common.Bus
      .ask[Boolean, Tell]: p =>
        Tell(pov.gameId, RoundBus.IsOnGame(pov.color, p))
      .flatMap:
        if _ then funit
        else f

  private def asyncOpponentName(pov: Pov): Fu[String] =
    namer.playerText(pov.opponent)(using lightUser.optional)

private object PushApi:

  case class Data(
      title: String,
      body: String,
      stacking: Stacking,
      urgency: Urgency,
      payload: Data.Payload,
      mobileCompatible: Option[LichessMobileVersion] = None,
      iosBadge: Option[Int] = None,
      // https://firebase.google.com/docs/cloud-messaging/concept-options#data_messages
      firebaseMod: Option[Data.FirebaseMod] = None
  )

  object Data:
    // firebase doesn't support nested data object
    type KeyValue = Seq[(String, String)]
    case class Payload(userId: Option[UserId], userData: KeyValue)
    def payload(userId: UserId)(pairs: (String, String)*): Payload = Payload(userId.some, pairs)
    def payload(pairs: (String, String)*): Payload = Payload(none, pairs)

    type KeyValueMod = Data.KeyValue => Data.KeyValue
    enum FirebaseMod(val mod: KeyValueMod):
      case NotifOnly(m: KeyValueMod) extends FirebaseMod(m)
      case DataOnly extends FirebaseMod(identity)
