package lila.jsBot

import reactivemongo.api.bson.*

import lila.db.JSON
import lila.db.dsl.{ *, given }

final private class JsBotRepo(bots: Coll, assets: Coll)(using Executor):

  def getVersions(botId: Option[UserId] = none): Fu[List[BotJson]] =
    bots
      .find(botId.so(v => $doc("uid" -> v)), $doc("_id" -> 0).some)
      .sort($doc("version" -> -1))
      .cursor[Bdoc]()
      .list(Int.MaxValue)
      .map:
        _.map(JSON.jval).map(BotJson(_))

  def getLatestBots(): Fu[List[BotJson]] =
    bots
      .aggregateWith[Bdoc](readPreference = ReadPref.sec): framework =>
        import framework.*
        List(
          Sort(Descending("version")),
          GroupField("uid")("doc" -> FirstField("$ROOT")),
          ReplaceRootField("doc"),
          Project($doc("_id" -> 0))
        )
      .list(Int.MaxValue)
      .map:
        _.map(JSON.jval).map(BotJson(_))

  def putBot(bot: BotJson, author: UserId): Fu[BotJson] = for
    fullBot <- bots.find($doc("uid" -> bot.uid)).sort($doc("version" -> -1)).one[Bdoc]
    nextVersion = fullBot.flatMap(_.int("version")).getOrElse(-1) + 1 // race condition
    newBot = bot.withMeta(BotMeta(bot.uid, author, nextVersion))
    _ <- bots.insert.one(JSON.bdoc(newBot.value))
  yield newBot

  def getAssets: Fu[Map[String, AssetName]] =
    assets
      .find($doc())
      .cursor[Bdoc]()
      .list(Int.MaxValue)
      .map: docs =>
        for
          doc <- docs
          id <- doc.getAsOpt[String]("_id")
          name <- doc.getAsOpt[AssetName]("name")
        yield id -> name
      .map(_.toMap)

  def nameAsset(tpe: Option[AssetType], key: AssetKey, name: AssetName, author: Option[UserId]): Funit =
    // filter out bookCovers as they share the same key as the book
    if !(tpe.has("book") && key.endsWith(".png")) then
      val id = if tpe.has("book") then key.dropRight(4) else key
      val setDoc = $doc("name" -> name) ++ author.so(a => $doc("author" -> a))
      assets.update.one($doc("_id" -> id), $doc("$set" -> setDoc), upsert = true).void
    else funit

  def deleteAsset(key: String): Funit =
    assets.delete.one($doc("_id" -> key)).void

end JsBotRepo
