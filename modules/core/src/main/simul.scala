package lila.core
package simul

import chess.variant.Variant
import lila.core.rating.Score

trait Simul:
  def id: SimulId
  def name: String
  def variants: List[Variant]
  def hostId: UserId
  def hostScore: Score
  def playerScore(userId: UserId): Option[Score]
  def playerIds: List[UserId]

trait SimulApi:
  def byIds(ids: List[SimulId]): Fu[List[Simul]]

case class OnStart(simul: Simul) extends AnyVal
case class GetHostIds(promise: Promise[Set[UserId]])
case class PlayerMove(gameId: GameId)
