# Rest API

The REST API is an interface that helps interact with both the  @ref:[Write Side](write-side.md) and the @ref:[Read Side](read-side.md) using HTTP-Json.

* [Api service definition](#api-service-definition)
* [Api service implementation](#api-service-implementation)

Before one can define his/her api service he/she must define the various protocol messages that describe the REST
service to be implemented.

## Api Service Definition

Lagom-pb offers an interface that helps define the lagom api service which is : `io.superflat.lagompb.BaseService`.


_**The api service definition must be a scala trait.**_

Example:

```scala
package io.superflat.lagompb.samples.account.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall}
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import io.superflat.lagompb.samples.protobuf.account.apis.{
  ApiResponse,
  OpenAccountRequest,
  ReceiveMoneyRequest,
  TransferMoneyRequest
}
import io.superflat.lagompb.BaseService

trait AccountService extends BaseService {

  def openAccount: ServiceCall[OpenAccountRequest, ApiResponse]
  def transferMoney(accountId: String): ServiceCall[TransferMoneyRequest, ApiResponse]
  def receiveMoney(accountId: String): ServiceCall[ReceiveMoneyRequest, ApiResponse]
  def getAccount(accountId: String): ServiceCall[NotUsed, ApiResponse]

  override val routes: Seq[Descriptor.Call[_, _]] = Seq(
    restCall(Method.POST, "/api/accounts", openAccount _),
    restCall(Method.PATCH, "/api/accounts/:accountId/transfer", transferMoney _),
    restCall(Method.PATCH, "/api/accounts/:accountId/receive", receiveMoney _),
    restCall(Method.GET, "/api/accounts/:accountId", getAccount _)
  )
}
```

_**Note: The api service definition must be done in a separate sbt module(recommended way). So a lagom-pb/lagom service requires at least two sbt modules in an sdbt multi-modules project.**_

## Api Service implementation
Once the api service is defined, we can implement the api service by extending the `io.superflat.lagompb.BaseServiceImpl` mixed in with the api definition trait [See api definition section](#api-service-definition).
