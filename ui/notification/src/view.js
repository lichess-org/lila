var m = require('mithril');

    var drawMentionedNotification = function(notification) {

        var content = notification.content;
        var category = content.category;
        var topic = content.topic;
        var mentionedBy = content.mentionedBy.name;
        var postId = content.postId;

        var mentionedByProfile = "/@/" + mentionedBy;
        var postUrl = "/forum/redirect/post/" + postId;

        return m('div', [
                m('a', {href: mentionedByProfile}, mentionedBy),
                m('span', ' mentioned you in the '),
                m('a', {href: postUrl, class: "forum_post_link"}, topic),
                m('span', ' forum thread')
            ]
        );
    };

    var drawStudyInviteNotification = function(notification) {
        var content = notification.content;
        var invitedBy = content.invitedBy.name;
        var studyName = content.studyName;
        var studyId = content.studyId;

        var invitedByProfile = "/@/" + invitedBy;
        var studyUrl = "/study/" + studyId;

        return m('div', [
            m('a', {href: invitedByProfile}, invitedBy),
            m('span', " invited you to their "),
            m('a', {href: studyUrl}, studyName),
            m('span', " study")
          ]
        );
    }

    var drawNotification = function (notification) {
        var content = null;
        switch (notification.type) {
            case "mentioned" : content =  drawMentionedNotification(notification); break;
            case "invitedStudy": content = drawStudyInviteNotification(notification); break;
            default: console.dir(notification); console.error("unhandled notification"); break;
        }

        var date = new Date(notification.date);
        return m('div', {class: 'site_notification'}, [
            content,
            m('time', {class:"moment-from-now", datetime: date})
        ]);
    };

    function recentNotifications(ctrl) {
        return ctrl.data.map(drawNotification);
    }

module.exports = function(ctrl) {

    if (ctrl.vm.initiating) return m('div.initiating', m.trust(lichess.spinnerHtml));

    return m('div', {class: "site_notifications_box"}, [
        recentNotifications(ctrl),
        m('a', {href:"/notifications"}, "See more")
    ]);
};
