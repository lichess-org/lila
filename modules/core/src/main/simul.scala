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
  def isSimulHost(userId: UserId): Fu[Boolean]

case class OnStart(simul: Simul) extends AnyVal
case class PlayerMove(gameId: GameId)
