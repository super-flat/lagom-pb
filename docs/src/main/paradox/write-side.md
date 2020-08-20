# Write Side

Write Side helps define the persistence entity/aggregate and how data come to the application by commands.

* [commands](#commands)
* [events](#events)
* [state](#state)
* [commands handler](#commands-handler)
* [events handler](#events-handler)
* [aggregate root](#aggregate-root)

Let us now dive into the details of these components.

To implement the write side you require the following dependencies:

@@dependency[sbt,Maven] {
  group="io.superflat"
  artifact="lagompb-core_2.13"
  version="0.4.0"
}

## Commands
Commands must be defined in [protocol buffer messages](https://developers.google.com/protocol-buffers/docs/proto3). The commands can also be used as the api requests.

Example:
```proto
syntax = "proto3";

package account;

message OpenBankAccount {
    string company_uuid = 1;
    string account_id = 2;
    double balance = 3;
    string account_owner = 4;
}
```

## Events
Events as commands must be defined in [protocol buffer messages](https://developers.google.com/protocol-buffers/docs/proto3).

Example:
```proto
syntax = "proto3";

package account;

message AccountOpened {
    string company_uuid = 1;
    string account_id = 2;
    double balance = 3;
    string account_owner = 4;
}
```

## State
Aggregate State must be defined in protocol buffer message. 
It is the result of events that have occurred on the entity/aggregate root. 
The state can be used as the api response when the request is successful.

Example:
```proto
syntax = "proto3";

package account;

message BankAccount {
    string account_id = 1;
    double account_balance = 2;
    string account_owner = 3;
    bool is_closed = 4;
}
```

## Commands handler
Commands handler is the meat of the aggregate. They encode the business rules of your entity/aggregate and act as a guardian of the aggregate/entity consistency. 
Commands handler must first validate that the incoming command can be applied to the current model state. 
The implementation of a command handler must extend the `io.superflat.lagompb.CommandHandler[TState]` where `TState` is the generated scala case class from the state proto definition. See [state section](#state)

The only function to override is `handle(command: LagompbCommand, state: TState, stateMeta: StateMeta): Try[CommandHandlerResponse]`.

## Events handler
Event handlers **_mutate the state_** of the Aggregate by applying the events to it. 
Event handlers must be pure functions. This allows them to be tested without the need of the whole akka system.
Events handler must extend the `io.superflat.lagompb.EventHandler[TState]` must be extended where `TState` is the generated scala case class from the state proto definition. 

The only function to override is `handle(event: scalapb.GeneratedMessage, state: TState): TState`. 
As one can see the event handler makes available the current state of the aggregate/entity.

## Journal and Snapshot encryption
One has the ability to add an encryption Journal and Snapshot persistence. This can come in handy in GDPR-like deletion cases and general message security.
These are the features to expect:
* It encrypts event and state Any messages in place in the wrapper
* The encryption happens in the command handler
* The default behavior is to encrypt nothing in cases where you have not provided encryption.

## Aggregate Root
The aggregate root or model is defined in terms of **_Commands_**, **_Events_**, and **_State_** in the world of ES/CQRS and that has been the approach taken into lagom-pb. 
The aggregate root must extend the `io.superflat.lagompb.AggregateRoot[TState]` where `TState` is the generated scala case class from the state proto definition. See [state section](#state).

There are only four attributes to override:

* Commands handler. See [commands handler section](#commands-handler)
* Events handler. See [events handler section](#events-handler)
* Encryption Adapter. See [journal and snapshot encryption section](#journal-and-snapshot-encryption)
* Initial state
* Aggregate name

Example:

```scala
package io.superflat.lagompb.samples.account

import akka.actor.ActorSystem
import io.superflat.lagompb.{AggregateRoot, CommandHandler, EventHandler}
import io.superflat.lagompb.encryption.EncryptionAdapter
import io.superflat.lagompb.samples.protobuf.account.state.BankAccount
import scalapb.GeneratedMessageCompanion

final class AccountAggregate(
    actorSystem: ActorSystem,
    commandHandler: CommandHandler[BankAccount],
    eventHandler: EventHandler[BankAccount],
    encryptionAdapter: EncryptionAdapter
) extends AggregateRoot[BankAccount](actorSystem, commandHandler, eventHandler, encryptionAdapter) {

  override def aggregateName: String = "Account"

  override def stateCompanion: GeneratedMessageCompanion[BankAccount] = BankAccount
}
```
