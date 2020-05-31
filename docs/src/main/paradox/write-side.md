# Write Side

Write Side helps define the persistence entity/aggregate and how data come to the application by commands.

* [commands](#commands)
* [events](#events)
* [state](#state)
* [commands handler](#commands-handler)
* [events handler](#events-handler)
* [aggregate root](#aggregate-root)

Let us now dive into the details of these components.

To be able to implement the write side the following dependency is required:

@@dependency[sbt,Maven] {
  group="io.superflat"
  artifact="lagompb-core_2.13"
  version="0.1.0"
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
The implementation of a command handler must extend the `lagompb.LagompbCommandHandler[TState]` where `TState` is the generated scala case class from the state proto definition. See [state section](#state)

The only function to override is `handle(command: LagompbCommand, state: TState, stateMeta: StateMeta): Try[CommandHandlerResponse]`.

## Events handler
Event handlers **_mutate the state_** of the Aggregate by applying the events to it. 
Event handlers must be pure functions. This allows them to be tested without the need of the whole akka system.
Events handler must extend the `lagompb.LagompbEventHandler[TState] must be extended where `TState` is the generated scala case class from the state proto definition. 

The only function to override is `handle(event: scalapb.GeneratedMessage, state: TState): TState`. 
As one can see the event handler makes available the current state of the aggregate/entity.

## Aggregate Root
The aggregate root or model is defined in terms of **_Commands_**, **_Events_**, and **_State_** in the world of ES/CQRS and that has been the approach taken into lagom-pb. 
The aggregate root must extend the `lagompb.LagompbAggregate[TState]` where `TState` is the generated scala case class from the state proto definition. See [state section](#state).

There are only four attributes to override:

* Commands handler. See [commands handler section](#commands-handler)
* Events handler. See [events handler section](#events-handler)
* Initial state
* Aggregate name

Example:

```scala
import akka.actor.ActorSystem
import lagompb.LagompbAggregate
import lagompb.LagompbCommandHandler
import lagompb.LagompbEventHandler
import lagompb.tests.TestState
import scalapb.GeneratedMessageCompanion
import com.typesafe.config.Config

final class TestAggregate(
    actorSystem: ActorSystem,
    config: Config,
    commandHandler: LagompbCommandHandler[TestState],
    eventHandler: LagompbEventHandler[TestState]
) extends LagompbAggregate[TestState](actorSystem, config, commandHandler, eventHandler) {

  override def aggregateName: String = "TestAggregate"

  override def stateCompanion: GeneratedMessageCompanion[TestState] = TestState
}

```

