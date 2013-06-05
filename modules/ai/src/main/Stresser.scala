package lila.ai

import akka.actor._
import akka.pattern.{ ask, pipe }

import lila.db.api._
import lila.game.tube.pgnTube
import makeTimeout.short

private[ai] final class Stresser(env: Env, system: ActorSystem) {

  def apply {
  
    play(1)

  }

  private def play(level: Int) = new Actor {

    private case class Game(moves: List[String], it: Int)

    def newMoves = 
      $find($query() skip util.Random.nextInt(30000)) map {
        Moves(_.split(' ').toList, 0) 
      }

    override def preStart {
      newMoves pipeTo self
    }

    def receive = {
      case Game( => newMoves pipeTo self
      case Moves(moves) ⇒ moves splitAt 1 match {
        case (List(move), rest) => ai.play(rest take it mkString " ", none, level) addFailureEffect {
        case e ⇒ logwarn("[ai] server play: " + e)
      } >> {
        if (i
      }
    }
  }

  private val ai = env.stockfishServer
}
