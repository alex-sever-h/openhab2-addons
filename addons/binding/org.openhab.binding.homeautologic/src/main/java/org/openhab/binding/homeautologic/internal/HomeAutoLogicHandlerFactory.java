/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeautologic.internal;

import static org.openhab.binding.homeautologic.HomeAutoLogicBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.homeautologic.handler.HomeAutoLogicSensorHandler;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link HomeAutoLogicHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alexandru-Sever Horin - Initial contribution
 */
public class HomeAutoLogicHandlerFactory extends BaseThingHandlerFactory {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_SWITCH,
            THING_TYPE_SENSOR);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_SENSOR)) {
            return new HomeAutoLogicSensorHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_SWITCH)) {
            return new HomeAutoLogicSensorHandler(thing);
        }

        return null;
    }
}
