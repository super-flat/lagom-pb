# Read Side

Read Side helps define how the events persisted by the Aggregate root are processed. 

To be able to implement the readSide the following dependency is required:

@@dependency[sbt,Maven] {
  group="io.superflat"
  artifact="lagompb-readside_2.13"
  version="0.8.2"
}

Kindly check the latest version from @link:[Versions](https://github.com/super-flat/lagom-pb/releases) { open=new }

## Implementation

The following classes can be extended to implement a read side processor.

- `io.superflat.lagompb.readside.ReadSideProcessor`
- `io.superflat.lagompb.readside.TypedReadSideProcessor` 

The **_event_** and the **_resulting state_** is made available for processing. The read offsets are stored in postgres database.

- `io.superflat.lagompb.readside.KafkaPublisher` This is used to send the **_event_**, and the **_resulting state_** into kafka as serialized protocol buffer message.
The following protocol buffer messages are used to persist the messages to kafka:

```proto
syntax = "proto3";

package lagompb;

option java_package = "io.superflat.lagompb.protobuf";

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

