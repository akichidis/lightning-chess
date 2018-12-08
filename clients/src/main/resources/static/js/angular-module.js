"use strict";

$(document).ready(function() {
    var apiBaseURL = "http://localhost:10015/api/";
    var peers = new Array();

    $('#createGameModal').modal({show: false});

    $("#newGameBtn").click(function() {
        //render opponents options
        $("#opponents option").remove();

        $.each(peers, function(i, item) {
            $("#opponents").append($('<option>', {
                value: item.x500Principal.name,
                text: item.x500Principal.name
            }));
        });

        $('#createGameModal').modal('show');
    });

    $("#abandonGameBtn").click(function() {
    });


    $("#modalCreateGameBtn").click(function() {
        var opponentX500Name = $("#opponents").val();
        var nickname = $("#nickname").val();

        var createGameEndpoint = apiBaseURL + "create-game";

        var postData = { "opponentX500Name": opponentX500Name, "userNickname": nickname }

        $.ajax({
            url: createGameEndpoint,
            type: 'post',
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify(postData),
            success: function(data) {
                console.log(data);
            }
        });
    });

    var retrieveNickname = function() {
        $.get(apiBaseURL + "me", function(data) {
            var myName = data.me.x500Principal.name;

            $("#myNickname").html(myName);
        });
    };

    var retrievePeers = function() {
        $.get(apiBaseURL + "peers", function(data) {
            peers = data.peers;
            console.log(peers);
        });
    }

    retrieveNickname();
    retrievePeers();
});