# Read Side

Read Side helps define how the events persisted by the Aggregate root are processed. 

Lagom-pb offers two types of read side implementation:

* [lagom based read side](#lagom-based-read-side)
* [akka projection based read side](#akka-projection-based-read-side)

To be able to implement the readSide the following dependency is required:

@@dependency[sbt,Maven] {
  group="io.superflat"
  artifact="lagompb-readside_2.13"
  version="0.1.0"
}

## Lagom based read side

At the moment to implement the lagom based read side, **_only one_** of the following classes can be implemented:

* `lagompb.LagompbSlickRead` for a slick based implementation. The **_event_** and the **_resulting state_** is made available 
for processing. The offsets are stored in postgres database since the only supported persistence storage at the moment is postgres.
* `lagompb.LagompbJdbcRead` for JDBC based implementation.

The only limitation with any lagom based read side is that you can only have one implementation which does not give the
freedom of having multiple read side ti handle different events based upon the business rule. See @link:[Issue #34](https://github.com/lagom/lagom/issues/1346) { open=new }

## Akka Projection based read side

The solution to the limitation of the lagom based read side is the integration of akka projection. The following classes can be extended
to implement a read side processor.

* `lagompb.LagompbSlickProjection` The **_event_** and the **_resulting state_** is made available for processing. The read offsets are stored in postgres database.
* `lagompb.LagompbKafkaProjection` This is used to send the **_event_**, and the **_resulting state_** into kafka as serialized protocol buffer message.
The following protocol buffer messages are used to persist the messages to kafka:

```proto
syntax = "proto3";

package lagompb.protobuf;

import "google/protobuf/any.proto";

message KafkaEvent {
    // the service name
    string service_name = 1;
    // the kafka partition key
    string partition_key = 2;
    // the actual event
    google.protobuf.Any event = 3;
    // the actual state with state meta
    StateWrapper state = 4;
}

message StateWrapper {
    // the entity state
    google.protobuf.Any state = 1;
    // metadata from the event that made this state
    MetaData meta = 2;
}
```
@@@ note

Any protocol buffer message deserializer can be used to unmarshal the byte array persisted into kafka topic.

Lagom-pb via the akka projection maintain read offsets in postgres database. 

**_Many instances_** of these abstract classes can be instantiated depending upon the business rules.

@@@

