var m = require('mithril');
var util = require('../util');
var scoring = require('../score');
var stages = require('../stage/list');

function makeStars(nb) {
  var stars = [];
  for (var i = 0; i < 4 - nb; i++)
    stars.push(
      m('i', {
        'data-icon': '',
      })
    );
  return stars;
}

function ribbon(ctrl, s, status, res) {
  if (status === 'future') return;
  var content;
  if (status === 'ongoing') {
    var p = ctrl.stageProgress(s);
    content = p[0] ? p.join(' / ') : ctrl.trans.noarg('play');
  } else content = makeStars(scoring.getStageRank(s, res.scores));
  if (status !== 'future')
    return m(
      'span.ribbon-wrapper',
      m(
        'span.ribbon',
        {
          class: status,
        },
        content
      )
    );
}

function whatNext(ctrl) {
  var makeStage = function (href, img, title, subtitle, done) {
    var transTitle = ctrl.trans.noarg(title);
    return m(
      'a',
      {
        class: 'stage done' + titleVerbosityClass(transTitle),
        href: href,
      },
      [
        done ? m('span.ribbon-wrapper', m('span.ribbon.done', makeStars(1))) : null,
        m('img', {
          src: util.assetUrl + 'images/learn/' + img + '.svg',
        }),
        m('div.text', [m('h3', transTitle), m('p.subtitle', ctrl.trans.noarg(subtitle))]),
      ]
    );
  };
  var userId = ctrl.data._id;
  return m('div.categ.what_next', [
    m('h2', ctrl.trans.noarg('whatNext')),
    m('p', ctrl.trans.noarg('youKnowHowToPlayChess')),
    m('div.categ_stages', [
      userId
        ? makeStage('/@/' + userId, 'beams-aura', 'register', 'getAFreeLichessAccount', true)
        : makeStage('/signup', 'beams-aura', 'register', 'getAFreeLichessAccount'),
      makeStage('/practice', 'robot-golem', 'practice', 'learnCommonChessPositions'),
      makeStage('/training', 'bullseye', 'puzzles', 'exerciseYourTacticalSkills'),
      makeStage('/video', 'tied-scroll', 'videos', 'watchInstructiveChessVideos'),
      makeStage('/#hook', 'sword-clash', 'playPeople', 'opponentsFromAroundTheWorld'),
      makeStage('/#ai', 'vintage-robot', 'playMachine', 'testYourSkillsWithTheComputer'),
    ]),
  ]);
}

function titleVerbosityClass(title) {
  return title.length > 13 ? (title.length > 18 ? ' vvv' : ' vv') : '';
}

module.exports = function (ctrl) {
  return m('div.learn.learn--map', [
    m('div.learn__side', ctrl.opts.side.view()),
    m('div.learn__main.learn-stages', [
      stages.categs.map(function (categ) {
        return m('div.categ', [
          m('h2', ctrl.trans.noarg(categ.name)),
          m(
            'div.categ_stages',
            categ.stages.map(function (s) {
              var res = ctrl.data.stages[s.key];
              var complete = ctrl.isStageIdComplete(s.id);
              var prevComplete = ctrl.isStageIdComplete(s.id - 1);
              var status = 'future';
              if (complete) status = 'done';
              else if (prevComplete || res) status = 'ongoing';
              var title = ctrl.trans.noarg(s.title);
              return m(
                'a',
                {
                  class: 'stage ' + status + titleVerbosityClass(title),
                  href: '/' + s.id,
                  config: m.route,
                },
                [
                  ribbon(ctrl, s, status, res),
                  m('img', {
                    src: s.image,
                  }),
                  m('div.text', [m('h3', title), m('p.subtitle', ctrl.trans.noarg(s.subtitle))]),
                ]
              );
            })
          ),
        ]);
      }),
      whatNext(ctrl),
    ]),
  ]);
};
