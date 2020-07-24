# Rest API

The REST API is an interface that helps interact with both the  @ref:[Write Side](write-side.md) and the @ref:[Read Side](read-side.md) using HTTP-Json.

* [api service definition](#api-service-definition)
* [api service implementation](#api-service-implementation)
* [application loader](#application-loader)

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

## Application Loader

The application loader defines how your lagom-pb/lagom based application should start. Two loading are available:

* the development mode (for local development)
* the production mode

To define the application loader, we must _**first**_ define the application by extending the `io.superflat.lagompb.BaseApplication`. 
Once the application is implemented then we can define the application loader by extending the `com.lightbend.lagom.scaladsl.server.LagomApplicationLoader`.

Example:

```scala
package io.superflat.lagompb.samples.account

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.api.Descriptor
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server.{
  LagomApplication,
  LagomApplicationContext,
  LagomApplicationLoader,
  LagomServer
}
import com.softwaremill.macwire.wire
import io.superflat.lagompb.{AggregateRoot, BaseApplication, CommandHandler, EventHandler}
import io.superflat.lagompb.encryption.{NoEncryption, ProtoEncryption}
import io.superflat.lagompb.samples.account.api.AccountService
import io.superflat.lagompb.samples.protobuf.account.state.BankAccount

abstract class AccountApplication(context: LagomApplicationContext) extends BaseApplication(context) {
  // Let us hook in the readSide Processor
  lazy val accountRepository: AccountRepository =
    wire[AccountRepository]

  // wire up the various event and command handler
  lazy val eventHandler: EventHandler[BankAccount] = wire[AccountEventHandler]
  lazy val commandHandler: CommandHandler[BankAccount] = wire[AccountCommandHandler]
  lazy val aggregate: AggregateRoot[BankAccount] = wire[AccountAggregate]
  lazy val encryption: ProtoEncryption = NoEncryption
  lazy val accountProjection: AccountReadProjection = wire[AccountReadProjection]

  override def aggregateRoot: AggregateRoot[_] = aggregate

  override def server: LagomServer =
    serverFor[AccountService](wire[AccountServiceImpl])
      .additionalRouter(wire[AccountGrpcServiceImpl])
  lazy val accountKafkaProjection: AccountKafkaProjection = wire[AccountKafkaProjection]

  accountProjection.init()
  accountKafkaProjection.init()
}

class AccountApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[AccountService])
}

```
