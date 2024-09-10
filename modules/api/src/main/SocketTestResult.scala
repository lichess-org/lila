package lila.api

import play.api.libs.json.*

import lila.db.JSON

final class SocketTestResult(resultsDb: lila.db.AsyncCollFailingSilently)(using Executor):
  def put(results: JsObject) = resultsDb: coll =>
    coll.insert.one(JSON.bdoc(results)).void
