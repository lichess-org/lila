package lila.teamSearch

private case class Query(text: String):
  def transform = lila.search.spec.Query.team(text)
