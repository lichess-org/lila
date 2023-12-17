package lila.push

import akka.actor.*
import play.api.libs.json.*

import lila.challenge.Challenge
import lila.common.String.shorten
import lila.common.{ LilaFuture, LightUser, LazyFu }
import lila.common.Json.given
import lila.game.{ Game, Namer, Pov }
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.push.TourSoon
import lila.hub.actorApi.round.{ IsOnGame, MoveEvent }
import lila.notify.*
import lila.notify.{ NotificationPref, NotifyAllows }

final private class PushApi(
    firebasePush: FirebasePush,
    webPush: WebPush,
    proxyRepo: lila.round.GameProxyRepo,
    roundMobile: lila.round.RoundMobile,
    gameRepo: lila.game.GameRepo,
    notifyAllows: lila.notify.GetNotifyAllows,
    postApi: lila.forum.ForumPostApi
)(using Executor, Scheduler)(using lightUser: LightUser.GetterFallback):

  private[push] def notifyPush(to: Iterable[NotifyAllows], content: NotificationContent): Funit =
    content match
      case PrivateMessage(sender, text) =>
        lightUser(sender).flatMap(luser => privateMessage(to.head, sender, luser.titleName, text))
      case MentionedInThread(mentioner, topic, _, _, postId) =>
        lightUser(mentioner).flatMap(luser => forumMention(to.head, luser.titleName, topic, postId))
      case StreamStart(streamerId, streamerName) =>
        streamStart(to, streamerId, streamerName)
      case InvitedToStudy(invitedBy, studyName, studyId) =>
        lightUser(invitedBy).flatMap(luser => invitedToStudy(to.head, luser.titleName, studyName, studyId))
      case _ => funit

  private def payload(userId: UserId)(data: JsObject): JsObject =
    Json.obj("userId" -> userId, "userData" -> data)

  def finish(game: Game): Funit =
    if !game.isCorrespondence || game.hasAi then funit
    else
      game.userIds.traverse_ { userId =>
        Pov(game, userId) so: pov =>
          IfAway(pov):
            maybePush(
              userId,
              _.finish,
              NotificationPref.GameEvent,
              data = LazyFu: () =>
                for
                  nbMyTurn <- gameRepo.countWhereUserTurn(userId)
                  opponent <- asyncOpponentName(pov)
                yield PushApi.Data(
                  title = pov.win match
                    case Some(true)  => "You won!"
                    case Some(false) => "You lost."
                    case _           => "It's a draw."
                  ,
                  body = s"Your game with $opponent is over.",
                  stacking = Stacking.GameFinish,
                  urgency = Urgency.VeryLow,
                  payload = payload(userId):
                    Json.obj(
                      "type"   -> "gameFinish",
                      "gameId" -> game.id,
                      "fullId" -> pov.fullId
                    )
                  ,
                  iosBadge = nbMyTurn.some.filter(0 <)
                )
            )
      }

  def move(move: MoveEvent): Funit =
    LilaFuture.delay(2 seconds):
      proxyRepo.game(move.gameId) flatMap:
        _.filter(_.playable) so: game =>
          game.sans.lastOption.so: sanMove =>
            val pov = Pov(game, game.player.color)
            game.player.userId so: userId =>
              IfAway(pov):
                maybePush(
                  userId,
                  _.move,
                  NotificationPref.GameEvent,
                  data = LazyFu: () =>
                    for
                      nbMyTurn <- gameRepo.countWhereUserTurn(userId)
                      opponent <- asyncOpponentName(pov)
                      payload  <- corresGamePayload(pov, "gameMove", userId)
                    yield PushApi.Data(
                      title = "It's your turn!",
                      body = s"$opponent played $sanMove",
                      stacking = Stacking.GameMove,
                      urgency = Urgency.Normal,
                      payload = payload,
                      iosBadge = nbMyTurn.some.filter(0 <)
                    )
                )

  def takebackOffer(gameId: GameId): Funit =
    LilaFuture.delay(1 seconds):
      proxyRepo.game(gameId) flatMap:
        _.filter(_.playable).so: game =>
          game.players.collect {
            case p if p.isProposingTakeback => Pov(game, game opponent p)
          } so { pov => // the pov of the receiver
            pov.player.userId so: userId =>
              IfAway(pov):
                maybePush(
                  userId,
                  _.takeback,
                  NotificationPref.GameEvent,
                  data = LazyFu: () =>
                    for
                      opponent <- asyncOpponentName(pov)
                      payload  <- corresGamePayload(pov, "gameTakebackOffer", userId)
                    yield PushApi
                      .Data(
                        title = "Takeback offer",
                        body = s"$opponent proposes a takeback",
                        stacking = Stacking.GameTakebackOffer,
                        urgency = Urgency.Normal,
                        payload = payload
                      )
                )
          }

  def drawOffer(gameId: GameId): Funit =
    LilaFuture.delay(1 seconds):
      proxyRepo.game(gameId) flatMap:
        _.filter(_.playable).so: game =>
          game.players.collect {
            case p if p.isOfferingDraw => Pov(game, game opponent p)
          } so { pov => // the pov of the receiver
            pov.player.userId so: userId =>
              IfAway(pov):
                maybePush(
                  userId,
                  _.takeback,
                  NotificationPref.GameEvent,
                  data = LazyFu: () =>
                    for
                      opponent <- asyncOpponentName(pov)
                      payload  <- corresGamePayload(pov, "gameDrawOffer", userId)
                    yield PushApi.Data(
                      title = "Draw offer",
                      body = s"$opponent offers a draw",
                      stacking = Stacking.GameDrawOffer,
                      urgency = Urgency.Normal,
                      payload = payload
                    )
                )
          }

  def corresAlarm(pov: Pov): Funit =
    pov.player.userId.so: userId =>
      maybePush(
        userId,
        _.corresAlarm,
        NotificationPref.GameEvent,
        data = LazyFu: () =>
          for
            opponent <- asyncOpponentName(pov)
            payload  <- corresGamePayload(pov, "corresAlarm", userId)
          yield PushApi.Data(
            title = "Time is almost up!",
            body = s"You are about to lose on time against $opponent",
            stacking = Stacking.GameMove,
            urgency = Urgency.High,
            payload = payload
          )
      )

  private def corresGamePayload(pov: Pov, typ: String, userId: UserId): Fu[JsObject] =
    roundMobile
      .json(pov.game, pov.fullId.anyId, socket = none)
      .map: round =>
        payload(userId):
          Json.obj(
            "type"   -> typ,
            "gameId" -> pov.gameId,
            "fullId" -> pov.fullId,
            "round"  -> round
          )

  def privateMessage(to: NotifyAllows, senderId: UserId, senderName: String, text: String): Funit =
    filterPush(
      to,
      _.message,
      LazyFu.sync:
        PushApi.Data(
          title = senderName,
          body = text,
          stacking = Stacking.PrivateMessage,
          urgency = Urgency.Normal,
          payload = payload(to.userId):
            Json.obj(
              "type"     -> "newMessage",
              "threadId" -> senderId
            )
        )
    )

  def invitedToStudy(to: NotifyAllows, invitedBy: String, studyName: StudyName, studyId: StudyId): Funit =
    filterPush(
      to,
      _.message,
      LazyFu.sync:
        PushApi.Data(
          title = studyName.value,
          body = s"$invitedBy invited you to $studyName",
          stacking = Stacking.InvitedStudy,
          urgency = Urgency.Normal,
          payload = payload(to.userId):
            Json.obj(
              "type"      -> "invitedStudy",
              "invitedBy" -> invitedBy,
              "studyName" -> studyName,
              "studyId"   -> studyId,
              "url"       -> s"https://lichess.org/study/$studyId"
            )
        )
    )

  def challengeCreate(c: Challenge): Funit =
    c.destUser.so: dest =>
      c.challengerUser.ifFalse(c.hasClock) so: challenger =>
        lightUser(challenger.id) flatMap: lightChallenger =>
          maybePush(
            dest.id,
            _.challenge.create,
            NotificationPref.Challenge,
            LazyFu.sync:
              PushApi.Data(
                title = s"${lightChallenger.titleName} (${challenger.rating.show}) challenges you!",
                body = describeChallenge(c),
                stacking = Stacking.ChallengeCreate,
                urgency = Urgency.Normal,
                payload = payload(dest.id):
                  Json.obj(
                    "type"        -> "challengeCreate",
                    "challengeId" -> c.id
                  )
              )
          )

  def challengeAccept(c: Challenge, joinerId: Option[UserId]): Funit =
    c.challengerUser.ifTrue(c.finalColor.white && !c.hasClock) so: challenger =>
      joinerId so lightUser.optional flatMap: lightJoiner =>
        maybePush(
          challenger.id,
          _.challenge.accept,
          NotificationPref.Challenge,
          LazyFu.sync:
            PushApi.Data(
              title = s"${lightJoiner.fold("A player")(_.titleName)} accepts your challenge!",
              body = describeChallenge(c),
              stacking = Stacking.ChallengeAccept,
              urgency = Urgency.Normal,
              payload = payload(challenger.id):
                Json.obj(
                  "type"        -> "challengeAccept",
                  "challengeId" -> c.id
                )
            )
        )

  def tourSoon(tour: TourSoon): Funit =
    tour.userIds.toList.traverse_ : userId =>
      maybePush(
        userId,
        _.tourSoon,
        NotificationPref.TournamentSoon,
        LazyFu.sync:
          PushApi
            .Data(
              title = tour.tourName,
              body = "The tournament is about to start!",
              stacking = Stacking.ChallengeAccept,
              urgency = Urgency.Normal,
              payload = payload(userId):
                Json.obj(
                  "type"     -> "tourSoon",
                  "tourId"   -> tour.tourId,
                  "tourName" -> tour.tourName,
                  "path"     -> s"/${if tour.swiss then "swiss" else "tournament"}/${tour.tourId}"
                )
            )
      )

  def forumMention(to: NotifyAllows, mentionedBy: String, topicName: String, postId: ForumPostId): Funit =
    filterPush(
      to,
      _.forumMention,
      LazyFu: () =>
        postApi.getPost(postId) map: post =>
          PushApi.Data(
            title = topicName,
            body = post.fold(topicName)(p => shorten(p.text, 57 - 3, "...")),
            stacking = Stacking.ForumMention,
            urgency = Urgency.Low,
            payload = payload(to.userId):
              Json.obj(
                "type"        -> "forumMention",
                "mentionedBy" -> mentionedBy,
                "topic"       -> topicName,
                "postId"      -> postId,
                "url"         -> s"https://lichess.org/forum/redirect/post/$postId"
              )
          )
    )

  def streamStart(recips: Iterable[NotifyAllows], streamerId: UserId, streamerName: String): Funit =
    val pushData = LazyFu.sync:
      PushApi.Data(
        title = streamerName,
        body = streamerName + " started streaming",
        stacking = Stacking.StreamStart,
        urgency = Urgency.Low,
        payload = Json.obj(
          "userData" -> Json.obj(
            "type"       -> "streamStart",
            "streamerId" -> streamerId,
            "url"        -> s"https://lichess.org/streamer/$streamerId/redirect"
          )
        )
      )
    val webRecips = recips.collect { case u if u.allows.web => u.userId }
    webPush(webRecips, pushData).addEffects { res =>
      lila.mon.push.send.streamStart("web", res.isSuccess, webRecips.size)
    } andDo:
      recips collect { case u if u.allows.device => u.userId } foreach:
        firebasePush(_, pushData).addEffects: res =>
          lila.mon.push.send.streamStart("firebase", res.isSuccess, 1)

  private type MonitorType = lila.mon.push.send.type => ((String, Boolean, Int) => Unit)

  private def maybePush(
      userId: UserId,
      monitor: MonitorType,
      event: NotificationPref.Event,
      data: LazyFu[PushApi.Data]
  ): Funit =
    notifyAllows(userId, event).flatMap: allows =>
      filterPush(NotifyAllows(userId, allows), monitor, data)

  private def filterPush(to: NotifyAllows, monitor: MonitorType, data: LazyFu[PushApi.Data]): Funit = for
    _ <- to.allows.web so webPush(to.userId, data).addEffects: res =>
      monitor(lila.mon.push.send)("web", res.isSuccess, 1)
    _ <- to.allows.device so firebasePush(to.userId, data).addEffects: res =>
      monitor(lila.mon.push.send)("firebase", res.isSuccess, 1)
  yield ()

  private def describeChallenge(c: Challenge) =
    import lila.challenge.Challenge.TimeControl.*
    List(
      if c.mode.rated then "Rated" else "Casual",
      c.timeControl match
        case Unlimited         => "Unlimited"
        case Correspondence(d) => s"$d days"
        case c: Clock          => c.show
      ,
      c.variant.name
    ) mkString " • "

  private def IfAway(pov: Pov)(f: => Funit): Funit =
    lila.common.Bus.ask[Boolean]("roundSocket") { p =>
      Tell(pov.gameId.value, IsOnGame(pov.color, p))
    } flatMap:
      if _ then funit
      else f

  private def asyncOpponentName(pov: Pov): Fu[String] =
    Namer.playerText(pov.opponent)(using lightUser.optional)

private object PushApi:

  case class Data(
      title: String,
      body: String,
      stacking: Stacking,
      urgency: Urgency,
      payload: JsObject,
      iosBadge: Option[Int] = None
  )
