package lila.puzzle

import cats.implicits._
import scala.concurrent.duration._

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi

import lila.common.paginator.Paginator
import lila.common.config.MaxPerPage
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class PuzzleApi(
    colls: PuzzleColls,
    trustApi: PuzzleTrustApi,
    countApi: PuzzleCountApi
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem, mode: play.api.Mode) {

  import Puzzle.{ BSONFields => F }
  import BsonHandlers._

  object puzzle {

    def find(id: Puzzle.Id): Fu[Option[Puzzle]] =
      colls.puzzle(_.byId[Puzzle](id.value))

    def exists(id: Puzzle.Id): Fu[Boolean] =
      colls.puzzle(_.exists($id(id)))

    def existsBySfen(sfen: Sfen): Fu[Boolean] =
      colls.puzzle(_.exists($doc(F.sfen -> sfen.value)))

    def fromGame(gameId: lila.game.Game.ID): Fu[List[Puzzle]] =
      colls.puzzle(_.list[Puzzle]($doc(F.gameId -> gameId), 5))

    def delete(id: Puzzle.Id): Funit =
      colls.puzzle(_.delete.one($id(id.value))).void

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

    def submitted(user: User, page: Int): Fu[Paginator[Puzzle]] =
      colls.puzzle { coll =>
        Paginator(
          adapter = new Adapter[Puzzle](
            collection = coll,
            selector = $doc("submittedBy" -> user.id),
            projection = none,
            sort = $sort desc "plays"
          ),
          page,
          MaxPerPage(15)
        )
      }
  }

  object round {

    def find(user: User, puzzleId: Puzzle.Id): Fu[Option[PuzzleRound]] =
      colls.round(_.byId[PuzzleRound](PuzzleRound.Id(user.id, puzzleId).toString))

    def exists(user: User, puzzleId: Puzzle.Id): Fu[Boolean] =
      colls.round(_.exists($id(PuzzleRound.Id(user.id, puzzleId).toString)))

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

    private val sequencer =
      new lila.hub.DuctSequencers(
        maxSize = 16,
        expiration = 5 minutes,
        timeout = 3 seconds,
        name = "puzzle.vote"
      )

    def update(id: Puzzle.Id, user: User, vote: Boolean): Funit =
      sequencer(id.value) {
        round
          .find(user, id)
          .flatMap {
            _ ?? { prevRound =>
              trustApi.vote(user, prevRound, vote) flatMap {
                _ ?? { weight =>
                  val voteValue = (if (vote) 1 else -1) * weight
                  lila.mon.puzzle.vote(vote, prevRound.win).increment()
                  updatePuzzle(id, voteValue, prevRound.vote) zip
                    colls.round {
                      _.updateField($id(prevRound.id), PuzzleRound.BSONFields.vote, voteValue)
                    } void
                }
              }
            }
          }
      }

    private def updatePuzzle(puzzleId: Puzzle.Id, newVote: Int, prevVote: Option[Int]): Funit =
      colls.puzzle { coll =>
        import Puzzle.{ BSONFields => F }
        coll.one[Bdoc](
          $id(puzzleId.value),
          $doc(F.voteUp -> true, F.voteDown -> true, F.id -> false)
        ) flatMap {
          _ ?? { doc =>
            val prevUp   = ~doc.int(F.voteUp)
            val prevDown = ~doc.int(F.voteDown)
            val up       = prevUp + ~newVote.some.filter(0 <) - ~prevVote.filter(0 <)
            val down     = prevDown - ~newVote.some.filter(0 >) + ~prevVote.filter(0 >)
            coll.update
              .one(
                $id(puzzleId.value),
                $set(
                  F.voteUp   -> up,
                  F.voteDown -> down,
                  F.vote     -> ((up - down).toFloat / (up + down))
                )
              )
              .void
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
              if (newThemes.isEmpty || !PuzzleRound.themesLookSane(newThemes))
                fuccess($unset(F.themes, F.puzzle).some)
              else
                vote match {
                  case None =>
                    fuccess(
                      $set(
                        F.themes -> newThemes
                      ).some
                    )
                  case Some(_) =>
                    trustApi.theme(user) map2 { weight =>
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

  object casual {

    private val store = new lila.memo.ExpireSetMemo(30 minutes)

    private def key(user: User, id: Puzzle.Id) = s"${user.id}:${id}"

    def set(user: User, id: Puzzle.Id) = store.put(key(user, id))

    def apply(user: User, id: Puzzle.Id) = store.get(key(user, id))
  }

  object submissions {

    def addNew(
        sfen: Sfen,
        line: List[Usi],
        ambProms: List[Int],
        themes: List[String],
        source: Either[Option[String], lila.game.Game.ID],
        submittedBy: Option[String]
    ): Funit =
      (for {
        validSfen     <- fuccess(sfen.toSituation(shogi.variant.Standard).map(_.toSfen)) orFail "Invalid sfen"
        validLine     <- fuccess(line.toNel) orFail "No moveline"
        alreadyExists <- puzzle.existsBySfen(validSfen)
        _             <- alreadyExists ?? fufail[Unit]("Puzzle with the same position already present")
        similarExists <- source.fold(
          _ => fuccess(false),
          gameId =>
            puzzle
              .fromGame(gameId)
              .map(_.exists(p => Math.abs(~p.sfen.stepNumber - ~validSfen.stepNumber) >= 10))
        )
        _  <- similarExists ?? fufail[Unit](s"Similar puzzle exists (~${source.map(_.toString)})")
        id <- makeId
      } yield Puzzle(
        id = id,
        sfen = validSfen,
        line = validLine,
        ambiguousPromotions = ambProms,
        glicko = Puzzle.glickoDefault(validLine.size),
        plays = 0,
        vote = 0f,
        gameId = source.toOption,
        themes = themes.flatMap(PuzzleTheme.find).map(_.key).toSet,
        author = source.left.toOption.flatten,
        description = None,
        submittedBy = submittedBy
      )).flatMap { p =>
        colls.puzzle(_.insert.one(p))
      }.void

    private def makeId: Fu[Puzzle.Id] = {
      val id = Puzzle.Id(lila.common.ThreadLocalRandom nextString 5)
      puzzle.exists(id).flatMap {
        case true  => makeId
        case false => fuccess(id)
      }
    }

  }
}
