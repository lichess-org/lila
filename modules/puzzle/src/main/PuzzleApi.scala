package lila.puzzle

import cats.syntax.all.*

import lila.common.paginator.Paginator
import lila.common.config.{ Max, MaxPerPage }
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.user.User

final class PuzzleApi(
    colls: PuzzleColls,
    trustApi: PuzzleTrustApi,
    countApi: PuzzleCountApi,
    openingApi: PuzzleOpeningApi
)(using ec: Executor, scheduler: Scheduler):

  import BsonHandlers.given

  object puzzle:

    def find(id: PuzzleId): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id))

    def of(user: User, page: Int): Fu[Paginator[Puzzle]] =
      colls.puzzle { coll =>
        Paginator(
          adapter = new Adapter[Puzzle](
            collection = coll,
            selector = $doc("users" -> user.id),
            projection = none,
            sort = $sort desc "glicko.r"
          ),
          page,
          MaxPerPage(30)
        )
      }

  private[puzzle] object round:

    def find(user: User, puzzleId: PuzzleId): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzleId).toString))

    private[PuzzleApi] def exists(user: User, puzzleId: PuzzleId): Fu[Boolean] =
      colls.round(_.exists($id(PuzzleRound.Id(user.id, puzzleId).toString)))

    def upsert(r: PuzzleRound, angle: PuzzleAngle): Funit =
      val roundDoc = roundHandler.write(r) ++
        $doc(
          PuzzleRound.BSONFields.user  -> r.id.userId,
          PuzzleRound.BSONFields.theme -> angle.some.filter(_ != PuzzleAngle.mix)
        )
      colls.round(_.update.one($id(r.id), roundDoc, upsert = true)).void

  object vote:

    private val sequencer = lila.hub.AsyncActorSequencers[PuzzleId](
      maxSize = Max(32),
      expiration = 1 minute,
      timeout = 3 seconds,
      name = "puzzle.vote",
      logging = false
    )

    def update(id: PuzzleId, user: User, vote: Boolean): Funit =
      sequencer(id) {
        round
          .find(user, id)
          .flatMapz { prevRound =>
            trustApi.vote(user, prevRound, vote) flatMapz { weight =>
              val voteValue = (if (vote) 1 else -1) * weight
              lila.mon.puzzle.vote.count(vote, prevRound.win.yes).increment()
              updatePuzzle(id, voteValue, prevRound.vote) zip
                colls.round {
                  _.updateField($id(prevRound.id), PuzzleRound.BSONFields.vote, voteValue)
                } void
            }
          }
      }.monSuccess(_.puzzle.vote.future).recoverDefault

    private def updatePuzzle(puzzleId: PuzzleId, newVote: Int, prevVote: Option[Int]): Funit =
      colls.puzzle { coll =>
        import Puzzle.{ BSONFields as F }
        coll.one[Bdoc](
          $id(puzzleId),
          $doc(F.voteUp -> true, F.voteDown -> true, F.day -> true, F.id -> false)
        ) flatMapz { doc =>
          val prevUp   = ~doc.int(F.voteUp)
          val prevDown = ~doc.int(F.voteDown)
          val up       = (prevUp + ~newVote.some.filter(0 <) - ~prevVote.filter(0 <)) atLeast newVote
          val down     = (prevDown - ~newVote.some.filter(0 >) + ~prevVote.filter(0 >)) atLeast -newVote
          coll.update
            .one(
              $id(puzzleId),
              $set(
                F.voteUp   -> up,
                F.voteDown -> down,
                F.vote     -> ((up - down).toFloat / (up + down))
              ) ++ {
                (newVote <= -100 && doc
                  .getAsOpt[Instant](F.day)
                  .exists(_ isAfter nowInstant.minusDays(1))) so
                  $unset(F.day)
              }
            )
            .void
        }
      }

  def angles: Fu[PuzzleAngle.All] = for {
    themes   <- theme.categorizedWithCount
    openings <- openingApi.collection
  } yield PuzzleAngle.All(themes, openings)

  object theme:

    private[PuzzleApi] def categorizedWithCount: Fu[List[(lila.i18n.I18nKey, List[PuzzleTheme.WithCount])]] =
      countApi.countsByTheme map { counts =>
        PuzzleTheme.categorized.map { case (cat, puzzles) =>
          cat -> puzzles.map { pt =>
            PuzzleTheme.WithCount(pt, counts.getOrElse(pt.key, 0))
          }
        }
      }

    def vote(user: User, id: PuzzleId, theme: PuzzleTheme.Key, vote: Option[Boolean]): Funit =
      round.find(user, id) flatMapz { round =>
        round.themeVote(theme, vote) so { newThemes =>
          import PuzzleRound.{ BSONFields as F }
          val update =
            if (newThemes.isEmpty || !PuzzleRound.themesLookSane(newThemes))
              fuccess($unset(F.themes, F.puzzle).some)
            else
              vote match
                case None => fuccess($set(F.themes -> newThemes).some)
                case Some(v) =>
                  trustApi.theme(user) map2 { weight =>
                    $set(
                      F.themes -> newThemes,
                      F.puzzle -> id,
                      F.weight -> weight
                    )
                  }
          update flatMapz { up =>
            lila.mon.puzzle.vote.theme(theme.value, vote, round.win.yes).increment()
            colls.round(_.update.one($id(round.id), up)) zip
              colls.puzzle(_.updateField($id(round.id.puzzleId), Puzzle.BSONFields.dirty, true)) void
          }
        }
      }

  object casual:

    private val store = lila.memo.ExpireSetMemo[CacheKey](30 minutes)

    private def key(user: User, id: PuzzleId) = CacheKey(s"${user.id}:${id}")

    def setCasualIfNotYetPlayed(user: User, puzzle: Puzzle): Funit =
      !round.exists(user, puzzle.id) mapz store.put(key(user, puzzle.id))

    def apply(user: User, id: PuzzleId) = store.get(key(user, id))
