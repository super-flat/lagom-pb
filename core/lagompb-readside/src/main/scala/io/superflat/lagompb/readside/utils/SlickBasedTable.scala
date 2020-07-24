package io.superflat.lagompb.readside.utils

import slick.jdbc.PostgresProfile.api._

import scala.reflect.ClassTag

/**
 * SlickBasedTable helps create slick schema definitions.
 *
 * @param tag tag name
 * @param schemaName the database schema name
 * @param tableName the database table name
 * @tparam E the entity case class
 */
abstract class SlickBasedTable[E: ClassTag](tag: Tag, schemaName: Option[String], tableName: String)
    extends Table[E](tag, schemaName, tableName) {}
