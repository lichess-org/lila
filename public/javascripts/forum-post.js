
// Map of old post contents by post number in case the user clicks 'edit' on multiple posts
// on the same page (although rare.)
var oldContents = {};

var editForumPost = function(postId, postNumber) {
    var postSelector = $("#" + postNumber);

    // We grab the text element of the post and turn it into a textarea to make it editable
    oldContents[postNumber] = postSelector.find(".message");

    var old = oldContents[postNumber];

    var postContents = old.text();

    var editForm = $('<form>', {
                                id: 'post-edit-form-' + postNumber,
                                method: 'POST',
                                action:"/forum/post/" + postId
                                });

    var formTextArea = $('<textarea>', {id:'post-edit-area-' + postNumber, name:"changes", class:'edit-post-box'});
    formTextArea.text(postContents);

    var formSubmitButton = $('<input>', {type:'submit', value: 'Submit'});
    var formCancelButton = $('<a>', {'data-icon':'s', onclick:'cancelEdit(' + postNumber + ')' }).text("Cancel");

    editForm.append(formTextArea);
    editForm.append(formSubmitButton);
    editForm.append(formCancelButton);

    old.replaceWith(editForm);

    $("#edit-button-" + postNumber).hide();
};

var cancelEdit = function(postNumber) {
    $('#post-edit-form-' + postNumber).replaceWith(oldContents[postNumber]);

    $("#edit-button-" + postNumber).show();
}