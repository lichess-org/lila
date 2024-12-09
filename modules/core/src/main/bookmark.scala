package lila.core
package bookmark

type BookmarkExists = (game.Game, Option[userId.UserId]) => Fu[Boolean]
