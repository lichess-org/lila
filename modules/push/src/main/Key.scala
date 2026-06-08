package lila.push

private enum Key(val value: String):
  case GameFinish extends Key("gameFinish")
  case GameMove extends Key("gameMove")
  case GameTakebackOffer extends Key("gameTakebackOffer")
  case GameDrawOffer extends Key("gameDrawOffer")
  case PrivateMessage extends Key("privateMessage")
  case ChallengeCreate extends Key("challengeCreate")
  case ChallengeAccept extends Key("challengeAccept")
  case TourSoon extends Key("tourSoon")
  case ForumMention extends Key("forumMention")
  case StreamStart extends Key("streamStart")
  case InvitedStudy extends Key("invitedStudy")
  case BroadcastRound extends Key("broadcastRound")
