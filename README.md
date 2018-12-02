<p align="center">
  <img src="./lightning-chess-full-logo.png" alt="Lightning Chess" width="256">
</p>

# Lightning Chess

Lightning Chess is an open source 2-player chess app for the [Corda](https://corda.net) blockchain, initialy built as a proof of concept to show that Corda can support offline transactions. The approach is similar to the [lightning network](https://lightning.network) suggested for Bitcoin and could theoretically be applied to any turn-based games. In short, two transactions are required to fully store a chess game on ledger, one to create the game and another to declare the winner (or a draw). In the meantime, players move pieces without notarising transactions, but by exchanging digital signatures on each move. The lightning chess protocol ensures that the smart contract will cryptographically verify the validity of the results.

Note that Lightning Chess is still under development, the full Cordapp is still not functional and it's not recommended for production use.

## Features

* Web-based user interface that allows Corda node owners to interactively play an online chess game.
* Full Cordapp to store chess states in Corda's Vault and validate the results. 
* Offline gameplay (in terms of notarisation).

## Implementation iterations

- Basic PoC in which the smart contract verifies digital signatures only. This version assumes no malicious users and time to move is not taken into account. We are currently at this stage.
- The smart contract is equipped with chess state validation logic (i.e., if a move is valid). If a user signs an invalid move, this can be used as a cryptographic evidence of malicious activity and the the opponent will be able to provide this proof to win the game. Along the same lines, a checkmate is automatically checked by the contract's verify method. 
- Use Oracles for handling disputes on "time to respond".

## Contributing

We welcome contributions to Lightning Chess! You may find the list of contributors [here](./CONTRIBUTORS.md).

## License

[Apache 2.0](./LICENSE.md)

## Acknowledgements

[Corda](https://corda.net), a blockchain and smart contract platform developed by [R3](https://r3.com). As a blockchain platform, Corda allows parties to transact directly, with value. Smart contracts allow Corda to do this using complex agreements and any asset type. This capability has broad applications across industries including finance, supply chain and healthcare.
