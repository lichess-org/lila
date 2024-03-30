package lila.hub
package simul

import chess.variant.Variant
import lila.hub.rating.Score

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
