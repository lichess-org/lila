db.swiss.ensureIndex({teamId:1,startsAt:1})
db.swiss_player.ensureIndex({s:1,c:-1})
db.swiss_pairing.ensureIndex({s:1,r:1})
db.swiss_pairing.ensureIndex({s:1,u:1,r:1})
