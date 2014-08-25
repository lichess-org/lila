$(function() {
    var since = Date.now();
    function startTime()
    {
        var now = Date.now(),
        elapsed = new Date(now - since),
        h=formatTime(elapsed.getUTCHours()),
        m=formatTime(elapsed.getUTCMinutes()),
        s=formatTime(elapsed.getUTCSeconds());
        $('#time > span').text(h+":"+m+":"+s);
        setTimeout(function(){startTime();},500);
    }

    function formatTime(i)
    {
        if (i < 10) {
            i="0" + i;
        }
        return i;
    }

    startTime();
});
