const colls = [
  "activity",
  "boosting",
  "cache",
  "challenge",
  "chat",
  "coordinate_score",
  "crosstable2",
  "f_categ",
  "f_post",
  "f_topic",
  "fishnet_analysis",
  "fishnet_client",
  "flag",
  "forecast",
  "game5",
  "game_note",
  "history3",
  "learn_progress",
  "matchup",
  "modlog",
  "msg_msg",
  "msg_thread",
  "notify",
  "oldCache",
  "perf_stat",
  "plan_charge",
  "plan_patron",
  "playban",
  "pref",
  "ranking",
  "report2",
  "round_alarm",
  "security",
  "seek",
  "seek_archive",
  "shutup",
  "simul",
  "study",
  "study_chapter",
  "study_topic",
  "study_user_topic",
  "team",
  "team_member",
  "timeline_entry",
  "tournament2",
  "tournament_leaderboard",
  "tournament_pairing",
  "tournament_player",
  "trophyKind",
  "user4",
  "video",
];

colls.forEach(function(coll) {
   indexes = db[coll].getIndexes();
   if(indexes && indexes.length>0) {
        print("//Indexes for " + coll + ":");
   }
   indexes.forEach(function(index) {
       var options = {};
       if(index.unique) {
           options.unique = index.unique;
       }
       if(index.sparse) {
           options.sparse = index.sparse;
       }
       if(index.partialFilterExpression) {
           options.partialFilterExpression = index.partialFilterExpression;
       }
        // options.background = true;
       options = JSON.stringify(options);
       var key = JSON.stringify(index.key);
       if(key !== '{"_id":1}') {
        print('db.' + coll + '.createIndex(' + key + ', ' + options + ');');
       }
   });
});
