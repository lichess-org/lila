package lila.push

private enum Stacking(val key: String, val message: String):

  case GameFinish extends Stacking("gameFinish", "$[notif_count] games are over")
  case GameMove extends Stacking("gameMove", "It's your turn in $[notif_count] games")
  case GameTakebackOffer extends Stacking("gameTakebackOffer", "Takeback offers in $[notif_count] games")
  case GameDrawOffer extends Stacking("gameDrawOffer", "Draw offers in $[notif_count] games")
  case PrivateMessage extends Stacking("privateMessage", "You have $[notif_count] new messages")
  case ChallengeCreate extends Stacking("challengeCreate", "You have $[notif_count] new challenges")
  case ChallengeAccept extends Stacking("challengeAccept", "$[notif_count] players accepted your challenges")
  case TourSoon extends Stacking("tourSoon", "$[notif_count] tournaments are starting")
  case ForumMention extends Stacking("forumMention", "You have been mentioned $[notif_count] times")
  case StreamStart extends Stacking("streamStart", "$[notif_count] streamers streaming")
  case InvitedStudy extends Stacking("invitedStudy", "You have $[notif_count] study invites")
  case Generic extends Stacking("generic", "$[notif_count] notifications")
