package lila.push

sealed abstract private class Stacking(val key: String, val message: String)

private object Stacking:

  case object GameFinish extends Stacking("gameFinish", "$[notif_count] games are over")
  case object GameMove   extends Stacking("gameMove", "It's your turn in $[notif_count] games")
  case object GameTakebackOffer
      extends Stacking("gameTakebackOffer", "Takeback offers in $[notif_count] games")
  case object GameDrawOffer   extends Stacking("gameDrawOffer", "Draw offers in $[notif_count] games")
  case object PrivateMessage  extends Stacking("privateMessage", "You have $[notif_count] new messages")
  case object ChallengeCreate extends Stacking("challengeCreate", "You have $[notif_count] new challenges")
  case object ChallengeAccept
      extends Stacking("challengeAccept", "$[notif_count] players accepted your challenges")
  case object TourSoon     extends Stacking("tourSoon", "$[notif_count] tournaments are starting")
  case object ForumMention extends Stacking("forumMention", "You have been mentioned $[notif_count] times")
  case object StreamStart  extends Stacking("streamStart", "$[notif_count] streamers streaming")
  case object InvitedStudy extends Stacking("invitedStudy", "You have $[notif_count] study invites")
