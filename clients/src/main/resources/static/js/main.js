"use strict";

var USER_MESSAGE_STATE_GAME_NOT_CREATED = 0;
var USER_MESSAGE_STATE_USER_TURN = 1;
var USER_MESSAGE_STATE_OPPONENT_TURN = 2;

var CURRENT_GAME;

var REJECTED_GAME_IDS = new Set();

$(document).ready(function() {
    var signaturesConsole = $("#signaturesPanel");
    var messagePopup = $('#messagePopup');
    var createGamePopupErrorAlert = $("#createGameErrorAlert");
    var userMessagesPanel = $("#userTurnPanelDiv");

    var apiBaseURL = "api/";
    var peers = new Array();
    var myName, me;
    var meX500;

    $('#createGameModal').modal({show: false});
    $('#messagePopup').modal({show: false});
    $('#newChallengeGameModal').modal({show: false});

    $("#newGameBtn").click(function() {
        if ($(this).hasClass("disabled")) {
            return false;
        }

        //render opponents options
        $("#opponents option").remove();

        $.each(peers, function(i, X500Name) {
            $("#opponents").append($('<option>', {
                value: X500Name,
                text: X500Name
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

        var createGameEndpoint = apiBaseURL + "games";

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

                // The new game is stored on the global variable
                CURRENT_GAME = new Game(data.gameId, data.transactionId, "", myName, opponentX500Name, true);

                messagePopup.find(".text")
                            .addClass("center")
                            .html('<h4>Game created successfully!</h4><br/>Game ID: ' + data.gameId);

                messagePopup.modal('show');

                // This essentially "starts" the game
                setupNewGame(CURRENT_GAME);
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
                var userColor = CURRENT_GAME.isOrganiser ? 'White' : 'Black';

                userMessagesPanel.html("<b>" + userColor + " plays:</b><br/> It's your turn!");
                break;
            case USER_MESSAGE_STATE_OPPONENT_TURN:
                var opponentColor = CURRENT_GAME.isOrganiser ? 'Black' : 'White';

                userMessagesPanel.html("<b>" + opponentColor + " plays:</b><br/> Waiting for opponent's move...");
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
            myName = data.me;

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
                        text: new Date().toUTCString() + " - " + signature
                    }));
    }

    var setupNewGame = function(chessGame) {
        signaturesConsole.find("option").remove();

        //Set user's status message
        printUserMessage(chessGame.isMyTurn ? USER_MESSAGE_STATE_USER_TURN : USER_MESSAGE_STATE_OPPONENT_TURN);

        appendToSignaturesConsole("New game started. Signed tx id: " + chessGame.transactionId);

        initChessBoard(chessGame.isOrganiser ? 'white' : 'black');

        //Setup chessboard in start position
        board.start();

        $("#newGameBtn").addClass("disabled");
        $("#abandonGameBtn").removeClass("disabled");

        if (!chessGame.isMyTurn) {
            scheduleForNextGameResponse();
        }
    }

    var scheduleForNextGameResponse = function() {
        setTimeout(function() {
            var retrieveGameMovesEndpoint = apiBaseURL + "games/" + CURRENT_GAME.id + "/moves";

            $.get(retrieveGameMovesEndpoint, function(response) {
                if (response != null && response != "") {
                    var gameMove = response.gameMove;

                    var move = new SignedGameMove(gameMove.gameId,
                                                  gameMove.index,
                                                  gameMove.fen,
                                                  gameMove.move,
                                                  gameMove.previousSignature,
                                                  response.signature);

                    if (!CURRENT_GAME.moveExists(move)) {
                        CURRENT_GAME.addOpponentMove(move);
                        return;
                    }
                }

                scheduleForNextGameResponse();
            });
        }, 3000);
    }

    var pollForNewGames = function() {
        setTimeout(function() {
            var retrieveGamesEndpoint = apiBaseURL + "games?size=1";

            $.get(retrieveGamesEndpoint, function(data) {
                var latestGameObj = data.gameStates[0];

                if (latestGameObj == null) {
                    pollForNewGames();
                    return;
                }

                var game = new Game(latestGameObj.state.data.gameId,
                                    latestGameObj.ref.txhash,
                                    latestGameObj.state.data.playerANickname,
                                    latestGameObj.state.data.playerA,
                                    latestGameObj.state.data.playerB,
                                    latestGameObj.state.data.playerA == myName);

                if (REJECTED_GAME_IDS.has(game.id) === false &&
                   ((CURRENT_GAME != null && game.id != CURRENT_GAME.id) || !game.isOrganiser)) {
                    $("#challengeOpponentName").html(game.opponentNickname);

                    $("#newChallengeGameModal").modal('show');

                    $("#rejectGameChallengeBtn").unbind('click').click(function() {
                        REJECTED_GAME_IDS.add(game.id);

                        pollForNewGames();
                    });

                    $("#acceptGameChallengeBtn").unbind('click').click(function() {
                        CURRENT_GAME = game;

                        $("#newChallengeGameModal").modal('hide');

                        $("#opponentNickname").html(CURRENT_GAME.playerA_X500Name);

                        setupNewGame(game);
                    });
                } else {
                    //if no new game found, then poll again
                    pollForNewGames();
                }
            });

        }, 3000);
    }


    /*
     * Called when the current user moves a piece on the board
     */
    var onGameMove = function(move, beforeMoveFenString, afterMoveFenString) {
        // Mark the current move, sign & send to opponent
        CURRENT_GAME.movePlayed(move, beforeMoveFenString);

        // Update the message on UI
        printUserMessage(CURRENT_GAME.isMyTurn ? USER_MESSAGE_STATE_USER_TURN : USER_MESSAGE_STATE_OPPONENT_TURN);
    }


    var initChessBoard = function(orientation) {
        $("#chessBoard").replaceWith('<div id="chessBoard"></div>');

        setupChessBoard(onGameMove, orientation);
    }

    retrieveNickname();
    retrievePeers();
    initChessBoard('white');

    pollForNewGames();


    function SignedGameMove(gameId, index, fen, move, previousSignature, signature) {
        this.gameId = gameId;
        this.index = index;
        this.fen = fen;
        this.move = move;
        this.previousSignature = previousSignature;
        this.signature = signature;
    }

    function Game(id, transactionId, opponentNickname, playerA_X500Name, playerB_X500Name, isOrganiser) {
        this.id = id;
        this.transactionId = transactionId;
        this.opponentNickname = opponentNickname;
        this.playerA_X500Name = playerA_X500Name;
        this.playerB_X500Name = playerB_X500Name;
        this.isOrganiser = isOrganiser;
        this.isMyTurn = isOrganiser;
        this.moveIndex = 0;
        this.previousSignature;
        this.signedGameMoves = new Set();

        CHESS_MOVE_ENABLED = this.isMyTurn;

        this.moveExists = function(signedGameMove) {
            return this.signedGameMoves.has(signedGameMove.index);
        }

        this.flipTurn = function() {
            //flip the game turn
            //game.flipTurn();

            // Enable again the move
            CHESS_MOVE_ENABLED = this.isMyTurn = !this.isMyTurn;
        }

        this.addOpponentMove = function(signedGameMove) {
            console.log("Got new move for game:" + signedGameMove.gameId);

            if (signedGameMove.gameId != this.id) {
                console.error("Received move for another game id! " + signedGameMove.gameId);
            }

            if (signedGameMove.index != this.moveIndex + 1) {
                console.error("Received move index other than the expected!");
            }

            if (signedGameMove.fen != board.fen()) {
                console.error("Received previous fen other than the current!");
            }

            // Add the move on the set of played moves
            this.signedGameMoves.add(signedGameMove.index);

            // Update the move index
            this.moveIndex = signedGameMove.index;

            // Play the move
            var move = JSON.parse(signedGameMove.move);
            board.move(move.from + "-" + move.to);
            game.move(move);

            appendToSignaturesConsole("Move: " + signedGameMove.move + " - signature: " + signedGameMove.signature);

            this.flipTurn();
        }

        this.movePlayed = function(move, fenString) {
            // increment the series index
            this.moveIndex += 1;

            var signGameMove = apiBaseURL + "games/" + this.id + "/moves";

            var postData = { "gameId": this.id,
                             "opponentX500Name": isOrganiser ? playerB_X500Name : playerA_X500Name,
                             "index": this.moveIndex,
                             "fen": fenString,
                             "move": JSON.stringify(move) }

            var thisGame = this;

            $.ajax({
                url: signGameMove,
                type: 'post',
                dataType: 'json',
                contentType: 'application/json',
                data: JSON.stringify(postData),
                success: function(data) {
                    console.log(data);

                    thisGame.previousSignature = data.signature

                    // write to console
                    appendToSignaturesConsole("Move: " + JSON.stringify(move) + " - signature: " + JSON.stringify(data.signature));

                    // Now start listening for next opponent move
                    scheduleForNextGameResponse();

                    thisGame.flipTurn();
                 },
                error: function(data) {
                }
            });
        }
    }
});