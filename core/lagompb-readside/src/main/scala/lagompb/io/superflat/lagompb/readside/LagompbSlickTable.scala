package io.superflat.lagompb.readside

import slick.jdbc.PostgresProfile.api._

import scala.reflect.ClassTag

/**
 * LagompbSlickTable will be implemented by any slick schema definition
 *
 * @param tag tag name
 * @param schemaName the database schema name
 * @param tableName the database table name
 * @tparam E the entity case class
 */
abstract class LagompbSlickTable[E: ClassTag](tag: Tag, schemaName: Option[String], tableName: String)
    extends Table[E](tag, schemaName, tableName) {}
