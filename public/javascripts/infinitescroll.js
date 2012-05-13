$(function() {
    $('div.infinitescroll').each(function() {
        $(this).infinitescroll({
            navSelector: "div.pager",
            nextSelector: "div.pager a:last",
            itemSelector: "div.infinitescroll .paginated_element",
            loadingText: "",
            donetext: "---"
        }, function() {
            $('body').trigger('lichess.content_loaded');
        }).find('div.pager').hide();
    });
});
