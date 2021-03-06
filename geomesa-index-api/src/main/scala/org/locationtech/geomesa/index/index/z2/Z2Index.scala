/***********************************************************************
 * Copyright (c) 2013-2017 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.index.index.z2

import org.geotools.factory.Hints
import org.locationtech.geomesa.index.api.{FilterStrategy, WrappedFeature}
import org.locationtech.geomesa.index.conf.QueryHints._
import org.locationtech.geomesa.index.geotools.GeoMesaDataStore
import org.locationtech.geomesa.index.index.BaseFeatureIndex
import org.locationtech.geomesa.index.strategies.SpatialFilterStrategy
import org.locationtech.geomesa.utils.geotools.GeometryUtils
import org.opengis.feature.simple.SimpleFeatureType

trait Z2Index[DS <: GeoMesaDataStore[DS, F, W], F <: WrappedFeature, W, R, C]
    extends BaseFeatureIndex[DS, F, W, R, C, Z2IndexValues] with SpatialFilterStrategy[DS, F, W] {

  override val name: String = "z2"

  override protected val keySpace: Z2IndexKeySpace = Z2IndexKeySpace

  override protected def useFullFilter(sft: SimpleFeatureType,
                                       ds: DS,
                                       filter: FilterStrategy[DS, F, W],
                                       indexValues: Option[Z2IndexValues],
                                       hints: Hints): Boolean = {
    // if the user has requested strict bounding boxes, we apply the full filter
    // if the spatial predicate is rectangular (e.g. a bbox), the index is fine enough that we
    // don't need to apply the filter on top of it. this may cause some minor errors at extremely
    // fine resolutions, but the performance is worth it
    // if we have a complicated geometry predicate, we need to pass it through to be evaluated
    val looseBBox = Option(hints.get(LOOSE_BBOX)).map(Boolean.unbox).getOrElse(ds.config.looseBBox)
    lazy val simpleGeoms = indexValues.toSeq.flatMap(_.geometries.values).forall(GeometryUtils.isRectangular)

    !looseBBox || !simpleGeoms
  }
}
