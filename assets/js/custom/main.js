// Avoid `console` errors in browsers that lack a console. -- http://html5boilerplate.com/
(function() {
    var method;
    var noop = function () {};
    var methods = [
        'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
        'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
        'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
        'timeStamp', 'trace', 'warn'
    ];
    var length = methods.length;
    var console = (window.console = window.console || {});

    while (length--) {
        method = methods[length];

        // Only stub undefined methods.
        if (!console[method]) {
            console[method] = noop;
        }
    }
}());

//Share icons
function windowPopup(url, width, height) {
  // Calculate the position of the popup so
  // it’s centered on the screen.
  var left = (screen.width / 2) - (width / 2),
      top = (screen.height / 2) - (height / 2);

  window.open(
    url,
    "",
    "menubar=no,toolbar=no,resizable=yes,scrollbars=yes,width=" + width + ",height=" + height + ",top=" + top + ",left=" + left
  );
}
$(".js-social-share").on("click", function(e) {
  e.preventDefault();
  windowPopup($(this).attr("href"), 500, 300);
});

//Share page link
$('#share-page-link').on('click', function(){
  $(this).select();
});

$('.js-hide-button').on('click', function(e) {
  e.preventDefault();
  var hideContent = $(this).parents('.js-hide-me');
  hideContent.hide();
});


//Developer Easter egg
function helpObjective8 () {
  window.location = "https://github.com/d-cent/objective8";
}
console.log("Greetings fellow developers, we need your help -- helpObjective8()");