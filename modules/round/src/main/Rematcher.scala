package lila.round

import chess.format.Fen
import chess.variant.*
import chess.{ Board, Castles, Clock, Color as ChessColor, Ply, Game as ChessGame, Situation }
import ChessColor.{ Black, White }

import lila.common.Bus
import lila.game.{ AnonCookie, Event, Game, GameRepo, PerfPicker, Pov, Rematches, Source }
import lila.memo.ExpireSetMemo
import lila.user.{ User, UserRepo }
import lila.i18n.{ defaultLang, I18nKeys as trans }

final private class Rematcher(
    gameRepo: GameRepo,
    userRepo: UserRepo,
    idGenerator: lila.game.IdGenerator,
    messenger: Messenger,
    onStart: OnStart,
    rematches: Rematches
)(using Executor):

  private given play.api.i18n.Lang = defaultLang

  private val declined = ExpireSetMemo[GameFullId](1 minute)

  private val rateLimit = lila.memo.RateLimit[GameFullId](
    credits = 2,
    duration = 1 minute,
    key = "round.rematch",
    log = false
  )

  private val chess960 = ExpireSetMemo[GameId](3 hours)

  def isOffering(pov: Pov): Boolean = rematches isOffering pov.ref

  def yes(pov: Pov): Fu[Events] =
    pov match
      case Pov(game, color) if game.playerCouldRematch =>
        if (isOffering(!pov) || game.opponent(color).isAi)
          rematches.getAcceptedId(game.id).fold(rematchJoin(pov))(rematchExists(pov))
        else if (!declined.get(pov.flip.fullId) && rateLimit.zero(pov.fullId)(true))
          rematchCreate(pov)
        else fuccess(List(Event.RematchOffer(by = none)))
      case _ => fuccess(List(Event.ReloadOwner))

  def no(pov: Pov): Fu[Events] =
    if isOffering(pov) then
      pov.opponent.userId foreach { forId =>
        Bus.publish(lila.hub.actorApi.round.RematchCancel(pov.gameId), s"rematchFor:$forId")
      }
      messenger.volatile(pov.game, trans.rematchOfferCanceled.txt())
    else if isOffering(!pov) then
      declined put pov.fullId
      messenger.volatile(pov.game, trans.rematchOfferDeclined.txt())
    rematches.drop(pov.gameId)
    fuccess(List(Event.RematchOffer(by = none)))

  private def rematchExists(pov: Pov)(nextId: GameId): Fu[Events] =
    gameRepo game nextId flatMap {
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))
    }

  private def rematchCreate(pov: Pov): Fu[Events] =
    rematches.offer(pov.ref) map { _ =>
      messenger.volatile(pov.game, trans.rematchOfferSent.txt())
      pov.opponent.userId foreach { forId =>
        Bus.publish(lila.hub.actorApi.round.RematchOffer(pov.gameId), s"rematchFor:$forId")
      }
      List(Event.RematchOffer(by = pov.color.some))
    }

  private def rematchJoin(pov: Pov): Fu[Events] =

    def createGame(withId: Option[GameId]) = for {
      nextGame <- returnGame(pov, withId).map(_.start)
      _ = rematches.accept(pov.gameId, nextGame.id)
      _ = if (pov.game.variant == Chess960 && !chess960.get(pov.gameId)) chess960.put(nextGame.id)
      _ <- gameRepo insertDenormalized nextGame
    } yield
      messenger.volatile(pov.game, trans.rematchOfferAccepted.txt())
      onStart(nextGame.id)
      redirectEvents(nextGame)

    rematches.get(pov.gameId) match
      case None                                    => createGame(none)
      case Some(Rematches.NextGame.Accepted(id))   => gameRepo game id mapz redirectEvents
      case Some(Rematches.NextGame.Offered(_, id)) => createGame(id.some)

  private def returnGame(pov: Pov, withId: Option[GameId]): Fu[Game] =
    for {
      initialFen <- gameRepo initialFen pov.game
      situation = initialFen.flatMap(Fen.readWithMoveNumber(pov.game.variant, _))
      pieces = pov.game.variant match
        case Chess960 =>
          if (chess960 get pov.gameId) Chess960.pieces
          else situation.fold(Chess960.pieces)(_.situation.board.pieces)
        case FromPosition => situation.fold(Standard.pieces)(_.situation.board.pieces)
        case variant      => variant.pieces
      users <- userRepo byIds pov.game.userIds
      board = Board(pieces, variant = pov.game.variant).updateHistory(
        _.copy(
          lastMove = situation.flatMap(_.situation.board.history.lastMove),
          castles = situation.fold(Castles.init)(_.situation.board.history.castles)
        )
      )
      ply = situation.fold(Ply(0))(_.ply)
      sloppy = Game.make(
        chess = ChessGame(
          situation = Situation(
            board = board,
            color = situation.fold[chess.Color](White)(_.situation.color)
          ),
          clock = pov.game.clock map { c =>
            Clock(c.config)
          },
          ply = ply,
          startedAtPly = ply
        ),
        whitePlayer = returnPlayer(pov.game, White, users),
        blackPlayer = returnPlayer(pov.game, Black, users),
        mode = if (users.exists(_.lame)) chess.Mode.Casual else pov.game.mode,
        source = pov.game.source | Source.Lobby,
        daysPerTurn = pov.game.daysPerTurn,
        pgnImport = None
      )
      game <- withId.fold(sloppy withUniqueId idGenerator) { id => fuccess(sloppy withId id) }
    } yield game

  private def returnPlayer(game: Game, color: ChessColor, users: List[User]): lila.game.Player =
    game.opponent(color).aiLevel match
      case Some(ai) => lila.game.Player.make(color, ai.some)
      case None =>
        lila.game.Player.make(
          color,
          game.opponent(color).userId.flatMap { id =>
            users.find(_.id == id)
          },
          PerfPicker.mainOrDefault(game)
        )

  private def redirectEvents(game: Game): Events =
    val whiteId = game fullIdOf White
    val blackId = game fullIdOf Black
    List(
      Event.RedirectOwner(White, blackId, AnonCookie.json(game pov Black)),
      Event.RedirectOwner(Black, whiteId, AnonCookie.json(game pov White)),
      // tell spectators about the rematch
      Event.RematchTaken(game.id)
    )
