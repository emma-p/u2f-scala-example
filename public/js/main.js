$(document).ready(function() {
    var REGISTER_BTN = $('[data-input="register"]');
    var TOUCH = $('[data-display="touch"]');
    var TOKEN_RESPONSE = $('#tokenResponse');

    var onTouch = function(data) {
        if(data.errorCode) {
            console.error("U2F failed with error: " + data.errorMessage);
            return;
        }
        TOKEN_RESPONSE.val(JSON.stringify(data));
        TOUCH.submit();
    };

    TOUCH.hide();

    REGISTER_BTN.on('click', function() {
        console.log('register');

        $.post('/register', function(data) {
            console.log('request data arrived from app');
            u2f.register(data.registerRequests, data.authenticateRequests, onTouch);
            TOUCH.show();
        });
    });
});