package lila.relation

export lila.Lila.{ *, given }

type Relation = Boolean
val Follow: Relation = true
val Block: Relation  = false

private type ID                  = String
private type OnlineStudyingCache = com.github.blemale.scaffeine.Cache[ID, String]
