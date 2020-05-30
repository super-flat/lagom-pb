package lagompb.readside.utils

import lagompb.readside.LagompbSlickTable
import slick.dbio.Effect
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slickProfile.api._

import scala.concurrent.Future

/**
 * Repository interface that will be implemented by any slick postgres readSide
 */
abstract class LagompbSlickPgRepository[T <: LagompbSlickTable[E], E](tableQuery: TableQuery[T], database: Database) {

  // It will be used to interact with the database
  val query: TableQuery[T] = tableQuery

  /**
   * Creates a new record in the database
   *
   * @param model the data model to persist
   * @return the created data model
   */
  def save(model: E): Future[E]

  /**
   * Retrieves a record from the database with the corresponding id.
   *
   * @param entityId the record unique identifier
   * @return the record fetched or None if it was not found
   */
  def read(entityId: String): Future[Option[E]]

  /**
   * Retrieves all records from the database
   *
   * @return the list of records fetched
   */
  def all(): Future[Seq[E]]

  /**
   * Update the corresponding record in the database.
   *
   * @param model the data model of the record to update
   * @return the updated record or None if it was not found
   */
  def update(model: E): Future[Option[E]]

  /**
   * Deletes a record from the database with the corresponding id.
   *
   * @param entityId the record unique identifier
   * @return the deleted record or None if it was not found
   */
  def delete(entityId: String): Future[Option[E]]

  /**
   * Creates the database table if the table does not exist.
   *
   * @return
   */
  def createSchema(): PostgresProfile.api.DBIOAction[Unit, PostgresProfile.api.NoStream, Effect.Schema]
}
