package lila.game
package importer

import chess.format.pgn.PgnStr
import chess.{ ByColor, ErrorStr, Mode }
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.into
import lila.core.game.{ Game, ImportedGame }
import lila.game.GameExt.finish
import lila.tree.ImportResult

private val maxPlies = 600

final class Importer(gameRepo: lila.core.game.GameRepo)(using Executor):

  def importAsGame(pgn: PgnStr, forceId: Option[GameId] = none)(using me: Option[MyId]): Fu[Game] =
    import lila.db.dsl.{ *, given }
    import lila.core.game.BSONFields as F
    import gameRepo.gameHandler
    gameRepo.coll
      .one[Game]($doc(s"${F.pgnImport}.h" -> lila.game.PgnImport.hash(pgn)))
      .flatMap:
        case Some(game) => fuccess(game)
        case None =>
          for
            g <- parseImport(pgn, me).toFuture
            game = forceId.fold(g.sloppy)(g.withId)
            _ <- gameRepo.insertDenormalized(game, initialFen = g.initialFen)
            _ <- game.pgnImport
              .flatMap(_.user)
              .isDefined
              .so:
                // import date, used to make a compound sparse index with the user
                gameRepo.coll.updateField($id(game.id), s"${F.pgnImport}.ca", game.createdAt).void
            _ <- gameRepo.finish(game.id, game.winnerColor, None, game.status)
          yield game

case class ImportData(pgn: PgnStr, analyse: Option[String])

val form = Form:
  mapping(
    "pgn"     -> nonEmptyText.into[PgnStr].verifying("invalidPgn", p => parseImport(p, none).isRight),
    "analyse" -> optional(nonEmptyText)
  )(ImportData.apply)(unapply)

val parseImport: (PgnStr, Option[UserId]) => Either[ErrorStr, ImportedGame] = (pgn, user) =>
  lila.tree.parseImport(pgn).map { case ImportResult(game, result, replay, initialFen, parsed) =>
    val dbGame = lila.core.game
      .newImportedGame(
        chess = game,
        players = ByColor: c =>
          lila.game.Player.makeImported(c, parsed.tags.names(c), parsed.tags.ratings(c)),
        mode = Mode.Casual,
        source = lila.core.game.Source.Import,
        pgnImport = PgnImport.make(user = user, date = parsed.tags.anyDate, pgn = pgn).some
      )
      .sloppy
      .start
      .pipe: dbGame =>
        result.fold(dbGame)(res => dbGame.finish(res.status, res.winner))
    ImportedGame(dbGame, initialFen)
  }
