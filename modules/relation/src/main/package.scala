package lila.relation

export lila.Lila.{ *, given }

export lila.hub.relation.{ Relation, Block, Follow }

private type OnlineStudyingCache = com.github.blemale.scaffeine.Cache[UserId, String]
