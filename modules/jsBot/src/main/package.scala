package lila.jsBot

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("jsBot")

val publicBotKeys = BotKey.from:
  List()

val betaBotKeys = BotKey.from:
  List("centipawn", "tal-e")

val devBotKeys = BotKey.from:
  List("terrence", "howard", "professor", "lila")
