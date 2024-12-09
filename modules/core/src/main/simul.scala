package lila.core
package simul

import _root_.chess.variant.Variant

import lila.core.id.{ GameId, SimulId }
import lila.core.rating.Score
import lila.core.userId.UserId

trait Simul:
  def id: SimulId
  def name: String
  def variants: List[Variant]
  def hostId: UserId
  def hostScore: Score
  def playerScore(userId: UserId): Option[Score]
  def playerIds: List[UserId]
  def fullName: String

trait SimulApi:
  def byIds(ids: List[SimulId]): Fu[List[Simul]]
  def isSimulHost(userId: UserId): Fu[Boolean]

case class OnStart(simul: Simul) extends AnyVal
case class PlayerMove(gameId: GameId)
