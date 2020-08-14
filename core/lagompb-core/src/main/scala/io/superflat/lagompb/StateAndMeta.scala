package io.superflat.lagompb

import io.superflat.lagompb.protobuf.v1.core.MetaData

/**
 * StateAndMeta wraps the actual aggregate state with some meta data about the state
 *
 * @param state the aggregate state
 * @param metaData the state meta
 * @tparam A state scala type
 */
final case class StateAndMeta[A](state: A, metaData: MetaData)
