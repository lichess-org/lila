package lila.playban

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import lila.game.{ Game, Pov }
import lila.message.{ MessageApi, ModPreset }
import lila.user.{ User, UserRepo }

private final class SandbagWatch(messenger: MessageApi) {

  import SandbagWatch._

  def apply(game: Game): Funit = (game.rated && game.finished) ?? {
    game.userIds.map { userId =>
      (records getIfPresent userId, isSandbag(game, userId)) match {
        case (None, false) => funit
        case (Some(record), false) => updateRecord(userId, record + Good)
        case (record, true) => updateRecord(userId, (record | newRecord) + Sandbag)
      }
    }.sequenceFu.void
  }

  private def sendMessage(userId: User.ID): Funit = for {
    mod <- UserRepo.lichess
    user <- UserRepo byId userId
  } yield (mod zip user).headOption.?? {
    case (m, u) =>
      lila.log("sandbag").info(s"https://lichess.org/@/${u.username}")
      messenger.sendPreset(m, u, ModPreset.sandbagAuto).void
  }

  private def updateRecord(userId: User.ID, record: Record) =
    if (record.immaculate) fuccess(records invalidate userId)
    else {
      records.put(userId, record)
      if (record.latestIsSandbag && record.outcomes.size > 1) sendMessage(userId)
      else funit
    }

  private val records: Cache[User.ID, Record] = Scaffeine()
    .expireAfterWrite(6 hours)
    .build[User.ID, Record]

  private def isSandbag(game: Game, userId: User.ID): Boolean =
    game.playerByUserId(userId).exists { player =>
      game.opponent(player).wins && game.turns <= {
        if (game.variant == chess.variant.Atomic) 3
        else 5
      }
    }
}

private object SandbagWatch {

  sealed trait Outcome
  case object Good extends Outcome
  case object Sandbag extends Outcome

  val maxOutcomes = 5

  case class Record(outcomes: List[Outcome]) {

    def +(outcome: Outcome) = copy(outcomes = outcome :: outcomes.take(maxOutcomes - 1))

    def latestIsSandbag = outcomes.headOption.exists(_ == Sandbag)

    def immaculate = outcomes.size == maxOutcomes && outcomes.forall(Good.==)
  }

  val newRecord = Record(Nil)
}
