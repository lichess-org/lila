"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function app(wrap, toggle) {
    var $wrap = $(wrap), $input = $wrap.find('input');
    window.lichess.userAutocomplete($input, {
        focus: 1,
        friend: true,
        onSelect: function (q) {
            execute(q.name || q);
            $input.val('');
        }
    }).done(function () {
        $input.on('blur', function () { return wrap.classList.contains('shown') && toggle; });
    });
}
exports.app = app;
function execute(q) {
    if (!q)
        return;
    if (q[0] === '/')
        command(q.slice(1));
    else
        location.href = '/@/' + q;
}
function command(q) {
    var parts = q.split(' '), exec = parts[0];
    if (exec === 'tv' || exec === 'follow')
        location.href = '/@/' + parts[1] + '/tv';
    else if (exec === 'play' || exec === 'challenge' || exec === 'match')
        location.href = '/?user=' + parts[1] + '#friend';
    else
        alert('Unknown command: ' + q);
}
