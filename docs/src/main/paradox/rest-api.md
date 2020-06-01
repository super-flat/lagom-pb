# Rest API

The REST API is an interface that helps interact with both the  @ref:[Write Side](write-side.md) and the @ref:[Read Side](read-side.md) using HTTP-Json.

* [api service definition](#api-service-definition)
* [api service implementation](#api-service-implementation)
* [application loader](#application-loader)

Before one can define his/her api service he/she must define the various protocol messages that describe the REST
service to be implemented.

## Api Service Definition

Lagom-pb offers two interfaces that help define the lagom api service: 

* `lagompb.LagompbServiceWithKafka` it comes bundled with the lagom message broker api to allow publishing aggregate events and snapshots into a kafka topic. 
The kafka topic is automatically created on behalf of the service using the following format `<service_name>.events`. 
The `<service_name>` must be set via the environment variable `SERVICE_NAME`.
This will require setting up kafka configuration. We will talk about how to configure a lagom-pb based app.
* `lagompb.LagompbService` Aggregate events are not pushed to kafka. However, one can make use of @ref:[Read Side](read-side.md#akka-projection-based-read-side)
to spawn an instance of `lagompb.LagompbKafkaProjection` to push events and snapshots to kafka.

With both trait there is only one thing to define:

* the api routes

_**The api service definition must be a scala trait.**_

Example:

```scala
import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.Service.restCall
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Descriptor, ServiceCall}
import io.superflat.protobuf.account.apis.{ApiResponse, OpenAccountRequest, ReceiveMoneyRequest, TransferMoneyRequest}
import lagompb.LagompbService

trait AccountService extends LagompbService {

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
Once the api service is defined, we can implement the api service by extending the `lagompb.LagompbServiceImpl`. 
If we are dealing with an api service with message broker or `lagompb.LagompbServiceImplWithKafka` mixed in with the api definition trait [See api definition section](#api-service-definition).

## Application Loader

The application loader defines how your lagom-pb/lagom based application should start. Two loading are available:

* the development mode (for local development)
* the production mode

To define the application loader, we must _**first**_ define the application by extending the `lagompb.LagompbApplication`. 
Once the application is implemented then we can define the application loader by extending the `lagompb.LagomApplicationLoader`.

Example:

```scala
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
import io.superflat.account.api.AccountService
import io.superflat.protobuf.account.state.BankAccount
import lagompb.{LagompbAggregate, LagompbApplication, LagompbCommandHandler, LagompbEventHandler}

abstract class AccountApplication(context: LagomApplicationContext) extends LagompbApplication(context) {
  // Let us hook in the readSide Processor
  lazy val accountRepository: AccountRepository =
    wire[AccountRepository]

  lazy val accountProjection: AccountReadProjection = wire[AccountReadProjection]
  lazy val accountKafkaProjection: AccountKafkaProjection = wire[AccountKafkaProjection]

  accountProjection.init()
  accountKafkaProjection.init()

  // wire up the various event and command handler
  lazy val eventHandler: LagompbEventHandler[BankAccount] = wire[AccountEventHandler]
  lazy val commandHandler: LagompbCommandHandler[BankAccount] = wire[AccountCommandHandler]
  lazy val aggregate: LagompbAggregate[BankAccount] = wire[AccountAggregate]

  override def aggregateRoot: LagompbAggregate[_] = aggregate

  override def server: LagomServer =
    serverFor[AccountService](wire[AccountServiceImpl])
      .additionalRouter(wire[AccountGrpcServiceImpl]) // gRPC service integration
}

class AccountApplicationLoader extends LagomApplicationLoader {
  override def load(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new AccountApplication(context) with LagomDevModeComponents

  override def describeService: Option[Descriptor] = Some(readDescriptor[AccountService])
}

```
