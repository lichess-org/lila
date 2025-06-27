# scripts to run with cron on the lila server

These are all optional. You don't need to set them up on your dev machine.

For production, they may be useful, depending on your setup and data volume.

Example:

```
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin

### running on prod db
# m h dom mon dow user  command
0  3 * * * root jobq -q mongo_decay mongosh --quiet 172.16.0.50:27017/lichess /home/lichess/deploy/cron/mongodb-report-score-decay.js
0  4 * * * root jobq -q mongo_queue mongosh --quiet 172.16.0.50:27017/lichess /home/lichess/deploy/cron/mongodb-queue-stats.js
*/2 * * * * root jobq -q mongo_patron mongosh --quiet 172.16.0.50:27017/lichess /home/lichess/deploy/cron/mongodb-patron-denorm.js
26 * * * * root jobq -q mongo_patron mongosh --quiet 172.16.0.50:27017/lichess /home/lichess/deploy/cron/mongodb-ublog-similar-incremental.js
* * * * * root jobq -q mongo_fscday25 mongosh --quiet 172.16.0.50:27017/lichess /home/lichess/deploy/cron/mongodb-tournament-participation-trophies.js

### running on puzzle db
# m h dom mon dow user  command
14 */3 * * *  root  jobq -q mongo_puzzles mongosh --quiet rubik:27017/puzzler /home/lichess/deploy/cron/mongodb-puzzle-denormalize-themes.js
27 */3 * * *  root  jobq -q mongo_puzzles mongosh --quiet rubik:27017/puzzler /home/lichess/deploy/cron/mongodb-puzzle-regen-paths.js
```
