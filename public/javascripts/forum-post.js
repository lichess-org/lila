
var oldContents = null;

var editForumPost = function(postNumber) {
    var postSelector = $("#" + postNumber);

    // We grab the text element of the post and turn it into a textarea to make it editable
    oldContents = postSelector.find(".message")

    var postContents = oldContents.text();

    var editableArea = $("<textarea id='post-edit-area-" + postNumber + "' style='width: 100%; height:300px'>");

    editableArea.text(postContents);

    oldContents.replaceWith(editableArea);

    $("#edit-button-" + postNumber).hide();
    $("#edit-submit-button-" + postNumber).show();
    $("#edit-cancel-button-" + postNumber).show();
}

var submitEdit = function(postNumber, postId) {
    var newContents = $("#post-edit-area-" + postNumber).val();

    jQuery.post("/forum/post/" + postId, newContents, function(succ) {
        var currentPage = window.location.href;

        if (currentPage.indexOf("#") != -1) {
            location.reload();
        } else {
            var refreshTo = currentPage + "#" + postNumber;
            location.reload();
            window.location.href = refreshTo;
        }

    }).fail(function(fail) {
        console.dir(fail);
    });
}

var cancelEdit = function(postNumber) {
    $('#post-edit-area-' + postNumber).replaceWith(oldContents);

    $("#edit-button-" + postNumber).show();
    $("#edit-submit-button-" + postNumber).hide();
    $("#edit-cancel-button-" + postNumber).hide();
}