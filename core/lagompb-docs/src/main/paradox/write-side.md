## Write Side Implementation

The Write Side helps define the persistence entity/aggregate. To be able to define an aggregate root using lagom-common
the following implementations are required:

* [commands](#commands)
* [events](#events)
* [state](#state)
* [commands handler](#commands-handler)
* [events handler](#events-handler)
* [aggregate root](#aggregate-root)


Let us now dive into the details of these components.

## Commands
Commands must be defined in [protocol buffer messages](https://developers.google.com/protocol-buffers/docs/proto3). The commands can also be used as the api requests.

Example:
```protobuf
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
```protobuf
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
State is also defined in protocol buffer message. The state represents the actual business entity being modelled. It is the result of events that have occurred on the entity/aggregate root. The state can be used as the api response when the request is successful.

Example:
```protobuf
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
The commands handler is the meat of the aggregate. They encode the business rules of your entity/aggregate and act as a guardian of the aggregate/entity consistency. 
The commands handler must first validate that the incoming command can be applied to the current model state. The implementation of a command handler must extends the `lagompb.BaseCommandHandler[TState]` where `TState` is the generated scala case class from the state proto definition. See (state section)[#state]

The only function to override is `handle(command: BaseCommand, state: TState, stateMeta: StateMeta): Try[CommandHandlerResponse]`.

## Events handler
The event handlers are used **_to mutate the state_** of the Aggregate by applying the events to it. Event handlers must be pure functions. 
To implement the events handler the `lagompb.BaseEventHandler[TState] must be extended where `TState` is the generated scala case class from the state proto definition. 

The only function to override is `handle(event: scalapb.GeneratedMessage, state: TState): TState`. As one can see the event handler makes available the current state of the aggregate/entity.

## Aggregate Root
The aggregate root or model is defined in terms of Commands, Events, and State in the world of ES/CQRS and that has been the approach taken in lagom-common. 
The aggregate root is implemented by simply extending the `lagompb.BaseAggregate[TState]` where `TState` is the generated scala case class from the state proto definition. See [state section](#state).

There are only four attributes to override:

* the commands hander. See [commands handler section](#commands-handler)
* the events handler. See [events handler section](#events-handler)
* the initial state
* the aggregate name

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

