$(function() {
  
  //Policy page
  $('.vote-link').on('click', function(evt) {
    evt.preventDefault();
    var val = parseInt( $(this).find('#count').text() );
    $(this).find('#count').text( val+1 );
  });

  $('#js-toggle-responses').on('click', function(evt) {
    evt.preventDefault();
    $('#js-item-responses').toggleClass('hidden');
  });

});