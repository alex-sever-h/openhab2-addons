/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homeautologic.internal;

import static org.openhab.binding.homeautologic.HomeAutoLogicBindingConstants.*;

import java.util.Collection;
import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.homeautologic.handler.HomeAutoLogicSensorHandler;
import org.openhab.binding.homeautologic.handler.HomeAutoLogicSwitchHandler;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link HomeAutoLogicHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alexandru-Sever Horin - Initial contribution
 */
public class HomeAutoLogicHandlerFactory extends BaseThingHandlerFactory {

    static int minWindowSize = 10;

    static private ItemRegistry itemRegistry;

    public void setItemRegistry(ItemRegistry itemRegistry_) {
        itemRegistry = itemRegistry_;
        Collection<Item> ci = itemRegistry.getAll();

        System.out.println(ci);
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        itemRegistry = null;
    }

    static public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_SWITCH,
            THING_TYPE_SENSOR);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        boolean value = SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
        System.out.println(thingTypeUID + " " + value);
        return value;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        System.out.println("---->>>" + thingTypeUID);

        if (thingTypeUID.equals(THING_TYPE_SENSOR)) {
            HomeAutoLogicSensorHandler sensorHandler = new HomeAutoLogicSensorHandler(thing);
            return sensorHandler;
        } else if (thingTypeUID.equals(THING_TYPE_SWITCH)) {
            HomeAutoLogicSwitchHandler homeAutoLogicSwitchHandler = new HomeAutoLogicSwitchHandler(thing);
            homeAutoLogicSwitchHandler.setItemRegistry(itemRegistry);
            return homeAutoLogicSwitchHandler;
        }

        return null;
    }
}
