package lila.swiss

import lila.user.User
export lila.Lila.{ *, given }

val lichessTeamId = TeamId("lichess-swiss")

type Ranking = Map[lila.user.User.ID, Int]

private val logger = lila.log("swiss")

// FIDE TRF player IDs
private type PlayerIds = Map[User.ID, Int]
private type IdPlayers = Map[Int, User.ID]
