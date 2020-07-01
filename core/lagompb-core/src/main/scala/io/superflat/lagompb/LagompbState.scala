package io.superflat.lagompb

import io.superflat.lagompb.protobuf.core.MetaData

/**
 * LagompbState wraps the actual aggregate state with some meta data about the state
 *
 * @param state the aggregate state
 * @param metaData the state meta
 * @tparam TState
 */
final case class LagompbState[TState](state: TState, metaData: MetaData)
