# lagom-pb = lagom + protocol buffer

[![Build Status](https://travis-ci.org/super-flat/lagom-pb.svg?branch=master)](https://travis-ci.org/super-flat/lagom-pb)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/59e8747c2777466cb75d73d5fea8c8a8)](https://app.codacy.com/gh/super-flat/lagom-pb?utm_source=github.com&utm_medium=referral&utm_content=super-flat/lagom-pb&utm_campaign=Badge_Grade_Dashboard)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/67ead50b17f842dbab2dec43922535da)](https://www.codacy.com/gh/super-flat/lagom-pb?utm_source=github.com&utm_medium=referral&utm_content=super-flat/lagom-pb&utm_campaign=Badge_Coverage)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Scala shared code for [lagom](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html) development using protocol buffer.

This library helps write lagom microservices easily by making use of protocol buffer messages to define the es/cqrs core
components like _**api requests/responses**_, _**grpc services**_, _**events**_, _**commands**_ and _**state**_.

## Features

- Implementation of an HTTP/Json based microservice using REST interfaces by defining _**api requests and responses as protobuf messages**_.

- Implementation of a gRPC based microservice using protocol buffer messages. More info: [gRPC](https://grpc.io/).

- ReadSide in-built battery via ([Message broker api](https://www.lagomframework.com/documentation/1.6.x/scala/MessageBrokerApi.html)) to publish domain events to kafka as serialized protocol buffer messages 
and ([Akka Projection](https://doc.akka.io/docs/akka-projection/current/)).

- Easy definition of aggregate root, events and command handlers.

- Pure testable functions for events and commands handlers.

- Easy definition of api service descriptors either with message broker api or without.

- Easy implementation of api service either with message broker api or without.

- Metadata adds some `revision number` that can help easily implement optimistic lock.

- At every event handled a snapshot of the aggregate state with the metadata are made available for the readSide.

- All events, snapshots and readSide offsets are persisted to Postgres SQL.

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2020 superflat

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0]
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
