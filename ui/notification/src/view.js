var m = require('mithril');

function genericNotification(notification, url, icon, content) {
  return m('a.site_notification', {
    class: notification.type,
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
      m('strong', content.mentionedBy.name),
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
      m('strong', content.invitedBy.name),
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
  return ctrl.data.map(drawNotification);
}

module.exports = function(ctrl) {

  if (ctrl.vm.initiating) return m('div.initiating', m.trust(lichess.spinnerHtml));

  return m('div', {
    class: "site_notifications_box"
  }, [
    recentNotifications(ctrl),
    m('a.more', {
      href: "/notifications"
    }, "See more")
  ]);
};
