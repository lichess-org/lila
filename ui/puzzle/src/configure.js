var merge = require('lodash-node/modern/objects/merge');
var puzzle = require('./puzzle');

module.exports = function(data, config) {

  merge(data, config);

  data.puzzle.initialMove = puzzle.str2move(data.puzzle.initialMove);
};

  // (let [puzzle (-> (:puzzle config)
  //                  (rename-key :initialMove :initial-move)
  //                  (rename-key :initialPly :initial-ply)
  //                  (rename-key :gameId :game-id)
  //                  (update-in [:initial-move] str->move)
  //                  (update-in [:lines] parse-lines)
  //                  (assoc :opponent-color (opposite-color (get-in config [:puzzle :color]))))]
  //   {:puzzle puzzle
  //    :mode (:mode config) ; view | play | try
  //    :progress []
  //    :comment nil ; :fail | :retry | :great
  //    :attempt (rename-key (:attempt config) :userRatingDiff :user-rating-diff)
  //    :win (:win config)
  //    :voted (:voted config)
  //    :user (:user config)
  //    :difficulty (:difficulty config)
  //    :ctrl ctrl
  //    :router router
  //    :trans trans
  //    :started-at (js/Date.)
  //    :chess (chess/make (:fen puzzle))
  //    :chessground (chessground.api/main
  //                   {:fen (:fen puzzle)
  //                    :orientation (:color puzzle)
  //                    :turnColor (:opponent-color puzzle)
  //                    :movable {:free false
  //                              :color (when (#{"play" "try"} (:mode config))
  //                                       (:color puzzle))
  //                              :events {:after #(ctrl :user-move [%1 %2])}}
  //                    :animation {:enabled true
  //                                :duration 200}
  //                    :premovable {:enabled true}})}))
