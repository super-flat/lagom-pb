package lagompb.readside.utils.rdbms

import java.sql.Connection

import scala.concurrent.Future

/**
 * Repository interface that will be implemented by any jdbc readSide
 *
 * @tparam T the data model type
 */
trait LagompbJdbcRepository[T] {

  /**
   * Creates a new record in the database
   *
   * @param model    the data model to persist
   * @param connection the implicit database connection
   * @return the created data model
   */
  def save(model: T)(implicit connection: Connection): Future[T]

  /**
   * Retrieves a record from the database with the corresponding id.
   *
   * @param entityId the record unique identifier
   * @param connection the implicit database connection
   * @return the record fetched or None if it was not found
   */
  def read(entityId: String)(implicit connection: Connection): Future[Option[T]]

  /**
   * Retrieves all records from the database
   *
   * @return the list of records fetched
   */
  def all()(implicit connection: Connection): Future[Seq[T]]

  /**
   * Update the corresponding record in the database.
   *
   * @param model the data model of the record to update
   * @param connection the implicit database connection
   * @return the updated record or None if it was not found
   */
  def update(model: T)(implicit connection: Connection): Future[Option[T]]

  /**
   * Deletes a record from the database with the corresponding id.
   *
   * @param entityId the record unique identifier
   * @param connection the implicit database connection
   * @return the deleted record or None if it was not found
   */
  def delete(entityId: String)(implicit connection: Connection): Future[Option[T]]

}
