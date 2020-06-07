# Lagom-pb
Lagom-pb = @link:[lagom](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html) { open=new } + @link:[Protocol Buffer](https://developers.google.com/protocol-buffers/docs/proto3)

[![Build Status](https://travis-ci.org/super-flat/lagom-pb.svg?branch=master)](https://travis-ci.org/super-flat/lagom-pb)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/59e8747c2777466cb75d73d5fea8c8a8)](https://app.codacy.com/gh/super-flat/lagom-pb?utm_source=github.com&utm_medium=referral&utm_content=super-flat/lagom-pb&utm_campaign=Badge_Grade_Dashboard)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Scala shared code for @link:[lagom](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html) { open=new } development. 

This library helps write lagom microservices easily by making use of protocol buffer messages to define the es/cqrs core
components like _**api requests/responses**_, _**grpc services**_, _**events**_, _**commands**_ and _**state**_. 

@@toc { depth=1 }

@@@ index

* [Write Model](write-side.md)
* [Read Model](read-side.md)
* [Rest Implementation](rest-api.md)
* [gRPC Implementation](grpc.md)
* [Configuration](configuration.md)

@@@

## Sample Project

@link:[Sample](https://github.com/super-flat/lagom-pb-sample)
