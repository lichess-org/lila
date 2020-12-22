package lila.puzzle

import cats.implicits._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.db.AsyncColl
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.{ User, UserRepo }

final class PuzzleApi(
    colls: PuzzleColls,
    trustApi: PuzzleTrustApi,
    countApi: PuzzleCountApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Puzzle.{ BSONFields => F }
  import BsonHandlers._

  object puzzle {

    def find(id: Puzzle.Id): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id.value))

    def delete(id: Puzzle.Id): Funit =
      colls.puzzle(_.delete.one($id(id.value))).void
  }

  object round {

    def find(user: User, puzzleId: Puzzle.Id): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzleId).toString))

    def upsert(r: PuzzleRound, theme: PuzzleTheme.Key): Funit = {
      val roundDoc = RoundHandler.write(r) ++
        $doc(
          PuzzleRound.BSONFields.user  -> r.id.userId,
          PuzzleRound.BSONFields.theme -> theme.some.filter(_ != PuzzleTheme.mix.key)
        )
      colls.round(_.update.one($id(r.id), roundDoc, upsert = true)).void
    }
  }

  object vote {

    def update(id: Puzzle.Id, user: User, vote: Boolean): Funit =
      round
        .find(user, id)
        .flatMap {
          _ ?? { prevRound =>
            trustApi.vote(user, prevRound, vote) flatMap {
              _ ?? { weight =>
                val voteValue = (if (vote) 1 else -1) * weight
                lila.mon.puzzle.vote(vote, prevRound.win).increment()
                colls.puzzle {
                  _.incField($id(id), F.vote, voteValue - ~prevRound.vote).void
                }.void zip
                  colls.round {
                    _.updateField($id(prevRound.id), PuzzleRound.BSONFields.vote, voteValue)
                  } void
              }
            }
          }
        }
  }

  object theme {

    def categorizedWithCount: Fu[List[(lila.i18n.I18nKey, List[PuzzleTheme.WithCount])]] =
      countApi.countsByTheme map { counts =>
        PuzzleTheme.categorized.map { case (cat, puzzles) =>
          cat -> puzzles.map { pt =>
            PuzzleTheme.WithCount(pt, counts.getOrElse(pt.key, 0))
          }
        }
      }

    def vote(user: User, id: Puzzle.Id, theme: PuzzleTheme.Key, vote: Option[Boolean]): Funit =
      round.find(user, id) flatMap {
        _ ?? { round =>
          round.themeVote(theme, vote) ?? { newThemes =>
            import PuzzleRound.{ BSONFields => F }
            val update =
              if (newThemes.isEmpty) fuccess($unset(F.themes, F.puzzle).some)
              else
                vote match {
                  case None =>
                    fuccess(
                      $set(
                        F.themes -> newThemes
                      ).some
                    )
                  case Some(v) =>
                    trustApi.theme(user, round, theme, v) map2 { weight =>
                      $set(
                        F.themes -> newThemes,
                        F.puzzle -> id,
                        F.weight -> weight
                      )
                    }
                }
            update flatMap {
              _ ?? { up =>
                lila.mon.puzzle.voteTheme(theme.value, vote, round.win).increment()
                colls.round(_.update.one($id(round.id), up)) zip
                  colls.puzzle(_.updateField($id(round.id.puzzleId), Puzzle.BSONFields.dirty, true)) void
              }
            }
          }
        }
      }
  }
}
