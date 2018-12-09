"use strict";

var USER_MESSAGE_STATE_GAME_NOT_CREATED = 0;
var USER_MESSAGE_STATE_USER_TURN = 1;
var USER_MESSAGE_STATE_OPPONENT_TURN = 2;

$(document).ready(function() {
    var signaturesConsole = $("#signaturesPanel");
    var messagePopup = $('#messagePopup');
    var createGamePopupErrorAlert = $("#createGameErrorAlert");
    var userMessagesPanel = $("#userTurnPanelDiv");

    var apiBaseURL = "http://localhost:10015/api/";
    var peers = new Array();
    var myName;

    $('#createGameModal').modal({show: false});
    $('#messagePopup').modal({show: false});

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
        $("#createGameErrorAlert .alertText").html("");

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

                $("#myNickname").html(myName + " (" + nickname + ")");
                $("#opponentNickname").html(opponentX500Name);

                $("#createGameModal").modal('hide');

                messagePopup.find(".text").html(JSON.stringify(data));
                messagePopup.modal('show');

                signaturesConsole.find("option").remove();

                //Setup chessboard in start position
                board.start();

                //Set user's status message
                printUserMessage(USER_MESSAGE_STATE_USER_TURN);
            },
            error: function(data) {
                createGamePopupErrorAlert.find(".alertText").html(JSON.stringify(data));

                createGamePopupErrorAlert.alert('show');
            }
        });
    });

    var printUserMessage = function(state) {
        switch (state) {
            case USER_MESSAGE_STATE_GAME_NOT_CREATED:
                userMessagesPanel.html("No game created yet");
                break;
            case USER_MESSAGE_STATE_USER_TURN:
                userMessagesPanel.html("<b>White plays:</b><br/> It's your turn!");
                break;
            case USER_MESSAGE_STATE_OPPONENT_TURN:
                userMessagesPanel.html("<b>Black plays:</b><br/> Waiting for opponent's move...");
                break;
        }
    }


    var toggleCreateGameErrorAlert = function(){
        createGamePopupErrorAlert.toggleClass('hide');
        return false; // Keep close.bs.alert event from removing from DOM
    }

    createGamePopupErrorAlert.on('close.bs.alert', toggleCreateGameErrorAlert);

    var retrieveNickname = function() {
        $.get(apiBaseURL + "me", function(data) {
            myName = data.me.x500Principal.name;

            $("#myNickname").html(myName);
        });
    };

    var retrievePeers = function() {
        $.get(apiBaseURL + "peers", function(data) {
            peers = data.peers;
            console.log(peers);
        });
    }

    var appendToSignaturesConsole = function(signature) {
        signaturesConsole.prepend($('<option>', {
                        value: signature,
                        text: new Date() + " - " + signature
                    }));
    }

    retrieveNickname();
    retrievePeers();
    setupChessBoard(appendToSignaturesConsole);
});