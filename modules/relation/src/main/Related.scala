package lila.relation

case class Related(
  user: lila.user.User,
  nbGames: Int,
  followable: Boolean,
  relation: Option[Relation])
