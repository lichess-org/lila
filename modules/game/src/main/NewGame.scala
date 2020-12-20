package lila.game

// Wrapper around newly created games. We do not know if the id is unique, yet.
case class NewGame(sloppy: Game) extends AnyVal {
  def withId(id: Game.ID): Game = sloppy.withId(id)
  def withUniqueId(implicit idGenerator: IdGenerator): Fu[Game] =
    idGenerator.game dmap sloppy.withId

  def start: NewGame = NewGame(sloppy.start)

  // Forward methods as needed, but do not expose the unchecked id.
  def variant     = sloppy.variant
  def finished    = sloppy.finished
  def winnerColor = sloppy.winnerColor
  def status      = sloppy.status
  def history     = sloppy.history
}
