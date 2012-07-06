package controllers

import lila._
import http.Context

import play.api._
import mvc._

import play.api.libs.concurrent._
import play.api.Play.current
import akka.dispatch.Future

object Ai extends LilaController {

  val craftyServer = env.ai.craftyServer
  val stockfishServer = env.ai.stockfishServer
  val isServer = env.ai.isServer
  implicit val executor = Akka.system.dispatcher

  def playCrafty = Action { implicit req ⇒
    IfServer {
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
  }

  def playStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    IfServer {
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
  }

  def analyseStockfish = Action { implicit req ⇒
    implicit val ctx = Context(req, None)
    IfServer {
      Async {
        stockfishServer.analyse(
          pgn = getOr("pgn", ""),
          initialFen = get("initialFen")
        ).asPromise map { res ⇒
            res.fold(
              err ⇒ InternalServerError(err.shows),
              analyse ⇒ Ok(analyse.encode)
            )
          }
      }
    }
  }

  def reportStockfish = Action { implicit req ⇒
    IfServer {
      Async {
        env.ai.stockfishServerReport.fold(
          _ map { case (play, analyse) ⇒ Ok("%d %d".format(play, analyse)) },
          Future(NotFound)
        ).asPromise
      }
    }
  }

  private def IfServer(result: ⇒ Result) =
    isServer.fold(result, BadRequest("Not an AI server"))
}
