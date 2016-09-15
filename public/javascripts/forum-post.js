
var oldContents = null;

var editForumPost = function(postNumber) {
    var postSelector = $("#" + postNumber);

    // We grab the text element of the post and turn it into a textarea to make it editable
    oldContents = postSelector.find(".message")

    var postContents = oldContents.text();

    var editableArea = $("<textarea id='post-edit-area' style='width: 100%; height:300px'>");

    editableArea.text(postContents);

    oldContents.replaceWith(editableArea);

    $("#edit-button").hide();
    $("#edit-submit-button").show();
    $("#edit-cancel-button").show();

}

var cancelEdit = function(postNumber) {
    $('#post-edit-area').replaceWith(oldContents);

    $("#edit-button").show();
    $("#edit-submit-button").hide();
    $("#edit-cancel-button").hide();
}