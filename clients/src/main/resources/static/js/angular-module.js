"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($scope, $http, $location, $uibModal) {
    const allNodesApiBaseUrls = {"O=PlayerA, L=London, C=GB":"http://localhost:10015/api/",
                                 "O=PlayerB, L=New York, C=US":"http://localhost:10012/api/"}

    const demoApp = this;

    demoApp.allNodesApiBaseUrls = allNodesApiBaseUrls

    // Try to connect first via the PlayerA
    const apiBaseURL = allNodesApiBaseUrls["O=PlayerA, L=London, C=GB"];
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                demoApp: () => demoApp,
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.signatures = [];

    demoApp.getSignatures = () => {

            let player;
            let allRetrievedSignatures = [];

           for (player in allNodesApiBaseUrls) {
                 $http.get(allNodesApiBaseUrls[player] + "signatures")
                .then((response) => {
                                const retrievedSignatures = Object.keys(response.data)
                                                        .map((key) => response.data[key])
                                                        .reverse();

                                allRetrievedSignatures = allRetrievedSignatures.concat(retrievedSignatures)

                                demoApp.signatures = allRetrievedSignatures
                        });
           }

            demoApp.signatures = $scope.user;
    };

});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, demoApp, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;

    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            $uibModalInstance.close();

            const nodeBaseApiUrl = demoApp.allNodesApiBaseUrls[modalInstance.form.player];

            const createGameEndpoint = `${nodeBaseApiUrl}create-game?&nickname=${modalInstance.form.nickname}`;

            // Create PO and handle success / fail responses.
            $http.post(createGameEndpoint).then(
                (result) => {
                    modalInstance.displayMessage(result);
                },
                (result) => {
                    modalInstance.displayMessage(result);
                }
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create IOU modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the IOU.
    function invalidFormInput() {
        return false;
        //return isNaN(modalInstance.form.value) || (modalInstance.form.counterparty === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});