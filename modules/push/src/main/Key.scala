package lila.push

private enum Key:
  case gameFinish, gameMove, gameTakebackOffer, gameDrawOffer, privateMessage, challengeCreate,
    challengeAccept, tourSoon, forumMention, streamStart, invitedStudy, broadcastRound

private given play.api.libs.json.Writes[Key] = scalalib.json.Json.writeAs(_.toString)
