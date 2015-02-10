//Google webfont
WebFont.load({
  google: {
    families: [ 'Titillium+Web:300,700:latin' ]
  }
});

//Social media popups
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
var jsSocialShares = document.querySelectorAll(".js-social-share");
if (jsSocialShares) {
  [].forEach.call(jsSocialShares, function(anchor) {
    anchor.addEventListener("click", function(e) {
      e.preventDefault();

      windowPopup(this.href, 500, 300);
    });
  });
}