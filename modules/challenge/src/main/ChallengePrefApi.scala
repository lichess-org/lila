package lila.challenge

import reactivemongo.api.bson.*
import lila.db.dsl.{ *, given }

final class ChallengePrefApi(
    colls: ChallengeColls
)(using Executor):

  private val coll = colls.challengePref

  def upsert(o: ChallengePref, userName: String): Funit =
    coll.update
      .one(
        $id(userName),
        $doc(
          "variant"   -> o.variant,
          "timeMode"  -> o.timeMode,
          "gameMode"  -> o.gameMode,
          "time"      -> o.time,
          "increment" -> o.increment,
          "days"      -> o.days,
          "fen"       -> o.fen
        ),
        upsert = true
      )
      .void

  def find(userName: String): Fu[Option[ChallengePref]] = 
    coll.find($id(userName)).one[ChallengePref]
