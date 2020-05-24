# lagompb = lagom + protocol buffer

Scala shared code for [lagom](https://www.lagomframework.com/documentation/1.6.x/scala/Home.html) development. 

This library helps write lagom microservices easily by making use of protocol buffer messages to define the es/cqrs core
components like _**api requests/responses**_, _**grpc services**_, _**events**_, _**commands**_ and _**state**_. 

@@toc

@@@ index

* [Write Model](write-side.md)
* [Read Model](read-side.md)
* [Rest Implementation](rest-api.md)
* [gRPC Implementation](grpc.md)
* [Configuration](configuration.md)

@@@

Prior to using this library kindly add the content of the `plugins.sbt` of this repository into to your `project/plugins.sbt`:
