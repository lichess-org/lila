package controllers

import lila._
import http.Context

import play.api._
import mvc._

import play.api.libs.concurrent.Akka
import play.api.Play.current

object Ai extends LilaController {

  private val craftyServer = env.ai.craftyServer
  private val stockfishServer = env.ai.stockfishServer

  def playCrafty = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    Async {
      Akka.future {
        craftyServer.play(fen = getOr("fen", ""), level = getIntOr("level", 1))
      } map { res ⇒
        res.fold(
          err ⇒ BadRequest(err.shows),
          op ⇒ Ok(op.unsafePerformIO)
        )
      }
    }
  }

  def playStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    Async {
      Akka.future {
        stockfishServer.play(
          pgn = getOr("pgn", ""), 
          initialFen = get("initialFen"),
          level = getIntOr("level", 1))
      } map { res ⇒
        res.fold(
          err ⇒ BadRequest(err.shows),
          op ⇒ Ok(op.unsafePerformIO)
        )
      }
    }
  }

  def analyseStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    Async {
      Akka.future {
        stockfishServer.analyse(
          pgn = getOr("pgn", ""), 
          initialFen = get("initialFen"))
      } map { res ⇒
        res.fold(
          err ⇒ BadRequest(err.shows),
          op ⇒ Ok(op.unsafePerformIO.encode)
        )
      }
    }
  }
}
