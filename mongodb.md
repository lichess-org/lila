lichess and mongodb
===================

I'm not a mongodb expert. This document gives no recommendation, but describes the choices I made for lichess. Please let me know what can be improved!

The good ol time
----------------

Back in 2009 lichess was using mysql, with tables for `game`, `player` and even `piece` models. It required quite a lot of JOIN and lacked schema flexibility.
So I implemented a custom storage based on model binary serialization with file persistence... which of course was very hard to query. 
Finally, in 2010, I learnt about document databases and chose mongodb for its update performances and sql-like indexes. 

Lichess database
----------------

The schema is made of 25 collections. At the time of writing (Oct 13 2012), the DB contains 10,2 million objects and occupies 12,8 GB on the filesystem. All indexes fit in 824 MB.

Storing games
-------------

A game data is span in 4 collections:

- `game` contains the frequently needed infos such as the clock, usernames, piece positions, game status, number of turns... Here's an [example of game object](https://gist.github.com/3886230).

- `room` contains the private chat of the game players

- `watcher_room` contains the public chat of the game spectators

- `pgn` contains the game history in pgn format, allowing for replays, analysis and takeback.

So each game results in 4 objects in 4 distinct collections. Everything could fit in a single nested object, but I prefer separating the data based on use cases. I only need the `game` informations to display a board or apply a move. Having 4 collections keeps the very busy `game` collection small, so most operations are cheap.

### Compression!

There are more than 10,000 games played every day, so I try to shrink their DB representation as much as possible to reduce data transfer and keep the whole thing in memory.

In mongodb key names are stored for each object, so I reduced them to one or two chars.
I also use data custom encodings to save as many bytes as possible. For instance, here's how piece positions are stored: `1pCkJqJPKNKPJPJNKBJQkRtBtRMPGPPP`.

In average, a game fits in 1,24 KB:

    > db.game4.stats().avgObjSize + db.room.stats().avgObjSize + db.watcher_room.stats().avgObjSize + db.pgn.stats().avgObjSize
    1241.775641951236

### Indexes

Here are the indexes I need on the `game` collection for my common queries:

- status (created, started, resigned, checkmate, ...) as an Integer
- userIds as a pair of strings; the index is sparse to exclude anonymous games
- winnerUserId, string sparse index
- createdAt descendant
- userIds + createdAt compound index: to show one player's games in chronological order
- bookmark sparse int: number of player bookmarks used to show popular games

The `_id` is a random 8 chars string used in urls. `room`, `watcher_room` and `pgn` objects are linked to the `game` objects by using the same `_id`.

Storing users
-------------

User data is split in 4 collections:

- `user` for the frequently accessed data. See an [example of user object](https://gist.github.com/3886345). It contains security data, user preferences and a good deal of denormalized game counts. Note that the `_id` field is the lowercased username. It allows readable references to users in other collection objects. It also allows doing html links to users without loading them from the database, as the `_id` is enough to make a link like http://lichess.org/@/thibault. 

- `config` stores user preferences for AI, friend and lobby games. Here's a [config object from the database](https://gist.github.com/3886367). The config `_id` is the user `_id`.

- `user_history` remembers every game played by a user, and the corresponding ELO rating variations. The array is indexed by timestamp int and I use it to display ELO charts.

- `security` associates usernames (= user ids) to browser cookies and IP addresses. It is used for authentication and spam prevention.

Like for `game` I could have it all in a big object, but I like keeping the `user` collection small.

The schema
----------

All collections and references:

![lichess.org mongodb schema](https://raw.github.com/ornicar/lila/master/public/images/lichess_mongodb_schema.png)
