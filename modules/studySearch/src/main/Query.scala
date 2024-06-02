package lila.studySearch

private[studySearch] case class Query(text: String, userId: Option[UserId]):
  def transform = lila.search.spec.Query.study(text, userId.map(_.value))
