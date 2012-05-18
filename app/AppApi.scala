package lila

import user._
import game.{ GameRepo, DbGame }
import chess.{ Color, White, Black }
import round.{ IsConnectedOnGame, GetGameVersion, RoomRepo, Progress, Event }
import analyse.GameInfo

import scalaz.effects._
import akka.actor._
import akka.pattern.ask
import akka.dispatch.{ Future, Promise }
import akka.util.duration._
import akka.util.Timeout
import play.api.libs.concurrent._
import play.api.Play.current

final class AppApi(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    roundSocket: round.Socket,
    messenger: round.Messenger,
    eloUpdater: EloUpdater,
    gameInfo: DbGame ⇒ IO[GameInfo]) {

  private implicit val timeout = Timeout(300 millis)
  private implicit val executor = Akka.system.dispatcher

  //def show(fullId: String): Future[IO[Valid[Map[String, Any]]]] =
    //futureVersion(DbGame takeGameId fullId) map { version ⇒
      //for {
        //povOption ← gameRepo pov fullId
        //gameInfo ← povOption.fold(
          //pov ⇒ messenger render pov.game.id map { roomHtml ⇒
            //Map(
              //"version" -> version,
              //"roomHtml" -> roomHtml,
              //"possibleMoves" -> {
                //if (pov.game playableBy pov.player)
                  //pov.game.toChess.situation.destinations map {
                    //case (from, dests) ⇒ from.key -> (dests.mkString)
                  //} toMap
                //else null
              //}
            //).success
          //},
          //io(GameNotFound)
        //)
      //} yield gameInfo
    //}

  //def join(
    //fullId: String,
    //url: String,
    //messages: String,
    //entryData: String): IO[Valid[Unit]] = for {
    //povOption ← gameRepo pov fullId
    //op ← povOption.fold(
      //pov ⇒ for {
        //p1 ← starter.start(pov.game, entryData)
        //p2 ← messenger.systemMessages(p1.game, messages) map { evts ⇒
          //p1 + Event.RedirectOwner(!pov.color, url) ++ evts
        //}
        //_ ← gameRepo save p2
        //_ ← roundSocket send p2
      //} yield success(),
      //io(GameNotFound)
    //)
  //} yield op

  //def start(gameId: String, entryData: String): IO[Valid[Unit]] =
    //gameRepo game gameId flatMap { gameOption ⇒
      //gameOption.fold(
        //g1 ⇒ for {
          //progress ← starter.start(g1, entryData)
          //_ ← gameRepo save progress
          //_ ← roundSocket send progress
        //} yield success(Unit),
        //io { !!("No such game") }
      //)
    //}

  //def rematchAccept(
    //gameId: String,
    //newGameId: String,
    //colorName: String,
    //whiteRedirect: String,
    //blackRedirect: String,
    //entryData: String,
    //messageString: String): IO[Valid[Unit]] = Color(colorName).fold(
    //color ⇒ for {
      //newGameOption ← gameRepo game newGameId
      //g1Option ← gameRepo game gameId
      //result ← (newGameOption |@| g1Option).apply(
        //(newGame, g1) ⇒ {
          //val progress = Progress(g1, List(
            //Event.RedirectOwner(White, whiteRedirect),
            //Event.RedirectOwner(Black, blackRedirect),
            //// tell spectators to reload the table
            //Event.ReloadTable(White),
            //Event.ReloadTable(Black)))
          //for {
            //_ ← gameRepo save progress
            //_ ← roundSocket send progress
            //newProgress ← starter.start(newGame, entryData)
            //newProgress2 ← messenger.systemMessages(
              //newProgress.game, messageString
            //) map newProgress.++
            //_ ← gameRepo save newProgress2
            //_ ← roundSocket send newProgress2
          //} yield success()
        //}
      //).fold(identity, io(GameNotFound))
    //} yield result,
    //io { !!("Wrong color name") }
  //)

  //def reloadTable(gameId: String): IO[Valid[Unit]] = for {
    //g1Option ← gameRepo game gameId
    //result ← g1Option.fold(
      //g1 ⇒ {
        //val progress = Progress(g1, Color.all map Event.ReloadTable)
        //for {
          //_ ← gameRepo save progress
          //_ ← roundSocket send progress
        //} yield success()
      //},
      //io(GameNotFound)
    //)
  //} yield result

  def adjust(username: String): IO[Unit] = for {
    userOption ← userRepo byUsername username
    _ ← userOption.fold(
      user ⇒ for {
        _ ← (user.elo > User.STARTING_ELO).fold(
          eloUpdater.adjust(user, User.STARTING_ELO), io()
        )
        _ ← userRepo setEngine user.id
      } yield (),
      io()
    )
  } yield ()

  //private def futureVersion(gameId: String): Future[Int] =
    //roundSocket.hubMaster ? GetGameVersion(gameId) mapTo manifest[Int]
}
