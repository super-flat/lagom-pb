package lagompb.readside.utils.rdbms

import slick.dbio.Effect
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slickProfile.api._

import scala.concurrent.Future

/**
  * Repository interface that will be implemented by any slick postgres readSide
  */
trait LagompbSlickPgRepository[T] {

  /**
    * Creates a new record in the database
    *
    * @param model    the data model to persist
    * @param database the implicit database connection
    * @return the created data model
    */
  def save(model: T)(implicit database: Database): Future[T]

  /**
    * Retrieves a record from the database with the corresponding id.
    *
    * @param entityId the record unique identifier
    * @param database the implicit database connection
    * @return the record fetched or None if it was not found
    */
  def read(entityId: String)(implicit database: Database): Future[Option[T]]

  /**
    * Retrieves all records from the database
    *
    * @return the list of records fetched
    */
  def all()(implicit database: Database): Future[Seq[T]]

  /**
    * Update the corresponding record in the database.
    *
    * @param model the data model of the record to update
    * @param database the implicit database connection
    * @return the updated record or None if it was not found
    */
  def update(model: T)(implicit database: Database): Future[Option[T]]

  /**
    * Deletes a record from the database with the corresponding id.
    *
    * @param entityId the record unique identifier
    * @param database the implicit database connection
    * @return the deleted record or None if it was not found
    */
  def delete(entityId: String)(implicit database: Database): Future[Option[T]]

  /**
    * Creates the database table if the table does not exist.
    *
    * @return
    */
  def createSchema()(
    implicit database: Database
  ): PostgresProfile.api.DBIOAction[Unit,
                                    PostgresProfile.api.NoStream,
                                    Effect.Schema]
}
