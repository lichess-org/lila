var m = require('mithril');

function userFullName(u) {
  return u.title ? u.title + ' ' + u.name : u.name;
}

function genericNotification(notification, url, icon, content) {
  return m('a.site_notification', {
    class: notification.type + (notification.read ? '' : ' new'),
    href: url
  }, [
    m('i', {
      'data-icon': icon
    }),
    m('span.content', content)
  ]);
}

function drawTime(notification) {
  return m('time', {
    class: "moment-from-now",
    datetime: new Date(notification.date).toISOString()
  });
};

function drawMentionedNotification(notification) {
  var content = notification.content;
  var url = "/forum/redirect/post/" + content.postId

  return genericNotification(notification, url, 'd', [
    m('span', [
      m('strong', userFullName(content.mentionedBy)),
      drawTime(notification)
    ]),
    m('span', ' mentioned you in « ' + content.topic + ' ».')
  ]);
};

function drawStudyInviteNotification(notification) {
  var content = notification.content;
  var url = "/study/" + content.studyId;

  return genericNotification(notification, url, '', [
    m('span', [
      m('strong', userFullName(content.invitedBy)),
      drawTime(notification)
    ]),
    m('span', " invited you to « " + content.studyName + ' ».')
  ]);
};

function drawUnhandled(notification) {
  console.dir(notification);
  console.error(notification, "unhandled notification");
};

var drawHandlers = {
  mentioned: drawMentionedNotification,
  invitedStudy: drawStudyInviteNotification
};

function drawNotification(notification) {
  var handler = drawHandlers[notification.type] || drawUnhandled;
  return handler(notification);
}

function recentNotifications(ctrl) {
  return m('div.notifications', {
    config: function() {
      $('body').trigger('lichess.content_loaded');
    }
  }, ctrl.data.pager.currentPageResults.map(drawNotification));
}

function empty() {
  return m('div', {
    class: 'empty text',
    'data-icon': '',
  }, 'No notifications.');
}

module.exports = function(ctrl) {

  if (ctrl.vm.initiating) return m('div.initiating', m.trust(lichess.spinnerHtml));

  var nb = ctrl.data.pager.currentPageResults.length;

  return [
    ctrl.data.pager.previousPage ? m('div.pager.prev', {
      'data-icon': 'S',
      onclick: ctrl.previousPage
    }) : null,
    nb ? recentNotifications(ctrl) : empty(),
    ctrl.data.pager.nextPage ? m('div.pager.next', {
      'data-icon': 'R',
      onclick: ctrl.nextPage
    }) : null
  ];
};
